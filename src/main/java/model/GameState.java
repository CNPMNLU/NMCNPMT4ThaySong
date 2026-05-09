package model;

import java.time.LocalDateTime;

public class GameState {
    private String id;
    private String roomId;
    private String currentTurnId;
    private String status; // "ongoing" | "finished"
    private String winnerId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int totalTurns;
    private String mode;     // PvP | PvE
    private String difficulty; // Easy | Hard

    public GameState() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCurrentTurnId() { return currentTurnId; }
    public void setCurrentTurnId(String currentTurnId) { this.currentTurnId = currentTurnId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public int getTotalTurns() { return totalTurns; }
    public void setTotalTurns(int totalTurns) { this.totalTurns = totalTurns; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
