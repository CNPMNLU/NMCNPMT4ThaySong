package service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = System.getenv("MAIL_HOST");
    private static final String SMTP_PORT = System.getenv("MAIL_PORT") != null ? System.getenv("MAIL_PORT") : "587";
    private static final String SMTP_USER = System.getenv("MAIL_USER");
    private static final String SMTP_PASS = System.getenv("MAIL_PASS");
    private static final String FROM_NAME = "Battleship Game";

    public void sendVerificationEmail(String toEmail, String verifyUrl) throws MessagingException {
        String subject = "[Battleship] Xác thực địa chỉ email của bạn";
        String body = "<div style='font-family:sans-serif;max-width:480px;margin:auto'>"
            + "<h2 style='color:#3b82f6'>⚓ Battleship</h2>"
            + "<p>Cảm ơn bạn đã đăng ký. Nhấn nút bên dưới để xác thực email:</p>"
            + "<a href='" + verifyUrl + "' style='display:inline-block;margin:20px 0;padding:12px 28px;"
            + "background:#3b82f6;color:#fff;border-radius:8px;text-decoration:none;font-weight:600'>"
            + "Xác thực Email</a>"
            + "<p style='color:#9ca3af;font-size:0.85em'>Link có hiệu lực trong 24 giờ.<br>"
            + "Nếu bạn không đăng ký tài khoản này, hãy bỏ qua email này.</p>"
            + "</div>";
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_USER, FROM_NAME, "UTF-8"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
        msg.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(msg);
    }
}
