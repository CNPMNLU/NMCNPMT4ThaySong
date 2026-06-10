<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đăng ký — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div style="text-align:center;margin-bottom:8px;font-size:3rem;">⚓</div>
    <h1>BATTLESHIP</h1>
    <p class="subtitle">Tạo tài khoản để tham chiến</p>

    <% String err = (String) request.getAttribute("error"); if (err != null) { %>
    <div class="alert alert-error"><%= err %></div>
    <% } %>

    <form action="${pageContext.request.contextPath}/register" method="post">
      <div class="form-group">
        <label>Tên đăng nhập</label>
        <% String su = (String) request.getAttribute("savedUsername"); if (su == null) su = ""; %>
        <input type="text" name="username" placeholder="Tối thiểu 3 ký tự" required autofocus value="<%= su %>">
      </div>
      <div class="form-group">
        <label>Email</label>
        <% String se = (String) request.getAttribute("savedEmail"); if (se == null) se = ""; %>
        <input type="email" name="email" placeholder="example@email.com" required value="<%= se %>">
      </div>
      <div class="form-group">
        <label>Mật khẩu</label>
        <input type="password" name="password" placeholder="Tối thiểu 6 ký tự" required>
      </div>
      <div class="form-group">
        <label>Xác nhận mật khẩu</label>
        <input type="password" name="confirmPassword" placeholder="Nhập lại mật khẩu" required>
      </div>
      <button type="submit" class="btn btn-primary">Đăng ký</button>
    </form>
    <p style="text-align:center;margin-top:20px;color:var(--text-muted);font-size:0.9rem;">
      Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
    </p>
  </div>
</div>
</body>
</html>
