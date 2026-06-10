package model;

import java.time.LocalDateTime;

/**
 * ============================================================
 * MODEL: Player (Người chơi / Tài khoản người dùng)
 * ============================================================
 * Class này là "khuôn mẫu" đại diện cho 1 dòng dữ liệu trong bảng `users` của DB.
 * Mọi thông tin liên quan đến tài khoản đều được lưu ở đây.
 *
 * Hỗ trợ 3 hình thức tài khoản:
 *   1. Tài khoản thường   → có passwordHash, verifyToken
 *   2. Đăng nhập Google   → có googleId, passwordHash = NULL
 *   3. Đăng nhập Facebook → có facebookId, passwordHash = NULL
 * ============================================================
 */
public class Player {

    // ── THÔNG TIN CƠ BẢN ────────────────────────────────────────────────────

    /** Khóa chính – UUID sinh ngẫu nhiên, dạng "550e8400-e29b-41d4-a716-..." */
    private String id;

    /** Tên đăng nhập hiển thị, unique trong hệ thống */
    private String username;

    /**
     * Mật khẩu đã được hash bằng SHA-256 (hex string 64 ký tự).
     * = NULL nếu tài khoản được tạo qua Google hoặc Facebook OAuth
     *   (vì người dùng không đặt mật khẩu khi dùng OAuth).
     */
    private String passwordHash;

    /** Địa chỉ email. Có thể NULL nếu OAuth không trả về email. */
    private String email;

    // ── XÁC THỰC EMAIL ──────────────────────────────────────────────────────

    /**
     * Cờ đánh dấu email đã được xác thực chưa.
     * - false: tài khoản vừa đăng ký, chưa click link trong email
     * - true : đã xác thực (hoặc tài khoản OAuth – tự động = true)
     * Nếu = false thì không cho đăng nhập (LoginServlet sẽ chặn).
     */
    private boolean emailVerified;

    /**
     * Token ngẫu nhiên (base64url, 32 bytes) gửi kèm trong link email xác thực.
     * Dạng: "https://...app/verify-email?token=<verifyToken>"
     * Sau khi xác thực thành công → set = NULL trong DB.
     */
    private String verifyToken;

    /**
     * Thời điểm email xác thực được gửi lần gần nhất.
     * Dùng để kiểm tra token có hết hạn chưa:
     *   nếu verifySentAt + 24h < NOW() → token hết hạn, cần gửi lại.
     */
    private LocalDateTime verifySentAt;

    // ── ĐẶT LẠI MẬT KHẨU ───────────────────────────────────────────────────

    /**
     * Token ngẫu nhiên gửi kèm link đặt lại mật khẩu qua email.
     * Dạng: "https://...app/reset-password?token=<resetToken>"
     * Sau khi đặt lại thành công → set = NULL trong DB.
     */
    private String resetToken;

    /**
     * Thời điểm resetToken hết hạn (thường = lúc tạo + 1 giờ).
     * Nếu NOW() > resetTokenExpiry → link đã hết hạn, không cho dùng.
     */
    private LocalDateTime resetTokenExpiry;

    // ── OAUTH ────────────────────────────────────────────────────────────────

    /**
     * "sub" (subject ID) từ Google OAuth – là ID duy nhất của tài khoản Google.
     * Dùng để tìm xem người dùng này đã có tài khoản trong hệ thống chưa
     * khi họ đăng nhập lại bằng Google.
     * = NULL nếu đây là tài khoản thường hoặc Facebook.
     */
    private String googleId;

    /**
     * "id" từ Facebook Graph API – là ID duy nhất của tài khoản Facebook.
     * Tương tự googleId nhưng dành cho Facebook.
     * = NULL nếu đây là tài khoản thường hoặc Google.
     */
    private String facebookId;

    // ── METADATA ─────────────────────────────────────────────────────────────

    /** Thời điểm tài khoản được tạo (= NOW() lúc INSERT). */
    private LocalDateTime createdAt;

    /** Thời điểm đăng nhập gần nhất (cập nhật mỗi lần đăng nhập thành công). */
    private LocalDateTime lastLogin;

    // ── CONSTRUCTOR ──────────────────────────────────────────────────────────

    /** Constructor rỗng – bắt buộc để UserDAO.map() có thể new Player() rồi set từng field. */
    public Player() {}

    // ── GETTERS & SETTERS ────────────────────────────────────────────────────
    // (Chuẩn JavaBean – chỉ get/set đơn giản, không có logic phụ)

    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }

    public String getUsername()                        { return username; }
    public void setUsername(String username)           { this.username = username; }

    public String getPasswordHash()                    { return passwordHash; }
    public void setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }

    public String getEmail()                           { return email; }
    public void setEmail(String email)                 { this.email = email; }

    public boolean isEmailVerified()                   { return emailVerified; }
    public void setEmailVerified(boolean emailVerified){ this.emailVerified = emailVerified; }

    public String getVerifyToken()                     { return verifyToken; }
    public void setVerifyToken(String verifyToken)     { this.verifyToken = verifyToken; }

    public LocalDateTime getVerifySentAt()             { return verifySentAt; }
    public void setVerifySentAt(LocalDateTime v)       { this.verifySentAt = v; }

    public String getResetToken()                      { return resetToken; }
    public void setResetToken(String resetToken)       { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry()         { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime v)   { this.resetTokenExpiry = v; }

    public String getGoogleId()                        { return googleId; }
    public void setGoogleId(String googleId)           { this.googleId = googleId; }

    public String getFacebookId()                      { return facebookId; }
    public void setFacebookId(String facebookId)       { this.facebookId = facebookId; }

    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)  { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin()                { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin)  { this.lastLogin = lastLogin; }
}