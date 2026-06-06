package model;

/**
 * KhoaDang: direction field đổi từ String sang enum Direction.
 *
 * Thay đổi:
 *   - private String direction  →  private Direction direction
 *   - setDirection(String)      →  setDirection(Direction)
 *   - Thêm setDirectionFromString(String) để các chỗ đọc DB/JSON không cần sửa ngay
 *   - getDirection() trả Direction, getDirectionCode() trả "H"/"V" cho DB/JSON
 *
 * Các chỗ còn dùng String direction phải migrate dần sang Direction.
 * BoardService.placeShip(), checkSunk() trong GameService đã được cập nhật
 * cùng commit này.
 */

public class Ship {
    private String id;
    private String boardId;
    private String type;
    private int length;
    private int startX, startY;
    private Direction direction; // "H" or "V"
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
    public boolean isSunk() { return sunk; }
    public void setSunk(boolean sunk) { this.sunk = sunk; }
    /** Trả về enum Direction — dùng trong logic Java. */
    public Direction getDirection() { return direction != null ? direction : Direction.H; }

    /** Setter nhận enum — dùng trong code Java mới. */
    public void setDirection(Direction direction) { this.direction = direction; }



    /**
     * Setter nhận String "H"/"V" — dùng khi đọc từ DB, JSON, hoặc
     * các chỗ cũ chưa migrate. Tương thích ngược với code hiện có.
     */
    public void setDirectionFromString(String s) { this.direction = Direction.fromString(s); }

    /**
     * Trả về "H" hoặc "V" — dùng khi ghi DB hoặc serialize JSON.
     * Thay thế getDirection() ở các chỗ cần String.
     */
    public String getDirectionCode() {
        return getDirection().toCode();
    }
}
