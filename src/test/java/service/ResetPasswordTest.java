package service;

import dao.UserDAO;
import model.Player;
import service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC-02.5 – Đặt lại mật khẩu (Reset Password)
 *
 * Development Testing – Kiểm thử đơn vị cho:
 * - PasswordResetService.validateToken(token)
 * - PasswordResetService.resetPassword(token, newPassword, confirmPassword)
 *
 * Sau khi reset thành công:
 * - password_hash được cập nhật bằng SHA-256(newPassword)
 * - reset_token = NULL, reset_token_expiry = NULL
 */
@DisplayName("UC-02.5 – Đặt lại mật khẩu (Reset Password)")
class ResetPasswordTest {

    @Mock private UserDAO      mockDAO;
    @Mock private EmailService mockEmail;

    private PasswordResetService service;

    private static final String VALID_TOKEN = "valid-reset-token-xyz";
    private static final String USER_ID     = "uuid-reset-002";
    private static final String NEW_PASS    = "newPass123";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        service = new PasswordResetService();
        setField("userDAO",      mockDAO);
        setField("emailService", mockEmail);
    }

    // ═══ validateToken() ════════════════════════════════════════════════════

    @Test
    @DisplayName("TC02.5_01: validateToken – token hợp lệ, chưa hết hạn → trả về Player")
    void TC02_5_01_validateToken_valid_returnsPlayer() throws Exception {
        Player p = makePlayerWithToken(LocalDateTime.now().plusMinutes(30));
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);

        Player result = service.validateToken(VALID_TOKEN);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
    }

    @Test
    @DisplayName("TC02.5_02: validateToken – token null → IllegalArgumentException")
    void TC02_5_02_validateToken_null_throwsException() {
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateToken(null));
        assertTrue(ex.getMessage().contains("không hợp lệ"));
    }

    @Test
    @DisplayName("TC02.5_03: validateToken – token rỗng → IllegalArgumentException")
    void TC02_5_03_validateToken_empty_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validateToken("   "));
    }

    @Test
    @DisplayName("TC02.5_04: validateToken – token không tồn tại trong DB → IllegalArgumentException")
    void TC02_5_04_validateToken_notInDb_throwsException() throws Exception {
        when(mockDAO.findByResetToken("unknown-token")).thenReturn(null);
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateToken("unknown-token"));
        assertTrue(ex.getMessage().contains("không hợp lệ") || ex.getMessage().contains("đã được sử dụng"));
    }

    @Test
    @DisplayName("TC02.5_05: validateToken – token hết hạn (expiry < now) → IllegalArgumentException 'đã hết hạn'")
    void TC02_5_05_validateToken_expired_throwsException() throws Exception {
        Player p = makePlayerWithToken(LocalDateTime.now().minusMinutes(1));
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateToken(VALID_TOKEN));
        assertTrue(ex.getMessage().contains("hết hạn"),
                "Thông báo phải đề cập token đã hết hạn");
    }

    @Test
    @DisplayName("TC02.5_06: validateToken – expiry = null → IllegalArgumentException (expiry null coi như hết hạn)")
    void TC02_5_06_validateToken_nullExpiry_throwsException() throws Exception {
        Player p = makePlayerWithToken(null);
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);

        assertThrows(IllegalArgumentException.class,
                () -> service.validateToken(VALID_TOKEN));
    }

    // ═══ resetPassword() ════════════════════════════════════════════════════

    @Test
    @DisplayName("TC02.5_07: Reset thành công – updatePasswordAndClearResetToken() được gọi")
    void TC02_5_07_resetPassword_success_callsUpdate() throws Exception {
        Player p = makePlayerWithToken(LocalDateTime.now().plusMinutes(30));
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);
        doNothing().when(mockDAO).updatePasswordAndClearResetToken(eq(USER_ID), anyString());

        assertDoesNotThrow(() -> service.resetPassword(VALID_TOKEN, NEW_PASS, NEW_PASS));

        verify(mockDAO, times(1)).updatePasswordAndClearResetToken(eq(USER_ID), anyString());
    }

    @Test
    @DisplayName("TC02.5_08: Mật khẩu mới được hash SHA-256 trước khi lưu, KHÔNG lưu plain text")
    void TC02_5_08_newPasswordIsHashed() throws Exception {
        Player p = makePlayerWithToken(LocalDateTime.now().plusMinutes(30));
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockDAO).updatePasswordAndClearResetToken(any(), hashCaptor.capture());

        service.resetPassword(VALID_TOKEN, NEW_PASS, NEW_PASS);

        String savedHash = hashCaptor.getValue();
        assertNotEquals(NEW_PASS, savedHash, "Không được lưu plain text");
        assertEquals(UserService.hashPassword(NEW_PASS), savedHash,
                "Phải lưu SHA-256 hash của mật khẩu mới");
        assertEquals(64, savedHash.length(), "SHA-256 = 64 hex chars");
    }

    @Test
    @DisplayName("TC02.5_09: Password < 6 ký tự → IllegalArgumentException")
    void TC02_5_09_passwordTooShort_throwsException() throws SQLException {
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(VALID_TOKEN, "12345", "12345"));
        assertTrue(ex.getMessage().contains("6 ký tự"));
        verify(mockDAO, never()).updatePasswordAndClearResetToken(any(), any());
    }

    @ParameterizedTest(name = "Password=\"{0}\" (< 6 ký tự)")
    @ValueSource(strings = {"", "a", "ab", "abc", "abcd", "abcde"})
    @DisplayName("TC02.5_10: Parametrized – tất cả password < 6 ký tự đều bị reject")
    void TC02_5_10_parametrized_shortPasswords(String shortPass) {
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(VALID_TOKEN, shortPass, shortPass));
    }

    @Test
    @DisplayName("TC02.5_11: Password null → IllegalArgumentException")
    void TC02_5_11_passwordNull_throwsException() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(VALID_TOKEN, null, null));
        verify(mockDAO, never()).updatePasswordAndClearResetToken(any(), any());
    }

    @Test
    @DisplayName("TC02.5_12: Password ≠ confirmPassword → 'Mật khẩu xác nhận không khớp'")
    void TC02_5_12_passwordMismatch_throwsException() throws SQLException {
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(VALID_TOKEN, "correctPass", "differentPass"));
        assertTrue(ex.getMessage().contains("không khớp"));
        verify(mockDAO, never()).updatePasswordAndClearResetToken(any(), any());
    }

    @Test
    @DisplayName("TC02.5_13: Token hết hạn khi POST reset → IllegalArgumentException")
    void TC02_5_13_expiredTokenOnPost_throwsException() throws Exception {
        Player p = makePlayerWithToken(LocalDateTime.now().minusMinutes(5));
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(VALID_TOKEN, NEW_PASS, NEW_PASS));
        verify(mockDAO, never()).updatePasswordAndClearResetToken(any(), any());
    }

    @Test
    @DisplayName("TC02.5_14: Token không tồn tại khi POST → IllegalArgumentException")
    void TC02_5_14_invalidTokenOnPost_throwsException() throws Exception {
        when(mockDAO.findByResetToken("bad-token")).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("bad-token", NEW_PASS, NEW_PASS));
        verify(mockDAO, never()).updatePasswordAndClearResetToken(any(), any());
    }

    // ── Validation thứ tự: password validate trước token validate ──────────

    @Test
    @DisplayName("TC02.5_15: Password validate trước token – password ngắn → lỗi ngay, không gọi DB")
    void TC02_5_15_passwordValidatedBeforeTokenLookup() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(VALID_TOKEN, "abc", "abc"));
        verify(mockDAO, never()).findByResetToken(any());
    }

    // ── Release Testing ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TC02.5_16 [Release]: Sau khi reset thành công, token bị xóa – updatePasswordAndClearResetToken tên method confirm")
    void TC02_5_16_release_clearResetTokenAfterSuccess() throws Exception {
        Player p = makePlayerWithToken(LocalDateTime.now().plusHours(1));
        when(mockDAO.findByResetToken(VALID_TOKEN)).thenReturn(p);
        doNothing().when(mockDAO).updatePasswordAndClearResetToken(any(), any());

        service.resetPassword(VALID_TOKEN, NEW_PASS, NEW_PASS);

        verify(mockDAO).updatePasswordAndClearResetToken(USER_ID, UserService.hashPassword(NEW_PASS));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Player makePlayerWithToken(LocalDateTime expiry) {
        Player p = new Player();
        p.setId(USER_ID);
        p.setEmail("user@example.com");
        p.setEmailVerified(true);
        p.setResetToken(VALID_TOKEN);
        p.setResetTokenExpiry(expiry);
        return p;
    }

    private void setField(String name, Object value) throws Exception {
        Field f = PasswordResetService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }
}