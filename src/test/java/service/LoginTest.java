package service;

import dao.UserDAO;
import model.Player;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UC-02.1 – Đăng nhập bằng tài khoản thường")
class LoginTest {

    @Mock private UserDAO mockDAO;
    private UserService userService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        userService = new UserService();
        Field f = UserService.class.getDeclaredField("userDAO");
        f.setAccessible(true);
        f.set(userService, mockDAO);
    }

    @Test
    @DisplayName("TC02.1_01: Đăng nhập thành công -> Trả về Player, gọi updateLastLogin")
    void TC02_1_01_authenticateSuccess() throws Exception {
        Player p = new Player();
        p.setId("uuid-user-01");
        p.setUsername("nhantruong");
        p.setPasswordHash(UserService.hashPassword("password123")); // Mã hóa chuẩn SHA-256
        p.setEmailVerified(true);

        when(mockDAO.findByUsername("nhantruong")).thenReturn(p);

        Player result = userService.authenticate("nhantruong", "password123");

        assertNotNull(result);
        assertEquals("uuid-user-01", result.getId());
        verify(mockDAO, times(1)).updateLastLogin("uuid-user-01");
    }

    @Test
    @DisplayName("TC02.1_02: Sai mật khẩu -> Báo lỗi chung chung (Chống Enumeration)")
    void TC02_1_02_wrongPassword_genericError() throws SQLException {
        Player p = new Player();
        p.setPasswordHash(UserService.hashPassword("correctPass"));
        when(mockDAO.findByUsername("nhantruong")).thenReturn(p);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.authenticate("nhantruong", "wrongPass"));

        assertEquals("Thông tin đăng nhập không chính xác", ex.getMessage());
        verify(mockDAO, never()).updateLastLogin(anyString());
    }

    @Test
    @DisplayName("TC02.1_03: Username không tồn tại -> Báo lỗi chung chung (Chống Enumeration)")
    void TC02_1_03_userNotFound_genericError() throws SQLException {
        when(mockDAO.findByUsername("ghost_user")).thenReturn(null);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.authenticate("ghost_user", "password123"));

        // Phải trả về cùng 1 thông báo với TC02.1_02
        assertEquals("Thông tin đăng nhập không chính xác", ex.getMessage());
    }

    @Test
    @DisplayName("TC02.1_04: Email chưa xác thực -> Throw lỗi chứa EMAIL_NOT_VERIFIED")
    void TC02_1_04_emailNotVerified() throws SQLException {
        Player p = new Player();
        p.setId("uuid-user-02");
        p.setPasswordHash(UserService.hashPassword("password123"));
        p.setEmailVerified(false); // Chưa xác thực

        when(mockDAO.findByUsername("nhantruong")).thenReturn(p);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                userService.authenticate("nhantruong", "password123"));

        // LoginServlet sẽ bắt chuỗi này để chuyển hướng sang /pending-verification.jsp
        assertTrue(ex.getMessage().startsWith("EMAIL_NOT_VERIFIED:uuid-user-02"));
    }
}