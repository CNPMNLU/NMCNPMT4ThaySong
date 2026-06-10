package service;

import model.*;
import service.ai.*;

import java.io.Serializable;

public class AIService implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String AI_USER_ID = "AI_PLAYER";
    public static final String AI_USERNAME = "AI";

    private final AIStrategy strategy;

    public AIService(String difficulty) {
        if ("Hard".equalsIgnoreCase(difficulty)) {
            strategy = new HardAIStrategy();
        } else {
            strategy = new EasyAIStrategy();
        }
    }

    public int[] selectTarget(Board opponentBoard) {
        return strategy.selectTarget(opponentBoard);
    }

    public void notifyResult(int x, int y, boolean hit, boolean sunk, Board opponentBoard) {
        strategy.onShotResult(x, y, hit, sunk, opponentBoard);
    }

    public void reset() {
        strategy.reset();
    }
}
