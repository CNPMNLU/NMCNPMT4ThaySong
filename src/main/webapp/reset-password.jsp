<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đặt lại mật khẩu — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <% if (Boolean.TRUE.equals(request.getAttribute("resetSuccess"))) { %>
      <div style="text-align:center">
        <div style="font-size:3.5rem;margin-bottom:12px">✅</div>
        <h1>Đổi mật khẩu thành công!</h1>
        <p class="subtitle">Bạn có thể đăng nhập bằng mật khẩu mới</p>
        <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:16px;display:block">Đăng nhập ngay</a>
      </div>
    <% } else if (request.getAttribute("tokenError") != null) { %>
      <div style="text-align:center">
        <div style="font-size:3.5rem;margin-bottom:12px">❌</div>
        <h1>Link không hợp lệ</h1>
        <p class="subtitle"><%= request.getAttribute("tokenError") %></p>
        <a href="${pageContext.request.contextPath}/forgot-password" class="btn btn-primary" style="margin-top:16px;display:block">Yêu cầu link mới</a>
      </div>
    <% } else { %>
      <div style="font-size:3rem;text-align:center;margin-bottom:8px">🔐</div>
      <h1>Đặt lại mật khẩu</h1>
      <% String username = (String) request.getAttribute("username"); if (username != null) { %>
      <p class="subtitle">Tài khoản: <strong><%= username %></strong></p>
      <% } %>
      <% String err = (String) request.getAttribute("error"); if (err != null) { %>
      <div class="alert alert-error"><%= err %></div>
      <% } %>
      <form action="${pageContext.request.contextPath}/reset-password" method="post">
        <input type="hidden" name="token" value="<%= request.getAttribute("token") != null ? request.getAttribute("token") : "" %>">
        <div class="form-group">
          <label>Mật khẩu mới</label>
          <input type="password" name="newPassword" placeholder="Tối thiểu 6 ký tự" required autofocus>
        </div>
        <div class="form-group">
          <label>Xác nhận mật khẩu mới</label>
          <input type="password" name="confirmPassword" placeholder="Nhập lại mật khẩu mới" required>
        </div>
        <button type="submit" class="btn btn-primary">Đặt lại mật khẩu</button>
      </form>
    <% } %>
  </div>
</div>
</body>
</html>
