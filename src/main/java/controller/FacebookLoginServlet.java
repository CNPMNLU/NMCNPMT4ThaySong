package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

@WebServlet("/auth/facebook")
public class FacebookLoginServlet extends HttpServlet {

    private static final String APP_ID = System.getenv("FACEBOOK_APP_ID");
    private static final String REDIRECT_URI = System.getenv("FACEBOOK_REDIRECT_URI");
    private static final String SCOPE = "email public_profile";
    private static final String AUTH_URL = "https://www.facebook.com/v19.0/dialog/oauth";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String state = UUID.randomUUID().toString();
        req.getSession(true).setAttribute("oauth_state_facebook", state);
        String url = AUTH_URL
                + "?client_id=" + URLEncoder.encode(APP_ID, "UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                + "&scope=" + URLEncoder.encode(SCOPE, "UTF-8")
                + "&state=" + URLEncoder.encode(state, "UTF-8")
                + "&response_type=code";
        resp.sendRedirect(url);
    }
}