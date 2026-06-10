package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * ============================================================
 * FILTER: AuthFilter (Bộ lọc xác thực – bảo vệ trang nội bộ)
 * ============================================================
 * Filter này hoạt động như một "người gác cổng" tự động.
 * Mọi request đến các URL được liệt kê bên dưới đều phải đi qua đây TRƯỚC
 * khi tới Servlet xử lý thực sự.
 *
 * Logic đơn giản:
 *   - Nếu session có "playerId"  → đã đăng nhập → cho đi tiếp (chain.doFilter)
 *   - Nếu session không có       → chưa đăng nhập → redirect về /login
 *
 * Các URL được bảo vệ (phải đăng nhập mới vào được):
 *   /setup, /game, /game.jsp, /history, /history.jsp,
 *   /leaderboard, /leaderboard.jsp, /matchDetail, /matchDetail.jsp
 *
 * Các URL KHÔNG được bảo vệ (public, ai cũng vào được):
 *   /login, /register, /verify-email, /forgot-password, /reset-password, ...
 * ============================================================
 */
@WebFilter(urlPatterns = {
        "/setup",
        "/game", "/game.jsp",
        "/history", "/history.jsp",
        "/leaderboard", "/leaderboard.jsp",
        "/matchDetail", "/matchDetail.jsp"
})
public class AuthFilter implements Filter {

    /**
     * Phương thức chính – được gọi với MỌI request khớp urlPatterns phía trên.
     *
     * @param request  request gốc (cast sang HttpServletRequest để lấy session)
     * @param response response gốc (cast sang HttpServletResponse để redirect)
     * @param chain    chuỗi filter tiếp theo; gọi chain.doFilter() = cho phép đi tiếp
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Cast sang HTTP để dùng được getSession() và sendRedirect()
        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // getSession(false) = lấy session ĐÃ TỒN TẠI, KHÔNG tạo mới nếu chưa có
        // (Dùng true sẽ tạo session trống, không phải mục đích ở đây)
        HttpSession session = req.getSession(false);

        // Kiểm tra session có tồn tại VÀ có chứa "playerId" không
        // "playerId" được set trong LoginServlet / GoogleCallbackServlet / FacebookCallbackServlet
        // khi đăng nhập thành công
        if (session != null && session.getAttribute("playerId") != null) {
            // ✅ Đã đăng nhập → cho đi tiếp đến Servlet thực sự
            chain.doFilter(request, response);
        } else {
            // ❌ Chưa đăng nhập → chuyển về trang login
            resp.sendRedirect(req.getContextPath() + "/login");
            // Không gọi chain.doFilter() → request dừng lại ở đây
        }
    }

    // Hai method dưới là bắt buộc theo interface Filter nhưng không cần xử lý gì
    @Override public void init(FilterConfig fc) {}    // khởi tạo filter (bỏ trống)
    @Override public void destroy() {}               // dọn dẹp khi server tắt (bỏ trống)
}