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

        String playerId = (String) session.getAttribute("playerId");

        try {
            List<Map<String,Object>> top = dao.getTopPlayersByElo(100);

            Map<String,Object> userRank = dao.getPlayerRank(playerId);

            String userTrend = dao.getPlayerTrend(playerId);

            req.setAttribute("topPlayers", top);
            req.setAttribute("userRank", userRank);
            req.setAttribute("userTrend", userTrend);
            req.getRequestDispatcher("/leaderboard.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/leaderboard.jsp").forward(req, resp);
        }
    }
}