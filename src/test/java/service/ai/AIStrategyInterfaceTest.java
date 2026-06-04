package service.ai;

import model.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import service.AIService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho COMMIT-01: AIStrategy interface với default methods.
 *
 * Các nhóm test:
 *   1. Default methods không throw khi Easy AI không override
 *   2. AIService dùng polymorphism, không downcast HardAIStrategy
 *   3. notifyResult() routing đúng theo difficulty
 *   4. reset() routing đúng theo difficulty
 */
@DisplayName("COMMIT-01 — AIStrategy interface default methods & AIService polymorphism")
class AIStrategyInterfaceTest {

    // -------------------------------------------------------------------------
    // 1. Default methods trên EasyAIStrategy
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("1. EasyAIStrategy — default methods không cần override")
    class EasyDefaultMethods {

        private EasyAIStrategy easy;
        private Board board;

        @BeforeEach
        void setUp() {
            easy = new EasyAIStrategy();
            board = BoardTestHelper.emptyBoard();
        }

        @Test
        @DisplayName("onShotResult(hit=true) không throw và không thay đổi hành vi selectTarget")
        void onShotResult_hit_doesNotThrow() {
            assertDoesNotThrow(() ->
                    easy.onShotResult(3, 4, true, false, board)
            );
        }

        @Test
        @DisplayName("onShotResult(hit=false) không throw")
        void onShotResult_miss_doesNotThrow() {
            assertDoesNotThrow(() ->
                    easy.onShotResult(3, 4, false, false, board)
            );
        }

        @Test
        @DisplayName("onShotResult(sunk=true) không throw")
        void onShotResult_sunk_doesNotThrow() {
            assertDoesNotThrow(() ->
                    easy.onShotResult(3, 4, true, true, board)
            );
        }

        @Test
        @DisplayName("reset() không throw")
        void reset_doesNotThrow() {
            assertDoesNotThrow(() -> easy.reset());
        }

        @Test
        @DisplayName("Sau khi gọi onShotResult nhiều lần, selectTarget vẫn trả về ô hợp lệ")
        void selectTarget_afterMultipleCallbacks_stillReturnsValidCell() {
            // Hit vài ô
            BoardTestHelper.markHit(board, 0, 0);
            BoardTestHelper.markHit(board, 1, 1);

            // Gọi onShotResult — Easy AI nên bỏ qua hoàn toàn
            easy.onShotResult(0, 0, true, false, board);
            easy.onShotResult(1, 1, false, false, board);

            int[] target = easy.selectTarget(board);

            assertNotNull(target, "selectTarget không được trả null khi còn ô trống");
            assertFalse(board.getCells()[target[0]][target[1]].isHit(),
                    "selectTarget không được chọn ô đã bắn");
        }
    }

    // -------------------------------------------------------------------------
    // 2. AIService — không downcast, dùng polymorphism
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("2. AIService — polymorphic dispatch, không downcast")
    class AIServicePolymorphism {

        @Test
        @DisplayName("AIService(Easy) tạo EasyAIStrategy — selectTarget trả về ô hợp lệ")
        void easyService_selectTarget_returnsValidCell() {
            AIService service = new AIService("Easy");
            Board board = BoardTestHelper.emptyBoard();

            int[] target = service.selectTarget(board);

            assertNotNull(target);
            assertEquals(2, target.length);
            assertTrue(target[0] >= 0 && target[0] < 10);
            assertTrue(target[1] >= 0 && target[1] < 10);
        }

        @Test
        @DisplayName("AIService(Hard) tạo HardAIStrategy — selectTarget trả về ô hợp lệ")
        void hardService_selectTarget_returnsValidCell() {
            AIService service = new AIService("Hard");
            Board board = BoardTestHelper.emptyBoard();

            int[] target = service.selectTarget(board);

            assertNotNull(target);
            assertEquals(2, target.length);
        }

        @Test
        @DisplayName("AIService.notifyResult() gọi được với Easy mà không throw — không cần downcast")
        void easyService_notifyResult_doesNotThrow() {
            AIService service = new AIService("Easy");
            Board board = BoardTestHelper.emptyBoard();

            // Đây là test cốt lõi COMMIT-01: trước đây gọi notifyResult() trên Easy
            // sẽ bị skip vì "hardStrategy == null". Giờ phải không throw và không-op.
            assertDoesNotThrow(() ->
                    service.notifyResult(5, 5, true, false, board)
            );
        }

        @Test
        @DisplayName("AIService.notifyResult() gọi được với Hard mà không throw — không cần downcast")
        void hardService_notifyResult_doesNotThrow() {
            AIService service = new AIService("Hard");
            Board board = BoardTestHelper.emptyBoard();

            assertDoesNotThrow(() ->
                    service.notifyResult(5, 5, true, false, board)
            );
        }

        @Test
        @DisplayName("AIService(null difficulty) fallback về Easy — không throw")
        void nullDifficulty_fallbackToEasy() {
            // Kiểm tra defensive constructor
            AIService service = new AIService(null);
            Board board = BoardTestHelper.emptyBoard();

            assertDoesNotThrow(() -> service.selectTarget(board));
        }

        @Test
        @DisplayName("AIService(unknown difficulty) fallback về Easy — không throw")
        void unknownDifficulty_fallbackToEasy() {
            AIService service = new AIService("Medium");
            Board board = BoardTestHelper.emptyBoard();

            assertDoesNotThrow(() -> service.selectTarget(board));
        }
    }

    // -------------------------------------------------------------------------
    // 3. notifyResult routing — Easy vs Hard có behavior khác nhau
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("3. notifyResult routing — Easy bỏ qua, Hard cập nhật queue")
    class NotifyResultRouting {

        @Test
        @DisplayName("Easy: notifyResult(hit) không ảnh hưởng selectTarget — vẫn random")
        void easy_notifyHit_doesNotAffectSelectTarget() {
            AIService service = new AIService("Easy");
            Board board = BoardTestHelper.emptyBoard();

            // Bắn trúng (3,3)
            BoardTestHelper.markHit(board, 3, 3);
            service.notifyResult(3, 3, true, false, board);

            // Bắn thêm 50 lần — Easy không bao giờ bắn lại (3,3) nhưng
            // cũng không ưu tiên ô kề — phân phối phải gồm nhiều ô khác nhau
            java.util.Set<String> chosen = new java.util.HashSet<>();
            for (int i = 0; i < 50; i++) {
                Board fresh = BoardTestHelper.emptyBoard();
                BoardTestHelper.markHit(fresh, 3, 3);
                int[] t = service.selectTarget(fresh);
                assertNotNull(t);
                assertFalse(t[0] == 3 && t[1] == 3, "Easy không được bắn ô đã hit");
                chosen.add(t[0] + "," + t[1]);
            }
            // Easy phải phân tán — không tập trung vào 4 ô kề (3,3)
            assertTrue(chosen.size() > 4,
                    "Easy AI phải random, không nên chỉ bắn quanh ô vừa trúng");
        }

        @Test
        @DisplayName("Hard: notifyResult(hit) làm selectTarget ưu tiên ô kề")
        void hard_notifyHit_prioritizesNeighbors() {
            AIService service = new AIService("Hard");
            Board board = BoardTestHelper.emptyBoard();

            // Báo trúng ô (5,5)
            BoardTestHelper.markHit(board, 5, 5);
            service.notifyResult(5, 5, true, false, board);

            // Lượt tiếp theo phải là một trong 4 ô kề (5,5)
            int[] next = service.selectTarget(board);
            assertNotNull(next);

            int[][] neighbors = {{4,5},{6,5},{5,4},{5,6}};
            assertTrue(BoardTestHelper.containsPos(neighbors, next[0], next[1]),
                    "Hard AI phải bắn ô kề sau khi trúng. Thực tế bắn: " + next[0] + "," + next[1]);
        }

        @Test
        @DisplayName("Hard: notifyResult(sunk) làm selectTarget quay về random")
        void hard_notifySunk_resetsToRandom() {
            AIService service = new AIService("Hard");
            Board board = BoardTestHelper.emptyBoard();

            // Trúng (5,5) rồi sunk
            BoardTestHelper.markHit(board, 5, 5);
            service.notifyResult(5, 5, true, false, board);
            service.notifyResult(5, 5, true, true, board); // sunk

            // Queue phải rỗng sau sunk — selectTarget random, không chỉ 4 ô kề
            java.util.Set<String> chosen = new java.util.HashSet<>();
            for (int i = 0; i < 50; i++) {
                Board fresh = BoardTestHelper.emptyBoard();
                BoardTestHelper.markHit(fresh, 5, 5);
                int[] t = service.selectTarget(fresh);
                assertNotNull(t);
                chosen.add(t[0] + "," + t[1]);
            }
            assertTrue(chosen.size() > 4,
                    "Sau sunk, Hard AI phải quay về random (nhiều ô khác nhau)");
        }
    }

    // -------------------------------------------------------------------------
    // 4. reset() routing
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("4. reset() — đảm bảo state không rò rỉ sang game mới")
    class ResetRouting {

        @Test
        @DisplayName("Easy: reset() không throw, selectTarget vẫn hoạt động sau reset")
        void easy_reset_doesNotAffectBehavior() {
            AIService service = new AIService("Easy");
            Board board = BoardTestHelper.emptyBoard();

            service.reset();
            int[] target = service.selectTarget(board);

            assertNotNull(target);
        }

        @Test
        @DisplayName("Hard: reset() sau hit xóa queue — selectTarget không còn bắn ô kề")
        void hard_reset_clearsQueueAfterHit() {
            AIService service = new AIService("Hard");
            Board board = BoardTestHelper.emptyBoard();

            // Trúng (5,5) → queue có ô kề
            BoardTestHelper.markHit(board, 5, 5);
            service.notifyResult(5, 5, true, false, board);

            // Reset (bắt đầu game mới)
            service.reset();

            // selectTarget phải random — không được dùng queue của game cũ
            java.util.Set<String> chosen = new java.util.HashSet<>();
            int[][] neighbors = {{4,5},{6,5},{5,4},{5,6}};
            int neighborCount = 0;

            for (int i = 0; i < 40; i++) {
                Board fresh = BoardTestHelper.emptyBoard();
                BoardTestHelper.markHit(fresh, 5, 5);
                int[] t = service.selectTarget(fresh);
                assertNotNull(t);
                chosen.add(t[0] + "," + t[1]);
                if (BoardTestHelper.containsPos(neighbors, t[0], t[1])) neighborCount++;
            }

            // Sau reset, bắn 40 lần không được 100% vào 4 ô kề
            assertFalse(neighborCount == 40,
                    "Sau reset(), Hard AI không được dùng huntQueue của game cũ");
            assertTrue(chosen.size() > 4,
                    "Sau reset(), Hard AI phải random rộng");
        }

        @Test
        @DisplayName("Hard: reset() có thể gọi nhiều lần liên tiếp mà không throw")
        void hard_reset_idempotent() {
            AIService service = new AIService("Hard");
            Board board = BoardTestHelper.emptyBoard();

            assertDoesNotThrow(() -> {
                service.reset();
                service.reset();
                service.reset();
                service.selectTarget(board);
            });
        }
    }
}