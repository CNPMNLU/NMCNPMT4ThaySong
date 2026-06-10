package service;

import dao.UserDAO;
import model.Player;
import java.time.LocalDateTime;

/**
 * ============================================================
 * SERVICE: PasswordResetService (Quên / Đặt lại mật khẩu)
 * ============================================================
 * Quản lý toàn bộ luồng reset mật khẩu:
 *
 *   Bước 1 – Người dùng nhập email (ForgotPasswordServlet)
 *              → requestReset(): gửi email chứa link reset
 *
 *   Bước 2 – Người dùng click link trong email (ResetPasswordServlet GET)
 *              → validateToken(): kiểm tra token còn hợp lệ không
 *
 *   Bước 3 – Người dùng nhập mật khẩu mới (ResetPasswordServlet POST)
 *              → resetPassword(): validate + cập nhật mật khẩu mới
 *
 * Token reset có thời hạn 1 giờ và chỉ dùng được 1 lần.
 * ============================================================
 */
public class PasswordResetService {

    private final UserDAO      userDAO      = new UserDAO();
    private final EmailService emailService = new EmailService();

    // ════════════════════════════════════════════════════════════════════════
    // BƯỚC 1: Yêu cầu reset – gửi email
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Xử lý yêu cầu quên mật khẩu: tìm user theo email và gửi link reset.
     *
     * Bảo mật quan trọng – "silent fail":
     *   Nếu email không tồn tại trong DB → KHÔNG báo lỗi, im lặng return.
     *   Lý do: nếu báo "Email không tồn tại" thì hacker có thể dùng form này
     *   để dò xem email nào đã có tài khoản (gọi là "user enumeration attack").
     *   ForgotPasswordServlet luôn hiển thị "Đã gửi" dù email có tồn tại hay không.
     *
     * Quy trình khi email hợp lệ:
     *   1. Tìm user theo email
     *   2. Sinh token ngẫu nhiên (dùng UserService.generateToken())
     *   3. Lưu token + thời hạn hết hạn (= now + 1 giờ) vào DB
     *   4. Gửi email chứa link: <baseUrl>/reset-password?token=<token>
     *
     * @param email   email người dùng nhập vào form
     * @param baseUrl URL gốc ứng dụng để ghép link reset
     */
    public void requestReset(String email, String baseUrl) throws Exception {
        // Tìm user theo email (null = email không tồn tại → im lặng bỏ qua)
        Player p = userDAO.findByEmail(email.trim());
        if (p == null) return; // silent fail – tránh user enumeration

        // Sinh token ngẫu nhiên bảo mật (32 bytes = 256 bits)
        String token = UserService.generateToken();

        // Lưu token vào DB kèm thời điểm hết hạn (1 giờ kể từ bây giờ)
        userDAO.saveResetToken(p.getId(), token, LocalDateTime.now().plusHours(1));

        // Gửi email chứa link đặt lại mật khẩu
        // Link dạng: http://localhost:8080/app/reset-password?token=K7dH2xP9...
        emailService.sendResetPasswordEmail(
                p.getEmail(), baseUrl + "/reset-password?token=" + token);
    }

    // ════════════════════════════════════════════════════════════════════════
    // BƯỚC 2: Kiểm tra token khi người dùng click link
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra xem token trong URL có hợp lệ và còn trong thời hạn không.
     *
     * Được gọi trong ResetPasswordServlet.doGet() khi người dùng click link email.
     * Nếu hợp lệ → trả về Player để Controller lấy username hiển thị trên form.
     * Nếu không hợp lệ → ném exception để Controller hiển thị thông báo lỗi.
     *
     * Các trường hợp không hợp lệ:
     *   - Token null hoặc rỗng      → "Link không hợp lệ"
     *   - Token không tồn tại trong DB → "Link không hợp lệ hoặc đã được sử dụng"
     *     (token bị xóa sau khi đặt lại thành công → không tìm thấy nữa)
     *   - Token đã hết hạn (>1 giờ) → "Link đã hết hạn. Vui lòng yêu cầu lại."
     *
     * @param token  chuỗi token lấy từ URL parameter ?token=...
     * @return Player tương ứng nếu token hợp lệ
     * @throws IllegalArgumentException nếu token không hợp lệ / hết hạn
     */
    public Player validateToken(String token) throws Exception {
        // Kiểm tra token có được truyền vào không (URL bị sửa tay)
        if (token == null || token.trim().isEmpty())
            throw new IllegalArgumentException("Link không hợp lệ");

        // Tìm user có reset_token khớp (null = token đã bị xóa hoặc không tồn tại)
        Player p = userDAO.findByResetToken(token.trim());
        if (p == null)
            throw new IllegalArgumentException(
                    "Link đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng");

        // Kiểm tra thời hạn: resetTokenExpiry phải > thời điểm hiện tại
        if (p.getResetTokenExpiry() == null
                || p.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Link đã hết hạn. Vui lòng yêu cầu lại.");

        return p; // token hợp lệ, trả về Player
    }

    // ════════════════════════════════════════════════════════════════════════
    // BƯỚC 3: Cập nhật mật khẩu mới
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Đặt mật khẩu mới sau khi người dùng điền form reset-password.
     *
     * Quy trình:
     *   1. Kiểm tra newPassword ≥ 6 ký tự
     *   2. Kiểm tra newPassword == confirmPassword
     *   3. Gọi validateToken() để xác nhận token vẫn hợp lệ lần nữa
     *      (phòng trường hợp token hết hạn trong lúc người dùng đang điền form)
     *   4. Hash mật khẩu mới bằng SHA-256
     *   5. Cập nhật password_hash vào DB
     *   6. Xóa reset_token và reset_token_expiry (token chỉ dùng 1 lần)
     *
     * @param token           token từ URL (lấy từ hidden input trong form)
     * @param newPassword     mật khẩu mới người dùng nhập
     * @param confirmPassword mật khẩu xác nhận (phải giống newPassword)
     * @throws IllegalArgumentException nếu mật khẩu không hợp lệ, không khớp, hoặc token lỗi
     */
    public void resetPassword(String token,
                              String newPassword,
                              String confirmPassword) throws Exception {
        // Validate mật khẩu mới trước khi tốn công validate token
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới phải ít nhất 6 ký tự");

        if (!newPassword.equals(confirmPassword))
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");

        // Validate token lần nữa (đề phòng link hết hạn trong lúc user điền form)
        Player p = validateToken(token);

        // Cập nhật mật khẩu mới (hash SHA-256) và xóa token khỏi DB
        userDAO.updatePasswordAndClearResetToken(
                p.getId(),
                UserService.hashPassword(newPassword) // hash trước khi lưu
        );
    }
}