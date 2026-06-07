<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Kết quả — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%
  String winner = (String) session.getAttribute("gameWinner");
  Integer score = (Integer) session.getAttribute("lastScore");
  String playerName = (String) session.getAttribute("playerName");
  boolean won = playerName != null && playerName.equals(winner);
  if (winner == null) {
    response.sendRedirect(request.getContextPath() + "/setup");
    return;
  }
%>
<div class="result-wrapper">
  <div class="result-card">
    <div class="result-icon"><%= won ? "🏆" : "💀" %></div>
    <h1 class="result-title <%= won ? "win" : "lose" %>">
      <%= won ? "Chiến thắng!" : "Thất bại!" %>
    </h1>
    <p class="result-score">
      <%= won ? "Xuất sắc! Bạn đã đánh chìm tất cả thuyền địch." : "AI đã đánh bại bạn lần này." %>
      <% if (score != null && score > 0) { %>
      <span>+<%= score %> điểm</span>
      <% } %>
    </p>
    <div class="result-actions">
      <a href="${pageContext.request.contextPath}/setup" class="btn btn-primary">🔄 Chơi lại</a>
      <a href="${pageContext.request.contextPath}/history" class="btn btn-secondary">📋 Lịch sử</a>
      <a href="${pageContext.request.contextPath}/leaderboard" class="btn btn-secondary">🏆 BXH</a>
    </div>
  </div>
</div>
</body>
</html>
