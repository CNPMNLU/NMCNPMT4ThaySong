<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Quên mật khẩu — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <% if (Boolean.TRUE.equals(request.getAttribute("sent"))) { %>
      <div style="text-align:center">
        <div style="font-size:3.5rem;margin-bottom:12px">📧</div>
        <h1>Kiểm tra Email</h1>
        <p class="subtitle">Nếu <strong><%= request.getAttribute("maskedEmail") %></strong> tồn tại trong hệ thống, chúng tôi đã gửi link đặt lại mật khẩu.</p>
        <div class="alert alert-success" style="text-align:left;margin-top:16px">
          Link có hiệu lực trong <strong>1 giờ</strong>. Kiểm tra cả thư mục Spam.
        </div>
        <a href="${pageContext.request.contextPath}/login" style="color:var(--text-muted);font-size:0.9rem;display:inline-block;margin-top:12px">← Quay lại đăng nhập</a>
      </div>
    <% } else { %>
      <div style="font-size:3rem;text-align:center;margin-bottom:8px">🔑</div>
      <h1>Quên mật khẩu</h1>
      <p class="subtitle">Nhập email để nhận link đặt lại mật khẩu</p>
      <% String err = (String) request.getAttribute("error"); if (err != null) { %>
      <div class="alert alert-error"><%= err %></div>
      <% } %>
      <form action="${pageContext.request.contextPath}/forgot-password" method="post">
        <div class="form-group">
          <label>Email</label>
          <input type="email" name="email" placeholder="Nhập email đã đăng ký" required autofocus>
        </div>
        <button type="submit" class="btn btn-primary">Gửi link đặt lại</button>
      </form>
      <p style="text-align:center;margin-top:20px;color:var(--text-muted);font-size:0.9rem">
        <a href="${pageContext.request.contextPath}/login">← Quay lại đăng nhập</a>
      </p>
    <% } %>
  </div>
</div>
</body>
</html>
