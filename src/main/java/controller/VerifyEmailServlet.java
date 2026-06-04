package controller;

import dao.UserDAO;
import model.Player;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/verify-email")
public class VerifyEmailServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            req.setAttribute("verifyStatus", "invalid");
            req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
            return;
        }
        try {
            Player p = userDAO.findByVerifyToken(token.trim());
            if (p == null) {
                req.setAttribute("verifyStatus", "invalid");
                req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
                return;
            }
            if (p.isEmailVerified()) {
                req.setAttribute("verifyStatus", "already");
                req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
                return;
            }
            if (p.getVerifySentAt() != null && p.getVerifySentAt().isBefore(LocalDateTime.now().minusHours(24))) {
                req.setAttribute("verifyStatus", "expired");
                req.setAttribute("expiredUserId", p.getId());
                req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
                return;
            }
            userDAO.markEmailVerified(p.getId());
            req.setAttribute("verifyStatus", "success");
            req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("verifyStatus", "error");
            req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
        }
    }
}
