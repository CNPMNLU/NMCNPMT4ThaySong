<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đăng nhập — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card">
    <div style="text-align:center;margin-bottom:8px;font-size:3rem;">⚓</div>
    <h1>BATTLESHIP</h1>
    <p class="subtitle">Đăng nhập để tham chiến</p>

    <% String errorMsg = (String) request.getAttribute("error");
       if (errorMsg != null) { %>
    <div class="alert alert-error"><%= errorMsg %></div>
    <% } %>

    <form action="${pageContext.request.contextPath}/login" method="post">
      <div class="form-group">
        <label>Tên đăng nhập</label>
        <%
          // Ưu tiên savedUsername từ LoginServlet (sau lỗi), rồi mới lấy từ request param
          String savedUsername = (String) request.getAttribute("savedUsername");
          if (savedUsername == null) savedUsername = request.getParameter("username");
          if (savedUsername == null) savedUsername = "";
        %>
        <input type="text" name="username" placeholder="Nhập username" required autofocus
               value="<%= savedUsername %>">
      </div>
      <div class="form-group">
        <label>Mật khẩu</label>
        <input type="password" name="password" placeholder="Nhập password" required>
      </div>
      <button type="submit" class="btn btn-primary">Đăng nhập</button>
    </form>
    <p style="text-align:center;margin-top:20px;color:var(--text-muted);font-size:0.9rem;">
      Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
    </p>
  </div>
</div>
</body>
</html>
