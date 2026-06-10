package dao;

import model.Player;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * ============================================================
 * DAO: UserDAO (Data Access Object cho bảng `users`)
 * ============================================================
 * Tất cả câu lệnh SQL liên quan đến tài khoản người dùng đều nằm ở đây.
 * Các tầng trên (Service, Controller) KHÔNG được viết SQL trực tiếp –
 * chúng chỉ gọi các method của class này.
 *
 * Cấu trúc nhóm method:
 *   INSERT  → insert, insertGoogleUser, insertFacebookUser
 *   SELECT  → findBy* (username, id, email, verifyToken, resetToken, googleId, facebookId)
 *   UPDATE  → markEmailVerified, updateVerifyToken, saveResetToken,
 *             updatePasswordAndClearResetToken, updateLastLogin
 *   DELETE  → deleteById
 * ============================================================
 */
public class UserDAO {

    // ════════════════════════════════════════════════════════════════════════
    // INSERT – Tạo mới tài khoản
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Thêm tài khoản thường (đăng ký bằng username + password) vào DB.
     *
     * Lưu ý:
     *   - email_verified mặc định = 0 (chưa xác thực).
     *   - verify_token được set sẵn để gửi qua email.
     *   - verify_sent_at = thời điểm hiện tại (dùng để kiểm tra hết hạn 24h).
     *   - password_hash là SHA-256 hex, KHÔNG lưu mật khẩu thô.
     */
    public void insert(Player player) throws SQLException {
        String sql = "INSERT INTO users "
                + "(id, username, password_hash, email, email_verified, verify_token, verify_sent_at, created_at) "
                + "VALUES (?,?,?,?,0,?,?,NOW())";

        // try-with-resources → tự động đóng Connection và PreparedStatement dù có lỗi hay không
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, player.getId());           // UUID
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getPasswordHash()); // SHA-256 hex
            ps.setString(4, player.getEmail());
            ps.setString(5, player.getVerifyToken());  // token gửi qua email
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now())); // thời điểm gửi

            ps.executeUpdate();
        }
    }

    /**
     * Thêm tài khoản từ Google OAuth vào DB.
     *
     * Khác với tài khoản thường:
     *   - password_hash = NULL (không có mật khẩu)
     *   - email_verified = 1 (Google đã xác thực email rồi, tin tưởng được)
     *   - google_id = "sub" từ Google (ID duy nhất của tài khoản Google)
     *   - Không có verify_token vì không cần xác thực email
     */
    public void insertGoogleUser(Player player) throws SQLException {
        String sql = "INSERT INTO users "
                + "(id, username, password_hash, email, email_verified, google_id, created_at) "
                + "VALUES (?,?,NULL,?,1,?,NOW())";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, player.getId());
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getEmail());
            ps.setString(4, player.getGoogleId()); // "sub" từ Google userinfo

            ps.executeUpdate();
        }
    }

    /**
     * Thêm tài khoản từ Facebook OAuth vào DB.
     *
     * Tương tự Google nhưng dùng facebook_id thay vì google_id.
     * facebook_id = "id" từ Facebook Graph API.
     */
    public void insertFacebookUser(Player player) throws SQLException {
        String sql = "INSERT INTO users "
                + "(id, username, password_hash, email, email_verified, facebook_id, created_at) "
                + "VALUES (?,?,NULL,?,1,?,NOW())";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, player.getId());
            ps.setString(2, player.getUsername());
            ps.setString(3, player.getEmail());
            ps.setString(4, player.getFacebookId()); // "id" từ Facebook Graph API

            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SELECT – Tìm kiếm tài khoản
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tìm user theo username – dùng khi đăng nhập tài khoản thường.
     * Trả về null nếu không tồn tại (LoginServlet dùng để báo "sai thông tin").
     */
    public Player findByUsername(String username) throws SQLException {
        return findOne("SELECT * FROM users WHERE username = ?", username);
    }

    /**
     * Tìm user theo ID – dùng khi gửi lại email xác thực
     * (ResendVerifyServlet gửi userId, cần lấy email để gửi).
     */
    public Player findById(String id) throws SQLException {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    /**
     * Tìm user theo email – dùng trong 2 trường hợp:
     *   1. Kiểm tra email đã tồn tại khi đăng ký (UserService.register)
     *   2. Tìm user để gửi email reset mật khẩu (PasswordResetService.requestReset)
     */
    public Player findByEmail(String email) throws SQLException {
        return findOne("SELECT * FROM users WHERE email = ?", email);
    }

    /**
     * Tìm user theo token xác thực email.
     * Dùng khi người dùng click vào link "Xác thực Email" trong hộp thư.
     * Trả về null nếu token không tồn tại (link giả / đã dùng rồi).
     */
    public Player findByVerifyToken(String token) throws SQLException {
        return findOne("SELECT * FROM users WHERE verify_token = ?", token);
    }

    /**
     * Tìm user theo token đặt lại mật khẩu.
     * Dùng khi người dùng click vào link "Đặt lại mật khẩu" trong hộp thư.
     * Trả về null nếu token không tồn tại hoặc đã bị xóa sau khi dùng.
     */
    public Player findByResetToken(String token) throws SQLException {
        return findOne("SELECT * FROM users WHERE reset_token = ?", token);
    }

    /**
     * Tìm user theo Google ID ("sub") – dùng trong GoogleCallbackServlet.
     * Mục đích: kiểm tra xem người dùng đã từng đăng nhập bằng Google này chưa.
     *   - Có rồi → lấy thông tin, tạo session luôn
     *   - Chưa có → tạo tài khoản mới (insertGoogleUser)
     */
    public Player findByGoogleId(String googleId) throws SQLException {
        return findOne("SELECT * FROM users WHERE google_id = ?", googleId);
    }

    /**
     * Tìm user theo Facebook ID – dùng trong FacebookCallbackServlet.
     * Logic tương tự findByGoogleId nhưng cho Facebook.
     */
    public Player findByFacebookId(String facebookId) throws SQLException {
        return findOne("SELECT * FROM users WHERE facebook_id = ?", facebookId);
    }

    // ════════════════════════════════════════════════════════════════════════
    // UPDATE – Cập nhật trạng thái tài khoản
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Đánh dấu email đã xác thực thành công.
     * Đồng thời xóa verify_token khỏi DB để link cũ không dùng lại được.
     * Gọi bởi VerifyEmailServlet sau khi kiểm tra token hợp lệ và chưa hết hạn.
     */
    public void markEmailVerified(String userId) throws SQLException {
        exec("UPDATE users SET email_verified=1, verify_token=NULL WHERE id=?", userId);
    }

    /**
     * Cập nhật verify_token mới khi gửi lại email xác thực.
     * Đồng thời cập nhật verify_sent_at = NOW() để tính lại thời hạn 24h.
     * Gọi bởi UserService.resendVerification().
     */
    public void updateVerifyToken(String userId, String token) throws SQLException {
        String sql = "UPDATE users SET verify_token=?, verify_sent_at=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now())); // reset đồng hồ 24h
            ps.setString(3, userId);

            ps.executeUpdate();
        }
    }

    /**
     * Lưu token đặt lại mật khẩu cùng thời điểm hết hạn vào DB.
     * Gọi bởi PasswordResetService.requestReset() ngay trước khi gửi email.
     *
     * @param expiry thường = LocalDateTime.now().plusHours(1) → hết hạn sau 1 giờ
     */
    public void saveResetToken(String userId, String token, LocalDateTime expiry) throws SQLException {
        String sql = "UPDATE users SET reset_token=?, reset_token_expiry=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiry)); // thời điểm hết hạn
            ps.setString(3, userId);

            ps.executeUpdate();
        }
    }

    /**
     * Cập nhật mật khẩu mới (đã hash) và xóa reset_token + reset_token_expiry.
     * Xóa token sau khi dùng đảm bảo link reset chỉ dùng được 1 lần.
     * Gọi bởi PasswordResetService.resetPassword() sau khi validate thành công.
     */
    public void updatePasswordAndClearResetToken(String userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash=?, reset_token=NULL, reset_token_expiry=NULL WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, newHash); // SHA-256 của mật khẩu mới
            ps.setString(2, userId);

            ps.executeUpdate();
        }
    }

    /**
     * Ghi lại thời điểm đăng nhập gần nhất.
     * Gọi trong UserService.authenticate() mỗi khi đăng nhập thành công.
     * Lỗi ở đây được bỏ qua (try-catch trong UserService) vì không quan trọng.
     */
    public void updateLastLogin(String userId) throws SQLException {
        exec("UPDATE users SET last_login=NOW() WHERE id=?", userId);
    }

    // ════════════════════════════════════════════════════════════════════════
    // DELETE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Xóa user khỏi DB theo ID.
     * Dùng duy nhất 1 chỗ: UserService.register() dùng để "rollback thủ công"
     * khi đã INSERT user thành công nhưng gửi email thất bại.
     * (Vì không dùng transaction DB, nên phải xóa tay để không có user "rác" trong DB.)
     */
    public void deleteById(String id) throws SQLException {
        exec("DELETE FROM users WHERE id=?", id);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS – Tái sử dụng code
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Helper dùng chung cho tất cả câu SELECT trả về 1 dòng.
     * Giảm lặp code: mọi findBy* đều chỉ khác nhau ở câu SQL và giá trị param.
     *
     * @return Player nếu tìm thấy, null nếu không có kết quả
     */
    private Player findOne(String sql, String param) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();

            // rs.next() = true nếu có ít nhất 1 dòng kết quả
            if (rs.next()) return map(rs); // chuyển dòng ResultSet → object Player
        }
        return null; // không tìm thấy
    }

    /**
     * Helper dùng chung cho UPDATE và DELETE không có giá trị trả về.
     * Tất cả câu lệnh chỉ có 1 tham số WHERE id=? hoặc WHERE id=?.
     */
    private void exec(String sql, String param) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            ps.executeUpdate();
        }
    }

    /**
     * Chuyển đổi 1 dòng ResultSet (từ DB) thành object Player.
     * Được gọi sau mỗi rs.next() trong các method findBy*.
     *
     * Lưu ý xử lý Timestamp → LocalDateTime:
     *   - Phải kiểm tra null trước khi .toLocalDateTime()
     *   - Các cột có thể NULL: verify_sent_at, reset_token_expiry, created_at, last_login
     */
    private Player map(ResultSet rs) throws SQLException {
        Player p = new Player();

        p.setId(rs.getString("id"));
        p.setUsername(rs.getString("username"));
        p.setPasswordHash(rs.getString("password_hash")); // có thể NULL (OAuth user)
        p.setEmail(rs.getString("email"));

        // email_verified lưu dạng INT (0/1) trong MySQL → chuyển sang boolean
        p.setEmailVerified(rs.getInt("email_verified") == 1);

        p.setVerifyToken(rs.getString("verify_token"));   // NULL sau khi đã xác thực
        p.setResetToken(rs.getString("reset_token"));     // NULL sau khi đã đặt lại MK
        p.setGoogleId(rs.getString("google_id"));         // NULL nếu không phải Google user
        p.setFacebookId(rs.getString("facebook_id"));     // NULL nếu không phải Facebook user

        // Timestamp → LocalDateTime (an toàn với null)
        Timestamp vsa = rs.getTimestamp("verify_sent_at");
        if (vsa != null) p.setVerifySentAt(vsa.toLocalDateTime());

        Timestamp rte = rs.getTimestamp("reset_token_expiry");
        if (rte != null) p.setResetTokenExpiry(rte.toLocalDateTime());

        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());

        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) p.setLastLogin(ll.toLocalDateTime());

        return p;
    }
}