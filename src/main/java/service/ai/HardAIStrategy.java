package service.ai;

import model.Board;
import model.Cell;
import java.util.*;

public class HardAIStrategy implements AIStrategy {
    private final Random rand = new Random();
    private final List<int[]> huntQueue = new ArrayList<>();
    private int[] lastHit = null;

    @Override
    public int[] selectTarget(Board opponentBoard) {
        Cell[][] cells = opponentBoard.getCells();

        // Remove already-hit positions from hunt queue
        huntQueue.removeIf(pos -> cells[pos[0]][pos[1]].isHit());

        if (!huntQueue.isEmpty()) {
            return huntQueue.remove(0);
        }

        // Random mode
        List<int[]> available = new ArrayList<>();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                if (!cells[x][y].isHit())
                    available.add(new int[]{x, y});
        if (available.isEmpty()) return null;
        return available.get(rand.nextInt(available.size()));
    }

    public void onHit(int x, int y, boolean sunk, Board opponentBoard) {
        Cell[][] cells = opponentBoard.getCells();
        if (sunk) {
            huntQueue.clear();
            lastHit = null;
            return;
        }
        lastHit = new int[]{x, y};
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10 && !cells[nx][ny].isHit()) {
                boolean alreadyQueued = huntQueue.stream().anyMatch(p -> p[0]==nx && p[1]==ny);
                if (!alreadyQueued) huntQueue.add(0, new int[]{nx, ny});
            }
        }
    }
}
