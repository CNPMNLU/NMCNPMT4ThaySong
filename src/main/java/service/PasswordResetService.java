package service;

import dao.UserDAO;
import model.Player;
import java.time.LocalDateTime;

public class PasswordResetService {
    private final UserDAO userDAO = new UserDAO();
    private final EmailService emailService = new EmailService();

    public void requestReset(String email, String baseUrl) throws Exception {
        Player p = userDAO.findByEmail(email.trim());
        if (p == null) return;
        String token = UserService.generateToken();
        userDAO.saveResetToken(p.getId(), token, LocalDateTime.now().plusHours(1));
        emailService.sendResetPasswordEmail(p.getEmail(), baseUrl + "/reset-password?token=" + token);
    }

    public Player validateToken(String token) throws Exception {
        if (token == null || token.trim().isEmpty())
            throw new IllegalArgumentException("Link không hợp lệ");
        Player p = userDAO.findByResetToken(token.trim());
        if (p == null)
            throw new IllegalArgumentException("Link đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng");
        if (p.getResetTokenExpiry() == null || p.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Link đã hết hạn. Vui lòng yêu cầu lại.");
        return p;
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) throws Exception {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới phải ít nhất 6 ký tự");
        if (!newPassword.equals(confirmPassword))
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        Player p = validateToken(token);
        userDAO.updatePasswordAndClearResetToken(p.getId(), UserService.hashPassword(newPassword));
    }
}
