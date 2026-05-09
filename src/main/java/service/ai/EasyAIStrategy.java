package service.ai;

import model.Board;
import model.Cell;
import java.util.*;

public class EasyAIStrategy implements AIStrategy {
    private final Random rand = new Random();

    @Override
    public int[] selectTarget(Board opponentBoard) {
        List<int[]> available = new ArrayList<>();
        Cell[][] cells = opponentBoard.getCells();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                if (!cells[x][y].isHit())
                    available.add(new int[]{x, y});
        if (available.isEmpty()) return null;
        return available.get(rand.nextInt(available.size()));
    }
}
