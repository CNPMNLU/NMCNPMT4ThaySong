package model;

import java.time.LocalDateTime;

public class GameRecord {
    private String id;
    private String roomId;
    private String player1Id;
    private String player2Id;
    private String winnerId;        // kept for backward-compat (may be null)
    private String mode;
    private int player1Score;
    private int player2Score;
    private int totalShots;
    private int durationSeconds;
    private LocalDateTime playedAt;

    // Từ JOIN users (nếu có tài khoản)
    private String player1Username;
    private String player2Username;
    private String winnerUsername;

    // Tên lưu trực tiếp trong game_records (dùng cho offline PvP & AI)
    private String player1Name;
    private String player2Name;
    private String winnerName;

    public GameRecord() {}

    // --- getters / setters ---
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

    public String getPlayer1Name() { return player1Name; }
    public void setPlayer1Name(String player1Name) { this.player1Name = player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public void setPlayer2Name(String player2Name) { this.player2Name = player2Name; }
    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }

    /** Trả về tên hiển thị player1 (ưu tiên cột name lưu sẵn, fallback username) */
    public String getDisplayPlayer1() {
        if (player1Name != null && !player1Name.isEmpty()) return player1Name;
        if (player1Username != null) return player1Username;
        return "Player 1";
    }
    /** Trả về tên hiển thị player2 */
    public String getDisplayPlayer2() {
        if (player2Name != null && !player2Name.isEmpty()) return player2Name;
        if (player2Username != null) return player2Username;
        return "PvE".equals(mode) ? "AI" : "Player 2";
    }
    /** Trả về tên hiển thị người thắng */
    public String getDisplayWinner() {
        if (winnerName != null && !winnerName.isEmpty()) return winnerName;
        if (winnerUsername != null) return winnerUsername;
        return "—";
    }
}
