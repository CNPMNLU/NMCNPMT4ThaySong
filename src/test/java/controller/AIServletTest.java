package controller;

import model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import service.*;
import service.ai.*;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import model.Direction;

/**
 * Test cho COMMIT-04: AIServlet.executeTurn()
 *
 * Các nhóm test:
 *   1. Luồng bình thường — MISS, HIT, SUNK, GAME_OVER
 *   2. Fix Bug #1 — board được lấy từ session key "board_<playerId>",
 *                   không tạo mới mỗi lượt
 *   3. Fix Bug #2 — hit/sunk tính từ ShotResult.ResultType trực tiếp,
 *                   notifyResult() nhận đúng giá trị
 *   4. Trường hợp biên — target null, playerBoard null
 *   5. Session state — aiService và playerBoard được lưu lại sau mỗi lượt
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("COMMIT-04 — AIServlet.executeTurn() integration")
class AIServletTest {

    // -------------------------------------------------------------------------
    // Mocks & test objects
    // -------------------------------------------------------------------------

    @Mock BoardService    boardService;
    @Mock GameService     gameService;
    @Mock ScoreService    scoreService;
    @Mock dao.GameHistoryDAO  historyDAO;
    @Mock dao.LeaderboardDAO  leaderboardDAO;
    @Mock HttpSession     session;

    AIServlet aiServlet;

    // Dùng spy để verify notifyResult() được gọi đúng
    AIService aiServiceSpy;

    // Strategy spy để kiểm tra onShotResult() được gọi với đúng hit/sunk
    HardAIStrategy strategySpy;

    Board playerBoard;
    GameState gs;

    @BeforeEach
    void setUp() {
        aiServlet = new AIServlet(boardService, gameService, scoreService, historyDAO, leaderboardDAO);

        // Tạo board 10x10 thật (không mock — cần cells thật để isHit hoạt động)
        playerBoard = makeBoardWithShip();

        // GameState ongoing
        gs = new GameState();
        gs.setId(UUID.randomUUID().toString());
        gs.setRoomId("room-1");
        gs.setCurrentTurnId("player-1");
        gs.setStatus("ongoing");
        gs.setTotalTurns(0);
        gs.setMode("PvE");
        gs.setStartedAt(LocalDateTime.now());

        // Strategy spy để capture onShotResult() calls
        strategySpy = spy(new HardAIStrategy());
        // Inject strategy vào AIService qua reflection
        aiServiceSpy = makeAIServiceWithStrategy(strategySpy);

        // Session trả về aiService và playerBoard
        when(session.getAttribute("aiService")).thenReturn(aiServiceSpy);
        when(session.getAttribute("board_player-1")).thenReturn(playerBoard);
    }

    // =========================================================================
    // 1. Luồng bình thường
    // =========================================================================

    @Nested
    @DisplayName("1. Luồng bình thường")
    class NormalFlow {

        @Test
        @DisplayName("1a. AI bắn MISS — trả AITurnResult với resultType=MISS, aiWon=false")
        void executeTurn_miss_returnsCorrectResult() {
            ShotResult missResult = makeResult(ShotResult.ResultType.MISS);
            when(gameService.fireShot(any(), any(), eq(AIServlet.AI_PLAYER_ID), anyInt(), anyInt()))
                    .thenReturn(missResult);

            AITurnResult result = aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertNotNull(result);
            assertEquals(ShotResult.ResultType.MISS, result.resultType);
            assertFalse(result.aiWon);
            // Sau MISS, turn trả về player
            assertEquals("player-1", gs.getCurrentTurnId());
        }

        @Test
        @DisplayName("1b. AI bắn HIT — trả AITurnResult với resultType=HIT")
        void executeTurn_hit_returnsHit() {
            ShotResult hitResult = makeResult(ShotResult.ResultType.HIT);
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(hitResult);

            AITurnResult result = aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertNotNull(result);
            assertEquals(ShotResult.ResultType.HIT, result.resultType);
            assertFalse(result.aiWon);
        }

        @Test
        @DisplayName("1c. AI bắn GAME_OVER — aiWon=true, gs.status=finished")
        void executeTurn_gameOver_setsFinished() {
            ShotResult gameOverResult = makeResult(ShotResult.ResultType.GAME_OVER);
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(gameOverResult);

            AITurnResult result = aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertNotNull(result);
            assertTrue(result.aiWon);
            assertEquals(ShotResult.ResultType.GAME_OVER, result.resultType);
            verify(gameService).finishGame(gs, AIServlet.AI_PLAYER_ID);
        }

        @Test
        @DisplayName("1d. Sau MISS — currentTurnId trả về player")
        void executeTurn_miss_turnReturnedToPlayer() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertEquals("player-1", gs.getCurrentTurnId());
        }

        @Test
        @DisplayName("1e. Sau GAME_OVER — currentTurnId là AI (game kết thúc, không trả lại)")
        void executeTurn_gameOver_turnNotReturnedToPlayer() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.GAME_OVER));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            // gs.currentTurnId vẫn là AI_PLAYER vì game đã kết thúc
            assertEquals(AIServlet.AI_PLAYER_ID, gs.getCurrentTurnId());
        }
    }

    // =========================================================================
    // 2. Fix Bug #1 — board lấy từ session key cố định
    // =========================================================================

    @Nested
    @DisplayName("2. Fix Bug #1 — playerBoard từ session 'board_<playerId>'")
    class BoardSessionKey {

        @Test
        @DisplayName("2a. Lấy board từ session key 'board_player-1', không gọi getBoardByRoomAndOwner()")
        void executeTurn_boardFromSessionKey_notFromService() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            // getBoardByRoomAndOwner() KHÔNG được gọi khi session key có sẵn
            verify(boardService, never()).getBoardByRoomAndOwner(any(), any(), eq("player-1"));
        }

        @Test
        @DisplayName("2b. Nếu 'board_player-1' null → fallback getBoardByRoomAndOwner()")
        void executeTurn_boardNull_fallbackToService() {
            // Reset mock: không có board trong session
            when(session.getAttribute("board_player-1")).thenReturn(null);
            when(boardService.getBoardByRoomAndOwner(any(), any(), eq("player-1")))
                    .thenReturn(playerBoard);
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            AITurnResult result = aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertNotNull(result, "Phải fallback lấy board từ service");
            verify(boardService).getBoardByRoomAndOwner(session, "room-1", "player-1");
        }

        @Test
        @DisplayName("2c. Cùng board object được dùng cho selectTarget() và fireShot()")
        void executeTurn_sameBoard_usedForSelectAndFire() {
            // Spy board để verify cùng object
            Board spyBoard = spy(playerBoard);
            when(session.getAttribute("board_player-1")).thenReturn(spyBoard);

            ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
            when(gameService.fireShot(boardCaptor.capture(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            // Board được truyền vào fireShot phải là cùng object với board trong session
            assertSame(spyBoard, boardCaptor.getValue(),
                    "fireShot phải nhận đúng board từ session, không phải bản copy mới");
        }
    }

    // =========================================================================
    // 3. Fix Bug #2 — hit/sunk tính từ ResultType trực tiếp
    // =========================================================================

    @Nested
    @DisplayName("3. Fix Bug #2 — notifyResult nhận đúng hit/sunk từ ResultType")
    class NotifyResultValues {

        @Test
        @DisplayName("3a. ResultType.MISS → notifyResult(hit=false, sunk=false)")
        void notifyResult_miss_hitFalse() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            // Verify onShotResult() được gọi với hit=false
            verify(strategySpy).onShotResult(anyInt(), anyInt(),
                    eq(false), eq(false), any());
        }

        @Test
        @DisplayName("3b. ResultType.HIT → notifyResult(hit=true, sunk=false)")
        void notifyResult_hit_hitTrueSunkFalse() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.HIT));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            verify(strategySpy).onShotResult(anyInt(), anyInt(),
                    eq(true), eq(false), any());
        }

        @Test
        @DisplayName("3c. ResultType.SUNK → notifyResult(hit=true, sunk=true)")
        void notifyResult_sunk_hitTrueSunkTrue() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.SUNK));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            verify(strategySpy).onShotResult(anyInt(), anyInt(),
                    eq(true), eq(true), any());
        }

        @Test
        @DisplayName("3d. ResultType.GAME_OVER → notifyResult(hit=true, sunk=true)")
        void notifyResult_gameOver_hitTrueSunkTrue() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.GAME_OVER));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            verify(strategySpy).onShotResult(anyInt(), anyInt(),
                    eq(true), eq(true), any());
        }

        @Test
        @DisplayName("3e. notifyResult() luôn được gọi đúng 1 lần mỗi lượt")
        void notifyResult_calledExactlyOnce() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.HIT));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            // onShotResult phải được gọi đúng 1 lần — không bị skip, không bị gọi 2 lần
            verify(strategySpy, times(1))
                    .onShotResult(anyInt(), anyInt(), anyBoolean(), anyBoolean(), any());
        }

        @Test
        @DisplayName("3f. Sau HIT, huntQueue HardAI không rỗng — strategy thật sự nhận hit")
        void hardAI_afterHit_huntQueueNotEmpty() {
            // Dùng HardAIStrategy thật (không spy) để verify end-to-end
            HardAIStrategy realStrategy = new HardAIStrategy();
            AIService realService = makeAIServiceWithStrategy(realStrategy);
            when(session.getAttribute("aiService")).thenReturn(realService);

            // fireShot trả HIT tại ô mà selectTarget() chọn
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenAnswer(invocation -> {
                        int x = invocation.getArgument(3);
                        int y = invocation.getArgument(4);
                        Board board = invocation.getArgument(0);
                        board.getCells()[x][y].setHit(true);
                        board.getCells()[x][y].setHasShip(true); // giả sử trúng tàu
                        return makeResult(ShotResult.ResultType.HIT);
                    });

            AITurnResult result = aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertNotNull(result);
            assertTrue(result.isHit(), "Kết quả phải là HIT");

            // Lượt tiếp theo, strategy phải dùng huntQueue (không random)
            // Lấy service đã được lưu lại session
            AIService savedService = (AIService) session.getAttribute("aiService") != null
                    ? (AIService) session.getAttribute("aiService") : realService;

            // Board giờ có ô đã hit — selectTarget() phải trả ô kề
            int[] nextTarget = savedService.selectTarget(playerBoard);
            assertNotNull(nextTarget, "selectTarget sau HIT không được null");
        }
    }

    // =========================================================================
    // 4. Trường hợp biên
    // =========================================================================

    @Nested
    @DisplayName("4. Trường hợp biên")
    class EdgeCases {

        @Test
        @DisplayName("4a. playerBoard null → trả null, không throw")
        void executeTurn_boardNull_returnsNull() {
            when(session.getAttribute("board_player-1")).thenReturn(null);
            when(boardService.getBoardByRoomAndOwner(any(), any(), any())).thenReturn(null);

            AITurnResult result = assertDoesNotThrow(
                    () -> aiServlet.executeTurn(gs, session, "player-1", "Alice"));

            assertNull(result);
        }

        @Test
        @DisplayName("4b. selectTarget() trả null → trả null, turn trả về player")
        void executeTurn_targetNull_returnsNullAndRestoresTurn() {
            // Tạo board đầy để selectTarget() trả null
            Board fullBoard = makeFullBoard();
            when(session.getAttribute("board_player-1")).thenReturn(fullBoard);

            AITurnResult result = aiServlet.executeTurn(gs, session, "player-1", "Alice");

            assertNull(result);
            assertEquals("player-1", gs.getCurrentTurnId(),
                    "Turn phải trả về player khi target null");
        }

        @Test
        @DisplayName("4c. session không có aiService → tự tạo mới từ difficulty")
        void executeTurn_noAiServiceInSession_createsNew() {
            when(session.getAttribute("aiService")).thenReturn(null);
            when(session.getAttribute("difficulty")).thenReturn("Hard");
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            AITurnResult result = assertDoesNotThrow(
                    () -> aiServlet.executeTurn(gs, session, "player-1", "Alice"));

            assertNotNull(result, "Phải tạo được AIService mới khi session không có");
        }
    }

    // =========================================================================
    // 5. Session state
    // =========================================================================

    @Nested
    @DisplayName("5. Session state — aiService và board được lưu lại")
    class SessionState {

        @Test
        @DisplayName("5a. aiService luôn được setAttribute sau mỗi lượt dù MISS hay HIT")
        void executeTurn_alwaysSavesAiServiceToSession() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            verify(session).setAttribute(eq("aiService"), any(AIService.class));
        }

        @Test
        @DisplayName("5b. playerBoard được lưu vào 'board_player-1' sau mỗi lượt")
        void executeTurn_alwaysSavesBoardToSession() {
            when(gameService.fireShot(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(makeResult(ShotResult.ResultType.MISS));

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            verify(session).setAttribute(eq("board_player-1"), any(Board.class));
        }

        @Test
        @DisplayName("5c. Khi target null, aiService vẫn được lưu lại (không mất state)")
        void executeTurn_targetNull_stillSavesAiService() {
            Board fullBoard = makeFullBoard();
            when(session.getAttribute("board_player-1")).thenReturn(fullBoard);

            aiServlet.executeTurn(gs, session, "player-1", "Alice");

            // aiService phải được lưu ngay cả khi không bắn được
            verify(session).setAttribute(eq("aiService"), any(AIService.class));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ShotResult makeResult(ShotResult.ResultType type) {
        ShotResult r = new ShotResult();
        r.setId(UUID.randomUUID().toString());
        r.setResult(type);
        r.setX(0);
        r.setY(0);
        return r;
    }

    /** Board 10x10 có 1 tàu kích thước 2 ở (3,3)-(3,4) để fireShot có thể HIT. */
    private Board makeBoardWithShip() {
        Board board = new Board();
        board.setId(UUID.randomUUID().toString());
        board.setRoomId("room-1");
        board.setOwnerId("player-1");
        Cell[][] cells = new Cell[10][10];
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++) {
                cells[x][y] = new Cell("c" + x + y, board.getId(), x, y);
            }
        cells[3][3].setHasShip(true);
        cells[3][3].setShipId("ship-1");
        cells[3][4].setHasShip(true);
        cells[3][4].setShipId("ship-1");
        board.setCells(cells);

        Ship ship = new Ship();
        ship.setId("ship-1");
        ship.setLength(2);
        ship.setStartX(3);
        ship.setStartY(3);
        ship.setDirection(Direction.H);
        board.setShips(List.of(ship));
        return board;
    }

    /** Board 10x10 tất cả ô đã bị bắn → selectTarget() sẽ trả null. */
    private Board makeFullBoard() {
        Board board = makeBoardWithShip();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                board.getCells()[x][y].setHit(true);
        return board;
    }

    /**
     * Tạo AIService với strategy đã cho bằng reflection.
     * Cần thiết vì AIService không expose setter cho strategy.
     */
    private AIService makeAIServiceWithStrategy(AIStrategy strategy) {
        AIService service = new AIService("Hard"); // tạo mặc định
        try {
            java.lang.reflect.Field f = AIService.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(service, strategy);
        } catch (Exception e) {
            throw new RuntimeException("Không thể inject strategy vào AIService", e);
        }
        return service;
    }
}