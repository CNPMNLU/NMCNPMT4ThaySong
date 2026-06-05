package controller;

import dao.UserDAO;
import model.Player;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@WebServlet("/auth/facebook/callback")
public class FacebookCallbackServlet extends HttpServlet {

    private static final String APP_ID       = System.getenv("FACEBOOK_APP_ID");
    private static final String APP_SECRET   = System.getenv("FACEBOOK_APP_SECRET");
    private static final String REDIRECT_URI = System.getenv("FACEBOOK_REDIRECT_URI");
    private static final String TOKEN_URL    = "https://graph.facebook.com/v19.0/oauth/access_token";
    private static final String USERINFO_URL = "https://graph.facebook.com/me?fields=id,name,email";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String state = req.getParameter("state");
        String saved = (String) req.getSession().getAttribute("oauth_state_facebook");
        req.getSession().removeAttribute("oauth_state_facebook");

        if (saved == null || !saved.equals(state)) {
            error(req, resp, "Yêu cầu không hợp lệ. Vui lòng thử lại."); return;
        }
        if (req.getParameter("error") != null) {
            error(req, resp, "Đăng nhập Facebook bị huỷ."); return;
        }
        String code = req.getParameter("code");
        if (code == null || code.isEmpty()) {
            error(req, resp, "Không nhận được mã xác thực từ Facebook."); return;
        }
        try {
            String tokenUrl = TOKEN_URL
                    + "?client_id="     + URLEncoder.encode(APP_ID,       "UTF-8")
                    + "&client_secret=" + URLEncoder.encode(APP_SECRET,   "UTF-8")
                    + "&redirect_uri="  + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                    + "&code="          + URLEncoder.encode(code,         "UTF-8");

            String tokenResponse = get(tokenUrl);
            String accessToken   = extractJsonString(tokenResponse, "access_token");

            String userInfoResponse = get(USERINFO_URL + "&access_token=" + URLEncoder.encode(accessToken, "UTF-8"));
            String facebookId = extractJsonString(userInfoResponse, "id");
            String email      = extractJsonString(userInfoResponse, "email");
            String name       = extractJsonString(userInfoResponse, "name");

            Player p = userDAO.findByFacebookId(facebookId);
            if (p == null) {
                p = new Player();
                p.setId(UUID.randomUUID().toString());
                p.setFacebookId(facebookId);
                p.setEmail(email.isEmpty() ? null : email);
                p.setEmailVerified(true);
                p.setUsername(uniqueUsername(sanitize(name)));
                userDAO.insertFacebookUser(p);
            }
            HttpSession s = req.getSession(true);
            s.setAttribute("playerId",   p.getId());
            s.setAttribute("playerName", p.getUsername());
            s.setMaxInactiveInterval(3600);
            resp.sendRedirect(req.getContextPath() + "/setup");
        } catch (Exception e) {
            getServletContext().log("FacebookCallbackServlet", e);
            error(req, resp, "Đăng nhập Facebook thất bại. Vui lòng thử lại.");
        }
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) return "";
        int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
        if (colonIdx == -1) return "";
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart == -1) return "";
        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            if (json.charAt(quoteEnd) == '"' && json.charAt(quoteEnd - 1) != '\\') break;
            quoteEnd++;
        }
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        InputStream is = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String sanitize(String name) {
        if (name == null || name.trim().isEmpty()) return "user";
        String s = name.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        return s.isEmpty() ? "user" : s.substring(0, Math.min(s.length(), 20));
    }

    private String uniqueUsername(String base) throws Exception {
        String c = base; int i = 1;
        while (userDAO.findByUsername(c) != null) c = base + i++;
        return c;
    }

    private void error(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }
}