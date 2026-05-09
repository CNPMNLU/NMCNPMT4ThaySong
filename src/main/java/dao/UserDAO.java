package dao;

import model.Player;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserDAO {

    public void insert(Player player) throws SQLException {
        String sql = "INSERT INTO users (id, username, password_hash, email, created_at) VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player.getId());
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getPasswordHash());
            ps.setString(4, player.getEmail());
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
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) p.setLastLogin(ll.toLocalDateTime());
        return p;
    }
}
