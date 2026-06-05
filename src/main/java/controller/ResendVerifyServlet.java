package controller;

import service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/resend-verification")
public class ResendVerifyServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = req.getParameter("userId");
        if (userId == null || userId.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/login"); return;
        }
        try {
            String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
            userService.resendVerification(userId.trim(), baseUrl);
            req.setAttribute("verifyStatus", "resent");
        } catch (Exception e) {
            req.setAttribute("verifyStatus", "error");
        }
        req.getRequestDispatcher("/verify-email.jsp").forward(req, resp);
    }
}
