package controller;

import dao.UserDAO;
import model.Player;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@WebServlet("/auth/google/callback")
public class GoogleCallbackServlet extends HttpServlet {

    private static final String CLIENT_ID     = System.getenv("GOOGLE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET");
    private static final String REDIRECT_URI  = System.getenv("GOOGLE_REDIRECT_URI");
    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String state = req.getParameter("state");
        String saved = (String) req.getSession().getAttribute("oauth_state_google");
        req.getSession().removeAttribute("oauth_state_google");

        if (saved == null || !saved.equals(state)) {
            error(req, resp, "Yêu cầu không hợp lệ. Vui lòng thử lại."); return;
        }
        if (req.getParameter("error") != null) {
            error(req, resp, "Đăng nhập Google bị huỷ."); return;
        }
        String code = req.getParameter("code");
        if (code == null || code.isEmpty()) {
            error(req, resp, "Không nhận được mã xác thực từ Google."); return;
        }
        try {
            String token    = new JSONObject(post(TOKEN_URL,
                "code="           + URLEncoder.encode(code,          "UTF-8")
              + "&client_id="     + URLEncoder.encode(CLIENT_ID,     "UTF-8")
              + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, "UTF-8")
              + "&redirect_uri="  + URLEncoder.encode(REDIRECT_URI,  "UTF-8")
              + "&grant_type=authorization_code")).getString("access_token");

            JSONObject info = new JSONObject(get(USERINFO_URL, token));
            String googleId = info.getString("sub");
            String email    = info.optString("email", "");
            String name     = info.optString("name",  "");

            Player p = userDAO.findByGoogleId(googleId);
            if (p == null) {
                p = new Player();
                p.setId(UUID.randomUUID().toString());
                p.setGoogleId(googleId);
                p.setEmail(email.isEmpty() ? null : email);
                p.setEmailVerified(true);
                p.setUsername(uniqueUsername(sanitize(name)));
                userDAO.insertGoogleUser(p);
            }
            HttpSession s = req.getSession(true);
            s.setAttribute("playerId",   p.getId());
            s.setAttribute("playerName", p.getUsername());
            s.setMaxInactiveInterval(3600);
            resp.sendRedirect(req.getContextPath() + "/setup");
        } catch (Exception e) {
            getServletContext().log("GoogleCallbackServlet", e);
            error(req, resp, "Đăng nhập Google thất bại. Vui lòng thử lại.");
        }
    }

    private String post(String url, String params) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        c.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));
        return read(c);
    }

    private String get(String url, String bearerToken) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("Authorization", "Bearer " + bearerToken);
        return read(c);
    }

    private String read(HttpURLConnection c) throws IOException {
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
