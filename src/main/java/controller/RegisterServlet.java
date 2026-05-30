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

        // --- Validate: username ---
        if (username == null || username.trim().isEmpty()) {
            req.setAttribute("error", "Vui lòng nhập tên đăng nhập");
            preserveInput(req, username, email);
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        // --- Validate: password ---
        if (password == null || password.isEmpty()) {
            req.setAttribute("error", "Vui lòng nhập mật khẩu");
            preserveInput(req, username, email);
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        // --- Validate: confirmPassword ---
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            req.setAttribute("error", "Vui lòng xác nhận mật khẩu");
            preserveInput(req, username, email);
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Mật khẩu xác nhận không khớp");
            preserveInput(req, username, email);
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        try {
            Player player = userService.register(username, password, email);
            HttpSession session = req.getSession(true);
            session.setAttribute("playerId",   player.getId());
            session.setAttribute("playerName", player.getUsername());
            resp.sendRedirect(req.getContextPath() + "/setup");
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage() != null ? e.getMessage() : "Đăng ký thất bại");
            preserveInput(req, username, email);
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
        }
    }

    private void preserveInput(HttpServletRequest req, String username, String email) {
        if (username != null) req.setAttribute("savedUsername", username.trim());
        if (email    != null) req.setAttribute("savedEmail",    email.trim());
    }
}