package dao;

import model.GameRecord;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryDAO {

    public void insert(GameRecord record) throws SQLException {
        String sql = "INSERT INTO game_records " +
                     "(id, room_id, player1_id, player2_id, player1_name, player2_name, winner_name, " +
                     " mode, player1_score, player2_score, total_shots, duration_seconds, played_at) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, record.getId());
            ps.setString(2, record.getRoomId());
            ps.setString(3, record.getPlayer1Id());

            // player2_id: NULL khi PvE hoặc PvP offline một tài khoản
            if (record.getPlayer2Id() != null) {
                ps.setString(4, record.getPlayer2Id());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }

            ps.setString(5, record.getPlayer1Name() != null ? record.getPlayer1Name() : "");
            ps.setString(6, record.getPlayer2Name() != null ? record.getPlayer2Name() : "AI");
            ps.setString(7, record.getWinnerName()  != null ? record.getWinnerName()  : "");
            ps.setString(8, record.getMode() != null ? record.getMode() : "PvE");
            ps.setInt(9,  record.getPlayer1Score());
            ps.setInt(10, record.getPlayer2Score());
            ps.setInt(11, record.getTotalShots());
            ps.setInt(12, record.getDurationSeconds());
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        }
    }

    /** Lấy toàn bộ lịch sử của một player (theo player1_id) */
    public List<GameRecord> findByUserId(String userId) throws SQLException {
        String sql = "SELECT gr.*, u1.username AS u1name, u2.username AS u2name " +
                     "FROM game_records gr " +
                     "  JOIN  users u1 ON gr.player1_id = u1.id " +
                     "  LEFT JOIN users u2 ON gr.player2_id = u2.id " +
                     "WHERE gr.player1_id = ? " +
                     "ORDER BY gr.played_at DESC";

        List<GameRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Lấy chi tiết 1 trận theo matchId */
    public GameRecord findById(String id) throws SQLException {
        String sql = "SELECT gr.*, u1.username AS u1name, u2.username AS u2name " +
                     "FROM game_records gr " +
                     "  JOIN  users u1 ON gr.player1_id = u1.id " +
                     "  LEFT JOIN users u2 ON gr.player2_id = u2.id " +
                     "WHERE gr.id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    private GameRecord map(ResultSet rs) throws SQLException {
        GameRecord r = new GameRecord();
        r.setId(rs.getString("id"));
        r.setRoomId(rs.getString("room_id"));
        r.setPlayer1Id(rs.getString("player1_id"));
        r.setPlayer2Id(rs.getString("player2_id"));
        r.setMode(rs.getString("mode"));
        r.setPlayer1Score(rs.getInt("player1_score"));
        r.setPlayer2Score(rs.getInt("player2_score"));
        r.setTotalShots(rs.getInt("total_shots"));
        r.setDurationSeconds(rs.getInt("duration_seconds"));
        Timestamp pa = rs.getTimestamp("played_at");
        if (pa != null) r.setPlayedAt(pa.toLocalDateTime());

        // Tên lưu trong bảng
        r.setPlayer1Name(rs.getString("player1_name"));
        r.setPlayer2Name(rs.getString("player2_name"));
        r.setWinnerName(rs.getString("winner_name"));

        // Username từ JOIN (nếu có)
        r.setPlayer1Username(rs.getString("u1name"));
        r.setPlayer2Username(rs.getString("u2name"));
        return r;
    }
}
