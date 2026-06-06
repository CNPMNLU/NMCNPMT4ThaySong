package controller;

import model.ShotResult;

/**
 * Kết quả trả về từ AIController.executeTurn().
 *
 * GameServlet nhận object này thay vì nhận thẳng JsonObject —
 * tách việc "tính toán lượt AI" khỏi việc "serialize ra HTTP response".
 *
 * Các field được đặt final và chỉ set qua constructor để đảm bảo
 * object luôn ở trạng thái hợp lệ sau khi tạo.
 */
public class AITurnResult {

    /** Tọa độ AI vừa bắn. */
    public final int x;
    public final int y;

    /** Kết quả lượt bắn của AI. */
    public final ShotResult.ResultType resultType;

    /**
     * true nếu AI thắng trận này (resultType == GAME_OVER).
     * GameServlet dùng flag này để trả {"gameOver":true} về frontend.
     */
    public final boolean aiWon;

    /**
     * Điểm của AI (luôn 0 — AI không có leaderboard score riêng).
     * Giữ field này để response JSON nhất quán với lượt player thắng.
     */
    public final int score;

    public AITurnResult(int x, int y, ShotResult.ResultType resultType, boolean aiWon, int score) {
        this.x          = x;
        this.y          = y;
        this.resultType = resultType;
        this.aiWon      = aiWon;
        this.score      = score;
    }

    /** Shorthand kiểm tra lượt bắn có trúng không (HIT / SUNK / GAME_OVER). */
    public boolean isHit() {
        return resultType == ShotResult.ResultType.HIT
                || resultType == ShotResult.ResultType.SUNK
                || resultType == ShotResult.ResultType.GAME_OVER;
    }

    /** Shorthand kiểm tra tàu bị chìm (SUNK / GAME_OVER). */
    public boolean isSunk() {
        return resultType == ShotResult.ResultType.SUNK
                || resultType == ShotResult.ResultType.GAME_OVER;
    }
}