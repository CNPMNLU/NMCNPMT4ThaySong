package service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * ============================================================
 * SERVICE: EmailService (Gửi email qua SMTP)
 * ============================================================
 * Chịu trách nhiệm duy nhất: soạn nội dung HTML và gửi email.
 * Được gọi bởi UserService và PasswordResetService – không gọi trực tiếp từ Controller.
 *
 * Cần cấu hình 3 biến môi trường bắt buộc:
 *   MAIL_HOST – địa chỉ SMTP server (vd: smtp.gmail.com)
 *   MAIL_USER – tài khoản email gửi
 *   MAIL_PASS – mật khẩu / App Password
 *
 * Tuỳ chọn:
 *   MAIL_PORT – cổng SMTP (mặc định 587 – STARTTLS)
 *
 * Hai loại email được hỗ trợ:
 *   1. sendVerificationEmail  → gửi khi đăng ký tài khoản mới
 *   2. sendResetPasswordEmail → gửi khi người dùng quên mật khẩu
 * ============================================================
 */
public class EmailService {

    // ── CẤU HÌNH TỪ BIẾN MÔI TRƯỜNG ────────────────────────────────────────
    // Đọc 1 lần khi class được load (static final), không đọc lại mỗi lần gửi

    /** SMTP server address (vd: "smtp.gmail.com", "smtp.mailgun.org") */
    private static final String HOST = System.getenv("MAIL_HOST");

    /**
     * Cổng SMTP:
     *   587  → STARTTLS (phổ biến nhất, mặc định)
     *   465  → SSL/TLS
     *   25   → Plain (thường bị chặn bởi ISP)
     */
    private static final String PORT = System.getenv("MAIL_PORT") != null
            ? System.getenv("MAIL_PORT") : "587";

    /** Email gửi đi (cũng là username SMTP auth) */
    private static final String USER = System.getenv("MAIL_USER");

    /** Mật khẩu SMTP (nên dùng App Password nếu dùng Gmail) */
    private static final String PASS = System.getenv("MAIL_PASS");

    /** Tên hiển thị của người gửi (người nhận thấy tên này trong hộp thư) */
    private static final String FROM_NAME = "Battleship Game";

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API – Gọi từ UserService và PasswordResetService
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gửi email xác thực địa chỉ email khi đăng ký tài khoản mới.
     *
     * Nội dung: HTML button dẫn đến verifyUrl, hết hạn sau 24 giờ.
     * verifyUrl = baseUrl + "/verify-email?token=<token>"
     *
     * @param to        địa chỉ email người nhận
     * @param verifyUrl link đầy đủ để xác thực (chứa token)
     * @throws MessagingException nếu gửi thất bại hoặc chưa cấu hình
     */
    public void sendVerificationEmail(String to, String verifyUrl) throws MessagingException {
        checkConfig(); // kiểm tra biến môi trường trước khi gửi

        // Soạn nội dung HTML email
        String body = "<div style='font-family:sans-serif;max-width:480px;margin:auto'>"
                + "<h2 style='color:#3b82f6'>⚓ Battleship</h2>"
                + "<p>Nhấn nút bên dưới để xác thực email của bạn:</p>"
                // Nút bấm xác thực – màu xanh, bo góc
                + "<a href='" + verifyUrl + "' style='display:inline-block;margin:20px 0;"
                + "padding:12px 28px;background:#3b82f6;color:#fff;border-radius:8px;"
                + "text-decoration:none;font-weight:600'>Xác thực Email</a>"
                + "<p style='color:#9ca3af;font-size:0.85em'>Link có hiệu lực trong 24 giờ.</p>"
                + "</div>";

        send(to, "[Battleship] Xác thực địa chỉ email", body);
    }

    /**
     * Gửi email đặt lại mật khẩu khi người dùng nhấn "Quên mật khẩu".
     *
     * Nội dung: HTML button dẫn đến resetUrl, hết hạn sau 1 giờ.
     * resetUrl = baseUrl + "/reset-password?token=<token>"
     *
     * @param to       địa chỉ email người nhận
     * @param resetUrl link đầy đủ để đặt lại mật khẩu (chứa token)
     * @throws MessagingException nếu gửi thất bại hoặc chưa cấu hình
     */
    public void sendResetPasswordEmail(String to, String resetUrl) throws MessagingException {
        checkConfig();

        // Soạn nội dung HTML email – màu đỏ để phân biệt với email xác thực
        String body = "<div style='font-family:sans-serif;max-width:480px;margin:auto'>"
                + "<h2 style='color:#3b82f6'>⚓ Battleship</h2>"
                + "<p>Nhấn nút bên dưới để đặt lại mật khẩu:</p>"
                // Nút bấm đặt lại – màu đỏ (cảnh báo hành động quan trọng)
                + "<a href='" + resetUrl + "' style='display:inline-block;margin:20px 0;"
                + "padding:12px 28px;background:#ef4444;color:#fff;border-radius:8px;"
                + "text-decoration:none;font-weight:600'>Đặt lại mật khẩu</a>"
                + "<p style='color:#9ca3af;font-size:0.85em'>Link có hiệu lực trong 1 giờ. "
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>"
                + "</div>";

        send(to, "[Battleship] Đặt lại mật khẩu", body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra các biến môi trường bắt buộc đã được set chưa.
     * Ném MessagingException (không phải RuntimeException) để caller có thể xử lý
     * và báo lỗi cụ thể cho người dùng/admin.
     */
    private void checkConfig() throws MessagingException {
        if (HOST == null || HOST.isEmpty()
                || USER == null || USER.isEmpty()
                || PASS == null || PASS.isEmpty()) {
            throw new MessagingException(
                    "Email chưa được cấu hình (MAIL_HOST, MAIL_USER, MAIL_PASS)");
        }
    }

    /**
     * Hàm gửi email thực sự qua Jakarta Mail (SMTP + STARTTLS).
     *
     * Luồng hoạt động:
     *   1. Tạo Properties cấu hình SMTP
     *   2. Tạo Session với Authenticator (cung cấp user/pass)
     *   3. Soạn MimeMessage (from, to, subject, body HTML)
     *   4. Gọi Transport.send() để gửi
     *
     * @param to       địa chỉ người nhận
     * @param subject  tiêu đề email
     * @param htmlBody nội dung HTML
     */
    private void send(String to, String subject, String htmlBody) throws MessagingException {

        // ── Bước 1: Cấu hình SMTP ────────────────────────────────────────────
        Properties props = new Properties();
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);
        props.put("mail.smtp.auth", "true");              // bắt buộc xác thực
        props.put("mail.smtp.starttls.enable", "true");   // mã hóa STARTTLS
        props.put("mail.smtp.connectiontimeout", "5000"); // timeout kết nối: 5 giây
        props.put("mail.smtp.timeout", "5000");           // timeout đọc/ghi: 5 giây

        // ── Bước 2: Tạo Session với xác thực ─────────────────────────────────
        // Authenticator là anonymous class cung cấp username/password SMTP
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USER, PASS);
            }
        });

        // ── Bước 3: Soạn nội dung email ──────────────────────────────────────
        Message msg = new MimeMessage(session);
        try {
            // Địa chỉ người gửi kèm tên hiển thị, mã hóa UTF-8 để tránh lỗi ký tự đặc biệt
            msg.setFrom(new InternetAddress(USER, FROM_NAME, "UTF-8"));

            // Địa chỉ người nhận (parse chuỗi email thành InternetAddress[])
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            // Tiêu đề email – encode Base64 UTF-8 để hiển thị đúng tiếng Việt
            msg.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));

            // Nội dung dạng HTML (không phải plain text)
            msg.setContent(htmlBody, "text/html; charset=UTF-8");

            // ── Bước 4: Gửi ──────────────────────────────────────────────────
            Transport.send(msg);

        } catch (UnsupportedEncodingException e) {
            // UnsupportedEncodingException không phải MessagingException
            // → wrap lại để caller chỉ cần catch MessagingException
            throw new MessagingException("Lỗi mã hóa ký tự UTF-8 khi gửi email", e);
        }
    }
}