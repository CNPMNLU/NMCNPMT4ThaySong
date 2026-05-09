package service.ai;

import model.Board;

public interface AIStrategy {
    int[] selectTarget(Board opponentBoard);
}
