package controller;

import service.UserService;
import model.Player;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("playerId") != null) {
            resp.sendRedirect(req.getContextPath() + "/setup"); return;
        }
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username        = req.getParameter("username");
        String password        = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String email           = req.getParameter("email");

        if (username == null || username.trim().isEmpty()) {
            forwardError(req, resp, "Vui lòng nhập tên đăng nhập", username, email); return;
        }
        if (password == null || password.isEmpty()) {
            forwardError(req, resp, "Vui lòng nhập mật khẩu", username, email); return;
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            forwardError(req, resp, "Vui lòng xác nhận mật khẩu", username, email); return;
        }
        if (!password.equals(confirmPassword)) {
            forwardError(req, resp, "Mật khẩu xác nhận không khớp", username, email); return;
        }

        try {
            String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
            Player player = userService.register(username, password, email, baseUrl);
            req.getSession(true).setAttribute("pendingVerifyId", player.getId());
            resp.sendRedirect(req.getContextPath() + "/pending-verification");
        } catch (Exception e) {
            forwardError(req, resp, e.getMessage() != null ? e.getMessage() : "Đăng ký thất bại", username, email);
        }
    }

    private void forwardError(HttpServletRequest req, HttpServletResponse resp, String msg, String username, String email)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        if (username != null) req.setAttribute("savedUsername", username.trim());
        if (email    != null) req.setAttribute("savedEmail", email.trim());
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }
}
