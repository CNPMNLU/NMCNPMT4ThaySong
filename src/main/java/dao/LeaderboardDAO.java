package dao;

import java.sql.*;
import java.util.*;

public class LeaderboardDAO {

    public void upsert(String userId, boolean won, int score) throws SQLException {
        String check = "SELECT id FROM leaderboard WHERE user_id=?";
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(check);
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String update = "UPDATE leaderboard SET total_games=total_games+1, " +
                    "total_wins=total_wins+?, total_losses=total_losses+?, " +
                    "total_score=total_score+?, " +
                    "best_score=GREATEST(best_score,?), " +
                    "win_rate=ROUND((total_wins+?)/(total_games+1)*100,2), " +
                    "updated_at=NOW() WHERE user_id=?";
                PreparedStatement ups = c.prepareStatement(update);
                ups.setInt(1, won ? 1 : 0);
                ups.setInt(2, won ? 0 : 1);
                ups.setInt(3, score);
                ups.setInt(4, score);
                ups.setInt(5, won ? 1 : 0);
                ups.setString(6, userId);
                ups.executeUpdate();
            } else {
                String insert = "INSERT INTO leaderboard (id,user_id,total_wins,total_losses,total_games,win_rate,best_score,total_score,updated_at) VALUES (?,?,?,?,1,?,?,?,NOW())";
                PreparedStatement ins = c.prepareStatement(insert);
                ins.setString(1, UUID.randomUUID().toString());
                ins.setString(2, userId);
                ins.setInt(3, won ? 1 : 0);
                ins.setInt(4, won ? 0 : 1);
                ins.setDouble(5, won ? 100.0 : 0.0);
                ins.setInt(6, score);
                ins.setInt(7, score);
                ins.executeUpdate();
            }
        }
    }

    public List<Map<String,Object>> getTopPlayers(int limit) throws SQLException {
        String sql = "SELECT l.*, u.username FROM leaderboard l JOIN users u ON l.user_id=u.id ORDER BY l.total_wins DESC, l.win_rate DESC LIMIT ?";
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("rank", rank++);
                row.put("username", rs.getString("username"));
                row.put("total_wins", rs.getInt("total_wins"));
                row.put("total_losses", rs.getInt("total_losses"));
                row.put("total_games", rs.getInt("total_games"));
                row.put("win_rate", rs.getDouble("win_rate"));
                row.put("best_score", rs.getInt("best_score"));
                row.put("total_score", rs.getInt("total_score"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String,Object>> getTopPlayersByElo(int limit) throws SQLException {
        String sql = "SELECT l.*, u.username, " +
                "RANK() OVER (ORDER BY l.elo_rating DESC) as rank " +
                "FROM leaderboard l " +
                "JOIN users u ON l.user_id = u.id " +
                "ORDER BY l.elo_rating DESC LIMIT ?";

        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("rank", rank++);
                row.put("username", rs.getString("username"));
                row.put("totalWins", rs.getInt("total_wins"));
                row.put("totalLosses", rs.getInt("total_losses"));
                row.put("totalGames", rs.getInt("total_games"));
                row.put("winRate", rs.getDouble("win_rate"));
                row.put("eloRating", rs.getInt("elo_rating"));
                row.put("level", rs.getInt("level"));
                row.put("userId", rs.getString("user_id"));
                list.add(row);
            }
        }
        return list;
    }

    public Map<String,Object> getPlayerRank(String userId) throws SQLException {
        String sql = "SELECT l.*, u.username, " +
                "(SELECT COUNT(*) + 1 FROM leaderboard WHERE elo_rating > l.elo_rating) as rank " +
                "FROM leaderboard l " +
                "JOIN users u ON l.user_id = u.id " +
                "WHERE l.user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("rank", rs.getInt("rank"));
                row.put("username", rs.getString("username"));
                row.put("totalWins", rs.getInt("total_wins"));
                row.put("totalLosses", rs.getInt("total_losses"));
                row.put("totalGames", rs.getInt("total_games"));
                row.put("winRate", rs.getDouble("win_rate"));
                row.put("eloRating", rs.getInt("elo_rating"));
                row.put("level", rs.getInt("level"));
                row.put("userId", userId);
                return row;
            }
        }
        return null;
    }

    public void updateEloRating(String userId, boolean isWinner) throws SQLException {
        int eloChange = isWinner ? 32 : -16;
        String sql = "UPDATE leaderboard SET " +
                "elo_rating = GREATEST(800, elo_rating + ?), " +
                "level = FLOOR(total_score / 1000) + 1 " +
                "WHERE user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eloChange);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateStreak(String userId, boolean isWinner) throws SQLException {
        if (isWinner) {
            String sql = "UPDATE leaderboard SET " +
                    "current_streak = current_streak + 1, " +
                    "max_streak = GREATEST(max_streak, current_streak + 1) " +
                    "WHERE user_id = ?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE leaderboard SET current_streak = 0 WHERE user_id = ?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.executeUpdate();
            }
        }
    }

    public String getPlayerTrend(String userId) throws SQLException {
        String sql = "SELECT " +
                "  (SELECT COUNT(*) + 1 FROM leaderboard WHERE elo_rating > l.elo_rating) as current_rank, " +
                "  (SELECT COUNT(*) FROM leaderboard_history " +
                "   WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)) as history_count " +
                "FROM leaderboard l WHERE l.user_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int currentRank = rs.getInt("current_rank");
                // Nếu rank giảm => up, tăng => down
                return currentRank < 100 ? "up" : currentRank > 100 ? "down" : "stable";
            }
        }
        return "stable";
    }
}
