package controller;

import model.*;
import service.AIService;
import service.BoardService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.UUID;

/**
 * KhoaDang: Gọi aiService.reset() khi bắt đầu game mới.
 *
 * Vấn đề cũ:
 *   - SetupServlet chỉ set session.setAttribute("aiService", null)
 *   - Khi GameServlet.doGet() tạo AIService mới, nếu session cũ vẫn còn
 *     và AIService chưa bị null (race condition hoặc code path khác),
 *     huntQueue của game trước bị giữ lại → Hard AI bắn tọa độ game cũ.
 *
 * Fix:
 *   - Vẫn set null để GameServlet tạo mới (behavior không đổi)
 *   - Thêm: nếu aiService còn tồn tại trong session trước khi null hóa,
 *     gọi reset() tường minh trước — đảm bảo không rò rỉ state dù
 *     object bị reuse bởi bất kỳ code path nào khác.
 *   - Xóa cả board_<playerId> cũ để tránh board game trước bị dùng lại.
 */
@WebServlet("/setup")
public class SetupServlet extends HttpServlet {
    private final BoardService boardService = new BoardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.getRequestDispatcher("/setup.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String playerId = (String) session.getAttribute("playerId");
        String action = req.getParameter("action");
        String mode = req.getParameter("mode");
        String difficulty = req.getParameter("difficulty");
        if (mode == null || mode.isEmpty()) mode = "PvE";
        if (difficulty == null || difficulty.isEmpty()) difficulty = "Easy";

        try {
            String roomId = UUID.randomUUID().toString();
            String boardId = UUID.randomUUID().toString();
            Board board = boardService.createBoard(boardId, roomId, playerId);

            if ("auto".equals(action)) {
                boardService.autoPlace(board);
            } else {
                String shipsJson = req.getParameter("ships");
                if (shipsJson != null && !shipsJson.isEmpty()) {
                    parseAndPlaceShips(board, shipsJson);
                } else {
                    boardService.autoPlace(board);
                }
            }
            board.setReady(true);

            // ------------------------------------------------------------------
            // KhoaDang: Reset AIService trước khi xóa khỏi session.
            //
            // Thứ tự quan trọng:
            //   1. Lấy ra instance cũ
            //   2. Gọi reset() — xóa huntQueue, lastHit, huntDirection
            //   3. Sau đó mới null hóa trong session
            //
            // Lý do không chỉ null hóa: nếu GameServlet giữ reference
            // tới AIService cũ trong cùng request (hiếm nhưng có thể),
            // reset() đảm bảo state bị xóa ngay cả trường hợp đó.
            // ------------------------------------------------------------------

            AIService existingAI = (AIService) session.getAttribute("aiService");
            if (existingAI != null) {
                existingAI.reset(); // Xóa huntQueue game cũ
            }
            // Xóa board cũ của player khỏi session — tránh board game trước
            // bị getBoardByRoomAndOwner() trả về cho game mới
            session.removeAttribute("board_" + playerId);

            // Ghi session mới
            session.setAttribute("roomId", roomId);
            session.setAttribute("boardId", boardId);
            session.setAttribute("mode", mode);
            session.setAttribute("difficulty", difficulty);
            session.setAttribute("board", board);
            session.setAttribute("board_" + playerId, board);
            session.setAttribute("gameState", null);
            session.setAttribute("aiBoard", null);
            session.setAttribute("aiService", null);

            if ("PvP".equals(mode)) {
                String shipsJsonRaw = req.getParameter("ships");
                session.setAttribute("shipsJson", shipsJsonRaw != null ? shipsJsonRaw : "[]");
                String p2name = req.getParameter("player2Name");
                session.setAttribute("player2Name", (p2name != null && !p2name.isEmpty()) ? p2name : "Người chơi 2");
            }

            resp.sendRedirect(req.getContextPath() + "/game");
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Lỗi thiết lập: " + e.getMessage());
            req.getRequestDispatcher("/setup.jsp").forward(req, resp);
        }
    }

    private void parseAndPlaceShips(Board board, String shipsJson) {
        shipsJson = shipsJson.trim();
        // Remove outer brackets
        if (shipsJson.startsWith("[")) shipsJson = shipsJson.substring(1);
        if (shipsJson.endsWith("]")) shipsJson = shipsJson.substring(0, shipsJson.length() - 1);

        // Split by },{
        String[] entries = shipsJson.split("\\},\\s*\\{");
        for (String entry : entries) {
            try {
                String type = extractJsonValue(entry, "type");
                int length = Integer.parseInt(extractJsonValue(entry, "length"));
                int x = Integer.parseInt(extractJsonValue(entry, "x"));
                int y = Integer.parseInt(extractJsonValue(entry, "y"));
                String dir = extractJsonValue(entry, "dir");

                Ship ship = new Ship();
                ship.setId(UUID.randomUUID().toString());
                ship.setBoardId(board.getId());
                ship.setType(type);
                ship.setLength(length);
                ship.setStartX(x);
                ship.setStartY(y);
                ship.setDirectionFromString(dir);
                boardService.placeShip(board, ship);
            } catch (Exception ignored) {
            }
        }
        if (board.getShips().size() < 5) boardService.autoPlace(board);
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx);
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}')
            end++;
        return json.substring(start, end).replace("\"", "").trim();
    }
}
