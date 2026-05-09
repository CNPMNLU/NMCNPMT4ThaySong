package controller;

import dao.LeaderboardDAO;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/leaderboard")
public class LeaderboardServlet extends HttpServlet {
    private final LeaderboardDAO dao = new LeaderboardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            List<Map<String,Object>> top = dao.getTopPlayers(20);
            req.setAttribute("topPlayers", top);
            req.getRequestDispatcher("/leaderboard.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/leaderboard.jsp").forward(req, resp);
        }
    }
}
