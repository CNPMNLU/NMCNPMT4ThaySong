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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UC-01.1 – Đăng ký tài khoản thường")
class RegisterTest {

    @Mock private UserDAO mockDAO;
    @Mock private EmailService mockEmail;

    private UserService userService;
    private static final String BASE_URL = "http://localhost:8080/battleship";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        userService = new UserService();
        setField("userDAO", mockDAO);
        setField("emailService", mockEmail);
    }

    @Test
    @DisplayName("TC01.1_01: Đăng ký thành công (Dev Mode) -> emailVerified = true, không gửi email")
    void TC01_1_01_registerSuccess_DevMode() throws Exception {
        // Giả lập Dev Mode (EMAIL_ENABLED = false)
        setField("EMAIL_ENABLED", false);
        when(mockDAO.findByUsername("nhantruong")).thenReturn(null);
        when(mockDAO.findByEmail("nhan@example.com")).thenReturn(null);

        Player p = userService.register("nhantruong", "password123", "nhan@example.com", BASE_URL);

        assertNotNull(p);
        assertEquals("nhantruong", p.getUsername());
        assertTrue(p.isEmailVerified());
        assertNull(p.getVerifyToken());
        verify(mockDAO, times(1)).insert(any(Player.class));
        verify(mockEmail, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("TC01.1_02: Đăng ký thành công (Prod Mode) -> emailVerified = false, gửi email")
    void TC01_1_02_registerSuccess_ProdMode() throws Exception {
        setField("EMAIL_ENABLED", true);
        when(mockDAO.findByUsername("nhantruong")).thenReturn(null);
        when(mockDAO.findByEmail("nhan@example.com")).thenReturn(null);
        doNothing().when(mockEmail).sendVerificationEmail(anyString(), anyString());

        Player p = userService.register("nhantruong", "password123", "nhan@example.com", BASE_URL);

        assertFalse(p.isEmailVerified());
        assertNotNull(p.getVerifyToken());
        verify(mockDAO, times(1)).insert(any(Player.class));
        verify(mockEmail, times(1)).sendVerificationEmail(eq("nhan@example.com"), contains("/verify-email?token="));
    }

    @Test
    @DisplayName("TC01.1_03: Trùng Username -> IllegalArgumentException")
    void TC01_1_03_duplicateUsername() throws SQLException {
        when(mockDAO.findByUsername("nhantruong")).thenReturn(new Player());

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("nhantruong", "password123", "nhan@example.com", BASE_URL));

        assertEquals("Username đã tồn tại", ex.getMessage());
        verify(mockDAO, never()).insert(any(Player.class));
    }

    @Test
    @DisplayName("TC01.1_04: Trùng Email -> IllegalArgumentException")
    void TC01_1_04_duplicateEmail() throws SQLException {
        when(mockDAO.findByUsername("nhantruong")).thenReturn(null);
        when(mockDAO.findByEmail("nhan@example.com")).thenReturn(new Player());

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("nhantruong", "password123", "nhan@example.com", BASE_URL));

        assertEquals("Email đã được sử dụng", ex.getMessage());
        verify(mockDAO, never()).insert(any(Player.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "abc"})
    @DisplayName("TC01.1_05: Password < 6 ký tự -> IllegalArgumentException")
    void TC01_1_05_shortPassword(String shortPass) {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("nhantruong", shortPass, "nhan@example.com", BASE_URL));
        assertTrue(ex.getMessage().contains("ít nhất 6 ký tự"));
    }

    @Test
    @DisplayName("TC01.1_06: Gửi email thất bại (Prod Mode) -> Rollback deleteById")
    void TC01_1_06_emailFail_rollback() throws Exception {
        setField("EMAIL_ENABLED", true);
        when(mockDAO.findByUsername("test")).thenReturn(null);
        when(mockDAO.findByEmail("test@example.com")).thenReturn(null);

        // Mock lỗi khi gửi email
        doThrow(new RuntimeException("SMTP Server Down"))
                .when(mockEmail).sendVerificationEmail(anyString(), anyString());

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("test", "password123", "test@example.com", BASE_URL));

        assertTrue(ex.getMessage().contains("Không thể gửi email xác thực"));
        // Đảm bảo dữ liệu đã insert bị xóa (Rollback)
        verify(mockDAO, times(1)).deleteById(anyString());
    }

    private void setField(String name, Object value) throws Exception {
        Field f = UserService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(userService, value);
    }
}