package controller;

import service.PasswordResetService;
import model.Player;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {
    private final PasswordResetService resetService = new PasswordResetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        try {
            Player p = resetService.validateToken(token);
            req.setAttribute("token", token);
            req.setAttribute("username", p.getUsername());
        } catch (Exception e) {
            req.setAttribute("tokenError", e.getMessage());
        }
        req.getRequestDispatcher("/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String token    = req.getParameter("token");
        String newPass  = req.getParameter("newPassword");
        String confirm  = req.getParameter("confirmPassword");
        try {
            resetService.resetPassword(token, newPass, confirm);
            req.setAttribute("resetSuccess", true);
        } catch (Exception e) {
            req.setAttribute("token", token);
            req.setAttribute("error", e.getMessage());
        }
        req.getRequestDispatcher("/reset-password.jsp").forward(req, resp);
    }
}
