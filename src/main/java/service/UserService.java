package service;

import dao.UserDAO;
import model.Player;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserService {
    private final UserDAO userDAO = new UserDAO();
    private final EmailService emailService = new EmailService();

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Player register(String username, String password, String email, String baseUrl) throws Exception {
        if (username == null || username.trim().length() < 3)
            throw new IllegalArgumentException("Username phải ít nhất 3 ký tự");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải ít nhất 6 ký tự");
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("Email là bắt buộc");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Email không đúng định dạng");
        if (userDAO.findByUsername(username.trim()) != null)
            throw new IllegalArgumentException("Username đã tồn tại");
        if (userDAO.findByEmail(email.trim()) != null)
            throw new IllegalArgumentException("Email đã được sử dụng");

        String token = generateToken();
        Player p = new Player();
        p.setId(UUID.randomUUID().toString());
        p.setUsername(username.trim());
        p.setPasswordHash(hashPassword(password));
        p.setEmail(email.trim());
        p.setVerifyToken(token);
        userDAO.insert(p);

        emailService.sendVerificationEmail(email.trim(), baseUrl + "/verify-email?token=" + token);
        return p;
    }

    public Player authenticate(String username, String password) throws Exception {
        Player p = userDAO.findByUsername(username);
        if (p == null)
            throw new IllegalArgumentException("Thông tin đăng nhập không chính xác");
        if (p.getPasswordHash() == null || !p.getPasswordHash().equals(hashPassword(password)))
            throw new IllegalArgumentException("Thông tin đăng nhập không chính xác");
        if (!p.isEmailVerified())
            throw new IllegalArgumentException("EMAIL_NOT_VERIFIED:" + p.getId());
        try { userDAO.updateLastLogin(p.getId()); } catch (Exception ignored) {}
        return p;
    }

    public void resendVerification(String userId, String baseUrl) throws Exception {
        Player p = userDAO.findById(userId);
        if (p == null) throw new IllegalArgumentException("Tài khoản không tồn tại");
        if (p.isEmailVerified()) throw new IllegalArgumentException("Email đã được xác thực");
        String token = generateToken();
        userDAO.updateVerifyToken(userId, token);
        emailService.sendVerificationEmail(p.getEmail(), baseUrl + "/verify-email?token=" + token);
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

    public static String generateToken() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
