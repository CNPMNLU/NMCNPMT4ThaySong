package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

@WebServlet("/auth/google")
public class GoogleLoginServlet extends HttpServlet {

    private static final String CLIENT_ID    = System.getenv("GOOGLE_CLIENT_ID");
    private static final String REDIRECT_URI = System.getenv("GOOGLE_REDIRECT_URI");
    private static final String SCOPE        = "openid email profile";
    private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String state = UUID.randomUUID().toString();
        req.getSession(true).setAttribute("oauth_state_google", state);
        String url = AUTH_URL
            + "?client_id="     + URLEncoder.encode(CLIENT_ID,    "UTF-8")
            + "&redirect_uri="  + URLEncoder.encode(REDIRECT_URI, "UTF-8")
            + "&response_type=code"
            + "&scope="         + URLEncoder.encode(SCOPE,        "UTF-8")
            + "&state="         + URLEncoder.encode(state,        "UTF-8")
            + "&access_type=online";
        resp.sendRedirect(url);
    }
}
