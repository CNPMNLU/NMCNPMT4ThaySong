package model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private String id;
    private String roomId;
    private String ownerId;
    private boolean ready;
    private List<Ship> ships = new ArrayList<>();
    private Cell[][] cells = new Cell[10][10];

    public Board() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public List<Ship> getShips() { return ships; }
    public void setShips(List<Ship> ships) { this.ships = ships; }
    public Cell[][] getCells() { return cells; }
    public void setCells(Cell[][] cells) { this.cells = cells; }

    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }
}
