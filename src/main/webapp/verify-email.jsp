<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Xác thực Email — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card" style="text-align:center">
    <%
      String status = (String) request.getAttribute("verifyStatus");
      if (status == null) status = "";
    %>

    <% if ("success".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">✅</div>
      <h1 style="color:var(--success)">Xác thực thành công!</h1>
      <p class="subtitle">Email của bạn đã được xác thực</p>
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:12px">Đăng nhập ngay</a>

    <% } else if ("already".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">✅</div>
      <h1>Email đã được xác thực</h1>
      <p class="subtitle">Tài khoản của bạn đã hoạt động</p>
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:12px">Đăng nhập</a>

    <% } else if ("expired".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">⏰</div>
      <h1>Link đã hết hạn</h1>
      <p class="subtitle">Link xác thực chỉ có hiệu lực trong 24 giờ</p>
      <%
        String expiredUserId = (String) request.getAttribute("expiredUserId");
        if (expiredUserId != null) {
      %>
      <form action="${pageContext.request.contextPath}/resend-verification" method="post" style="margin-top:16px">
        <input type="hidden" name="userId" value="<%= expiredUserId %>">
        <button type="submit" class="btn btn-primary">Gửi lại email xác thực</button>
      </form>
      <% } %>

    <% } else if ("resent".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">📧</div>
      <h1>Đã gửi lại!</h1>
      <div class="alert alert-success">Email xác thực mới đã được gửi. Vui lòng kiểm tra hộp thư.</div>
      <a href="${pageContext.request.contextPath}/login" style="color:var(--text-muted);font-size:0.9rem">← Quay lại đăng nhập</a>

    <% } else { %>
      <div style="font-size:3.5rem;margin-bottom:12px">❌</div>
      <h1>Link không hợp lệ</h1>
      <p class="subtitle">Link xác thực không tồn tại hoặc đã được sử dụng</p>
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:12px">Về trang đăng nhập</a>
    <% } %>
  </div>
</div>
</body>
</html>
