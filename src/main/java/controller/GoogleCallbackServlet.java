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
        String returnedState = req.getParameter("state");
        String savedState    = (String) req.getSession().getAttribute("oauth_state_google");
        req.getSession().removeAttribute("oauth_state_google");

        if (returnedState == null || !returnedState.equals(savedState)) {
            forwardError(req, resp, "Yêu cầu không hợp lệ. Vui lòng thử lại."); return;
        }
        if (req.getParameter("error") != null) {
            forwardError(req, resp, "Đăng nhập Google bị huỷ."); return;
        }
        String code = req.getParameter("code");
        if (code == null || code.isEmpty()) {
            forwardError(req, resp, "Không nhận được mã xác thực từ Google."); return;
        }

        try {
            String accessToken = new JSONObject(exchangeCode(code)).getString("access_token");
            JSONObject info    = new JSONObject(fetchUserInfo(accessToken));

            String googleId = info.getString("sub");
            String email    = info.optString("email", "");
            String name     = info.optString("name",  "");

            Player player = userDAO.findByGoogleId(googleId);

            if (player == null) {
                player = new Player();
                player.setId(UUID.randomUUID().toString());
                player.setGoogleId(googleId);
                player.setEmail(email.isEmpty() ? null : email);
                player.setEmailVerified(true);
                player.setUsername(uniqueUsername(sanitize(name)));
                userDAO.insertGoogleUser(player);
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("playerId",   player.getId());
            session.setAttribute("playerName", player.getUsername());
            session.setMaxInactiveInterval(3600);
            resp.sendRedirect(req.getContextPath() + "/setup");

        } catch (Exception e) {
            getServletContext().log("GoogleCallbackServlet error", e);
            forwardError(req, resp, "Đăng nhập Google thất bại. Vui lòng thử lại.");
        }
    }

    private String exchangeCode(String code) throws IOException {
        String params = "code="           + URLEncoder.encode(code,          "UTF-8")
                + "&client_id="     + URLEncoder.encode(CLIENT_ID,     "UTF-8")
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, "UTF-8")
                + "&redirect_uri="  + URLEncoder.encode(REDIRECT_URI,  "UTF-8")
                + "&grant_type=authorization_code";
        HttpURLConnection conn = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));
        return read(conn);
    }

    private String fetchUserInfo(String token) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(USERINFO_URL).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        return read(conn);
    }

    private String read(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
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
        String candidate = base;
        int i = 1;
        while (userDAO.findByUsername(candidate) != null) candidate = base + i++;
        return candidate;
    }

    private void forwardError(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }
}