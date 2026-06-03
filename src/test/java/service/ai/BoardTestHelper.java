package service.ai;

import model.Board;
import model.Cell;

/**
 * Factory tạo Board giả lập cho unit test AI.
 * Không dùng database hay Spring context — tất cả in-memory.
 */
public class BoardTestHelper {

    /** Board 10×10 với tất cả Cell được khởi tạo (isHit = false). */
    public static Board emptyBoard() {
        Board board = new Board();
        Cell[][] cells = new Cell[10][10];
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++) {
                cells[x][y] = new Cell("c" + x + y, "board1", x, y);
            }
        board.setCells(cells);
        return board;
    }

    /** Đánh dấu ô (x, y) đã bị bắn (isHit = true). */
    public static void markHit(Board board, int x, int y) {
        board.getCells()[x][y].setHit(true);
    }

    /**
     * Đánh dấu toàn bộ board đã bị bắn trừ danh sách ngoại lệ.
     * Dùng để test trường hợp board gần đầy.
     */
    public static void fillAllExcept(Board board, int[][] exceptions) {
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                board.getCells()[x][y].setHit(true);
        for (int[] pos : exceptions)
            board.getCells()[pos[0]][pos[1]].setHit(false);
    }

    /** Kiểm tra ô (x, y) có trong mảng tọa độ không. */
    public static boolean containsPos(int[][] positions, int x, int y) {
        for (int[] p : positions)
            if (p[0] == x && p[1] == y) return true;
        return false;
    }
}