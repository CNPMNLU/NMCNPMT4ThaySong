package dao;

import model.GameRecord;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // FILE: src/main/java/dao/GameHistoryDAO.java
// APPEND vào cuối class (trước dấu })

    /**
     * Lấy lịch sử theo PHÂN TRANG (Page dùng cho UI)
     * Page bắt đầu từ 1, mỗi trang 10 trận
     */
    public List<GameRecord> findByUserIdPaginated(String userId, int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT gr.*, u1.username AS u1name, u2.username AS u2name " +
                "FROM game_records gr " +
                "JOIN users u1 ON gr.player1_id = u1.id " +
                "LEFT JOIN users u2 ON gr.player2_id = u2.id " +
                "WHERE gr.player1_id = ? " +
                "ORDER BY gr.played_at DESC " +
                "LIMIT ? OFFSET ?";

        List<GameRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Đếm tổng số trận của người chơi
     */
    public int countByUserId(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) as cnt FROM game_records WHERE player1_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("cnt");
        }
        return 0;
    }

    /**
     * Lấy lịch sử theo CHẾ ĐỘ (PvE hoặc PvP)
     */
    public List<GameRecord> findByMode(String userId, String mode) throws SQLException {
        String sql = "SELECT gr.*, u1.username AS u1name, u2.username AS u2name " +
                "FROM game_records gr " +
                "JOIN users u1 ON gr.player1_id = u1.id " +
                "LEFT JOIN users u2 ON gr.player2_id = u2.id " +
                "WHERE gr.player1_id = ? AND gr.mode = ? " +
                "ORDER BY gr.played_at DESC";

        List<GameRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, mode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Lấy lịch sử theo KỲ HẠN (7 ngày gần đây, 30 ngày, v.v.)
     */
    public List<GameRecord> findByDateRange(String userId, String period) throws SQLException {
        String sql;
        if ("week".equals(period)) {
            sql = "SELECT gr.*, u1.username AS u1name, u2.username AS u2name " +
                    "FROM game_records gr " +
                    "JOIN users u1 ON gr.player1_id = u1.id " +
                    "LEFT JOIN users u2 ON gr.player2_id = u2.id " +
                    "WHERE gr.player1_id = ? AND gr.played_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "ORDER BY gr.played_at DESC";
        } else if ("month".equals(period)) {
            sql = "SELECT gr.*, u1.username AS u1name, u2.username AS u2name " +
                    "FROM game_records gr " +
                    "JOIN users u1 ON gr.player1_id = u1.id " +
                    "LEFT JOIN users u2 ON gr.player2_id = u2.id " +
                    "WHERE gr.player1_id = ? AND gr.played_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "ORDER BY gr.played_at DESC";
        } else {
            return findByUserId(userId); // mặc định tất cả thời gian
        }

        List<GameRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Lấy các trận THẮNG/THUA để tính thống kê
     */
    public Map<String, Object> getPlayerStats(String userId) throws SQLException {
        String sql = "SELECT " +
                "  COUNT(*) as total_games, " +
                "  SUM(CASE WHEN winner_name = (SELECT u.username FROM users u WHERE u.id = ?) THEN 1 ELSE 0 END) as wins, " +
                "  AVG(duration_seconds) as avg_duration, " +
                "  SUM(total_shots) as total_shots, " +
                "  AVG(player1_score) as avg_score " +
                "FROM game_records WHERE player1_id = ?";

        Map<String, Object> stats = new HashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats.put("totalGames", rs.getInt("total_games"));
                stats.put("wins", rs.getInt("wins"));
                stats.put("avgDuration", rs.getInt("avg_duration"));
                stats.put("totalShots", rs.getInt("total_shots"));
                stats.put("avgScore", rs.getDouble("avg_score"));
            }
        }
        return stats;
    }
}
