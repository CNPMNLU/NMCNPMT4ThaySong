package controller;

import dao.GameHistoryDAO;
import model.GameRecord;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
            // Lấy tham số từ UI
            String pageStr = req.getParameter("page");
            String mode = req.getParameter("mode");
            String period = req.getParameter("period");

            int page = 1;
            if (pageStr != null && !pageStr.isBlank()) {
                page = Math.max(1, Integer.parseInt(pageStr));
            }

            List<GameRecord> records;
            int totalGames;

            // Lọc theo mode (PvE, PvP)
            if ("PvE".equals(mode) || "PvP".equals(mode)) {
                records = dao.findByMode(playerId, mode);
            }
            // Lọc theo kỳ hạn (week, month, all)
            else if ("week".equals(period) || "month".equals(period)) {
                records = dao.findByDateRange(playerId, period);
            }
            // Mặc định: tất cả lịch sử
            else {
                records = dao.findByUserId(playerId);
            }

            // Tính toán phân trang
            final int PAGE_SIZE = 10;
            int totalPages = (int) Math.ceil((double) records.size() / PAGE_SIZE);
            int startIdx = (page - 1) * PAGE_SIZE;
            int endIdx = Math.min(startIdx + PAGE_SIZE, records.size());

            List<GameRecord> pageRecords = records.subList(startIdx, endIdx);

            // Tính thống kê toàn bộ
            Map<String, Object> stats = dao.getPlayerStats(playerId);

            req.setAttribute("records", pageRecords);
            req.setAttribute("stats", stats);
            req.setAttribute("currentPage", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("totalGames", records.size());

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Không thể tải lịch sử: " + e.getMessage());
        }
        req.getRequestDispatcher("/history.jsp").forward(req, resp);
    }
}
