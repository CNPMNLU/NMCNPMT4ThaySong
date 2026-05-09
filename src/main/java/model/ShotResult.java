package model;

import java.time.LocalDateTime;

public class ShotResult {
    public enum ResultType { HIT, MISS, SUNK, GAME_OVER }

    private String id;
    private String gameStateId;
    private String shooterId;
    private String targetBoardId;
    private int x, y;
    private ResultType result;
    private String shipId;
    private int turnNumber;
    private LocalDateTime shotAt;

    public ShotResult() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGameStateId() { return gameStateId; }
    public void setGameStateId(String gameStateId) { this.gameStateId = gameStateId; }
    public String getShooterId() { return shooterId; }
    public void setShooterId(String shooterId) { this.shooterId = shooterId; }
    public String getTargetBoardId() { return targetBoardId; }
    public void setTargetBoardId(String targetBoardId) { this.targetBoardId = targetBoardId; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public ResultType getResult() { return result; }
    public void setResult(ResultType result) { this.result = result; }
    public String getShipId() { return shipId; }
    public void setShipId(String shipId) { this.shipId = shipId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public LocalDateTime getShotAt() { return shotAt; }
    public void setShotAt(LocalDateTime shotAt) { this.shotAt = shotAt; }
}
