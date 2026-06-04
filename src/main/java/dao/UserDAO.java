package dao;

import model.Player;
import java.sql.*;
import java.time.LocalDateTime;

public class UserDAO {

    public void insert(Player player) throws SQLException {
        String sql = "INSERT INTO users (id, username, password_hash, email, email_verified, verify_token, verify_sent_at, created_at) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player.getId());
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getPasswordHash());
            ps.setString(4, player.getEmail());
            ps.setInt(5, 0);
            ps.setString(6, player.getVerifyToken());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public void insertGoogleUser(Player player) throws SQLException {
        String sql = "INSERT INTO users (id, username, password_hash, email, email_verified, google_id, created_at) VALUES (?,?,NULL,?,1,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player.getId());
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getEmail());
            ps.setString(4, player.getGoogleId());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public Player findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public Player findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public Player findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public Player findByGoogleId(String googleId) throws SQLException {
        String sql = "SELECT * FROM users WHERE google_id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, googleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public Player findByVerifyToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE verify_token = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public Player findByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE reset_token = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public void markEmailVerified(String userId) throws SQLException {
        String sql = "UPDATE users SET email_verified = 1, verify_token = NULL WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    public void updateVerifyToken(String userId, String token) throws SQLException {
        String sql = "UPDATE users SET verify_token = ?, verify_sent_at = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public void saveResetToken(String userId, String token, LocalDateTime expiry) throws SQLException {
        String sql = "UPDATE users SET reset_token = ?, reset_token_expiry = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiry));
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public void updatePasswordAndClearResetToken(String userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, reset_token = NULL, reset_token_expiry = NULL WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateLastLogin(String userId) throws SQLException {
        String sql = "UPDATE users SET last_login = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    private Player map(ResultSet rs) throws SQLException {
        Player p = new Player();
        p.setId(rs.getString("id"));
        p.setUsername(rs.getString("username"));
        p.setPasswordHash(rs.getString("password_hash"));
        p.setEmail(rs.getString("email"));
        p.setEmailVerified(rs.getInt("email_verified") == 1);
        p.setGoogleId(rs.getString("google_id"));
        p.setVerifyToken(rs.getString("verify_token"));
        Timestamp vsa = rs.getTimestamp("verify_sent_at");
        if (vsa != null) p.setVerifySentAt(vsa.toLocalDateTime());
        p.setResetToken(rs.getString("reset_token"));
        Timestamp rte = rs.getTimestamp("reset_token_expiry");
        if (rte != null) p.setResetTokenExpiry(rte.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) p.setLastLogin(ll.toLocalDateTime());
        return p;
    }
}