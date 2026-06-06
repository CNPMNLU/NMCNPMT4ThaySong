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

    /**
     * EMAIL_ENABLED=true  → bắt buộc xác thực email trước khi login
     * EMAIL_ENABLED=false hoặc không set → tự động verify, có thể login ngay
     */
    private static final boolean EMAIL_ENABLED =
        "true".equalsIgnoreCase(System.getenv("EMAIL_ENABLED"));

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

        Player p = new Player();
        p.setId(UUID.randomUUID().toString());
        p.setUsername(username.trim());
        p.setPasswordHash(hashPassword(password));
        p.setEmail(email.trim());

        if (EMAIL_ENABLED) {
            String token = generateToken();
            p.setVerifyToken(token);
            p.setEmailVerified(false);

            // Insert trước, nếu email fail thì xoá user (rollback thủ công)
            userDAO.insert(p);
            try {
                emailService.sendVerificationEmail(email.trim(), baseUrl + "/verify-email?token=" + token);
            } catch (Exception emailEx) {
                userDAO.deleteById(p.getId());
                throw new IllegalArgumentException(
                    "Không thể gửi email xác thực: " + emailEx.getMessage() +
                    ". Kiểm tra cấu hình MAIL_HOST, MAIL_USER, MAIL_PASS."
                );
            }
        } else {
            // Chế độ dev: không cần email, tự động verify
            p.setEmailVerified(true);
            p.setVerifyToken(null);
            userDAO.insert(p);
        }

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
        try {
            emailService.sendVerificationEmail(p.getEmail(), baseUrl + "/verify-email?token=" + token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể gửi email: " + e.getMessage());
        }
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
