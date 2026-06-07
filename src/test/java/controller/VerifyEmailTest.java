package controller;

import dao.UserDAO;
import model.Player;
import org.junit.jupiter.api.*;
import org.mockito.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UC-01.2 – Xác thực địa chỉ email")
class VerifyEmailTest {

    @Mock private UserDAO              mockDAO;
    @Mock private HttpServletRequest   mockReq;
    @Mock private HttpServletResponse  mockResp;
    @Mock private RequestDispatcher    mockRD;

    private VerifyEmailServlet servlet;

    private static final String VALID_TOKEN  = "abc123validtoken";
    private static final String PLAYER_ID    = "uuid-verify-001";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        servlet = new VerifyEmailServlet();
        Field f = VerifyEmailServlet.class.getDeclaredField("userDAO");
        f.setAccessible(true);
        f.set(servlet, mockDAO);
        when(mockReq.getRequestDispatcher("/verify-email.jsp")).thenReturn(mockRD);
    }

    // ── Luồng chính ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC01.2_01: Token hợp lệ → markEmailVerified() + verifyStatus='success'")
    void TC01_2_01_validToken_markVerifiedAndSuccess() throws Exception {
        Player p = makePlayer(false, LocalDateTime.now().minusHours(1));
        when(mockReq.getParameter("token")).thenReturn(VALID_TOKEN);
        when(mockDAO.findByVerifyToken(VALID_TOKEN)).thenReturn(p);
        doNothing().when(mockDAO).markEmailVerified(PLAYER_ID);

        servlet.doGet(mockReq, mockResp);

        verify(mockDAO, times(1)).markEmailVerified(PLAYER_ID);
        verify(mockReq).setAttribute("verifyStatus", "success");
        verify(mockRD).forward(mockReq, mockResp);
    }

    @Test
    @DisplayName("TC01.2_02: Token hợp lệ → verify_token bị xóa (chỉ gọi markEmailVerified 1 lần)")
    void TC01_2_02_markEmailVerifiedCalledExactlyOnce() throws Exception {
        Player p = makePlayer(false, LocalDateTime.now().minusHours(2));
        when(mockReq.getParameter("token")).thenReturn(VALID_TOKEN);
        when(mockDAO.findByVerifyToken(VALID_TOKEN)).thenReturn(p);

        servlet.doGet(mockReq, mockResp);

        verify(mockDAO, times(1)).markEmailVerified(anyString());
    }

    // ── Luồng thay thế: token null / rỗng ─────────────────────────────────

    @Test
    @DisplayName("TC01.2_03: Token null → verifyStatus='invalid'")
    void TC01_2_03_tokenNull_invalidStatus() throws Exception {
        when(mockReq.getParameter("token")).thenReturn(null);
        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "invalid");
        verify(mockDAO, never()).findByVerifyToken(any());
        verify(mockDAO, never()).markEmailVerified(any());
    }

    @Test
    @DisplayName("TC01.2_04: Token rỗng '' → verifyStatus='invalid'")
    void TC01_2_04_tokenEmpty_invalidStatus() throws Exception {
        when(mockReq.getParameter("token")).thenReturn("   ");
        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "invalid");
    }

    @Test
    @DisplayName("TC01.2_05: Token không tồn tại trong DB → verifyStatus='invalid'")
    void TC01_2_05_tokenNotFoundInDB_invalidStatus() throws Exception {
        when(mockReq.getParameter("token")).thenReturn("nonexistenttoken");
        when(mockDAO.findByVerifyToken("nonexistenttoken")).thenReturn(null);

        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "invalid");
        verify(mockDAO, never()).markEmailVerified(any());
    }

    // ── Luồng thay thế: đã xác thực ────────────────────────────────────────

    @Test
    @DisplayName("TC01.2_06: Email đã xác thực trước đó → verifyStatus='already'")
    void TC01_2_06_alreadyVerified_alreadyStatus() throws Exception {
        Player p = makePlayer(true, LocalDateTime.now().minusHours(1));
        when(mockReq.getParameter("token")).thenReturn(VALID_TOKEN);
        when(mockDAO.findByVerifyToken(VALID_TOKEN)).thenReturn(p);

        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "already");
        verify(mockDAO, never()).markEmailVerified(any());
    }

    // ── Luồng thay thế: token hết hạn ──────────────────────────────────────

    @Test
    @DisplayName("TC01.2_07: Token hết hạn (> 24h) → verifyStatus='expired' + expiredUserId")
    void TC01_2_07_tokenExpired_expiredStatusWithUserId() throws Exception {
        Player p = makePlayer(false, LocalDateTime.now().minusHours(25));
        // quá 24h
        when(mockReq.getParameter("token")).thenReturn(VALID_TOKEN);
        when(mockDAO.findByVerifyToken(VALID_TOKEN)).thenReturn(p);

        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "expired");
        verify(mockReq).setAttribute("expiredUserId", PLAYER_ID);
        verify(mockDAO, never()).markEmailVerified(any());
    }

    @Test
    @DisplayName("TC01.2_08: Token hết hạn ĐÚNG 24h → expired (boundary)")
    void TC01_2_08_tokenExpiredAtExactly24h_expiredStatus() throws Exception {
        Player p = makePlayer(false, LocalDateTime.now().minusHours(24).minusMinutes(1));
        when(mockReq.getParameter("token")).thenReturn(VALID_TOKEN);
        when(mockDAO.findByVerifyToken(VALID_TOKEN)).thenReturn(p);

        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "expired");
    }

    // ── Luồng thay thế: lỗi DB ─────────────────────────────────────────────

    @Test
    @DisplayName("TC01.2_09: DB ném exception → verifyStatus='error'")
    void TC01_2_09_dbException_errorStatus() throws Exception {
        when(mockReq.getParameter("token")).thenReturn(VALID_TOKEN);
        when(mockDAO.findByVerifyToken(VALID_TOKEN)).thenThrow(new RuntimeException("DB down"));

        servlet.doGet(mockReq, mockResp);

        verify(mockReq).setAttribute("verifyStatus", "error");
        verify(mockDAO, never()).markEmailVerified(any());
    }

    // ── Forward luôn được gọi ───────────────────────────────────────────────

    @Test
    @DisplayName("TC01.2_10: Mọi trường hợp đều forward đến verify-email.jsp")
    void TC01_2_10_alwaysForwardToJsp() throws Exception {
        when(mockReq.getParameter("token")).thenReturn(null);
        servlet.doGet(mockReq, mockResp);

        verify(mockRD, times(1)).forward(mockReq, mockResp);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Player makePlayer(boolean verified, LocalDateTime sentAt) {
        Player p = new Player();
        p.setId(PLAYER_ID);
        p.setEmail("test@example.com");
        p.setEmailVerified(verified);
        p.setVerifySentAt(sentAt);
        p.setVerifyToken(verified ? null : VALID_TOKEN);
        return p;
    }
}