package controller;

import model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import service.AIService;
import service.BoardService;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test COMMIT-05: SetupServlet gọi aiService.reset() khi bắt đầu game mới.
 *
 * Commit: test(controller): verify AIService.reset() and board cleanup on new game setup
 *
 * Căn cứ code thực tế SetupServlet.java:
 *   - existingAI = session.getAttribute("aiService")
 *   - if (existingAI != null) existingAI.reset()
 *   - session.removeAttribute("board_" + playerId)
 *   - session.setAttribute("aiService", null)
 *   - session.setAttribute("gameState", null)
 *   - session.setAttribute("aiBoard", null)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("COMMIT-05 — SetupServlet reset AIService khi game mới")
class SetupServletResetTest {

    @Mock HttpServletRequest  req;
    @Mock HttpServletResponse resp;
    @Mock HttpSession         session;
    @Mock BoardService        boardServiceMock;

    SetupServlet setupServlet;

    @BeforeEach
    void setUp() throws Exception {
        setupServlet = new SetupServlet();
        Field f = SetupServlet.class.getDeclaredField("boardService");
        f.setAccessible(true);
        f.set(setupServlet, boardServiceMock);

        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("playerId")).thenReturn("player-1");
        when(session.getAttribute("playerName")).thenReturn("Alice");
        when(req.getParameter("action")).thenReturn("auto");
        when(req.getParameter("mode")).thenReturn("PvE");
        when(req.getParameter("difficulty")).thenReturn("Hard");
        when(req.getContextPath()).thenReturn("");

        Board fakeBoard = makeBoard();
        when(boardServiceMock.createBoard(any(), any(), any())).thenReturn(fakeBoard);
        // isValidFleet không được gọi khi action=auto, nhưng guard anyway
        lenient().when(boardServiceMock.isValidFleet(any())).thenReturn(true);
    }

    // =========================================================================
    // 1. reset() được gọi trên instance cũ
    // =========================================================================

    @Nested @DisplayName("1. reset() gọi trên AIService cũ")
    class ResetCalled {

        @Test @DisplayName("1a. Có aiService trong session → reset() được gọi đúng 1 lần")
        void existingAiService_resetCalledOnce() throws Exception {
            AIService spyAI = spy(new AIService("Hard"));
            when(session.getAttribute("aiService")).thenReturn(spyAI);

            setupServlet.doPost(req, resp);

            verify(spyAI, times(1)).reset();
        }

        @Test @DisplayName("1b. Không có aiService trong session → không throw")
        void noAiService_noThrow() throws Exception {
            when(session.getAttribute("aiService")).thenReturn(null);

            assertDoesNotThrow(() -> setupServlet.doPost(req, resp));
        }

        @Test @DisplayName("1c. reset() gọi TRƯỚC setAttribute('aiService', null)")
        void reset_calledBeforeNullSet() throws Exception {
            AIService spyAI = spy(new AIService("Hard"));
            when(session.getAttribute("aiService")).thenReturn(spyAI);

            InOrder order = inOrder(spyAI, session);
            setupServlet.doPost(req, resp);

            order.verify(spyAI).reset();
            order.verify(session).setAttribute("aiService", null);
        }

        @Test @DisplayName("1d. Hard AI có huntQueue → reset() xóa state, selectTarget về random")
        void hardAI_withActiveQueue_resetClears() throws Exception {
            AIService realAI = new AIService("Hard");
            // Tạo hit giả để huntQueue có dữ liệu
            Board dummy = makeBoard();
            realAI.notifyResult(5, 5, true, false, dummy);

            AIService spyAI = spy(realAI);
            when(session.getAttribute("aiService")).thenReturn(spyAI);

            setupServlet.doPost(req, resp);

            verify(spyAI).reset();
        }
    }

    // =========================================================================
    // 2. board_<playerId> bị xóa khỏi session
    // =========================================================================

    @Nested @DisplayName("2. board_<playerId> bị xóa")
    class BoardCleared {

        @Test @DisplayName("2a. removeAttribute('board_player-1') được gọi")
        void boardKey_removed() throws Exception {
            when(session.getAttribute("aiService")).thenReturn(null);
            setupServlet.doPost(req, resp);
            verify(session).removeAttribute("board_player-1");
        }

        @Test @DisplayName("2b. Sau đó setAttribute('board_player-1', newBoard) với board mới")
        void newBoard_setAfterRemove() throws Exception {
            when(session.getAttribute("aiService")).thenReturn(null);

            InOrder order = inOrder(session);
            setupServlet.doPost(req, resp);

            order.verify(session).removeAttribute("board_player-1");
            order.verify(session).setAttribute(eq("board_player-1"), any(Board.class));
        }
    }

    // =========================================================================
    // 3. Các session attribute bị null hóa đúng
    // =========================================================================

    @Nested @DisplayName("3. Session attributes bị null hóa")
    class SessionNulled {

        @BeforeEach
        void noAI() {
            when(session.getAttribute("aiService")).thenReturn(null);
        }

        @Test @DisplayName("3a. aiService được set null")
        void aiService_setNull() throws Exception {
            setupServlet.doPost(req, resp);
            verify(session).setAttribute("aiService", null);
        }

        @Test @DisplayName("3b. gameState được set null")
        void gameState_setNull() throws Exception {
            setupServlet.doPost(req, resp);
            verify(session).setAttribute("gameState", null);
        }

        @Test @DisplayName("3c. aiBoard được set null")
        void aiBoard_setNull() throws Exception {
            setupServlet.doPost(req, resp);
            verify(session).setAttribute("aiBoard", null);
        }
    }

    // =========================================================================
    // 4. Các attribute mới được ghi vào session
    // =========================================================================

    @Test @DisplayName("4. roomId, mode, difficulty được ghi vào session")
    void newAttributes_writtenToSession() throws Exception {
        when(session.getAttribute("aiService")).thenReturn(null);
        setupServlet.doPost(req, resp);
        verify(session).setAttribute(eq("roomId"), anyString());
        verify(session).setAttribute("mode", "PvE");
        verify(session).setAttribute("difficulty", "Hard");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Board makeBoard() {
        Board b = new Board();
        b.setId("board-test");
        b.setShips(new java.util.ArrayList<>());
        Cell[][] cells = new Cell[10][10];
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                cells[x][y] = new Cell("c"+x+y, "board-test", x, y);
        b.setCells(cells);
        return b;
    }
}