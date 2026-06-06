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

    private static final boolean EMAIL_ENABLED =
        "true".equalsIgnoreCase(System.getenv("EMAIL_ENABLED"));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("playerId") != null) {
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
            error(req, resp, "Vui lòng nhập tên đăng nhập", username, email); return;
        }
        if (email == null || email.trim().isEmpty()) {
            error(req, resp, "Vui lòng nhập email", username, email); return;
        }
        if (password == null || password.isEmpty()) {
            error(req, resp, "Vui lòng nhập mật khẩu", username, email); return;
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            error(req, resp, "Mật khẩu xác nhận không khớp", username, email); return;
        }

        try {
            String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();
            Player p = userService.register(username, password, email, baseUrl);

            if (EMAIL_ENABLED) {
                req.getSession(true).setAttribute("pendingVerifyId", p.getId());
                resp.sendRedirect(req.getContextPath() + "/pending-verification.jsp");
            } else {
                // Dev mode: đăng ký xong → login luôn
                HttpSession session = req.getSession(true);
                session.setAttribute("playerId",   p.getId());
                session.setAttribute("playerName", p.getUsername());
                session.setMaxInactiveInterval(3600);
                resp.sendRedirect(req.getContextPath() + "/setup");
            }
        } catch (Exception e) {
            error(req, resp, e.getMessage() != null ? e.getMessage() : "Đăng ký thất bại", username, email);
        }
    }

    private void error(HttpServletRequest req, HttpServletResponse resp, String msg, String u, String e)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        if (u != null) req.setAttribute("savedUsername", u.trim());
        if (e != null) req.setAttribute("savedEmail", e.trim());
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }
}
