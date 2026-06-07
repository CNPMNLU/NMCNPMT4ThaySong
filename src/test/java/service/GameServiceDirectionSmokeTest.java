package service;

import model.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test COMMIT-06 (smoke): Direction enum không làm hỏng checkSunk() trong GameService.
 *
 * Commit: test(service): smoke-test Direction enum integration with GameService.checkSunk
 *
 * Phạm vi (UC08 — thành viên D):
 *   D tạo Direction enum → cần verify enum không làm hỏng logic của người khác.
 *   Chỉ test các path liên quan đến Direction, KHÔNG test toàn bộ GameService
 *   (đó là trách nhiệm của người implement GameService).
 *
 * Căn cứ GameService.java thực tế:
 *   checkSunk(): Direction dir = ship.getDirection() — enum, không null
 *   cx = (dir == Direction.H) ? startX+i : startX
 *   cy = (dir == Direction.V) ? startY+i : startY
 */
@DisplayName("COMMIT-06 smoke — Direction enum trong GameService.checkSunk()")
class GameServiceDirectionSmokeTest {

    private GameService gs;
    private GameState   gameState;

    @BeforeEach
    void setUp() {
        gs = new GameService();
        gameState = new GameState();
        gameState.setId(UUID.randomUUID().toString());
        gameState.setRoomId("room-1");
        gameState.setCurrentTurnId("p1");
        gameState.setStatus("ongoing");
        gameState.setTotalTurns(0);
    }

    @Test @DisplayName("Direction.H — bắn đủ tất cả ô theo trục x → SUNK")
    void horizontal_allHit_sunk() {
        Board board = makeBoard(Direction.H, 2, 3, 3); // tàu dài 3: (2,3),(3,3),(4,3)
        addDummyShipToBoard(board);

        fireAt(board, 2, 3);
        fireAt(board, 3, 3);
        ShotResult r = fireAt(board, 4, 3);
        assertEquals(ShotResult.ResultType.SUNK, r.getResult());
    }

    @Test @DisplayName("Direction.V — bắn đủ tất cả ô theo trục y → SUNK")
    void vertical_allHit_sunk() {
        Board board = makeBoard(Direction.V, 5, 1, 3); // tàu dài 3: (5,1),(5,2),(5,3)
        addDummyShipToBoard(board);

        fireAt(board, 5, 1);
        fireAt(board, 5, 2);
        ShotResult r = fireAt(board, 5, 3);
        assertEquals(ShotResult.ResultType.SUNK, r.getResult());
    }

    @Test @DisplayName("Direction.H — chỉ bắn 1 ô → HIT chưa SUNK")
    void horizontal_partialHit_notSunk() {
        Board board = makeBoard(Direction.H, 0, 0, 2);
        ShotResult r = fireAt(board, 0, 0);
        assertEquals(ShotResult.ResultType.HIT, r.getResult());
    }

    @Test @DisplayName("Direction.V — chỉ bắn 1 ô → HIT chưa SUNK")
    void vertical_partialHit_notSunk() {
        Board board = makeBoard(Direction.V, 0, 0, 2);
        ShotResult r = fireAt(board, 0, 0);
        assertEquals(ShotResult.ResultType.HIT, r.getResult());
    }

    @Test @DisplayName("Ship direction null → fallback H qua getDirection(), không throw")
    void nullDirection_fallbackH_noThrow() {
        Board board = makeBoard(Direction.H, 1, 1, 2); // (1,1),(2,1)
        board.getShips().get(0).setDirectionFromString(null); // direction → null → fallback H
        assertDoesNotThrow(() -> {
            fireAt(board, 1, 1);
            fireAt(board, 2, 1);
        });
    }

    @Test @DisplayName("Tàu cuối bị chìm → GAME_OVER")
    void lastShip_sunk_gameOver() {
        Board board = makeBoard(Direction.H, 0, 0, 2);
        fireAt(board, 0, 0);
        ShotResult r = fireAt(board, 1, 0);
        assertEquals(ShotResult.ResultType.GAME_OVER, r.getResult());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Board 10x10 với 1 tàu chính theo direction cho trước và 1 tàu phụ để tránh GAME_OVER. */
    private Board makeBoard(Direction dir, int sx, int sy, int length) {
        Board board = new Board();
        board.setId(UUID.randomUUID().toString());
        Cell[][] cells = new Cell[10][10];
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++)
                cells[x][y] = new Cell("c"+x+y, board.getId(), x, y);

        Ship ship = new Ship();
        ship.setId("ship-1");
        ship.setBoardId(board.getId());
        ship.setLength(length);
        ship.setStartX(sx);
        ship.setStartY(sy);
        ship.setDirection(dir);

        for (int i = 0; i < length; i++) {
            int cx = (dir == Direction.H) ? sx + i : sx;
            int cy = (dir == Direction.V) ? sy + i : sy;
            cells[cx][cy].setHasShip(true);
            cells[cx][cy].setShipId(ship.getId());
        }

        board.setCells(cells);
        board.setShips(new ArrayList<>(List.of(ship)));
        return board;
    }

    private void addDummyShipToBoard(Board board) {
        Ship dummyShip = new Ship();
        dummyShip.setId("ship-dummy");
        dummyShip.setBoardId(board.getId());
        dummyShip.setLength(2); // Thỏa mãn độ dài tối thiểu >= 2
        dummyShip.setStartX(8);
        dummyShip.setStartY(9);
        dummyShip.setDirection(Direction.H);

        Cell[][] cells = board.getCells();
        cells[8][9].setHasShip(true);
        cells[8][9].setShipId(dummyShip.getId());
        cells[9][9].setHasShip(true);
        cells[9][9].setShipId(dummyShip.getId());

        board.getShips().add(dummyShip);
    }

    private ShotResult fireAt(Board board, int x, int y) {
        return gs.fireShot(board, gameState, "shooter", x, y);
    }
}