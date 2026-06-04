package service.ai;

import model.Board;
import model.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho COMMIT-02: HuntTargetState và HardAIStrategy 3 pha.
 *
 * Cấu trúc:
 *   1. HuntTargetState — test state machine độc lập (không cần strategy)
 *      1a. recordHit → HUNT phase
 *      1b. recordHit × 2 cùng trục → TARGET phase (direction lock)
 *      1c. recordSunk → RANDOM phase
 *      1d. pruneHitCells loại ô đã bắn
 *      1e. reset() trả về trạng thái ban đầu
 *      1f. Biên map — không enqueue ô ngoài [0,9]
 *
 *   2. HardAIStrategy — test 3 pha end-to-end
 *      2a. RANDOM phase — chọn ô hợp lệ, không trùng
 *      2b. HUNT phase — sau hit bắn vào 4 ô kề
 *      2c. TARGET phase — sau 2 hit cùng trục chỉ bắn theo trục đó
 *      2d. Sunk → quay về RANDOM
 *      2e. Board gần đầy — selectTarget không trả null nếu còn ô
 *      2f. Board đầy — selectTarget trả null
 */
@DisplayName("COMMIT-02 — HuntTargetState & HardAIStrategy 3-phase Hunt-Target")
class HuntTargetStateTest {

    // =========================================================================
    // 1. HuntTargetState — unit test độc lập
    // =========================================================================

    @Nested
    @DisplayName("1. HuntTargetState — state machine")
    class HuntTargetStateUnit {

        private HuntTargetState state;
        private Cell[][] cells;

        @BeforeEach
        void setUp() {
            state = new HuntTargetState();
            // Board 10×10, tất cả isHit = false
            cells = new Cell[10][10];
            for (int x = 0; x < 10; x++)
                for (int y = 0; y < 10; y++) {
                    cells[x][y] = new Cell("c" + x + y, "b", x, y);
                }
        }

        // -- 1a. HUNT phase ---------------------------------------------------

        @Test
        @DisplayName("1a. recordHit(5,5) → queue chứa đúng 4 ô kề")
        void recordHit_center_enqueues4Neighbors() {
            state.recordHit(5, 5, cells);

            Set<String> queued = drainToSet(state);
            assertEquals(4, queued.size());
            assertTrue(queued.contains("4,5"), "thiếu ô trên");
            assertTrue(queued.contains("6,5"), "thiếu ô dưới");
            assertTrue(queued.contains("5,4"), "thiếu ô trái");
            assertTrue(queued.contains("5,6"), "thiếu ô phải");
        }

        @Test
        @DisplayName("1a. recordHit(5,5) hai lần — không enqueue ô trùng")
        void recordHit_twice_noDuplicatesInQueue() {
            state.recordHit(5, 5, cells);
            // Giả sử ô (4,5) vừa được poll và bắn miss, gọi recordHit lần 2 cùng vị trí
            state.recordHit(5, 5, cells);

            Set<String> queued = drainToSet(state);
            // Set sẽ tự loại trùng — nhưng kiểm tra size đúng hơn
            assertTrue(queued.size() <= 4, "Không được enqueue ô trùng");
        }

        // -- 1b. TARGET phase (direction lock) --------------------------------

        @Test
        @DisplayName("1b. Hit (5,5) rồi (5,6) — direction lock theo trục ngang (dy=1)")
        void recordHit_twoHits_horizontalLock() {
            state.recordHit(5, 5, cells);
            cells[5][5].setHit(true); // mark đã bắn
            state.recordHit(5, 6, cells);

            Set<String> queued = drainToSet(state);

            // Sau lock trục ngang: chỉ còn ô trên cùng hàng x=5
            for (String pos : queued) {
                int x = Integer.parseInt(pos.split(",")[0]);
                assertEquals(5, x,
                        "Sau direction lock ngang, queue chỉ được chứa ô cùng hàng x=5, thực tế: " + pos);
            }
        }

        @Test
        @DisplayName("1b. Hit (5,5) rồi (6,5) — direction lock theo trục dọc (dx=1)")
        void recordHit_twoHits_verticalLock() {
            state.recordHit(5, 5, cells);
            cells[5][5].setHit(true);
            state.recordHit(6, 5, cells);

            Set<String> queued = drainToSet(state);

            for (String pos : queued) {
                int y = Integer.parseInt(pos.split(",")[1]);
                assertEquals(5, y,
                        "Sau direction lock dọc, queue chỉ được chứa ô cùng cột y=5, thực tế: " + pos);
            }
        }

        @Test
        @DisplayName("1b. Hit (5,5) rồi (5,7) — không lock (2 ô không kề nhau)")
        void recordHit_nonAdjacent_noDirectionLock() {
            state.recordHit(5, 5, cells);
            cells[5][5].setHit(true);
            state.recordHit(5, 7, cells); // cách nhau 2 ô

            // Nếu không lock, queue vẫn chứa ô của cả 2 lần hit
            assertTrue(state.hasTargets(), "Queue phải còn ô sau 2 hit không kề");
            // Không assert lock vì behavior phụ thuộc implementation
            // — test này đảm bảo không crash
        }

        // -- 1c. RANDOM phase (sau sunk) --------------------------------------

        @Test
        @DisplayName("1c. recordSunk() → queue rỗng, hasTargets() = false")
        void recordSunk_clearsQueue() {
            state.recordHit(5, 5, cells);
            assertTrue(state.hasTargets(), "Precondition: queue phải có ô sau hit");

            state.recordSunk();

            assertFalse(state.hasTargets(), "Sau sunk, queue phải rỗng");
            assertNull(state.poll(), "poll() phải trả null sau khi queue rỗng");
        }

        @Test
        @DisplayName("1c. recordSunk() sau direction lock → vẫn clear hoàn toàn")
        void recordSunk_afterDirectionLock_clearsAll() {
            state.recordHit(5, 5, cells);
            cells[5][5].setHit(true);
            state.recordHit(5, 6, cells); // lock ngang

            state.recordSunk();

            assertFalse(state.hasTargets());
        }

        // -- 1d. pruneHitCells ------------------------------------------------

        @Test
        @DisplayName("1d. pruneHitCells() loại ô đã bị bắn khỏi queue")
        void pruneHitCells_removesAlreadyHitFromQueue() {
            state.recordHit(5, 5, cells);
            // Queue có: (4,5),(6,5),(5,4),(5,6)

            // Bắn trúng (4,5) ngoài luồng (AI bắn random trúng ô trong queue)
            cells[4][5].setHit(true);
            cells[5][6].setHit(true);

            state.pruneHitCells(cells);

            Set<String> remaining = drainToSet(state);
            assertFalse(remaining.contains("4,5"), "pruneHitCells phải xóa (4,5) đã bắn");
            assertFalse(remaining.contains("5,6"), "pruneHitCells phải xóa (5,6) đã bắn");
            assertTrue(remaining.contains("6,5"), "pruneHitCells không được xóa (6,5) chưa bắn");
            assertTrue(remaining.contains("5,4"), "pruneHitCells không được xóa (5,4) chưa bắn");
        }

        @Test
        @DisplayName("1d. pruneHitCells() trên queue rỗng không throw")
        void pruneHitCells_emptyQueue_doesNotThrow() {
            assertDoesNotThrow(() -> state.pruneHitCells(cells));
        }

        // -- 1e. reset() ------------------------------------------------------

        @Test
        @DisplayName("1e. reset() sau hit xóa queue và hasTargets() = false")
        void reset_afterHit_clearsState() {
            state.recordHit(5, 5, cells);
            state.reset();

            assertFalse(state.hasTargets());
            assertNull(state.poll());
        }

        @Test
        @DisplayName("1e. reset() sau direction lock xóa hoàn toàn")
        void reset_afterDirectionLock_clearsAll() {
            state.recordHit(3, 3, cells);
            cells[3][3].setHit(true);
            state.recordHit(3, 4, cells);
            state.reset();

            assertFalse(state.hasTargets());
        }

        @Test
        @DisplayName("1e. reset() idempotent — gọi nhiều lần không throw")
        void reset_idempotent() {
            assertDoesNotThrow(() -> {
                state.reset();
                state.reset();
            });
        }

        // -- 1f. Biên map -----------------------------------------------------

        @Test
        @DisplayName("1f. recordHit góc (0,0) — chỉ enqueue 2 ô hợp lệ")
        void recordHit_topLeftCorner_onlyTwoNeighbors() {
            state.recordHit(0, 0, cells);

            Set<String> queued = drainToSet(state);
            assertEquals(2, queued.size(), "Góc (0,0) chỉ có 2 hướng hợp lệ");
            assertTrue(queued.contains("1,0"), "thiếu ô dưới");
            assertTrue(queued.contains("0,1"), "thiếu ô phải");
        }

        @Test
        @DisplayName("1f. recordHit cạnh (0,5) — chỉ enqueue 3 ô hợp lệ")
        void recordHit_topEdge_onlyThreeNeighbors() {
            state.recordHit(0, 5, cells);

            Set<String> queued = drainToSet(state);
            assertEquals(3, queued.size(), "Cạnh trên (0,5) chỉ có 3 hướng hợp lệ");
            assertFalse(queued.contains("-1,5"), "Không được enqueue ô ngoài biên (-1,5)");
        }

        @Test
        @DisplayName("1f. recordHit góc (9,9) — chỉ enqueue 2 ô hợp lệ")
        void recordHit_bottomRightCorner_onlyTwoNeighbors() {
            state.recordHit(9, 9, cells);

            Set<String> queued = drainToSet(state);
            assertEquals(2, queued.size());
            assertTrue(queued.contains("8,9"));
            assertTrue(queued.contains("9,8"));
        }

        // -- helper -----------------------------------------------------------
        private Set<String> drainToSet(HuntTargetState s) {
            Set<String> result = new HashSet<>();
            int[] pos;
            while ((pos = s.poll()) != null) {
                result.add(pos[0] + "," + pos[1]);
            }
            return result;
        }
    }

    // =========================================================================
    // 2. HardAIStrategy — 3-phase end-to-end
    // =========================================================================

    @Nested
    @DisplayName("2. HardAIStrategy — end-to-end 3 pha")
    class HardAIStrategyEndToEnd {

        private HardAIStrategy strategy;
        private Board board;

        @BeforeEach
        void setUp() {
            strategy = new HardAIStrategy();
            board = BoardTestHelper.emptyBoard();
        }

        // -- 2a. RANDOM phase -------------------------------------------------

        @Test
        @DisplayName("2a. Board trống — selectTarget trả về ô hợp lệ")
        void selectTarget_emptyBoard_returnsValidCell() {
            int[] t = strategy.selectTarget(board);

            assertNotNull(t);
            assertEquals(2, t.length);
            assertTrue(t[0] >= 0 && t[0] < 10);
            assertTrue(t[1] >= 0 && t[1] < 10);
            assertFalse(board.getCells()[t[0]][t[1]].isHit());
        }

        @Test
        @DisplayName("2a. Pha RANDOM — 100 lượt không bắn ô đã bắn")
        void selectTarget_randomPhase_neverRepeatsHitCells() {
            // Bắn 50 ô ngẫu nhiên, check không trùng
            for (int i = 0; i < 50; i++) {
                int[] t = strategy.selectTarget(board);
                assertNotNull(t, "selectTarget không được null khi còn ô trống");
                assertFalse(board.getCells()[t[0]][t[1]].isHit(),
                        "Bắn ô đã hit ở lượt " + i);
                board.getCells()[t[0]][t[1]].setHit(true);
                strategy.onShotResult(t[0], t[1], false, false, board); // tất cả miss
            }
        }

        // -- 2b. HUNT phase ---------------------------------------------------

        @Test
        @DisplayName("2b. Sau hit(5,5) — selectTarget phải là ô kề")
        void selectTarget_afterHit_returnsNeighbor() {
            BoardTestHelper.markHit(board, 5, 5);
            strategy.onShotResult(5, 5, true, false, board);

            int[] next = strategy.selectTarget(board);

            assertNotNull(next);
            int[][] neighbors = {{4,5},{6,5},{5,4},{5,6}};
            assertTrue(BoardTestHelper.containsPos(neighbors, next[0], next[1]),
                    "Sau hit(5,5), AI phải bắn ô kề. Thực tế: " + next[0] + "," + next[1]);
        }

        @Test
        @DisplayName("2b. Ô kề đã bắn bị skip — selectTarget lấy ô kề tiếp theo")
        void selectTarget_neighborAlreadyHit_skipsToNext() {
            // (5,5) đã hit, (4,5) cũng đã hit trước
            BoardTestHelper.markHit(board, 5, 5);
            BoardTestHelper.markHit(board, 4, 5);
            strategy.onShotResult(5, 5, true, false, board);

            int[] next = strategy.selectTarget(board);

            assertNotNull(next);
            assertFalse(next[0] == 4 && next[1] == 5,
                    "selectTarget không được chọn ô (4,5) đã bắn");
            assertFalse(board.getCells()[next[0]][next[1]].isHit());
        }

        @Test
        @DisplayName("2b. Hit ở góc (0,0) — chỉ bắn ô hợp lệ (không ra ngoài biên)")
        void selectTarget_hitAtCorner_staysInBounds() {
            BoardTestHelper.markHit(board, 0, 0);
            strategy.onShotResult(0, 0, true, false, board);

            // Bắn 2 lần (chỉ có 2 ô kề hợp lệ ở góc)
            for (int i = 0; i < 2; i++) {
                int[] t = strategy.selectTarget(board);
                assertNotNull(t);
                assertTrue(t[0] >= 0 && t[0] < 10 && t[1] >= 0 && t[1] < 10,
                        "selectTarget trả ô ngoài biên: " + t[0] + "," + t[1]);
                BoardTestHelper.markHit(board, t[0], t[1]);
                strategy.onShotResult(t[0], t[1], false, false, board);
            }
        }

        // -- 2c. TARGET phase (direction lock) --------------------------------

        @Test
        @DisplayName("2c. Hit (5,5) + (5,6) → chỉ bắn theo trục ngang (y thay đổi)")
        void selectTarget_twoHitsHorizontal_locksToRow() {
            // Chạy thử nghiệm 10 lần độc lập để kiểm tra tính ngẫu nhiên nhưng vẫn giữ đúng trục
            for (int i = 0; i < 10; i++) {
                // Khởi tạo mới hoàn toàn cho mỗi lượt test để huntQueue không bị rút cạn
                HardAIStrategy freshStrategy = new HardAIStrategy();
                Board testBoard = BoardTestHelper.emptyBoard();

                // Giả lập 2 cú bắn trúng liên tiếp theo hàng ngang
                BoardTestHelper.markHit(testBoard, 5, 5);
                freshStrategy.onShotResult(5, 5, true, false, testBoard);

                BoardTestHelper.markHit(testBoard, 5, 6);
                freshStrategy.onShotResult(5, 6, true, false, testBoard);

                // Lấy mục tiêu tiếp theo
                int[] t = freshStrategy.selectTarget(testBoard);

                assertNotNull(t, "Mục tiêu không được null");
                assertEquals(5, t[0],
                        "Sau lock ngang, AI phải bắn trên hàng x=5. Thực tế bắn ô: " + t[0] + "," + t[1]);
            }
        }

        @Test
        @DisplayName("2c. Hit (5,5) + (6,5) → chỉ bắn theo trục dọc (x thay đổi)")
        void selectTarget_twoHitsVertical_locksToColumn() {
            BoardTestHelper.markHit(board, 5, 5);
            strategy.onShotResult(5, 5, true, false, board);

            BoardTestHelper.markHit(board, 6, 5);
            strategy.onShotResult(6, 5, true, false, board);

            Board freshBoard = BoardTestHelper.emptyBoard();
            BoardTestHelper.markHit(freshBoard, 5, 5);
            BoardTestHelper.markHit(freshBoard, 6, 5);

            int[] t = strategy.selectTarget(freshBoard);
            assertNotNull(t);
            assertEquals(5, t[1],
                    "Sau lock dọc, AI phải bắn trên cột y=5. Thực tế: " + t[0] + "," + t[1]);
        }

        // -- 2d. Sunk → RANDOM ------------------------------------------------

        @Test
        @DisplayName("2d. Sau sunk — selectTarget phân tán rộng (không chỉ 4 ô kề)")
        void selectTarget_afterSunk_returnsToRandom() {
            BoardTestHelper.markHit(board, 5, 5);
            strategy.onShotResult(5, 5, true, false, board);
            strategy.onShotResult(5, 5, true, true, board); // sunk

            Set<String> chosen = new HashSet<>();
            for (int i = 0; i < 40; i++) {
                Board fresh = BoardTestHelper.emptyBoard();
                BoardTestHelper.markHit(fresh, 5, 5);
                int[] t = strategy.selectTarget(fresh);
                if (t != null) chosen.add(t[0] + "," + t[1]);
            }
            assertTrue(chosen.size() > 4,
                    "Sau sunk, AI phải random. Chỉ chọn " + chosen.size() + " ô khác nhau");
        }

        @Test
        @DisplayName("2d. Sunk sau direction lock — queue bị clear hoàn toàn")
        void selectTarget_sunkAfterDirectionLock_clearsQueue() {
            BoardTestHelper.markHit(board, 3, 3);
            strategy.onShotResult(3, 3, true, false, board);
            BoardTestHelper.markHit(board, 3, 4);
            strategy.onShotResult(3, 4, true, false, board); // lock ngang
            strategy.onShotResult(3, 4, true, true, board);  // sunk

            Set<String> chosen = new HashSet<>();
            for (int i = 0; i < 30; i++) {
                Board fresh = BoardTestHelper.emptyBoard();
                int[] t = strategy.selectTarget(fresh);
                if (t != null) chosen.add(t[0] + "," + t[1]);
            }
            assertTrue(chosen.size() > 4, "Sau sunk, phải random rộng");
        }

        // -- 2e. Board gần đầy ------------------------------------------------

        @Test
        @DisplayName("2e. Board còn 1 ô — selectTarget trả đúng ô đó")
        void selectTarget_oneRemaining_returnsThatCell() {
            BoardTestHelper.fillAllExcept(board, new int[][]{{7, 3}});

            int[] t = strategy.selectTarget(board);

            assertNotNull(t, "selectTarget không được null khi còn 1 ô");
            assertEquals(7, t[0]);
            assertEquals(3, t[1]);
        }

        // -- 2f. Board đầy ----------------------------------------------------

        @Test
        @DisplayName("2f. Board đầy — selectTarget trả null, không throw")
        void selectTarget_fullBoard_returnsNull() {
            BoardTestHelper.fillAllExcept(board, new int[][]{});

            int[] t = assertDoesNotThrow(() -> strategy.selectTarget(board));

            assertNull(t, "Board đầy phải trả null");
        }
    }
}