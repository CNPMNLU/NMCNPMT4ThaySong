package service;

import model.Board;
import model.Direction;
import model.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BoardServiceTest {

    private BoardService boardService;
    private Board board;

    @BeforeEach
    public void setUp() {
        boardService = new BoardService();
        board = boardService.createBoard(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "player_test");
    }

    @Test
    public void testValidPlacementHorizontal() {
        Ship ship = new Ship();
        ship.setId(UUID.randomUUID().toString());
        ship.setBoardId(board.getId());
        ship.setType("Carrier");
        ship.setLength(5);
        ship.setStartX(0);
        ship.setStartY(0);
        ship.setDirection(Direction.H);

        assertTrue(boardService.isValidPlacement(board, ship), "Carrier horizontal at (0,0) should be valid");
        assertTrue(boardService.placeShip(board, ship), "Carrier placement should succeed");
        assertEquals(1, board.getShips().size());
        assertTrue(board.getCells()[0][0].isHasShip());
        assertTrue(board.getCells()[4][0].isHasShip());
        assertFalse(board.getCells()[5][0].isHasShip());
    }

    @Test
    public void testValidPlacementVertical() {
        Ship ship = new Ship();
        ship.setId(UUID.randomUUID().toString());
        ship.setBoardId(board.getId());
        ship.setType("Battleship");
        ship.setLength(4);
        ship.setStartX(5);
        ship.setStartY(5);
        ship.setDirection(Direction.V);

        assertTrue(boardService.isValidPlacement(board, ship), "Battleship vertical at (5,5) should be valid");
        assertTrue(boardService.placeShip(board, ship), "Battleship placement should succeed");
        assertEquals(1, board.getShips().size());
        assertTrue(board.getCells()[5][5].isHasShip());
        assertTrue(board.getCells()[5][8].isHasShip());
        assertFalse(board.getCells()[5][9].isHasShip());
    }

    @Test
    public void testOverlapRejected() {
        // First ship
        Ship ship1 = new Ship();
        ship1.setId(UUID.randomUUID().toString());
        ship1.setBoardId(board.getId());
        ship1.setType("Carrier");
        ship1.setLength(5);
        ship1.setStartX(2);
        ship1.setStartY(3);
        ship1.setDirection(Direction.H);
        assertTrue(boardService.placeShip(board, ship1));

        // Second ship attempting to overlap
        Ship ship2 = new Ship();
        ship2.setId(UUID.randomUUID().toString());
        ship2.setBoardId(board.getId());
        ship2.setType("Cruiser");
        ship2.setLength(3);
        ship2.setStartX(3);
        ship2.setStartY(1);
        ship2.setDirection(Direction.V); // crosses (3,3) which has ship1

        assertFalse(boardService.isValidPlacement(board, ship2), "Cruiser vertical crossing (3,3) should be invalid");
        assertFalse(boardService.placeShip(board, ship2), "Placement of overlapping Cruiser should fail");
        assertEquals(1, board.getShips().size(), "Only one ship should be present on the board");
    }

    @Test
    public void testOutOfBoundsHorizontalRejected() {
        Ship ship = new Ship();
        ship.setId(UUID.randomUUID().toString());
        ship.setBoardId(board.getId());
        ship.setType("Carrier");
        ship.setLength(5);
        ship.setStartX(7); // starts at 7, ends at 11 -> out of bounds
        ship.setStartY(2);
        ship.setDirection(Direction.H);

        assertFalse(boardService.isValidPlacement(board, ship), "Carrier starting at (7,2) H should extend out of bounds");
        assertFalse(boardService.placeShip(board, ship), "Placement extending out of bounds should fail");
    }

    @Test
    public void testOutOfBoundsVerticalRejected() {
        Ship ship = new Ship();
        ship.setId(UUID.randomUUID().toString());
        ship.setBoardId(board.getId());
        ship.setType("Battleship");
        ship.setLength(4);
        ship.setStartX(4);
        ship.setStartY(8); // starts at 8, ends at 11 -> out of bounds
        ship.setDirection(Direction.V);

        assertFalse(boardService.isValidPlacement(board, ship), "Battleship starting at (4,8) V should extend out of bounds");
        assertFalse(boardService.placeShip(board, ship), "Placement extending out of bounds should fail");
    }

    @Test
    public void testNegativeCoordinatesRejected() {
        Ship ship = new Ship();
        ship.setId(UUID.randomUUID().toString());
        ship.setBoardId(board.getId());
        ship.setType("Destroyer");
        ship.setLength(2);
        ship.setStartX(-1);
        ship.setStartY(5);
        ship.setDirection(Direction.H);

        assertFalse(boardService.isValidPlacement(board, ship), "Destroyer at negative start x should be rejected");
        assertFalse(boardService.placeShip(board, ship));
    }

    @Test
    public void testAutoPlaceNoOverlapAndValidFleet() {
        // Run autoPlace 100 times to guarantee there are never overlaps or invalid fleets
        for (int run = 0; run < 100; run++) {
            Board freshBoard = boardService.createBoard(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "player_auto");
            boardService.autoPlace(freshBoard);

            assertTrue(boardService.isValidFleet(freshBoard), "Auto-placed fleet should be fully valid");

            // Verify coordinates are clean and have exactly 17 ship cells total (5+4+3+3+2)
            int shipCellCount = 0;
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    if (freshBoard.getCells()[x][y].isHasShip()) {
                        shipCellCount++;
                    }
                }
            }
            assertEquals(17, shipCellCount, "Total ship cells must be exactly 17");
        }
    }
}
