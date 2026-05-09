package controller;

import dao.GameHistoryDAO;
import model.GameRecord;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/history")
public class HistoryServlet extends HttpServlet {
    private final GameHistoryDAO dao = new GameHistoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String playerId = (String) session.getAttribute("playerId");
        try {
            List<GameRecord> records = dao.findByUserId(playerId);
            req.setAttribute("records", records);
            req.getRequestDispatcher("/history.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/history.jsp").forward(req, resp);
        }
    }
}
