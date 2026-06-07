package service;

import dao.UserDAO;
import model.Player;
import service.EmailService;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC-02.4 – Quên mật khẩu (Forgot Password)
 *
 * Development Testing – Kiểm kiểm thử đơn vị cho PasswordResetService.requestReset()
 * UserDAO và EmailService được mock.
 *
 * Đặc điểm bảo mật quan trọng:
 * - requestReset() luôn return void (không throw), kể cả email không tồn tại
 * - Servlet luôn hiển thị "đã gửi" dù email có tồn tại hay không
 * → Chống Email Enumeration Attack
 */
@DisplayName("UC-02.4 – Quên mật khẩu (Forgot Password)")
class ForgotPasswordTest {

    @Mock private UserDAO      mockDAO;
    @Mock private EmailService mockEmail;

    private PasswordResetService service;

    private static final String EMAIL    = "user@example.com";
    private static final String USER_ID  = "uuid-reset-001";
    private static final String BASE_URL = "http://localhost:8080/battleship";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        service = new PasswordResetService();
        setField("userDAO",      mockDAO);
        setField("emailService", mockEmail);
    }

    // ── Luồng chính: email tồn tại ─────────────────────────────────────────

    @Test
    @DisplayName("TC02.4_01: Email tồn tại → saveResetToken() và sendResetPasswordEmail() được gọi")
    void TC02_4_01_emailExists_savesTokenAndSendsEmail() throws Exception {
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        doNothing().when(mockDAO).saveResetToken(eq(USER_ID), anyString(), any(LocalDateTime.class));
        doNothing().when(mockEmail).sendResetPasswordEmail(eq(EMAIL), anyString());

        assertDoesNotThrow(() -> service.requestReset(EMAIL, BASE_URL));

        verify(mockDAO, times(1)).saveResetToken(eq(USER_ID), anyString(), any(LocalDateTime.class));
        verify(mockEmail, times(1)).sendResetPasswordEmail(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("TC02.4_02: Reset token hết hạn sau 1 giờ (expiry = now + 1h)")
    void TC02_4_02_resetTokenExpiresInOneHour() throws Exception {
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        ArgumentCaptor<LocalDateTime> expiryCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        doNothing().when(mockDAO).saveResetToken(any(), any(), expiryCaptor.capture());
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), any());

        service.requestReset(EMAIL, BASE_URL);

        LocalDateTime expiry = expiryCaptor.getValue();
        LocalDateTime expectedMin = LocalDateTime.now().plusMinutes(59);
        LocalDateTime expectedMax = LocalDateTime.now().plusMinutes(61);
        assertTrue(expiry.isAfter(expectedMin) && expiry.isBefore(expectedMax),
                "Token phải hết hạn sau khoảng 1 giờ (±1 phút)");
    }

    @Test
    @DisplayName("TC02.4_03: URL email chứa /reset-password?token=...")
    void TC02_4_03_emailUrlContainsResetPath() throws Exception {
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        doNothing().when(mockDAO).saveResetToken(any(), any(), any());
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), urlCaptor.capture());

        service.requestReset(EMAIL, BASE_URL);

        String url = urlCaptor.getValue();
        assertTrue(url.contains("/reset-password?token="),
                "URL phải chứa /reset-password?token=");
        assertTrue(url.startsWith(BASE_URL), "URL phải bắt đầu bằng baseUrl");
    }

    @Test
    @DisplayName("TC02.4_04: Token lưu DB khớp với token trong URL email")
    void TC02_4_04_tokenInDbMatchesTokenInEmail() throws Exception {
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        ArgumentCaptor<String> dbToken  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailUrl = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockDAO).saveResetToken(any(), dbToken.capture(), any());
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), emailUrl.capture());

        service.requestReset(EMAIL, BASE_URL);
        String tokenFromUrl = emailUrl.getValue().replaceAll(".*token=", "");
        assertEquals(dbToken.getValue(), tokenFromUrl,
                "Token trong DB phải khớp với token trong URL email");
    }

    @Test
    @DisplayName("TC02.4_05: Email được trim() trước khi tìm kiếm")
    void TC02_4_05_emailIsTrimmed() throws Exception {
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        doNothing().when(mockDAO).saveResetToken(any(), any(), any());
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), any());

        service.requestReset("  " + EMAIL + "  ", BASE_URL);

        verify(mockDAO).findByEmail(EMAIL);
    }

    // ── Luồng thay thế: email không tồn tại (chống Email Enumeration) ───────

    @Test
    @DisplayName("TC02.4_06: Email không tồn tại → KHÔNG throw, KHÔNG saveToken, KHÔNG gửi email")
    void TC02_4_06_emailNotFound_silentReturn() throws Exception {
        when(mockDAO.findByEmail("notfound@example.com")).thenReturn(null);
        assertDoesNotThrow(() -> service.requestReset("notfound@example.com", BASE_URL),
                "requestReset() không được throw kể cả khi email không tồn tại (bảo mật)");
        verify(mockDAO, never()).saveResetToken(any(), any(), any());
        verify(mockEmail, never()).sendResetPasswordEmail(any(), any());
    }

    @Test
    @DisplayName("TC02.4_07: Email không tồn tại vs tồn tại → method signature giống nhau (return void)")
    void TC02_4_07_sameMethodSignatureRegardlessOfEmailExistence() throws Exception {
        when(mockDAO.findByEmail("exists@ex.com")).thenReturn(makePlayer());
        when(mockDAO.findByEmail("ghost@ex.com")).thenReturn(null);
        doNothing().when(mockDAO).saveResetToken(any(), any(), any());
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), any());

        assertDoesNotThrow(() -> service.requestReset("exists@ex.com", BASE_URL));
        assertDoesNotThrow(() -> service.requestReset("ghost@ex.com", BASE_URL));
    }

    // ── Release Testing ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TC02.4_08 [Release]: Mỗi lần requestReset sinh token mới, khác nhau")
    void TC02_4_08_uniqueTokenEachCall() throws Exception {
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        ArgumentCaptor<String> t1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> t2 = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockDAO).saveResetToken(any(), t1.capture(), any());
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), any());

        service.requestReset(EMAIL, BASE_URL);

        reset(mockDAO, mockEmail);
        when(mockDAO.findByEmail(EMAIL)).thenReturn(makePlayer());
        doNothing().when(mockDAO).saveResetToken(any(), t2.capture(), any());
        doNothing().when(mockEmail).sendResetPasswordEmail(any(), any());

        service.requestReset(EMAIL, BASE_URL);

        assertNotEquals(t1.getValue(), t2.getValue(),
                "Mỗi lần reset phải sinh token mới");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Player makePlayer() {
        Player p = new Player();
        p.setId(USER_ID);
        p.setEmail(EMAIL);
        p.setEmailVerified(true);
        return p;
    }

    private void setField(String name, Object value) throws Exception {
        Field f = PasswordResetService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }
}