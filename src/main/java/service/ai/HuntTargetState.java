package service.ai;

import model.Cell;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * tách biệt toàn bộ state của pha Hunt-Target ra khỏi HardAIStrategy.
 *
 * Lý do tách:
 *   - Dễ unit-test độc lập, không cần khởi tạo strategy hay board
 *   - HardAIStrategy chỉ điều phối logic, không giữ state thô
 *   - reset() tập trung một chỗ, không bị bỏ sót field nào
 *
 * Vòng đời state qua các pha:
 *
 *   [RANDOM] ──hit──► [HUNT] ──2 hit cùng trục──► [TARGET] ──sunk──► [RANDOM]
 *
 *   RANDOM : huntQueue rỗng, huntDirection null
 *   HUNT   : huntQueue có ô kề 4 hướng, huntDirection null (chưa biết trục)
 *   TARGET : huntQueue chỉ còn ô trên 1 trục, huntDirection đã xác định
 */
public class HuntTargetState implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int BOARD_SIZE = 10;

    /**
     *hàng đợi các ô ưu tiên bắn.
     * - Pha HUNT  : chứa tối đa 4 hướng kề quanh lastHit
     * - Pha TARGET: chứa các ô theo đúng trục huntDirection
     */
    private final Deque<int[]> huntQueue = new ArrayDeque<>();

    /**
     * Ô trúng đạn gần nhất.
     * null khi đang ở pha RANDOM.
     */
    private int[] lastHit = null;

    /**
     * Hướng đã xác định sau khi trúng 2 ô liên tiếp trên cùng trục.
     * - null      : chưa xác định (pha HUNT)
     * - {1, 0}    : trục dọc (tăng x)
     * - {0, 1}    : trục ngang (tăng y)
     * Khi đã lock, huntQueue được lọc để chỉ giữ ô trên trục này.
     */
    private int[] huntDirection = null;


    /** true nếu đang ở pha HUNT hoặc TARGET (có ô cần bắn theo ưu tiên). */
    public boolean hasTargets() {
        return !huntQueue.isEmpty();
    }

    /**
     * Lấy ô tiếp theo từ queue (FIFO).
     * Trả về null nếu queue rỗng — caller phải kiểm tra hasTargets() trước.
     */
    public int[] poll() {
        return huntQueue.poll();
    }

    /**
     * Gọi khi bắn trúng ô (x, y) nhưng tàu chưa chìm.
     *
     * Logic:
     * 1. Cập nhật lastHit.
     * 2. Nếu đã có lastHit trước đó → thử lock direction (chuyển sang pha TARGET).
     * 3. Nếu direction đã được lock → chỉ enqueue theo trục đó.
     * 4. Nếu chưa lock → enqueue 4 hướng kề hợp lệ (pha HUNT).
     *
     * @param x     tọa độ x ô vừa trúng
     * @param y     tọa độ y ô vừa trúng
     * @param cells trạng thái board hiện tại (để lọc ô đã bắn và ô ngoài biên)
     */
    public void recordHit(int x, int y, Cell[][] cells) {
        // Thử lock direction nếu đây là hit thứ 2 trở đi
        if (lastHit != null && huntDirection == null) {
            tryLockDirection(lastHit, x, y, cells);
        }

        lastHit = new int[]{x, y};

        if (huntDirection != null) {
            // Pha TARGET: chỉ enqueue 2 đầu của trục đã xác định
            enqueueAlongAxis(x, y, cells);
        } else {
            // Pha HUNT: enqueue 4 hướng kề
            enqueueNeighbors(x, y, cells);
        }
    }

    /**
     * Gọi khi tàu bị đánh chìm hoàn toàn.
     * Reset state về RANDOM để tìm tàu tiếp theo.
     */
    public void recordSunk() {
        huntQueue.clear();
        lastHit = null;
        huntDirection = null;
    }

    /**
     * Lọc bỏ các ô trong queue đã bị bắn (do bắn ngẫu nhiên trùng queue).
     * Gọi ở đầu selectTarget() trước khi poll().
     */
    public void pruneHitCells(Cell[][] cells) {
        huntQueue.removeIf(pos ->
                !isInBounds(pos[0], pos[1]) || cells[pos[0]][pos[1]].isHit()
        );
    }

    /** Reset toàn bộ state về ban đầu (pha RANDOM). */
    public void reset() {
        huntQueue.clear();
        lastHit = null;
        huntDirection = null;
    }


    /**
     * Thử xác định hướng từ 2 ô trúng liên tiếp.
     * Nếu 2 ô nằm trên cùng hàng hoặc cột → lock direction và
     * lọc huntQueue để chỉ giữ ô cùng trục.
     */
    private void tryLockDirection(int[] prev, int curX, int curY, Cell[][] cells) {
        int dx = curX - prev[0];
        int dy = curY - prev[1];

        // Chỉ lock nếu 2 ô kề nhau trực tiếp (không nhảy ô)
        if (Math.abs(dx) + Math.abs(dy) != 1) return;

        // Chuẩn hóa về vector đơn vị
        huntDirection = new int[]{Integer.signum(dx), Integer.signum(dy)};

        // Lọc queue: xóa ô không cùng trục với huntDirection
        huntQueue.removeIf(pos -> {
            int pdx = pos[0] - prev[0];
            int pdy = pos[1] - prev[1];
            // Giữ lại ô nằm trên trục: cùng hướng hoặc ngược hướng
            boolean sameAxis = (huntDirection[0] != 0 && pdy == 0)
                    || (huntDirection[1] != 0 && pdx == 0);
            return !sameAxis;
        });
    }

    /**
     * Enqueue 2 ô theo trục huntDirection tính từ (x, y):
     * tiếp tục theo hướng và ngược hướng.
     */
    private void enqueueAlongAxis(int x, int y, Cell[][] cells) {
        int[] fwd = {x + huntDirection[0],  y + huntDirection[1]};
        int[] bwd = {x - huntDirection[0],  y - huntDirection[1]};

        offerIfValid(fwd[0], fwd[1], cells);
        offerIfValid(bwd[0], bwd[1], cells);
    }

    /**
     * Enqueue 4 hướng kề (up/down/left/right) của ô (x, y).
     * Bỏ qua ô ngoài biên, đã bắn, hoặc đã có trong queue.
     */
    private void enqueueNeighbors(int x, int y, Cell[][] cells) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            offerIfValid(x + d[0], y + d[1], cells);
        }
    }

    private void offerIfValid(int x, int y, Cell[][] cells) {
        if (isInBounds(x, y) && !cells[x][y].isHit() && !isQueued(x, y)) {
            huntQueue.offer(new int[]{x, y});
        }
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    private boolean isQueued(int x, int y) {
        return huntQueue.stream().anyMatch(p -> p[0] == x && p[1] == y);
    }
}