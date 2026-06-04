package controller;

import dao.GameHistoryDAO;
import dao.LeaderboardDAO;
import jakarta.servlet.http.HttpSession;
import model.*;
import service.AIService;
import service.BoardService;
import service.GameService;
import service.ScoreService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Điều phối toàn bộ lượt bắn của AI trong chế độ PvE.
 *
 * Trách nhiệm (Single Responsibility):
 *   - Lấy AIService từ session (hoặc khởi tạo nếu chưa có)
 *   - Gọi selectTarget → fireShot → notifyResult
 *   - Xử lý trường hợp AI thắng: finish game, lưu record, cập nhật leaderboard
 *   - Trả về AITurnResult để GameServlet serialize ra JSON
 *
 * KHÔNG có trong class này:
 *   - Đọc HttpServletRequest / ghi HttpServletResponse (việc của GameServlet)
 *   - Logic bắn của player (việc của GameServlet)
 *   - Serialize JSON (việc của GameServlet)
 *
 * Các lỗi được sửa so với doAITurn() cũ:
 *   - [Lỗi #1] aiService luôn được lưu lại session sau mỗi lượt
 *   - [Lỗi #2] Không còn downcast — dùng polymorphic AIStrategy (COMMIT-01)
 *   - [Lỗi #3] Guard ALREADY_HIT: không gọi notifyResult khi bắn trùng ô
 *   - [COMMIT-11] target == null được xử lý tường minh, không silent fail
 *   - [COMMIT-12] aiWon flag được set rõ ràng trong AITurnResult
 */
public class AIServlet {

    private static final Logger log = Logger.getLogger(AIServlet.class.getName());

    /** ID cố định đại diện cho AI trong session và GameState. */
    public static final String AI_PLAYER_ID   = "AI_PLAYER";
    public static final String AI_PLAYER_NAME = "AI";

    // -------------------------------------------------------------------------
    // Dependencies — inject thủ công (không dùng framework DI)
    // -------------------------------------------------------------------------

    private final BoardService    boardService;
    private final GameService     gameService;
    private final ScoreService    scoreService;
    private final GameHistoryDAO  historyDAO;
    private final LeaderboardDAO  leaderboardDAO;

    public AIServlet(BoardService boardService,
                        GameService gameService,
                        ScoreService scoreService,
                        GameHistoryDAO historyDAO,
                        LeaderboardDAO leaderboardDAO) {
        this.boardService  = boardService;
        this.gameService   = gameService;
        this.scoreService  = scoreService;
        this.historyDAO    = historyDAO;
        this.leaderboardDAO = leaderboardDAO;
    }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    /**
     * Thực hiện một lượt bắn đầy đủ của AI.
     *
     * Luồng xử lý:
     *   1. Lấy AIService và playerBoard từ session
     *   2. Set currentTurn → AI_PLAYER
     *   3. selectTarget() — nếu null thì trả về null (board đầy, không bình thường)
     *   4. fireShot() trên playerBoard
     *   5. Guard ALREADY_HIT: bỏ qua notifyResult nếu ô đã bắn
     *   6. notifyResult() cập nhật huntQueue
     *   7. Nếu GAME_OVER: finish game, lưu record, update leaderboard
     *   8. Nếu chưa kết thúc: trả turn lại cho player
     *   9. Lưu aiService và playerBoard vào session
     *  10. Trả về AITurnResult để GameServlet build JSON
     *
     * @param gs         GameState hiện tại (mutable — được cập nhật trực tiếp)
     * @param session    HttpSession của người chơi
     * @param playerId   ID của người chơi (để lấy playerBoard và lưu leaderboard)
     * @param playerName Tên hiển thị người chơi (dùng trong GameRecord)
     * @return AITurnResult chứa tọa độ, resultType, aiWon flag
     *         hoặc null nếu không thể thực hiện lượt (target null hoặc board lỗi)
     */
    public AITurnResult executeTurn(GameState gs,
                                    HttpSession session,
                                    String playerId,
                                    String playerName) {
        // ------------------------------------------------------------------
        // 1. Lấy AIService từ session — tạo mới nếu chưa có
        //    (Lỗi #1 cũ: tạo mới mỗi lần → mất huntQueue. Nay luôn lưu lại cuối method)
        // ------------------------------------------------------------------
        AIService aiService = resolveAIService(session);

        // ------------------------------------------------------------------
        // 2. Lấy playerBoard — AI bắn vào board của người chơi
        // ------------------------------------------------------------------
        Board playerBoard = boardService.getBoardByRoomAndOwner(session, gs.getRoomId(), playerId);
        if (playerBoard == null) {
            log.warning("[AIController] playerBoard null cho playerId=" + playerId);
            return null;
        }

        // ------------------------------------------------------------------
        // 3. Set lượt về AI trước khi bắn
        // ------------------------------------------------------------------
        gs.setCurrentTurnId(AI_PLAYER_ID);

        // ------------------------------------------------------------------
        // 4. Chọn ô — nếu null thì board đầy (trường hợp bất thường)
        //    [COMMIT-11] Xử lý tường minh thay vì silent return null
        // ------------------------------------------------------------------
        int[] target = aiService.selectTarget(playerBoard);
        if (target == null) {
            log.warning("[AIController] selectTarget() trả null — board có thể đầy. "
                    + "roomId=" + gs.getRoomId());
            // Trả turn lại player để game không bị kẹt
            gs.setCurrentTurnId(playerId);
            saveToSession(session, aiService, playerBoard, playerId);
            return null;
        }

        int tx = target[0];
        int ty = target[1];

        // ------------------------------------------------------------------
        // 5. Thực hiện bắn
        // ------------------------------------------------------------------
        ShotResult aiResult = gameService.fireShot(playerBoard, gs, AI_PLAYER_ID, tx, ty);
        ShotResult.ResultType resultType = aiResult.getResult();

        // ------------------------------------------------------------------
        // 6. Guard ALREADY_HIT — không gọi notifyResult khi bắn trùng
        //    [Lỗi #3] Tránh Hard AI enqueue ô đã bắn vào huntQueue
        //    Lưu ý: ALREADY_HIT sẽ được thêm trong COMMIT-03;
        //    hiện tại guard theo isHit() trên cell trước khi fireShot
        //    — cell đã được mark isHit bởi fireShot nên dùng resultType MISS
        //    từ ô đã bắn để nhận biết.
        // ------------------------------------------------------------------
        boolean isAlreadyHit = (resultType == ShotResult.ResultType.MISS
                && playerBoard.getCells()[tx][ty].isHit()
                && !playerBoard.getCells()[tx][ty].isHasShip());
        // Khi COMMIT-03 thêm ALREADY_HIT vào enum, thay điều kiện trên bằng:
        // boolean isAlreadyHit = (resultType == ShotResult.ResultType.ALREADY_HIT);

        if (!isAlreadyHit) {
            // ------------------------------------------------------------------
            // 7. Thông báo kết quả cho strategy — polymorphic, không downcast
            //    [Lỗi #2 đã sửa ở COMMIT-01]
            // ------------------------------------------------------------------
            aiService.notifyResult(tx, ty, aiResult.isHit(), aiResult.isSunk(), playerBoard);
        } else {
            log.warning("[AIController] AI bắn ô đã bắn (" + tx + "," + ty + ") — bỏ qua notifyResult");
        }

        // ------------------------------------------------------------------
        // 8. Xử lý kết thúc game nếu AI thắng
        // ------------------------------------------------------------------
        boolean aiWon = (resultType == ShotResult.ResultType.GAME_OVER);
        int score = 0;

        if (aiWon) {
            gameService.finishGame(gs, AI_PLAYER_ID);
            saveGameRecord(gs, playerId, playerName);
            saveLeaderboard(playerId, false /* player thua */, 0);

            session.setAttribute("gameWinner", AI_PLAYER_NAME);
            session.setAttribute("lastScore", 0);

            log.info("[AIController] AI thắng. roomId=" + gs.getRoomId()
                    + " player=" + playerName + " turns=" + gs.getTotalTurns());
        } else {
            // Trả turn lại cho người chơi
            gs.setCurrentTurnId(playerId);
        }

        // ------------------------------------------------------------------
        // 9. Lưu state vào session — bắt buộc cuối mỗi lượt
        //    [Lỗi #1] aiService phải được lưu lại để huntQueue không mất
        // ------------------------------------------------------------------
        saveToSession(session, aiService, playerBoard, playerId);

        // ------------------------------------------------------------------
        // 10. Trả về kết quả cho GameServlet serialize
        //     [COMMIT-12] aiWon flag rõ ràng trong AITurnResult
        // ------------------------------------------------------------------
        return new AITurnResult(tx, ty, resultType, aiWon, score);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Lấy AIService từ session, tạo mới nếu chưa tồn tại.
     * Difficulty được đọc từ GameState để nhất quán với cài đặt ban đầu.
     */
    private AIService resolveAIService(HttpSession session) {
        AIService aiService = (AIService) session.getAttribute("aiService");
        if (aiService == null) {
            String difficulty = (String) session.getAttribute("difficulty");
            aiService = new AIService(difficulty != null ? difficulty : "Easy");
            log.fine("[AIController] Tạo AIService mới, difficulty=" + difficulty);
        }
        return aiService;
    }

    /**
     * Lưu aiService và playerBoard vào session sau mỗi lượt.
     * Tách thành method riêng để đảm bảo không bỏ sót trong bất kỳ
     * đường dẫn code nào (kể cả khi target null).
     */
    private void saveToSession(HttpSession session,
                               AIService aiService,
                               Board playerBoard,
                               String playerId) {
        session.setAttribute("aiService", aiService);
        if (playerBoard != null) {
            session.setAttribute("board_" + playerId, playerBoard);
        }
    }

    /**
     * Lưu GameRecord khi AI thắng.
     * player1 = người chơi (thua), player2/winner = AI.
     */
    private void saveGameRecord(GameState gs, String playerId, String playerName) {
        try {
            long duration = gs.getStartedAt() != null
                    ? Duration.between(gs.getStartedAt(), LocalDateTime.now()).getSeconds() : 0;

            GameRecord record = new GameRecord();
            record.setId(UUID.randomUUID().toString());
            record.setRoomId(gs.getRoomId() != null ? gs.getRoomId() : UUID.randomUUID().toString());
            record.setPlayer1Id(playerId);
            record.setPlayer2Id(null);               // AI không có UUID trong bảng users
            record.setPlayer1Name(playerName);
            record.setPlayer2Name(AI_PLAYER_NAME);
            record.setWinnerName(AI_PLAYER_NAME);
            record.setMode("PvE");
            record.setPlayer1Score(0);
            record.setPlayer2Score(0);               // AI không tính điểm leaderboard
            record.setTotalShots(gs.getTotalTurns());
            record.setDurationSeconds((int) duration);

            historyDAO.insert(record);
            log.info("[AIController] saveGameRecord OK: playerId=" + playerId);
        } catch (Exception e) {
            log.log(Level.SEVERE, "[AIController] saveGameRecord FAILED: " + e.getMessage(), e);
        }
    }

    private void saveLeaderboard(String userId, boolean won, int score) {
        try {
            leaderboardDAO.upsert(userId, won, score);
        } catch (Exception e) {
            log.log(Level.WARNING, "[AIController] saveLeaderboard FAILED: " + e.getMessage(), e);
        }
    }
}