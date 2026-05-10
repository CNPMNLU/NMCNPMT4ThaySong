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
        String matchId  = req.getParameter("id");

        // ── Xem chi tiết 1 trận ────────────────────────────────────────────
        if (matchId != null && !matchId.isBlank()) {
            try {
                GameRecord detail = dao.findById(matchId);
                if (detail == null) {
                    req.setAttribute("error", "Trận đấu không tồn tại.");
                    showList(req, resp, playerId);
                    return;
                }
                // Kiểm tra quyền: chỉ player của trận mới được xem
                boolean isOwner = playerId.equals(detail.getPlayer1Id())
                               || playerId.equals(detail.getPlayer2Id());
                if (!isOwner) {
                    req.setAttribute("error", "Bạn không có quyền xem trận đấu này (Access Denied).");
                    showList(req, resp, playerId);
                    return;
                }
                req.setAttribute("matchDetail", detail);
                req.getRequestDispatcher("/matchDetail.jsp").forward(req, resp);
            } catch (Exception e) {
                e.printStackTrace();
                req.setAttribute("error", "Lỗi khi tải chi tiết trận: " + e.getMessage());
                showList(req, resp, playerId);
            }
            return;
        }

        // ── Danh sách lịch sử ──────────────────────────────────────────────
        showList(req, resp, playerId);
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, String playerId)
            throws ServletException, IOException {
        try {
            List<GameRecord> records = dao.findByUserId(playerId);
            req.setAttribute("records", records);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không thể tải lịch sử: " + e.getMessage());
        }
        req.getRequestDispatcher("/history.jsp").forward(req, resp);
    }
}
