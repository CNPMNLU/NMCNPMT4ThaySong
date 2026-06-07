package controller;

import service.UserService;
import model.Player;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * ============================================================
 * CONTROLLER: RegisterServlet  –  Đăng ký tài khoản thường
 * ============================================================
 * URL mapping: /register
 *
 * GET  /register  → Hiển thị form đăng ký (register.jsp)
 *                   Nếu đã đăng nhập rồi → redirect /setup
 *
 * POST /register  → Nhận dữ liệu form, gọi UserService.register()
 *
 *   Sau khi đăng ký thành công:
 *     EMAIL_ENABLED=true  → lưu pendingVerifyId vào session
 *                           redirect sang pending-verification.jsp
 *                           (trang thông báo "Kiểm tra email của bạn")
 *     EMAIL_ENABLED=false → tạo session đăng nhập luôn, redirect /setup
 *
 *   Nếu có lỗi (trùng username, email sai định dạng, v.v.)
 *     → quay lại register.jsp, giữ lại username/email đã nhập
 * ============================================================
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    /** UserService chứa toàn bộ logic validate + tạo user */
    private final UserService userService = new UserService();

    /**
     * Đọc cùng biến môi trường với UserService để quyết định luồng sau đăng ký.
     * EMAIL_ENABLED=true  → bắt xác thực email
     * EMAIL_ENABLED=false → đăng nhập luôn (dev mode)
     */
    private static final boolean EMAIL_ENABLED =
            "true".equalsIgnoreCase(System.getenv("EMAIL_ENABLED"));

    // ════════════════════════════════════════════════════════════════════════
    // GET – Hiển thị form đăng ký
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Nếu đã đăng nhập rồi thì không cần đăng ký nữa
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("playerId") != null) {
            resp.sendRedirect(req.getContextPath() + "/setup");
            return;
        }

        // Chưa đăng nhập → hiển thị form đăng ký
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST – Xử lý form đăng ký
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Đặt encoding UTF-8 để đọc đúng tiếng Việt từ form
        req.setCharacterEncoding("UTF-8");

        // Lấy dữ liệu từ form HTML (name="username", name="password", ...)
        String username        = req.getParameter("username");
        String password        = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String email           = req.getParameter("email");

        // ── Validate sơ bộ ở Controller (trước khi gọi Service) ─────────────
        // Validate ở đây chỉ để kiểm tra "có nhập vào hay không"
        // Validate chi tiết hơn (độ dài, định dạng email, trùng DB) do UserService làm

        if (username == null || username.trim().isEmpty()) {
            error(req, resp, "Vui lòng nhập tên đăng nhập", username, email); return;
        }
        if (email == null || email.trim().isEmpty()) {
            error(req, resp, "Vui lòng nhập email", username, email); return;
        }
        if (password == null || password.isEmpty()) {
            error(req, resp, "Vui lòng nhập mật khẩu", username, email); return;
        }
        // Kiểm tra 2 ô mật khẩu có khớp nhau không (trước khi gọi Service)
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            error(req, resp, "Mật khẩu xác nhận không khớp", username, email); return;
        }

        try {
            // Ghép baseUrl để UserService tạo link xác thực email đầy đủ
            // Ví dụ: "http://localhost:8080/battleship"
            String baseUrl = req.getScheme() + "://"    // http hoặc https
                    + req.getServerName() + ":"  // localhost hoặc domain
                    + req.getServerPort()        // 8080, 443, ...
                    + req.getContextPath();      // /battleship hoặc ""

            // Gọi UserService xử lý toàn bộ logic đăng ký
            Player p = userService.register(username, password, email, baseUrl);

            // ── Xử lý sau khi đăng ký thành công ────────────────────────────
            if (EMAIL_ENABLED) {
                // Production: lưu userId vào session để trang pending biết cần verify ai
                req.getSession(true).setAttribute("pendingVerifyId", p.getId());
                // Redirect sang trang thông báo "Hãy kiểm tra email của bạn"
                resp.sendRedirect(req.getContextPath() + "/pending-verification.jsp");
            } else {
                // Dev mode: tạo session đăng nhập luôn, không cần xác thực email
                HttpSession session = req.getSession(true);
                session.setAttribute("playerId",   p.getId());
                session.setAttribute("playerName", p.getUsername());
                session.setMaxInactiveInterval(3600); // session tồn tại 1 giờ không hoạt động
                resp.sendRedirect(req.getContextPath() + "/setup");
            }

        } catch (Exception e) {
            // UserService ném IllegalArgumentException với message mô tả lỗi cụ thể
            // (vd: "Username đã tồn tại", "Email không đúng định dạng", ...)
            error(req, resp,
                    e.getMessage() != null ? e.getMessage() : "Đăng ký thất bại",
                    username, email);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER – Hiển thị lỗi và giữ lại dữ liệu người dùng đã nhập
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Set thông báo lỗi và dữ liệu đã nhập vào request, rồi forward về register.jsp.
     *
     * "savedUsername" và "savedEmail" được dùng trong register.jsp để điền lại
     * vào ô input, tránh người dùng phải nhập lại từ đầu khi có lỗi.
     * (UX tốt: chỉ xóa mật khẩu khi lỗi, giữ lại username và email)
     */
    private void error(HttpServletRequest req, HttpServletResponse resp,
                       String msg, String u, String e)
            throws ServletException, IOException {
        req.setAttribute("error",         msg);   // thông báo lỗi hiển thị trong JSP
        if (u != null) req.setAttribute("savedUsername", u.trim()); // điền lại username
        if (e != null) req.setAttribute("savedEmail",    e.trim()); // điền lại email
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }
}