package service;

import model.*;
import service.ai.*;

public class AIService {
    public static final String AI_USER_ID = "AI_PLAYER";
    public static final String AI_USERNAME = "AI";

    private AIStrategy strategy;
    private HardAIStrategy hardStrategy;

    public AIService(String difficulty) {
        if ("Hard".equalsIgnoreCase(difficulty)) {
            hardStrategy = new HardAIStrategy();
            strategy = hardStrategy;
        } else {
            strategy = new EasyAIStrategy();
        }
    }

    public int[] selectTarget(Board opponentBoard) {
        return strategy.selectTarget(opponentBoard);
    }

    public void notifyResult(int x, int y, boolean hit, boolean sunk, Board opponentBoard) {
        if (hardStrategy != null && hit) {
            hardStrategy.onHit(x, y, sunk, opponentBoard);
        }
    }
}
