package controller;

import org.junit.jupiter.api.*;
import org.mockito.*;

import jakarta.servlet.http.*;

import static org.mockito.Mockito.*;

@DisplayName("UC-03 – Đăng xuất")
class LogoutTest {

    @Mock private HttpServletRequest mockReq;
    @Mock private HttpServletResponse mockResp;
    @Mock private HttpSession mockSession;

    private LogoutServlet servlet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servlet = new LogoutServlet();
        when(mockReq.getContextPath()).thenReturn("/battleship");
    }

    @Test
    @DisplayName("TC03_01: Đang có Session -> Gọi invalidate() và redirect về /login")
    void TC03_01_activeSession_invalidatesAndRedirects() throws Exception {
        // getSession(false) trả về session hiện tại
        when(mockReq.getSession(false)).thenReturn(mockSession);

        servlet.doGet(mockReq, mockResp);

        verify(mockSession, times(1)).invalidate();
        verify(mockResp, times(1)).sendRedirect("/battleship/login");
    }

    @Test
    @DisplayName("TC03_02: Đã hết hạn Session (Session = null) -> Bỏ qua invalidate, vẫn redirect")
    void TC03_02_nullSession_redirectsSafely() throws Exception {
        // getSession(false) trả về null (session đã timeout)
        when(mockReq.getSession(false)).thenReturn(null);

        servlet.doGet(mockReq, mockResp);

        verify(mockSession, never()).invalidate(); // Đảm bảo không bị NullPointerException
        verify(mockResp, times(1)).sendRedirect("/battleship/login");
    }

    @Test
    @DisplayName("TC03_03: Hỗ trợ cả method POST")
    void TC03_03_postMethod_callsDoGet() throws Exception {
        when(mockReq.getSession(false)).thenReturn(mockSession);

        servlet.doPost(mockReq, mockResp);

        verify(mockSession, times(1)).invalidate();
        verify(mockResp, times(1)).sendRedirect("/battleship/login");
    }
}