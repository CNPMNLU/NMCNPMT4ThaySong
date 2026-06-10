<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đăng nhập — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<style>
  .divider{display:flex;align-items:center;gap:12px;margin:20px 0;color:var(--text-muted);font-size:0.85rem}
  .divider::before,.divider::after{content:'';flex:1;height:1px;background:var(--border)}
  .btn-oauth{display:flex;align-items:center;justify-content:center;gap:10px;width:100%;
    padding:11px;border-radius:var(--radius);border:1px solid var(--border);
    background:transparent;color:var(--text);font-size:0.92rem;cursor:pointer;
    text-decoration:none;transition:background 0.2s;box-sizing:border-box;margin-bottom:10px}
  .btn-oauth:hover{background:rgba(255,255,255,0.06);text-decoration:none}
</style>
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div style="text-align:center;margin-bottom:8px;font-size:3rem;">⚓</div>
    <h1>BATTLESHIP</h1>
    <p class="subtitle">Đăng nhập để tham chiến</p>

    <% String err = (String) request.getAttribute("error"); if (err != null) { %>
    <div class="alert alert-error"><%= err %></div>
    <% } %>

    <form action="${pageContext.request.contextPath}/login" method="post">
      <div class="form-group">
        <label>Tên đăng nhập</label>
        <% String su = (String) request.getAttribute("savedUsername"); if (su == null) su = ""; %>
        <input type="text" name="username" placeholder="Nhập username" required autofocus value="<%= su %>">
      </div>
      <div class="form-group">
        <label style="display:flex;justify-content:space-between;align-items:center">
          Mật khẩu
        </label>
        <a href="${pageContext.request.contextPath}/forgot-password"
           style="font-size:0.8rem;font-weight:400;text-transform:none;letter-spacing:0">Quên mật khẩu?</a>
        <input type="password" name="password" placeholder="Nhập password" required>
      </div>
      <button type="submit" class="btn btn-primary">Đăng nhập</button>
    </form>

    <div class="divider">hoặc</div>

    <a href="${pageContext.request.contextPath}/auth/google" class="btn-oauth">
      <svg width="18" height="18" viewBox="0 0 48 48">
        <path fill="#EA4335" d="M24 9.5c3.1 0 5.8 1.1 8 2.9l6-6C34.5 3.2 29.5 1 24 1 14.8 1 7 6.6 3.7 14.4l7 5.4C12.4 13.6 17.7 9.5 24 9.5z"/>
        <path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v8.5h12.7c-.6 3-2.3 5.5-4.8 7.2l7.3 5.7c4.3-4 6.3-9.8 6.3-16.9z"/>
        <path fill="#FBBC05" d="M10.7 28.6A14.5 14.5 0 0 1 9.5 24c0-1.6.3-3.2.8-4.6l-7-5.4A23.9 23.9 0 0 0 0 24c0 3.9.9 7.5 2.6 10.8l8.1-6.2z"/>
        <path fill="#34A853" d="M24 47c5.4 0 10-1.8 13.3-4.8l-7.3-5.7c-1.8 1.2-4.1 1.9-6 1.9-6.3 0-11.6-4.2-13.5-9.8l-8 6.2C7 42.3 14.9 47 24 47z"/>
      </svg>
      Tiếp tục với Google
    </a>

    <a href="${pageContext.request.contextPath}/auth/facebook" class="btn-oauth">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="#1877F2">
        <path d="M24 12.073C24 5.405 18.627 0 12 0S0 5.405 0 12.073C0 18.1 4.388 23.094 10.125 24v-8.437H7.078v-3.49h3.047V9.41c0-3.025 1.792-4.697 4.533-4.697 1.312 0 2.686.236 2.686.236v2.97h-1.513c-1.491 0-1.956.93-1.956 1.886v2.268h3.328l-.532 3.49h-2.796V24C19.612 23.094 24 18.1 24 12.073z"/>
      </svg>
      Tiếp tục với Facebook
    </a>

    <p style="text-align:center;margin-top:20px;color:var(--text-muted);font-size:0.9rem;">
      Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
    </p>
  </div>
</div>
</body>
</html>
