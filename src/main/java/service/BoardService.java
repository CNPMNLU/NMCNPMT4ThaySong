package service;

import jakarta.servlet.http.HttpSession;
import model.*;
import java.sql.*;
import java.util.*;
import dao.DBConnection;

public class BoardService {

    public static final int[][] SHIP_CONFIGS = {
        {5}, {4}, {3}, {3}, {2}
    };
    public static final String[] SHIP_TYPES = {"Carrier","Battleship","Cruiser","Submarine","Destroyer"};

    public Board createBoard(String boardId, String roomId, String ownerId) {
        Board board = new Board();
        board.setId(boardId);
        board.setRoomId(roomId);
        board.setOwnerId(ownerId);
        Cell[][] cells = new Cell[10][10];
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++) {
                Cell cell = new Cell(UUID.randomUUID().toString(), boardId, x, y);
                cells[x][y] = cell;
            }
        board.setCells(cells);
        return board;
    }

    public boolean placeShip(Board board, Ship ship) {
        if (!isValidPlacement(board, ship)) return false;
        board.getShips().add(ship);
        Cell[][] cells = board.getCells();
        for (int i = 0; i < ship.getLength(); i++) {
            int cx = ship.getDirection().equals("H") ? ship.getStartX() + i : ship.getStartX();
            int cy = ship.getDirection().equals("V") ? ship.getStartY() + i : ship.getStartY();
            cells[cx][cy].setHasShip(true);
            cells[cx][cy].setShipId(ship.getId());
        }
        return true;
    }

    public boolean isValidPlacement(Board board, Ship ship) {
        int x = ship.getStartX(), y = ship.getStartY(), len = ship.getLength();
        if ("H".equals(ship.getDirection())) {
            if (x + len > 10) return false;
        } else {
            if (y + len > 10) return false;
        }
        if (x < 0 || y < 0 || x >= 10 || y >= 10) return false;
        Cell[][] cells = board.getCells();
        for (int i = 0; i < len; i++) {
            int cx = "H".equals(ship.getDirection()) ? x + i : x;
            int cy = "V".equals(ship.getDirection()) ? y + i : y;
            if (cells[cx][cy].isHasShip()) return false;
        }
        return true;
    }

    public void autoPlace(Board board) {
        board.getShips().clear();
        Cell[][] cells = board.getCells();
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 10; y++) {
                cells[x][y].setHasShip(false);
                cells[x][y].setShipId(null);
            }
        Random rand = new Random();
        for (int i = 0; i < SHIP_TYPES.length; i++) {
            int len = SHIP_CONFIGS[i][0];
            boolean placed = false;
            while (!placed) {
                int x = rand.nextInt(10);
                int y = rand.nextInt(10);
                String dir = rand.nextBoolean() ? "H" : "V";
                Ship ship = new Ship();
                ship.setId(UUID.randomUUID().toString());
                ship.setBoardId(board.getId());
                ship.setType(SHIP_TYPES[i]);
                ship.setLength(len);
                ship.setStartX(x);
                ship.setStartY(y);
                ship.setDirection(dir);
                placed = placeShip(board, ship);
            }
        }
    }

    public void saveBoardToDB(Board board) throws Exception {
        // No-op: DB not required for gameplay
        if (true) return;
        try (Connection c = DBConnection.getConnection()) {
            String bsql = "INSERT INTO boards (id,room_id,owner_id,is_ready) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE is_ready=?";
            PreparedStatement bps = c.prepareStatement(bsql);
            bps.setString(1, board.getId());
            bps.setString(2, board.getRoomId());
            bps.setString(3, board.getOwnerId());
            bps.setBoolean(4, board.isReady());
            bps.setBoolean(5, board.isReady());
            bps.executeUpdate();

            // Delete existing ships/cells for this board
            c.prepareStatement("DELETE FROM cells WHERE board_id='" + board.getId() + "'").executeUpdate();
            c.prepareStatement("DELETE FROM ships WHERE board_id='" + board.getId() + "'").executeUpdate();

            String ssql = "INSERT INTO ships (id,board_id,type,length,start_x,start_y,direction,is_sunk) VALUES (?,?,?,?,?,?,?,?)";
            PreparedStatement sps = c.prepareStatement(ssql);
            for (Ship ship : board.getShips()) {
                sps.setString(1, ship.getId());
                sps.setString(2, ship.getBoardId());
                sps.setString(3, ship.getType());
                sps.setInt(4, ship.getLength());
                sps.setInt(5, ship.getStartX());
                sps.setInt(6, ship.getStartY());
                sps.setString(7, ship.getDirection());
                sps.setBoolean(8, ship.isSunk());
                sps.addBatch();
            }
            sps.executeBatch();

            String csql = "INSERT INTO cells (id,board_id,x,y,has_ship,ship_id,is_hit) VALUES (?,?,?,?,?,?,?)";
            PreparedStatement cps = c.prepareStatement(csql);
            Cell[][] cells = board.getCells();
            for (int x = 0; x < 10; x++) for (int y = 0; y < 10; y++) {
                Cell cell = cells[x][y];
                cps.setString(1, cell.getId());
                cps.setString(2, cell.getBoardId());
                cps.setInt(3, cell.getX());
                cps.setInt(4, cell.getY());
                cps.setBoolean(5, cell.isHasShip());
                cps.setString(6, cell.getShipId());
                cps.setBoolean(7, cell.isHit());
                cps.addBatch();
            }
            cps.executeBatch();
        }
    }

    public Board loadBoardFromDB(String boardId) throws SQLException {
        Board board = new Board();
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement bps = c.prepareStatement("SELECT * FROM boards WHERE id=?");
            bps.setString(1, boardId);
            ResultSet brs = bps.executeQuery();
            if (brs.next()) {
                board.setId(brs.getString("id"));
                board.setRoomId(brs.getString("room_id"));
                board.setOwnerId(brs.getString("owner_id"));
                board.setReady(brs.getBoolean("is_ready"));
            }

            PreparedStatement sps = c.prepareStatement("SELECT * FROM ships WHERE board_id=?");
            sps.setString(1, boardId);
            ResultSet srs = sps.executeQuery();
            List<Ship> ships = new ArrayList<>();
            while (srs.next()) {
                Ship ship = new Ship();
                ship.setId(srs.getString("id"));
                ship.setBoardId(srs.getString("board_id"));
                ship.setType(srs.getString("type"));
                ship.setLength(srs.getInt("length"));
                ship.setStartX(srs.getInt("start_x"));
                ship.setStartY(srs.getInt("start_y"));
                ship.setDirection(srs.getString("direction"));
                ship.setSunk(srs.getBoolean("is_sunk"));
                ships.add(ship);
            }
            board.setShips(ships);

            Cell[][] cells = new Cell[10][10];
            PreparedStatement cps = c.prepareStatement("SELECT * FROM cells WHERE board_id=?");
            cps.setString(1, boardId);
            ResultSet crs = cps.executeQuery();
            while (crs.next()) {
                Cell cell = new Cell();
                cell.setId(crs.getString("id"));
                cell.setBoardId(crs.getString("board_id"));
                cell.setX(crs.getInt("x"));
                cell.setY(crs.getInt("y"));
                cell.setHasShip(crs.getBoolean("has_ship"));
                cell.setShipId(crs.getString("ship_id"));
                cell.setHit(crs.getBoolean("is_hit"));
                cells[cell.getX()][cell.getY()] = cell;
            }
            board.setCells(cells);
        }
        return board;
    }

    public String getBoardIdByRoomAndOwner(String roomId, String ownerId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT id FROM boards WHERE room_id=? AND owner_id=?");
            ps.setString(1, roomId);
            ps.setString(2, ownerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id");
        }
        return null;
    }

    public Board getBoardByRoomAndOwner(HttpSession session, String roomId, String ownerId) {
        //trong lưu bộ nhớ, thường lưu board theo key động trong Session
        //ví dụ: "board_PLAYER123" hoặc lưu trong một Map phòng đấu
        String sessionKey = "board_" + ownerId;
        Board board = (Board) session.getAttribute(sessionKey);

        if (board == null) {
            Board setupBoard = (Board) session.getAttribute("board");
            if (setupBoard != null) {
                board = setupBoard;
                board.setRoomId(roomId);
                board.setOwnerId(ownerId);
            }else {
                //chưa có board nào trong session (trận mới khởi tạo), tạo mới 10x10
                //sinh một ID ngẫu nhiên cho Board
                String generatedBoardId = java.util.UUID.randomUUID().toString();
                board = createBoard(generatedBoardId, roomId, ownerId);
            }
            //đẩy ngược lại vào session để lưu trữ cho các lượt sau
            session.setAttribute(sessionKey, board);
        }

        return board;
    }
}
