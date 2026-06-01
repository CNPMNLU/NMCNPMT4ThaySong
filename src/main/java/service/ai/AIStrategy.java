package service.ai;

import model.Board;

import java.io.Serializable;

public interface AIStrategy extends Serializable {
    int[] selectTarget(Board opponentBoard);

    default void onShotResult(int x, int y, boolean hit, boolean sunk, Board opponentBoard) {
        //Easy AI: không cần xử lý gì — bắn random không phụ thuộc lịch sử hit/miss
    }

    default void reset() {
        //Easy AI: không có state nội bộ cần reset
    }
}