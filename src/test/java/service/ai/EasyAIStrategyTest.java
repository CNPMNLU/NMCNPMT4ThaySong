package service.ai;

import model.Board;
import model.Cell;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test: EasyAIStrategy — random targeting.
 *
 * Commit: test(ai): verify EasyAIStrategy never fires on hit cells and distributes randomly
 *
 * Căn cứ EasyAIStrategy.java thực tế:
 *   - selectTarget(): duyệt cells 10x10, collect !isHit(), random pick
 *   - onShotResult(): không override → default no-op
 *   - reset(): không override → default no-op
 */
@DisplayName("EasyAIStrategy unit tests")
class EasyAIStrategyTest {

    private EasyAIStrategy easy;
    private Board board;

    @BeforeEach
    void setUp() {
        easy  = new EasyAIStrategy();
        board = emptyBoard();
    }

    // =========================================================================
    // 1. Không bắn ô đã bắn — yêu cầu spec UC08
    // =========================================================================

    @Nested @DisplayName("1. Không bắn ô đã bắn (spec UC08)")
    class NeverRepeat {

        @Test @DisplayName("1a. Board 10x10, (0,0) isHit=true → không bao giờ chọn (0,0)")
        void hitCell_neverSelected() {
            board.getCells()[0][0].setHit(true);
            for (int i = 0; i < 30; i++) {
                int[] t = easy.selectTarget(board);
                assertNotNull(t);
                assertFalse(t[0] == 0 && t[1] == 0, "Không được chọn ô (0,0) đã hit");
            }
        }

        @Test @DisplayName("1b. 50 lượt liên tiếp, mỗi lượt mark hit → không bao giờ chọn ô đã hit")
        void fiftySequentialShots_noRepeat() {
            for (int i = 0; i < 50; i++) {
                int[] t = easy.selectTarget(board);
                assertNotNull(t, "Lượt " + i + ": không được null khi còn ô trống");
                assertFalse(board.getCells()[t[0]][t[1]].isHit(),
                        "Lượt " + i + ": chọn ô đã hit (" + t[0] + "," + t[1] + ")");
                board.getCells()[t[0]][t[1]].setHit(true);
            }
        }

        @Test @DisplayName("1c. Board còn 1 ô chưa bắn → trả đúng ô đó")
        void oneRemaining_returnsThatCell() {
            fillAllExcept(board, 7, 3);
            int[] t = easy.selectTarget(board);
            assertNotNull(t);
            assertEquals(7, t[0]);
            assertEquals(3, t[1]);
        }

        @Test @DisplayName("1d. Board đầy → trả null, không throw")
        void fullBoard_returnsNull() {
            for (int x = 0; x < 10; x++)
                for (int y = 0; y < 10; y++)
                    board.getCells()[x][y].setHit(true);
            assertNull(assertDoesNotThrow(() -> easy.selectTarget(board)));
        }
    }

    // =========================================================================
    // 2. Phân phối ngẫu nhiên
    // =========================================================================

    @Nested @DisplayName("2. Phân phối ngẫu nhiên")
    class RandomDistribution {

        @Test @DisplayName("2a. 200 lượt chọn ít nhất 30 ô khác nhau")
        void manyShots_wideDistribution() {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                int[] t = easy.selectTarget(emptyBoard());
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertTrue(seen.size() >= 30,
                    "Easy AI phải random rộng, chỉ thấy " + seen.size() + " ô khác nhau");
        }

        @Test @DisplayName("2b. 30 lượt không phải lúc nào cũng cùng 1 ô")
        void notAlwaysSameCell() {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 30; i++) {
                int[] t = easy.selectTarget(emptyBoard());
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertTrue(seen.size() > 1, "Easy AI không được deterministic");
        }
    }

    // =========================================================================
    // 3. Default methods từ AIStrategy interface (COMMIT-01)
    // =========================================================================

    @Nested @DisplayName("3. Default methods — no-op (COMMIT-01)")
    class DefaultMethods {

        @Test @DisplayName("3a. onShotResult(hit=true) không throw và không thay đổi distribution")
        void onShotResult_hit_noEffect() {
            board.getCells()[5][5].setHit(true);
            assertDoesNotThrow(() -> easy.onShotResult(5, 5, true, false, board));

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                Board fresh = emptyBoard();
                fresh.getCells()[5][5].setHit(true);
                int[] t = easy.selectTarget(fresh);
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertFalse(seen.contains("5,5"), "Không được chọn ô đã hit");
            assertTrue(seen.size() > 4, "Easy AI vẫn random sau onShotResult");
        }

        @Test @DisplayName("3b. onShotResult(hit=false) không throw")
        void onShotResult_miss_noThrow() {
            assertDoesNotThrow(() -> easy.onShotResult(3, 4, false, false, board));
        }

        @Test @DisplayName("3c. onShotResult(sunk=true) không throw")
        void onShotResult_sunk_noThrow() {
            assertDoesNotThrow(() -> easy.onShotResult(3, 4, true, true, board));
        }

        @Test @DisplayName("3d. reset() không throw, selectTarget vẫn hoạt động sau đó")
        void reset_noEffect() {
            assertDoesNotThrow(() -> easy.reset());
            int[] t = easy.selectTarget(board);
            assertNotNull(t);
            assertFalse(board.getCells()[t[0]][t[1]].isHit());
        }

        @Test @DisplayName("3e. onShotResult() gọi nhiều lần liên tiếp không throw")
        void onShotResult_multipleTimes_noThrow() {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++)
                    easy.onShotResult(i, i, true, false, board);
            });
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Board emptyBoard() {
        Board b = new Board(); b.setId("b");
        Cell[][] cells = new Cell[10][10];
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                cells[x][y] = new Cell("c"+x+y, "b", x, y);
        b.setCells(cells);
        return b;
    }

    private void fillAllExcept(Board b, int ex, int ey) {
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                if (!(x == ex && y == ey))
                    b.getCells()[x][y].setHit(true);
    }
}