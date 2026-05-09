package service;

import dao.UserDAO;
import model.Player;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.UUID;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    public Player register(String username, String password, String email) throws Exception {
        if (username == null || username.trim().length() < 3)
            throw new IllegalArgumentException("Username phải ít nhất 3 ký tự");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password phải ít nhất 6 ký tự");
        if (userDAO.findByUsername(username.trim()) != null)
            throw new IllegalArgumentException("Username đã tồn tại");

        Player p = new Player();
        p.setId(UUID.randomUUID().toString());
        p.setUsername(username.trim());
        p.setPasswordHash(hashPassword(password));
        p.setEmail(email != null ? email.trim() : null);
        userDAO.insert(p);
        return p;
    }

    public Player authenticate(String username, String password) throws Exception {
        Player p = userDAO.findByUsername(username);
        if (p == null) throw new IllegalArgumentException("Username không tồn tại");
        if (!p.getPasswordHash().equals(hashPassword(password)))
            throw new IllegalArgumentException("Mật khẩu không đúng");
        // Dùng try-catch riêng: lỗi cập nhật last_login KHÔNG được chặn đăng nhập
        try {
            userDAO.updateLastLogin(p.getId());
        } catch (Exception ignored) {
            // Bỏ qua nếu cột last_login chưa tồn tại trong DB
        }
        return p;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
