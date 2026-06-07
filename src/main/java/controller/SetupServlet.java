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

    // ─────────────────────────────────────────────
    // UC-04: Hiển thị trang thiết lập
    // ─────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.getRequestDispatcher("/setup.jsp").forward(req, resp);
    }

    // ─────────────────────────────────────────────
    // UC-04 + UC-05: Xử lý submit thiết lập trận đấu
    // ─────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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

        // Giá trị mặc định nếu client không gửi
        if (mode == null || mode.isEmpty())       mode = "PvE";
        if (difficulty == null || difficulty.isEmpty()) difficulty = "Easy";

        try {
            String roomId = UUID.randomUUID().toString();
            String boardId = UUID.randomUUID().toString();
            Board board = boardService.createBoard(boardId, roomId, playerId);

            Room room = new Room();
            room.setId(roomId);
            room.setName("Phòng đấu của " + playerId);
            room.setMode(mode);
            room.setDifficulty(difficulty);
            room.setStatus("playing");
            room.setPlayer1Id(playerId);

            String p1Name = (String) session.getAttribute("playerName");
            room.setPlayer1Name(p1Name != null ? p1Name : "Người chơi 1");

            // UC-04: Thiết lập đối thủ theo chế độ
            if ("PvP".equals(mode)) {
                String p2name = req.getParameter("player2Name");
                room.setPlayer2Name(
                    (p2name != null && !p2name.isEmpty()) ? p2name : "Người chơi 2"
                );
            } else {
                room.setPlayer2Name("AI");
            }

            // UC-05: Đặt thuyền (Auto hoặc Manual)
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

    // ─────────────────────────────────────────────
    // UC-05: Parse JSON thuyền từ client và đặt lên Board
    //   - Kiểm tra đủ 5 thuyền (BR-02)
    //   - Mỗi thuyền qua isValidPlacement (BR-03)
    //   - Kiểm tra hạm đội đúng chuẩn {5,4,3,3,2} (BR-02)
    // ─────────────────────────────────────────────
    private void parseAndPlaceShips(Board board, String shipsJson) throws Exception {
        if (shipsJson == null || shipsJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Không nhận được dữ liệu thiết lập thuyền!");
        }
        shipsJson = shipsJson.trim();
        // Loại bỏ ngoặc mảng bên ngoài
        if (shipsJson.startsWith("[")) shipsJson = shipsJson.substring(1);
        if (shipsJson.endsWith("]"))   shipsJson = shipsJson.substring(0, shipsJson.length() - 1);

        if (shipsJson.isEmpty()) {
            throw new IllegalArgumentException("Danh sách thuyền trống!");
        }

        // Phân tách từng object JSON thuyền
        String[] entries = shipsJson.split("\\},\\s*\\{");
        if (entries.length != 5) {
            throw new IllegalArgumentException("Bạn phải đặt chính xác 5 thuyền!");
        }

        for (String entry : entries) {
            String type = extractJsonValue(entry, "type");
            String lengthStr = extractJsonValue(entry, "length");
            String xStr = extractJsonValue(entry, "x");
            String yStr = extractJsonValue(entry, "y");
            String dir = extractJsonValue(entry, "dir");

            if (type.isEmpty() || lengthStr.isEmpty() || xStr.isEmpty() || yStr.isEmpty() || dir.isEmpty()) {
                throw new IllegalArgumentException("Dữ liệu thông tin thuyền không hợp lệ!");
            }

            int length = Integer.parseInt(lengthStr);
            int x      = Integer.parseInt(xStr);
            int y      = Integer.parseInt(yStr);

                Ship ship = new Ship();
                ship.setId(UUID.randomUUID().toString());
                ship.setBoardId(board.getId());
                ship.setType(type);
                ship.setLength(length);
                ship.setStartX(x);
                ship.setStartY(y);
            ship.setDirection(dir);

            // UC-05: Kiểm tra hợp lệ trước khi đặt (BR-03)
            if (!boardService.isValidPlacement(board, ship)) {
                char colLetter = (char) ('A' + x);
                int rowNumber  = y + 1;
                throw new IllegalArgumentException(
                    "Thuyền " + type + " ở vị trí " + colLetter + rowNumber
                    + " hướng " + ("H".equals(dir) ? "Ngang" : "Dọc")
                    + " không hợp lệ (vượt biên hoặc bị chồng lên thuyền khác)!"
                );
            }
            boardService.placeShip(board, ship);
        }

        // Kiểm tra hạm đội đúng chuẩn {5,4,3,3,2} (BR-02)
        if (!boardService.isValidFleet(board)) {
            throw new IllegalArgumentException(
                "Đội hình thuyền không đúng 5 thuyền tiêu chuẩn (5, 4, 3, 3, 2)!"
            );
    }
    }

    // ─────────────────────────────────────────────
    // Tiện ích: trích xuất giá trị từ JSON string thô
    // ─────────────────────────────────────────────
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx);
        int start = colon + 1;
        while (start < json.length()
               && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length()
               && json.charAt(end) != '"'
               && json.charAt(end) != ','
               && json.charAt(end) != '}') end++;
        return json.substring(start, end).replace("\"", "").trim();
    }
}
