<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Kiểm tra Email — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-wrapper">
  <div class="auth-card" style="text-align:center">
    <div style="font-size:3.5rem;margin-bottom:12px">📧</div>
    <h1>Kiểm tra Email</h1>
    <p class="subtitle">Chúng tôi đã gửi link xác thực đến email của bạn</p>
    <div class="alert alert-success" style="text-align:left">
      Mở email và nhấn vào link xác thực. Link có hiệu lực trong <strong>24 giờ</strong>.
    </div>
    <p style="color:var(--text-muted);font-size:0.88rem;margin-bottom:20px">
      Không thấy email? Kiểm tra thư mục <strong>Spam / Junk</strong>.
    </p>
    <%
      String pendingId = (String) session.getAttribute("pendingVerifyId");
      if (pendingId != null) {
    %>
    <form action="${pageContext.request.contextPath}/resend-verification" method="post" style="margin-bottom:16px">
      <input type="hidden" name="userId" value="<%= pendingId %>">
      <button type="submit" class="btn btn-secondary" style="width:100%">Gửi lại email xác thực</button>
    </form>
    <% } %>
    <a href="${pageContext.request.contextPath}/login" style="color:var(--text-muted);font-size:0.9rem">← Quay lại đăng nhập</a>
  </div>
</div>
</body>
</html>
