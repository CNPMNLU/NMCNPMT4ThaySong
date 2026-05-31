package service.ai;

import model.Board;
import model.Cell;

import java.util.*;

public class HardAIStrategy implements AIStrategy {
    private static final long serialVersionUID = 1L;
    private final Random rand = new Random();
    private final Deque<int[]> huntQueue = new ArrayDeque<>();
    private int[] lastHit = null;

    @Override
    public int[] selectTarget(Board opponentBoard) {
        Cell[][] cells = opponentBoard.getCells();

        // Remove already-hit positions from hunt queue
        huntQueue.removeIf(pos -> cells[pos[0]][pos[1]].isHit());

        if (!huntQueue.isEmpty()) {
            return huntQueue.poll(); //hunt
        }

        return randomUnhit(cells); //random
    }

    private int[] randomUnhit(Cell[][] cells) {
        List<int[]> available = new ArrayList<>();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                if (!cells[x][y].isHit())
                    available.add(new int[]{x, y});
        if (available.isEmpty()) return null;
        return available.get(rand.nextInt(available.size()));
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    /**
     * Kiểm tra ô (x,y) đã có trong huntQueue chưa để tránh enqueue trùng.
     * Dùng stream vì queue nhỏ (tối đa 4 × số tàu ≈ 20 phần tử).
     */
    private boolean isQueued(int x, int y) {
        return huntQueue.stream().anyMatch(p -> p[0] == x && p[1] == y);
    }

    @Override
    public void onShotResult(int x, int y, boolean hit, boolean sunk, Board opponentBoard) {
        if (!hit) return;

        if (sunk) {
            //tàu chìm hoàn toàn — xóa queue, bắt đầu tìm tàu mới
            huntQueue.clear();
            lastHit = null;
            return;
        }

        //trúng nhưng chưa chìm → thêm 4 hướng kề vào queue
        lastHit = new int[]{x, y};
        Cell[][] cells = opponentBoard.getCells();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (isInBounds(nx, ny) && !cells[nx][ny].isHit() && !isQueued(nx, ny)) {
                huntQueue.offer(new int[]{nx, ny});
            }
        }
    }

    @Override
    public void reset() {
        huntQueue.clear();
        lastHit = null;
    }
}
