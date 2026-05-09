package service;

public class ScoreService {
    private static final int BASE_SCORE = 1000;

    public int calculateScore(int totalShots, int durationSeconds) {
        int shotPenalty = totalShots * 5;
        int timePenalty = durationSeconds / 10;
        int score = BASE_SCORE - shotPenalty - timePenalty;
        return Math.max(score, 50);
    }
}
