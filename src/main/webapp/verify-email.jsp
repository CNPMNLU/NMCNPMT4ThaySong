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
      <h1>Xác thực thành công!</h1>
      <p class="subtitle">Email đã được xác thực. Bạn có thể đăng nhập ngay.</p>
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:16px;display:block">Đăng nhập</a>

    <% } else if ("already".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">✅</div>
      <h1>Email đã xác thực</h1>
      <p class="subtitle">Tài khoản đã hoạt động</p>
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:16px;display:block">Đăng nhập</a>

    <% } else if ("expired".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">⏰</div>
      <h1>Link đã hết hạn</h1>
      <p class="subtitle">Link xác thực chỉ có hiệu lực 24 giờ</p>
      <% String expiredId = (String) request.getAttribute("expiredUserId"); if (expiredId != null) { %>
      <form action="${pageContext.request.contextPath}/resend-verification" method="post" style="margin-top:16px">
        <input type="hidden" name="userId" value="<%= expiredId %>">
        <button type="submit" class="btn btn-primary" style="width:100%">Gửi lại email xác thực</button>
      </form>
      <% } %>

    <% } else if ("resent".equals(status)) { %>
      <div style="font-size:3.5rem;margin-bottom:12px">📧</div>
      <h1>Đã gửi lại!</h1>
      <div class="alert alert-success">Email xác thực mới đã được gửi.</div>
      <a href="${pageContext.request.contextPath}/login" style="color:var(--text-muted);font-size:0.9rem">← Quay lại đăng nhập</a>

    <% } else { %>
      <div style="font-size:3.5rem;margin-bottom:12px">❌</div>
      <h1>Link không hợp lệ</h1>
      <p class="subtitle">Link đã được dùng hoặc không tồn tại</p>
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary" style="margin-top:16px;display:block">Về trang đăng nhập</a>
    <% } %>
  </div>
</div>
</body>
</html>
