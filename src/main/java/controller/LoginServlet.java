package controller;

import service.UserService;
import model.Player;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("playerId") != null) {
            resp.sendRedirect(req.getContextPath() + "/setup"); return;
        }
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.trim().isEmpty()) {
            req.setAttribute("error", "Vui lòng nhập tên đăng nhập");
            req.getRequestDispatcher("/login.jsp").forward(req, resp); return;
        }
        if (password == null || password.isEmpty()) {
            req.setAttribute("error", "Vui lòng nhập mật khẩu");
            req.getRequestDispatcher("/login.jsp").forward(req, resp); return;
        }

        try {
            Player p = userService.authenticate(username.trim(), password);
            HttpSession session = req.getSession(true);
            session.setAttribute("playerId",   p.getId());
            session.setAttribute("playerName", p.getUsername());
            session.setMaxInactiveInterval(3600);
            resp.sendRedirect(req.getContextPath() + "/setup");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("EMAIL_NOT_VERIFIED:")) {
                String userId = msg.substring("EMAIL_NOT_VERIFIED:".length());
                req.getSession(true).setAttribute("pendingVerifyId", userId);
                resp.sendRedirect(req.getContextPath() + "/pending-verification.jsp");
                return;
            }
            req.setAttribute("error", msg);
            req.setAttribute("savedUsername", username.trim());
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Lỗi hệ thống. Vui lòng thử lại sau.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
