package controller;

import com.google.gson.*;
import dao.*;
import model.*;
import service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.*;
import java.util.*;

@WebServlet("/game")
public class GameServlet extends HttpServlet {
    private final BoardService boardService = new BoardService();
    private final GameService gameService = new GameService();
    private final ScoreService scoreService = new ScoreService();
    private final GameHistoryDAO historyDAO = new GameHistoryDAO();
    private final LeaderboardDAO leaderboardDAO = new LeaderboardDAO();

    /**
     * AIController được khởi tạo một lần cùng servlet.
     * Dependencies inject thủ công — nhất quán với cách các service khác được tạo.
     */
    private final AIServlet aiController = new AIServlet(
            boardService, gameService, scoreService, historyDAO, leaderboardDAO
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String roomId = (String) session.getAttribute("roomId");
        if (roomId == null) {
            resp.sendRedirect(req.getContextPath() + "/setup");
            return;
        }

        try {
            String playerId = (String) session.getAttribute("playerId");
            String mode = (String) session.getAttribute("mode");
            String difficulty = (String) session.getAttribute("difficulty");
            Board playerBoard = boardService.getBoardByRoomAndOwner(session, roomId, playerId);

            GameState gs = (GameState) session.getAttribute("gameState");
            if (gs == null) {
                gs = gameService.createGameState(roomId, playerId);
                gs.setMode(mode);
                gs.setDifficulty(difficulty);
                gs.setStartedAt(LocalDateTime.now());
                session.setAttribute("gameState", gs);
            }

            if ("PvE".equals(mode)) {
                Board aiBoard = (Board) session.getAttribute("aiBoard");
                if (aiBoard == null) {
                    aiBoard = boardService.createBoard(UUID.randomUUID().toString(), roomId, "AI_PLAYER");
                    boardService.autoPlace(aiBoard);
                    session.setAttribute("aiBoard", aiBoard);
                }
                AIService aiService = (AIService) session.getAttribute("aiService");
                if (aiService == null) {
                    aiService = new AIService(difficulty != null ? difficulty : "Easy");
                    session.setAttribute("aiService", aiService);
                }
            }

            req.setAttribute("gameState", gs);
            req.setAttribute("playerBoard", playerBoard);
            req.getRequestDispatcher("/game.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/setup");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("playerId") == null) {
            out.print("{\"error\":\"not_logged_in\"}");
            return;
        }

        String playerId = (String) session.getAttribute("playerId");
        String playerName = (String) session.getAttribute("playerName");   // tên user đăng nhập
        String p2Name = (String) session.getAttribute("player2Name");  // tên nhập tay PvP
        String mode = (String) session.getAttribute("mode");
        GameState gs = (GameState) session.getAttribute("gameState");
        Board playerBoard = boardService.getBoardByRoomAndOwner(session, (gs != null ? gs.getRoomId() : ""), playerId);
        Board aiBoard = (Board) session.getAttribute("aiBoard");

        if (playerName == null) playerName = "Player 1";
        if (p2Name == null) p2Name = "Người chơi 2";

        if (gs == null || playerBoard == null) {
            out.print("{\"error\":\"no_game\"}");
            return;
        }
        if (!"ongoing".equals(gs.getStatus())) {
            out.print("{\"error\":\"game_over\"}");
            return;
        }

        try {
            String action = req.getParameter("action");

            // PvP offline save — JS gọi sau khi game kết thúc phía client
            if ("pvp_save".equals(action)) {
                handlePvpSave(req, session, playerId, out);
                return;
            }

            // Timeout skip
            if ("skip".equals(action)) {
                JsonObject response = new JsonObject();
                response.addProperty("skipped", true);
                if ("PvE".equals(mode)) {
                    appendAIMove(response, gs, session, playerId, playerName);
                }
                out.print(response.toString());
                return;
            }

            if (!gs.getCurrentTurnId().equals(playerId)) {
                out.print("{\"error\":\"not_your_turn\"}");
                return;
            }

            int x = parseInt(req.getParameter("x"), -1);
            int y = parseInt(req.getParameter("y"), -1);
            if (x < 0 || x > 9 || y < 0 || y > 9) {
                out.print("{\"error\":\"invalid_coordinates\"}");
                return;
            }
            Board targetBoard = "PvE".equals(mode) ? aiBoard : playerBoard;
            ShotResult result = gameService.fireShot(targetBoard, gs, playerId, x, y);

            JsonObject response = new JsonObject();
            response.addProperty("result", result.getResult().name());
            response.addProperty("x", x);
            response.addProperty("y", y);

            if (result.getResult() == ShotResult.ResultType.GAME_OVER) {
                gs.setStatus("finished");
                gs.setWinnerId(playerId);

                int duration = (int) (gs.getStartedAt() != null
                        ? Duration.between(gs.getStartedAt(), LocalDateTime.now()).getSeconds() : 0);
                int score = scoreService.calculateScore(gs.getTotalTurns(), duration);

                String p2Display = "PvE".equals(mode) ? "AI" : p2Name;

                saveGameRecord(gs, playerId, null, playerName, p2Display, playerName, mode, score, 0);
                saveLeaderboard(playerId, true, score);

                response.addProperty("score", score);
                response.addProperty("winner", playerName);

                session.setAttribute("lastScore", score);
                session.setAttribute("gameWinner", playerName);

                session.removeAttribute("roomId");
                session.removeAttribute("gameState");
                if ("PvE".equals(mode)) {
                    session.removeAttribute("aiBoard");
                    session.removeAttribute("aiService");
                }
            }
            else if ("PvE".equals(mode)) {
                JsonObject aiMove = doAITurn(gs, playerBoard, aiBoard, aiService,
                                              playerId, playerName, session, response);
                if (aiMove != null) response.add("aiMove", aiMove);
            }
            session.setAttribute("gameState", gs);
            out.print(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Gọi AIController.executeTurn() và gắn kết quả vào JSON response.
     *
     * Đây là nơi duy nhất trong GameServlet biết về AIController —
     * tất cả logic AI đã nằm trong AIController.
     *
     * aiMove object có thêm field "gameOver": true khi AI thắng.
     */
    private void appendAIMove(JsonObject response,
                              GameState gs,
                              HttpSession session,
                              String playerId,
                              String playerName) {
        AITurnResult aiResult = aiController.executeTurn(gs, session, playerId, playerName);


        if (aiResult == null) {
            //target null — báo frontend biết AI không bắn được
            response.addProperty("aiError", "no_target");
            return;
        }

        JsonObject aiMove = new JsonObject();
        aiMove.addProperty("x",      aiResult.x);
        aiMove.addProperty("y",      aiResult.y);
        aiMove.addProperty("result", aiResult.resultType.name());

        if (aiResult.aiWon) {
            //Flag rõ ràng — frontend không phải parse result string
            aiMove.addProperty("gameOver", true);
            aiMove.addProperty("aiWon", true);
            response.addProperty("aiWon", true);
        }

        response.add("aiMove", aiMove);
        session.setAttribute("gameState", gs);
    }

    private void handlePvpSave(HttpServletRequest req,
                               HttpSession session,
                               String playerId,
                               PrintWriter out) {
        try {
            String winnerName = req.getParameter("winner");
//            String loserName  = req.getParameter("loser");
            int shots         = parseInt(req.getParameter("shots"), 0);
            int duration      = parseInt(req.getParameter("duration"), 0);

            String p1Name = (String) session.getAttribute("playerName");
            String p2Name = (String) session.getAttribute("player2Name");
            if (p1Name == null) p1Name = "Player 1";
            if (p2Name == null) p2Name = "Người chơi 2";
            if (winnerName == null) winnerName = p1Name;

            GameRecord record = new GameRecord();
            record.setId(UUID.randomUUID().toString());
            String roomId = (String) session.getAttribute("roomId");
            record.setRoomId(roomId != null ? roomId : UUID.randomUUID().toString());
            record.setPlayer1Id(playerId);
            record.setPlayer2Id(null);
            record.setPlayer1Name(p1Name);
            record.setPlayer2Name(p2Name);
            record.setWinnerName(winnerName);
            record.setMode("PvP");
            record.setPlayer1Score(p1Name.equals(winnerName) ? 1 : 0);
            record.setPlayer2Score(p2Name.equals(winnerName) ? 1 : 0);
            record.setTotalShots(shots);
            record.setDurationSeconds(duration);

            historyDAO.insert(record);
            boolean p1Won = p1Name.equals(winnerName);
            saveLeaderboard(playerId, p1Won, p1Won ? 1 : 0);
            //Dùng double quotes thay vì single quotes
            out.print("{\"ok\":true}");
        } catch (Exception e) {
            System.err.println("[GameServlet] pvp_save FAILED: " + e.getMessage());
            out.print("{\"error\":\"save_failed\"}");
        }
    }

    /**
     * @param p1Id       UUID của người chơi đăng nhập (bắt buộc có trong users)
     * @param p2Id       UUID player2 nếu cả 2 có tài khoản, null nếu offline/AI
     * @param p1Name     Tên hiển thị player1
     * @param p2Name     Tên hiển thị player2 ("AI" hoặc tên nhập tay)
     * @param winnerName Tên hiển thị người thắng
     */
    private void saveGameRecord(GameState gs, String p1Id, String p2Id,
                                String p1Name, String p2Name, String winnerName,
                                String mode, int p1Score, int p2Score) {
        try {
            GameRecord record = new GameRecord();
            record.setId(UUID.randomUUID().toString());
            record.setRoomId(gs.getRoomId() != null ? gs.getRoomId() : UUID.randomUUID().toString());
            record.setPlayer1Id(p1Id);
            record.setPlayer2Id(p2Id);
            record.setPlayer1Name(p1Name);
            record.setPlayer2Name(p2Name);
            record.setWinnerName(winnerName);
            record.setMode(mode != null ? mode : "PvE");
            record.setPlayer1Score(p1Score);
            record.setPlayer2Score(p2Score);
            record.setTotalShots(gs.getTotalTurns());
            long dur = gs.getStartedAt() != null
                    ? Duration.between(gs.getStartedAt(), LocalDateTime.now()).getSeconds() : 0;
            record.setDurationSeconds((int) dur);
            historyDAO.insert(record);
            System.out.println("[GameServlet] saveGameRecord OK: mode=" + mode
                    + " winner=" + winnerName + " p1=" + p1Name);
        } catch (Exception e) {
            System.err.println("[GameServlet] saveGameRecord FAILED: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void saveLeaderboard(String userId, boolean won, int score) {
        try {
            leaderboardDAO.upsert(userId, won, score);
        } catch (Exception e) {
            System.err.println("saveLeaderboard failed: " + e.getMessage());
        }
    }

    private int parseInt(String s, int defaultVal) {
        if (s == null) return defaultVal;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}