package model;

public class Cell {
    private String id;
    private String boardId;
    private int x, y;
    private boolean hasShip;
    private String shipId;
    private boolean isHit;

    public Cell() {}
    public Cell(String id, String boardId, int x, int y) {
        this.id = id; this.boardId = boardId; this.x = x; this.y = y;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public boolean isHasShip() { return hasShip; }
    public void setHasShip(boolean hasShip) { this.hasShip = hasShip; }
    public String getShipId() { return shipId; }
    public void setShipId(String shipId) { this.shipId = shipId; }
    public boolean isHit() { return isHit; }
    public void setHit(boolean hit) { isHit = hit; }
}
