package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebFilter(urlPatterns = {
        "/setup",
        "/game",
        "/game.jsp",
        "/history",
        "/history.jsp",
        "/leaderboard",
        "/leaderboard.jsp",
        "/matchDetail",
        "/matchDetail.jsp"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("playerId") != null;

        if (!loggedIn) {
            String requestedUrl = req.getRequestURI();
            String query        = req.getQueryString();
            if (query != null) requestedUrl += "?" + query;

            req.getSession(true).setAttribute("redirectAfterLogin", requestedUrl);
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}