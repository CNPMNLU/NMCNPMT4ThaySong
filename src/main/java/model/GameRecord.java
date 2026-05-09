package model;

import java.time.LocalDateTime;

public class GameRecord {
    private String id;
    private String roomId;
    private String player1Id;
    private String player2Id;
    private String winnerId;
    private String mode;
    private int player1Score;
    private int player2Score;
    private int totalShots;
    private int durationSeconds;
    private LocalDateTime playedAt;
    private String player1Username;
    private String player2Username;
    private String winnerUsername;

    public GameRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }
    public String getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public int getPlayer1Score() { return player1Score; }
    public void setPlayer1Score(int player1Score) { this.player1Score = player1Score; }
    public int getPlayer2Score() { return player2Score; }
    public void setPlayer2Score(int player2Score) { this.player2Score = player2Score; }
    public int getTotalShots() { return totalShots; }
    public void setTotalShots(int totalShots) { this.totalShots = totalShots; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
    public String getPlayer1Username() { return player1Username; }
    public void setPlayer1Username(String player1Username) { this.player1Username = player1Username; }
    public String getPlayer2Username() { return player2Username; }
    public void setPlayer2Username(String player2Username) { this.player2Username = player2Username; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
}
