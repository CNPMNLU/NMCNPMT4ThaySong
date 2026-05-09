package dao;

import model.GameRecord;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryDAO {

    public void insert(GameRecord record) throws SQLException {
        String sql = "INSERT INTO game_records (id,room_id,player1_id,player2_id,winner_id,mode,player1_score,player2_score,total_shots,duration_seconds,played_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getRoomId());
            ps.setString(3, record.getPlayer1Id());
            ps.setString(4, record.getPlayer2Id());
            ps.setString(5, record.getWinnerId());
            ps.setString(6, record.getMode());
            ps.setInt(7, record.getPlayer1Score());
            ps.setInt(8, record.getPlayer2Score());
            ps.setInt(9, record.getTotalShots());
            ps.setInt(10, record.getDurationSeconds());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public List<GameRecord> findByUserId(String userId) throws SQLException {
        String sql = "SELECT gr.*, u1.username as u1name, u2.username as u2name, uw.username as uwname " +
                     "FROM game_records gr " +
                     "JOIN users u1 ON gr.player1_id=u1.id " +
                     "LEFT JOIN users u2 ON gr.player2_id=u2.id " +
                     "JOIN users uw ON gr.winner_id=uw.id " +
                     "WHERE gr.player1_id=? OR gr.player2_id=? ORDER BY gr.played_at DESC";
        List<GameRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private GameRecord map(ResultSet rs) throws SQLException {
        GameRecord r = new GameRecord();
        r.setId(rs.getString("id"));
        r.setRoomId(rs.getString("room_id"));
        r.setPlayer1Id(rs.getString("player1_id"));
        r.setPlayer2Id(rs.getString("player2_id"));
        r.setWinnerId(rs.getString("winner_id"));
        r.setMode(rs.getString("mode"));
        r.setPlayer1Score(rs.getInt("player1_score"));
        r.setPlayer2Score(rs.getInt("player2_score"));
        r.setTotalShots(rs.getInt("total_shots"));
        r.setDurationSeconds(rs.getInt("duration_seconds"));
        Timestamp pa = rs.getTimestamp("played_at");
        if (pa != null) r.setPlayedAt(pa.toLocalDateTime());
        r.setPlayer1Username(rs.getString("u1name"));
        r.setPlayer2Username(rs.getString("u2name"));
        r.setWinnerUsername(rs.getString("uwname"));
        return r;
    }
}
