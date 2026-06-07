package service;

import dao.UserDAO;
import model.Player;
import service.EmailService;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC-01.3 – Gửi lại email xác thực (Resend Verification Email)
 *
 * Development Testing – Kiểm thử đơn vị cho UserService.resendVerification()
 * UserDAO và EmailService được mock.
 *
 * Luồng:
 * 1. findById(userId) – tìm user
 * 2. Kiểm tra !emailVerified
 * 3. generateToken() – sinh token mới
 * 4. updateVerifyToken(userId, token)
 * 5. sendVerificationEmail(email, url)
 */
@DisplayName("UC-01.3 – Gửi lại email xác thực")
class ResendVerifyTest {

    @Mock private UserDAO      mockDAO;
    @Mock private EmailService mockEmail;

    private UserService userService;

    private static final String USER_ID  = "uuid-resend-001";
    private static final String EMAIL    = "user@example.com";
    private static final String BASE_URL = "http://localhost:8080/battleship";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        userService = new UserService();
        setField(userService, "userDAO",      UserService.class, mockDAO);
        setField(userService, "emailService", UserService.class, mockEmail);
    }

    // ── Luồng chính ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC01.3_01: Gửi lại thành công – updateVerifyToken() và sendEmail() được gọi")
    void TC01_3_01_resend_success() throws Exception {
        Player p = makeUnverifiedPlayer();
        when(mockDAO.findById(USER_ID)).thenReturn(p);
        doNothing().when(mockDAO).updateVerifyToken(eq(USER_ID), anyString());
        doNothing().when(mockEmail).sendVerificationEmail(eq(EMAIL), anyString());

        assertDoesNotThrow(() -> userService.resendVerification(USER_ID, BASE_URL));

        verify(mockDAO, times(1)).updateVerifyToken(eq(USER_ID), anyString());
        verify(mockEmail, times(1)).sendVerificationEmail(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("TC01.3_02: Token mới được sinh và khác nhau mỗi lần")
    void TC01_3_02_newTokenGeneratedEachCall() throws Exception {
        when(mockDAO.findById(USER_ID)).thenReturn(makeUnverifiedPlayer());
        doNothing().when(mockEmail).sendVerificationEmail(any(), any());

        ArgumentCaptor<String> tokenCaptor1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor2 = ArgumentCaptor.forClass(String.class);

        userService.resendVerification(USER_ID, BASE_URL);
        verify(mockDAO).updateVerifyToken(eq(USER_ID), tokenCaptor1.capture());

        // Reset và gọi lần 2
        reset(mockDAO, mockEmail);
        when(mockDAO.findById(USER_ID)).thenReturn(makeUnverifiedPlayer());
        doNothing().when(mockEmail).sendVerificationEmail(any(), any());

        userService.resendVerification(USER_ID, BASE_URL);
        verify(mockDAO).updateVerifyToken(eq(USER_ID), tokenCaptor2.capture());

        assertNotEquals(tokenCaptor1.getValue(), tokenCaptor2.getValue(),
                "Mỗi lần gửi lại phải sinh token mới, khác nhau");
    }

    @Test
    @DisplayName("TC01.3_03: URL email chứa token dạng /verify-email?token=...")
    void TC01_3_03_emailUrlContainsToken() throws Exception {
        when(mockDAO.findById(USER_ID)).thenReturn(makeUnverifiedPlayer());
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockDAO).updateVerifyToken(any(), any());
        doNothing().when(mockEmail).sendVerificationEmail(eq(EMAIL), urlCaptor.capture());

        userService.resendVerification(USER_ID, BASE_URL);

        String url = urlCaptor.getValue();
        assertTrue(url.contains("/verify-email?token="),
                "URL phải chứa /verify-email?token=");
        assertTrue(url.startsWith(BASE_URL),
                "URL phải bắt đầu bằng baseUrl");
    }

    @Test
    @DisplayName("TC01.3_04: Token dùng để gọi updateVerifyToken và embed trong URL email là cùng một token")
    void TC01_3_04_sameTokenInDbAndEmail() throws Exception {
        when(mockDAO.findById(USER_ID)).thenReturn(makeUnverifiedPlayer());
        ArgumentCaptor<String> dbTokenCap  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailUrlCap = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockDAO).updateVerifyToken(eq(USER_ID), dbTokenCap.capture());
        doNothing().when(mockEmail).sendVerificationEmail(any(), emailUrlCap.capture());

        userService.resendVerification(USER_ID, BASE_URL);
        String dbToken    = dbTokenCap.getValue();
        String emailToken = emailUrlCap.getValue().replaceAll(".*token=", "");
        assertEquals(dbToken, emailToken,
                "Token lưu DB phải giống token trong URL email");
    }

    // ── Luồng thay thế: userId null / không tồn tại ─────────────────────────

    @Test
    @DisplayName("TC01.3_05: userId null → IllegalArgumentException")
    void TC01_3_05_userId_null_throwsException() throws SQLException {
        assertThrows(Exception.class, () ->
                userService.resendVerification(null, BASE_URL));
        verify(mockDAO, never()).updateVerifyToken(any(), any());
    }

    @Test
    @DisplayName("TC01.3_06: User không tồn tại trong DB → IllegalArgumentException")
    void TC01_3_06_userNotFound_throwsException() throws Exception {
        when(mockDAO.findById(USER_ID)).thenReturn(null);
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.resendVerification(USER_ID, BASE_URL));
        assertTrue(ex.getMessage().contains("không tồn tại"),
                "Thông báo phải đề cập tài khoản không tồn tại");
        verify(mockDAO, never()).updateVerifyToken(any(), any());
    }

    // ── Luồng thay thế: email đã xác thực ──────────────────────────────────

    @Test
    @DisplayName("TC01.3_07: Email đã xác thực → IllegalArgumentException")
    void TC01_3_07_emailAlreadyVerified_throwsException() throws Exception {
        Player p = makeUnverifiedPlayer();
        p.setEmailVerified(true);
        when(mockDAO.findById(USER_ID)).thenReturn(p);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.resendVerification(USER_ID, BASE_URL));
        assertTrue(ex.getMessage().contains("đã được xác thực"),
                "Thông báo phải đề cập email đã xác thực");
        verify(mockDAO, never()).updateVerifyToken(any(), any());
        verify(mockEmail, never()).sendVerificationEmail(any(), any());
    }

    // ── Luồng thay thế: gửi email lỗi ──────────────────────────────────────

    @Test
    @DisplayName("TC01.3_08: Email service lỗi → IllegalArgumentException 'Không thể gửi email'")
    void TC01_3_08_emailServiceFails_throwsException() throws Exception {
        when(mockDAO.findById(USER_ID)).thenReturn(makeUnverifiedPlayer());
        doNothing().when(mockDAO).updateVerifyToken(any(), any());
        doThrow(new RuntimeException("SMTP timeout")).when(mockEmail)
                .sendVerificationEmail(any(), any());
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.resendVerification(USER_ID, BASE_URL));
        assertTrue(ex.getMessage().contains("Không thể gửi email"));
    }

    // ── Release Testing ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TC01.3_09 [Release]: generateToken() trả về chuỗi Base64 URL-safe, độ dài hợp lý")
    void TC01_3_09_generateToken_isBase64UrlSafe() {
        String token = UserService.generateToken();
        assertNotNull(token);
        assertTrue(token.length() >= 32,
                "Token phải đủ dài để đảm bảo entropy");
        assertTrue(token.matches("[A-Za-z0-9_-]+"),
                "Token phải là Base64 URL-safe (không có +, /, =)");
    }

    @Test
    @DisplayName("TC01.3_10 [Release]: findById được gọi đúng với userId đã truyền vào")
    void TC01_3_10_findByIdCalledWithCorrectId() throws Exception {
        when(mockDAO.findById(USER_ID)).thenReturn(makeUnverifiedPlayer());
        doNothing().when(mockEmail).sendVerificationEmail(any(), any());

        userService.resendVerification(USER_ID, BASE_URL);

        verify(mockDAO, times(1)).findById(USER_ID);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Player makeUnverifiedPlayer() {
        Player p = new Player();
        p.setId(USER_ID);
        p.setEmail(EMAIL);
        p.setEmailVerified(false);
        p.setVerifyToken("old-token-abc");
        return p;
    }

    private static void setField(Object obj, String name, Class<?> cls, Object value) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }
}