package model;

public class Ship {
    private String id;
    private String boardId;
    private String type;
    private int length;
    private int startX, startY;
    private String direction; // "H" or "V"
    private boolean sunk;

    public Ship() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    public int getStartX() { return startX; }
    public void setStartX(int startX) { this.startX = startX; }
    public int getStartY() { return startY; }
    public void setStartY(int startY) { this.startY = startY; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public boolean isSunk() { return sunk; }
    public void setSunk(boolean sunk) { this.sunk = sunk; }
}
