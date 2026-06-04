package service.ai;

import model.Board;
import model.Cell;

import java.util.*;

/**
 * Chiến lược AI Hard: thuật toán Hunt-Target 3 pha.
 *
 * class này chỉ còn trách nhiệm điều phối:
 *   - Quyết định dùng ô từ HuntTargetState hay random
 *   - Chuyển kết quả bắn xuống HuntTargetState để cập nhật state
 *
 * Toàn bộ state (huntQueue, lastHit, huntDirection) nằm trong
 * HuntTargetState — có thể test độc lập mà không cần khởi tạo strategy.
 *
 * Ba pha hoạt động (được quản lý hoàn toàn bởi HuntTargetState):
 *
 * RANDOM ──────── trúng ──────►  HUNT ───── trúng lần 2 cùng trục ──────► TARGET ──────── sunk ──────► RANDOM
 */

public class HardAIStrategy implements AIStrategy {
    private static final long serialVersionUID = 1L;
    private final Random rand = new Random();

    /**
     * Toàn bộ state Hunt-Target được delegate sang class riêng.
     * HardAIStrategy không giữ lastHit, huntDirection, hay huntQueue trực tiếp.
     */
    private final HuntTargetState huntState = new HuntTargetState();

    /**
     * Chọn ô tiếp theo để bắn.
     *
     * Thứ tự ưu tiên:
     * 1. Ô trong huntQueue (pha HUNT hoặc TARGET) — bắn có chiến lược
     * 2. Ô ngẫu nhiên chưa bắn (pha RANDOM)
     *
     * @return int[]{x, y} hoặc null nếu board đã đầy (không còn ô nào)
     */
    @Override
    public int[] selectTarget(Board opponentBoard) {
        Cell[][] cells = opponentBoard.getCells();

        // Loại bỏ ô trong queue đã bị bắn trước khi poll
        huntState.pruneHitCells(cells);

        if (huntState.hasTargets()) {
            return huntState.poll(); // pha HUNT hoặc TARGET
        }

        return randomUnit(cells); // pha RANDOM
    }

    /**
     * Chọn ngẫu nhiên một ô chưa bắn trên board.
     * Trả về null nếu toàn bộ board đã được bắn (board full — không bình thường).
     */
    private int[] randomUnit(Cell[][] cells) {
        List<int[]> available = new ArrayList<>();
        for (int x = 0; x < cells.length; x++)
            for (int y = 0; y < cells[x].length; y++)
                if (!cells[x][y].isHit())
                    available.add(new int[]{x, y});

        if (available.isEmpty()) return null;
        return available.get(rand.nextInt(available.size()));
    }

    /**
     * Nhận phản hồi kết quả bắn và cập nhật trạng thái hunt.
     *
     * Các trường hợp:
     *   - miss (hit=false)          : không làm gì, tiếp tục random
     *   - hit + sunk (hit+sunk=true): tàu chìm → reset hunt, tìm tàu mới
     *   - hit + !sunk               : trúng nhưng chưa chìm → thêm ô kề vào queue
     *
     * Không được gọi khi result = ALREADY_HIT (guard ở AIService / GameServlet).
     */
    @Override
    public void onShotResult(int x, int y, boolean hit, boolean sunk, Board opponentBoard) {
        if (!hit) return;

        if (sunk) {
            huntState.recordSunk();
        } else {
            huntState.recordHit(x, y, opponentBoard.getCells());
        }
    }

    /**
     * Reset toàn bộ state về pha RANDOM.
     * Gọi từ AIService.reset() khi bắt đầu game mới.
     */
    @Override
    public void reset() {
        huntState.reset();
    }
}
