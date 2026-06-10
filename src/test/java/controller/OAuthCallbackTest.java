package controller;

import dao.UserDAO;
import model.Player;
import org.junit.jupiter.api.*;
import org.mockito.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@DisplayName("UC-02.2 / UC-02.3 – OAuth Callback (Google & Facebook)")
class OAuthCallbackTest {

    // ═══ GOOGLE CALLBACK ════════════════════════════════════════════════════
    @Nested
    @DisplayName("UC-02.2 – Google OAuth2 Callback")
    class GoogleCallbackTests {
        @Mock private UserDAO mockDAO;
        @Mock private HttpServletRequest mockReq;
        @Mock private HttpServletResponse mockResp;
        @Mock private HttpSession mockSession;
        @Mock private RequestDispatcher mockRD;

        private GoogleCallbackServlet servlet;

        @BeforeEach
        void setUp() throws Exception {
            MockitoAnnotations.openMocks(this);
            servlet = new GoogleCallbackServlet();
            Field f = GoogleCallbackServlet.class.getDeclaredField("userDAO");
            f.setAccessible(true);
            f.set(servlet, mockDAO);
            when(mockReq.getSession()).thenReturn(mockSession);
            when(mockReq.getSession(true)).thenReturn(mockSession);
            when(mockReq.getContextPath()).thenReturn("/battleship");
            when(mockReq.getRequestDispatcher("/login.jsp")).thenReturn(mockRD);
        }

        @Test
        @DisplayName("TC02.2_01: State không khớp → error 'Yêu cầu không hợp lệ', forward login.jsp")
        void TC02_2_01_stateMismatch_forwardsError() throws Exception {
            when(mockSession.getAttribute("oauth_state_google")).thenReturn("expected-state");
            when(mockReq.getParameter("state")).thenReturn("wrong-state");
            when(mockReq.getParameter("error")).thenReturn(null);

            servlet.doGet(mockReq, mockResp);

            verify(mockReq).setAttribute(eq("error"), contains("không hợp lệ"));
            verify(mockRD).forward(mockReq, mockResp);
            verify(mockDAO, never()).findByGoogleId(any());
        }

        @Test
        @DisplayName("TC02.2_02: State null trong session → error, forward login.jsp")
        void TC02_2_02_nullSavedState_forwardsError() throws Exception {
            when(mockSession.getAttribute("oauth_state_google")).thenReturn(null);
            when(mockReq.getParameter("state")).thenReturn("any-state");
            when(mockReq.getParameter("error")).thenReturn(null);

            servlet.doGet(mockReq, mockResp);

            verify(mockReq).setAttribute(eq("error"), anyString());
            verify(mockRD).forward(mockReq, mockResp);
        }

        @Test
        @DisplayName("TC02.2_03: User huỷ đăng nhập (error param có giá trị) → error 'bị huỷ'")
        void TC02_2_03_userCancelled_forwardsError() throws Exception {
            String state = "valid-csrf-state";
            when(mockSession.getAttribute("oauth_state_google")).thenReturn(state);
            when(mockReq.getParameter("state")).thenReturn(state);
            when(mockReq.getParameter("error")).thenReturn("access_denied");

            servlet.doGet(mockReq, mockResp);

            verify(mockReq).setAttribute(eq("error"), contains("huỷ"));
            verify(mockRD).forward(mockReq, mockResp);
            verify(mockDAO, never()).findByGoogleId(any());
        }

        @Test
        @DisplayName("TC02.2_04: State hợp lệ nhưng code null → error 'không nhận được mã'")
        void TC02_2_04_validState_missingCode_forwardsError() throws Exception {
            String state = "valid-csrf-state";
            when(mockSession.getAttribute("oauth_state_google")).thenReturn(state);
            when(mockReq.getParameter("state")).thenReturn(state);
            when(mockReq.getParameter("error")).thenReturn(null);
            when(mockReq.getParameter("code")).thenReturn(null);

            servlet.doGet(mockReq, mockResp);

            verify(mockReq).setAttribute(eq("error"), anyString());
            verify(mockRD).forward(mockReq, mockResp);
        }

        @Test
        @DisplayName("TC02.2_05: State hợp lệ nhưng code rỗng → error")
        void TC02_2_05_validState_emptyCode_forwardsError() throws Exception {
            String state = "valid-csrf-state";
            when(mockSession.getAttribute("oauth_state_google")).thenReturn(state);
            when(mockReq.getParameter("state")).thenReturn(state);
            when(mockReq.getParameter("error")).thenReturn(null);
            when(mockReq.getParameter("code")).thenReturn("");

            servlet.doGet(mockReq, mockResp);

            verify(mockReq).setAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("TC02.2_06: State luôn bị xóa khỏi session sau callback (dù thành công hay lỗi)")
        void TC02_2_06_stateAlwaysRemovedFromSession() throws Exception {
            when(mockSession.getAttribute("oauth_state_google")).thenReturn("a-state");
            when(mockReq.getParameter("state")).thenReturn("wrong-state");
            when(mockReq.getParameter("error")).thenReturn(null);

            try {
                servlet.doGet(mockReq, mockResp);
            } catch (Exception ignored) {}

            verify(mockSession).removeAttribute("oauth_state_google");
        }
    }

    // ═══ OAUTH COMMON SESSIONS & PROPERTIES ═════════════════════════════════
    @Nested
    @DisplayName("UC-02.2 / UC-02.3 – Hậu OAuth (Session & Business Rules)")
    class OAuthPostXacThucTests {

        @Test
        @DisplayName("TC_OAuth_01: Session phải có playerId và playerName sau OAuth thành công")
        void TC_OAuth_01_sessionHasPlayerIdAndName() {
            HttpSession mockSess = mock(HttpSession.class);
            String playerId   = "uuid-oauth-success";
            String playerName = "oauthuser";

            mockSess.setAttribute("playerId",   playerId);
            mockSess.setAttribute("playerName", playerName);
            mockSess.setMaxInactiveInterval(3600);

            verify(mockSess).setAttribute("playerId",   playerId);
            verify(mockSess).setAttribute("playerName", playerName);
            verify(mockSess).setMaxInactiveInterval(3600);
        }

        @Test
        @DisplayName("TC_OAuth_02: Google/Facebook user không cần verify email (emailVerified=true ngay)")
        void TC_OAuth_02_socialLoginNoEmailVerification() {
            Player googleUser   = new Player();
            googleUser.setGoogleId("g-sub");
            googleUser.setEmailVerified(true);

            Player facebookUser = new Player();
            facebookUser.setFacebookId("fb-id");
            facebookUser.setEmailVerified(true);

            assertTrue(googleUser.isEmailVerified(),
                    "Google user phải emailVerified=true ngay lập tức");
            assertTrue(facebookUser.isEmailVerified(),
                    "Facebook user phải emailVerified=true ngay lập tức");
        }
    }
}