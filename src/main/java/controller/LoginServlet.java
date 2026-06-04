package controller;

import service.UserService;
import model.Player;
import dao.UserDAO;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserService userService = new UserService();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("playerId") != null) {
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
            Player player = userService.authenticate(username.trim(), password);
            HttpSession session = req.getSession(true);
            session.setAttribute("playerId",   player.getId());
            session.setAttribute("playerName", player.getUsername());
            session.setMaxInactiveInterval(60 * 60);
            resp.sendRedirect(req.getContextPath() + "/setup");

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("EMAIL_NOT_VERIFIED:")) {
                String userId = msg.substring("EMAIL_NOT_VERIFIED:".length());
                req.getSession(true).setAttribute("pendingVerifyId", userId);
                resp.sendRedirect(req.getContextPath() + "/pending-verification");
                return;
            }
            req.setAttribute("error", msg);
            req.setAttribute("savedUsername", username);
            req.getRequestDispatcher("/login.jsp").forward(req, resp);

        } catch (SQLException e) {
            req.setAttribute("error", "Lỗi kết nối cơ sở dữ liệu. Vui lòng thử lại sau.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Đã xảy ra lỗi không mong muốn: " + e.getMessage());
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
