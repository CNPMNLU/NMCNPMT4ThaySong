package service;

import dao.UserDAO;
import model.Player;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ============================================================
 * SERVICE: UserService (Logic nghiệp vụ xác thực người dùng)
 * ============================================================
 * Đây là lớp trung gian giữa Controller và DAO.
 * Controller KHÔNG gọi UserDAO trực tiếp – phải đi qua đây
 * để đảm bảo validate và business logic luôn được thực thi.
 *
 * Ba chức năng chính:
 *   1. register()           – Đăng ký tài khoản + gửi email xác thực
 *   2. authenticate()       – Đăng nhập tài khoản thường
 *   3. resendVerification() – Gửi lại email xác thực
 *
 * Và 2 utility static dùng chung toàn hệ thống:
 *   hashPassword()  – SHA-256 mật khẩu
 *   generateToken() – Sinh token ngẫu nhiên bảo mật
 *
 * Chế độ hoạt động (biến môi trường EMAIL_ENABLED):
 *   "true"  → Bắt buộc xác thực email trước khi login (PRODUCTION)
 *   khác    → Tự động verify, login ngay sau đăng ký (DEVELOPMENT)
 * ============================================================
 */
public class UserService {

    private final UserDAO      userDAO      = new UserDAO();
    private final EmailService emailService = new EmailService();

    /**
     * Regex kiểm tra định dạng email cơ bản.
     * Pattern: <phần trước @> @ <domain> . <phần cuối>
     * Không dùng thư viện nặng, chỉ cần đủ để bắt lỗi rõ ràng.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * Đọc biến môi trường 1 lần khi class được load.
     * "true" (không phân biệt hoa thường) → bật chế độ yêu cầu xác thực email.
     */
    private static boolean EMAIL_ENABLED =
            "true".equalsIgnoreCase(System.getenv("EMAIL_ENABLED"));

    // ════════════════════════════════════════════════════════════════════════
    // 1. ĐĂNG KÝ TÀI KHOẢN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tạo tài khoản người dùng mới.
     *
     * Quy trình:
     *   [Validate] username ≥ 3 ký tự
     *   [Validate] password ≥ 6 ký tự
     *   [Validate] email đúng định dạng
     *   [Validate] username chưa tồn tại trong DB
     *   [Validate] email chưa tồn tại trong DB
     *   [Build]    tạo object Player với id UUID, password hash SHA-256
     *   [Branch]   nếu EMAIL_ENABLED=true:
     *                → INSERT vào DB với email_verified=false
     *                → gửi email xác thực
     *                → nếu gửi email lỗi: DELETE user vừa INSERT (manual rollback)
     *              nếu EMAIL_ENABLED=false (dev mode):
     *                → INSERT vào DB với email_verified=true (không cần xác thực)
     *
     * @param username  tên đăng nhập
     * @param password  mật khẩu thô (sẽ được hash trước khi lưu)
     * @param email     địa chỉ email
     * @param baseUrl   URL gốc của ứng dụng để ghép link xác thực
     *                  vd: "http://localhost:8080/battleship"
     * @return Player đã được lưu vào DB
     * @throws IllegalArgumentException nếu vi phạm bất kỳ điều kiện validate nào
     */
    public Player register(String username, String password,
                           String email, String baseUrl) throws Exception {

        // ── Bước 1: Validate đầu vào ─────────────────────────────────────────
        if (username == null || username.trim().length() < 3)
            throw new IllegalArgumentException("Username phải ít nhất 3 ký tự");

        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải ít nhất 6 ký tự");

        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("Email là bắt buộc");

        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Email không đúng định dạng");

        // Kiểm tra trùng trong DB (2 query riêng để thông báo lỗi cụ thể)
        if (userDAO.findByUsername(username.trim()) != null)
            throw new IllegalArgumentException("Username đã tồn tại");

        if (userDAO.findByEmail(email.trim()) != null)
            throw new IllegalArgumentException("Email đã được sử dụng");

        // ── Bước 2: Tạo object Player ─────────────────────────────────────────
        Player p = new Player();
        p.setId(UUID.randomUUID().toString()); // khóa chính UUID ngẫu nhiên
        p.setUsername(username.trim());
        p.setPasswordHash(hashPassword(password)); // KHÔNG bao giờ lưu mật khẩu thô
        p.setEmail(email.trim());

        // ── Bước 3: Lưu DB và gửi email (tùy chế độ) ────────────────────────
        if (EMAIL_ENABLED) {
            // === PRODUCTION MODE: yêu cầu xác thực email ===
            String token = generateToken(); // token ngẫu nhiên 32 bytes
            p.setVerifyToken(token);
            p.setEmailVerified(false); // chưa xác thực

            // INSERT trước để có dữ liệu trong DB
            userDAO.insert(p);

            try {
                // Gửi email chứa link: <baseUrl>/verify-email?token=<token>
                emailService.sendVerificationEmail(
                        email.trim(), baseUrl + "/verify-email?token=" + token);
            } catch (Exception emailEx) {
                // Gửi email thất bại → xóa user vừa INSERT để DB không có "user rác"
                // (Manual rollback vì không dùng transaction)
                userDAO.deleteById(p.getId());
                throw new IllegalArgumentException(
                        "Không thể gửi email xác thực: " + emailEx.getMessage()
                                + ". Kiểm tra cấu hình MAIL_HOST, MAIL_USER, MAIL_PASS.");
            }
        } else {
            // === DEVELOPMENT MODE: tự động verify, login ngay ===
            p.setEmailVerified(true);  // bỏ qua bước xác thực email
            p.setVerifyToken(null);    // không cần token
            userDAO.insert(p);
            // Không gửi email → không cần MAIL_HOST/USER/PASS khi dev local
        }

        return p; // trả về để Controller lấy id tạo session hoặc redirect
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. ĐĂNG NHẬP TÀI KHOẢN THƯỜNG
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Xác thực thông tin đăng nhập (username + password).
     *
     * Quy trình:
     *   [Tìm]    tìm user theo username → không có → báo lỗi chung chung
     *   [So sánh] hash(password) == passwordHash trong DB → không khớp → lỗi chung
     *   [Kiểm tra] emailVerified = true → chưa → ném lỗi đặc biệt với userId
     *   [Cập nhật] ghi last_login
     *   [Trả về]  object Player để Controller tạo session
     *
     * Bảo mật: khi username sai và password sai đều trả về cùng 1 thông báo
     * ("Thông tin đăng nhập không chính xác") để tránh lộ thông tin user có tồn tại hay không.
     *
     * Convention đặc biệt: khi email chưa xác thực, ném IllegalArgumentException
     * với message dạng "EMAIL_NOT_VERIFIED:<userId>" để LoginServlet nhận biết
     * và redirect sang trang pending-verification.
     *
     * @throws IllegalArgumentException nếu thông tin sai hoặc email chưa xác thực
     */
    public Player authenticate(String username, String password) throws Exception {
        // Tìm user theo username (null = không tồn tại)
        Player p = userDAO.findByUsername(username);
        if (p == null)
            throw new IllegalArgumentException("Thông tin đăng nhập không chính xác");

        // So sánh hash: hash(password nhập vào) == hash lưu trong DB
        // passwordHash == null khi tài khoản OAuth (không có mật khẩu)
        if (p.getPasswordHash() == null || !p.getPasswordHash().equals(hashPassword(password)))
            throw new IllegalArgumentException("Thông tin đăng nhập không chính xác");

        // Kiểm tra email đã xác thực chưa
        // Ném lỗi đặc biệt kèm userId để LoginServlet redirect đúng trang
        if (!p.isEmailVerified())
            throw new IllegalArgumentException("EMAIL_NOT_VERIFIED:" + p.getId());

        // Cập nhật last_login (lỗi ở đây không quan trọng, bỏ qua)
        try { userDAO.updateLastLogin(p.getId()); } catch (Exception ignored) {}

        return p;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. GỬI LẠI EMAIL XÁC THỰC
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Sinh token xác thực mới và gửi lại email.
     * Dùng khi token cũ đã hết hạn 24 giờ hoặc người dùng chưa nhận được email.
     *
     * @param userId  ID của user cần gửi lại (lấy từ session "pendingVerifyId")
     * @param baseUrl URL gốc để ghép link xác thực
     * @throws IllegalArgumentException nếu user không tồn tại hoặc đã verify rồi
     */
    public void resendVerification(String userId, String baseUrl) throws Exception {
        Player p = userDAO.findById(userId);

        if (p == null)
            throw new IllegalArgumentException("Tài khoản không tồn tại");

        // Không gửi lại nếu đã xác thực rồi (tránh gửi email vô nghĩa)
        if (p.isEmailVerified())
            throw new IllegalArgumentException("Email đã được xác thực");

        // Sinh token MỚI (vô hiệu hóa token cũ vì DB chỉ lưu 1 token)
        String token = generateToken();
        userDAO.updateVerifyToken(userId, token); // cập nhật DB + reset đồng hồ 24h

        try {
            emailService.sendVerificationEmail(
                    p.getEmail(), baseUrl + "/verify-email?token=" + token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể gửi email: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // STATIC UTILITIES – Dùng chung cả trong PasswordResetService
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Hash mật khẩu bằng SHA-256, trả về chuỗi hex 64 ký tự.
     *
     * Ví dụ: "password123" → "ef92b778bafe771207914a..."
     *
     * Lưu ý: SHA-256 không dùng salt nên không đủ mạnh so với bcrypt/Argon2.
     * Nếu nâng cấp bảo mật, thay thế method này bằng BCrypt là đủ.
     *
     * static vì PasswordResetService cũng cần dùng mà không muốn tạo UserService.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));

            // Chuyển byte[] → hex string (mỗi byte → 2 ký tự hex)
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // SHA-256 luôn tồn tại, không bao giờ xảy ra
        }
    }

    /**
     * Sinh token ngẫu nhiên bảo mật cao (cryptographically secure).
     *
     * Quy trình:
     *   1. SecureRandom tạo 32 bytes ngẫu nhiên thực sự (không phải Math.random)
     *   2. Encode sang Base64 URL-safe (không dùng +/ mà dùng -_ để an toàn trong URL)
     *   3. Bỏ padding "=" ở cuối để URL gọn hơn
     *
     * Kết quả: chuỗi ~43 ký tự, ví dụ "K7dH2xP9mNqR5tVw-Jk3sLn8YcAe1BfG"
     * Xác suất đoán trúng: 1/2^256 ≈ 0 → an toàn dùng làm token xác thực.
     *
     * static vì PasswordResetService cũng cần dùng.
     */
    public static String generateToken() {
        byte[] b = new byte[32]; // 32 bytes = 256 bits entropy
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}