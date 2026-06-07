package service.ai;

import model.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test: HardAIStrategy — Hunt-Target 3 pha đầy đủ.
 *
 * Commit: test(ai): verify HardAIStrategy Hunt-Target phases, boundary safety, and reset
 *
 * Căn cứ code thực tế:
 *   HardAIStrategy: delegate toàn bộ state sang HuntTargetState
 *   HuntTargetState: huntQueue (Deque), lastHit, huntDirection
 *   pruneHitCells() → poll() → randomUnit()
 *   recordHit() → tryLockDirection() → enqueueNeighbors() / enqueueAlongAxis()
 *   recordSunk() → clear all state
 *
 * Yêu cầu spec UC08:
 *   - Hard AI sau khi trúng sẽ bắn các ô kề
 *   - Hard AI xử lý đúng khi bắn ra ngoài biên hoặc đã bắn rồi
 */
@DisplayName("HardAIStrategy Hunt-Target full test suite")
class HardAIStrategyFullTest {

    private HardAIStrategy hard;
    private Board board;

    @BeforeEach
    void setUp() {
        hard  = new HardAIStrategy();
        board = emptyBoard();
    }

    // =========================================================================
    // 1. RANDOM phase
    // =========================================================================

    @Nested @DisplayName("1. Pha RANDOM")
    class RandomPhase {

        @Test @DisplayName("1a. Board trống → trả ô hợp lệ trong [0,9]x[0,9]")
        void emptyBoard_validCell() {
            int[] t = hard.selectTarget(board);
            assertNotNull(t);
            assertTrue(t[0] >= 0 && t[0] < 10 && t[1] >= 0 && t[1] < 10);
        }

        @Test @DisplayName("1b. 100 lượt sequential không bắn ô đã hit (spec UC08)")
        void hundredShots_noRepeat() {
            for (int i = 0; i < 100; i++) {
                int[] t = hard.selectTarget(board);
                assertNotNull(t, "Lượt " + i);
                assertFalse(board.getCells()[t[0]][t[1]].isHit(),
                        "Bắn ô đã hit lượt " + i + ": (" + t[0] + "," + t[1] + ")");
                board.getCells()[t[0]][t[1]].setHit(true);
                hard.onShotResult(t[0], t[1], false, false, board);
            }
        }

        @Test @DisplayName("1c. Board đầy → null, không throw (spec UC08 — board full edge)")
        void fullBoard_null() {
            for (int x = 0; x < 10; x++)
                for (int y = 0; y < 10; y++)
                    board.getCells()[x][y].setHit(true);
            assertNull(assertDoesNotThrow(() -> hard.selectTarget(board)));
        }

        @Test @DisplayName("1d. Board còn 1 ô → trả đúng ô đó")
        void oneRemaining_returnsThat() {
            for (int x = 0; x < 10; x++)
                for (int y = 0; y < 10; y++)
                    if (!(x == 4 && y == 6)) board.getCells()[x][y].setHit(true);
            int[] t = hard.selectTarget(board);
            assertNotNull(t);
            assertEquals(4, t[0]); assertEquals(6, t[1]);
        }
    }

    // =========================================================================
    // 2. HUNT phase — sau hit đầu tiên
    // =========================================================================

    @Nested @DisplayName("2. Pha HUNT — bắn kề sau hit (spec UC08)")
    class HuntPhase {

        @Test @DisplayName("2a. Sau hit(5,5) → lượt tiếp là 1 trong 4 ô kề")
        void afterHit_nextIsNeighbor() {
            markHit(5, 5);
            hard.onShotResult(5, 5, true, false, board);
            int[] next = hard.selectTarget(board);
            assertNotNull(next);
            assertTrue(isNeighbor(5, 5, next[0], next[1]),
                    "Phải bắn ô kề. Thực tế: " + next[0] + "," + next[1]);
        }

        @Test @DisplayName("2b. Ô kề đã hit bị prune — không bao giờ chọn ô đã bắn (spec UC08)")
        void neighborAlreadyHit_pruned() {
            markHit(5, 5); markHit(4, 5);
            hard.onShotResult(5, 5, true, false, board);
            int[] t = hard.selectTarget(board);
            assertNotNull(t);
            assertFalse(board.getCells()[t[0]][t[1]].isHit(),
                    "Không được chọn ô đã hit");
            assertFalse(t[0] == 4 && t[1] == 5, "Không được chọn (4,5) đã hit");
        }

        @Test @DisplayName("2c. Miss từ queue → tiếp tục hunt ô kề khác")
        void missFromQueue_continuesHunt() {
            markHit(5, 5);
            hard.onShotResult(5, 5, true, false, board);

            int[] t1 = hard.selectTarget(board);
            assertNotNull(t1);
            markHit(t1[0], t1[1]);
            hard.onShotResult(t1[0], t1[1], false, false, board);

            int[] t2 = hard.selectTarget(board);
            assertNotNull(t2);
            assertFalse(board.getCells()[t2[0]][t2[1]].isHit(),
                    "Lượt 2 không được bắn ô đã hit");
        }
    }

    // =========================================================================
    // 3. TARGET phase — direction lock sau 2 hit cùng trục
    // =========================================================================

    @Nested @DisplayName("3. Pha TARGET — lock direction sau 2 hit liên tiếp")
    class TargetPhase {

        @Test @DisplayName("3a. hit(5,5) + hit(5,6) → lock ngang, tiếp theo cùng hàng x=5")
        void twoHitsHorizontal_locksRow() {
            markHit(5, 5); hard.onShotResult(5, 5, true, false, board);
            markHit(5, 6); hard.onShotResult(5, 6, true, false, board);

            Board fresh = copyHits(board);
            int[] t = hard.selectTarget(fresh);
            assertNotNull(t);
            assertEquals(5, t[0],
                    "Sau lock ngang, phải bắn hàng x=5. Thực tế: " + t[0] + "," + t[1]);
        }

        @Test @DisplayName("3b. hit(5,5) + hit(6,5) → lock dọc, tiếp theo cùng cột y=5")
        void twoHitsVertical_locksColumn() {
            markHit(5, 5); hard.onShotResult(5, 5, true, false, board);
            markHit(6, 5); hard.onShotResult(6, 5, true, false, board);

            Board fresh = copyHits(board);
            int[] t = hard.selectTarget(fresh);
            assertNotNull(t);
            assertEquals(5, t[1],
                    "Sau lock dọc, phải bắn cột y=5. Thực tế: " + t[0] + "," + t[1]);
        }

        @Test @DisplayName("3c. Ô được chọn sau lock không phải ô đã hit")
        void afterLock_notAlreadyHit() {
            markHit(3, 3); hard.onShotResult(3, 3, true, false, board);
            markHit(3, 4); hard.onShotResult(3, 4, true, false, board);

            Board fresh = copyHits(board);
            int[] t = hard.selectTarget(fresh);
            if (t != null)
                assertFalse(fresh.getCells()[t[0]][t[1]].isHit(),
                        "Không được bắn ô đã hit: " + t[0] + "," + t[1]);
        }
    }

    // =========================================================================
    // 4. SUNK → reset về RANDOM
    // =========================================================================

    @Nested @DisplayName("4. Sunk → reset về RANDOM")
    class SunkResetsToRandom {

        @Test @DisplayName("4a. Sau sunk → selectTarget phân tán rộng (>4 ô / 40 lượt)")
        void afterSunk_returnsToRandom() {
            markHit(5, 5); hard.onShotResult(5, 5, true, false, board);
            hard.onShotResult(5, 5, true, true, board); // sunk

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 40; i++) {
                Board fresh = emptyBoard(); fresh.getCells()[5][5].setHit(true);
                int[] t = hard.selectTarget(fresh);
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertTrue(seen.size() > 4, "Sau sunk, phải random. Chỉ thấy " + seen.size() + " ô");
        }

        @Test @DisplayName("4b. recordSunk() sau direction lock → queue cleared hoàn toàn")
        void sunkAfterLock_clearsQueue() {
            markHit(2, 2); hard.onShotResult(2, 2, true, false, board);
            markHit(2, 3); hard.onShotResult(2, 3, true, false, board);
            hard.onShotResult(2, 3, true, true, board); // sunk

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 30; i++) {
                int[] t = hard.selectTarget(emptyBoard());
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertTrue(seen.size() > 4, "Sau sunk+lock, vẫn phải random");
        }
    }

    // =========================================================================
    // 5. Biên map — spec UC08: xử lý đúng khi bắn ra ngoài biên
    // =========================================================================

    @Nested @DisplayName("5. Biên map — không ra ngoài [0,9] (spec UC08)")
    class BoundaryCheck {

        @Test @DisplayName("5a. hit góc (0,0) → tất cả ô kề trong [0,9]")
        void hitCorner00_neighborsInBounds() {
            markHit(0, 0); hard.onShotResult(0, 0, true, false, board);
            for (int i = 0; i < 5; i++) {
                Board fresh = emptyBoard(); fresh.getCells()[0][0].setHit(true);
                int[] t = hard.selectTarget(fresh);
                if (t == null) continue;
                assertTrue(t[0] >= 0 && t[0] < 10, "x ngoài biên: " + t[0]);
                assertTrue(t[1] >= 0 && t[1] < 10, "y ngoài biên: " + t[1]);
            }
        }

        @Test @DisplayName("5b. hit góc (9,9) → ô kề trong [0,9]")
        void hitCorner99_inBounds() {
            markHit(9, 9); hard.onShotResult(9, 9, true, false, board);
            Board fresh = emptyBoard(); fresh.getCells()[9][9].setHit(true);
            int[] t = hard.selectTarget(fresh);
            if (t != null) {
                assertTrue(t[0] >= 0 && t[0] < 10);
                assertTrue(t[1] >= 0 && t[1] < 10);
            }
        }

        @Test @DisplayName("5c. Ô đã bắn trong queue bị prune trước khi poll (spec UC08)")
        void queueCell_alreadyHit_pruned() {
            markHit(5, 5); hard.onShotResult(5, 5, true, false, board);
            // Bắn trúng ô kề (4,5) bên ngoài luồng AI
            markHit(4, 5);

            int[] t = hard.selectTarget(board);
            assertNotNull(t);
            assertFalse(t[0] == 4 && t[1] == 5,
                    "pruneHitCells phải loại (4,5) đã bắn khỏi queue");
            assertFalse(board.getCells()[t[0]][t[1]].isHit());
        }

        @Test @DisplayName("5d. Lock dọc gần cạnh — không enqueue ô ngoài biên")
        void verticalLockNearEdge_noOutOfBounds() {
            markHit(0, 0); hard.onShotResult(0, 0, true, false, board);
            markHit(1, 0); hard.onShotResult(1, 0, true, false, board);

            for (int i = 0; i < 5; i++) {
                Board fresh = emptyBoard();
                fresh.getCells()[0][0].setHit(true);
                fresh.getCells()[1][0].setHit(true);
                int[] t = hard.selectTarget(fresh);
                if (t == null) continue;
                assertTrue(t[0] >= 0 && t[0] < 10, "x OOB: " + t[0]);
                assertTrue(t[1] >= 0 && t[1] < 10, "y OOB: " + t[1]);
            }
        }
    }

    // =========================================================================
    // 6. reset() — xóa state game cũ (COMMIT-05)
    // =========================================================================

    @Nested @DisplayName("6. reset() — không rò rỉ state sang game mới")
    class ResetState {

        @Test @DisplayName("6a. reset() sau hit → selectTarget không ưu tiên ô kề nữa")
        void reset_afterHit_noLongerHunts() {
            markHit(5, 5); hard.onShotResult(5, 5, true, false, board);
            hard.reset();

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 40; i++) {
                Board fresh = emptyBoard(); fresh.getCells()[5][5].setHit(true);
                int[] t = hard.selectTarget(fresh);
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertTrue(seen.size() > 4, "Sau reset(), không được chỉ bắn quanh (5,5)");
        }

        @Test @DisplayName("6b. reset() idempotent — gọi nhiều lần không throw")
        void reset_idempotent() {
            assertDoesNotThrow(() -> {
                hard.reset(); hard.reset(); hard.reset();
            });
        }

        @Test @DisplayName("6c. reset() sau direction lock → random rộng")
        void reset_afterDirectionLock_random() {
            markHit(3, 3); hard.onShotResult(3, 3, true, false, board);
            markHit(3, 4); hard.onShotResult(3, 4, true, false, board);
            hard.reset();

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 30; i++) {
                int[] t = hard.selectTarget(emptyBoard());
                if (t != null) seen.add(t[0] + "," + t[1]);
            }
            assertTrue(seen.size() > 4, "Sau reset() từ pha TARGET, vẫn phải random");
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
        b.setCells(cells); return b;
    }

    private void markHit(int x, int y) { board.getCells()[x][y].setHit(true); }

    private boolean isNeighbor(int ox, int oy, int tx, int ty) {
        return (Math.abs(tx - ox) + Math.abs(ty - oy)) == 1;
    }

    private Board copyHits(Board src) {
        Board dst = emptyBoard();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                dst.getCells()[x][y].setHit(src.getCells()[x][y].isHit());
        return dst;
    }
}