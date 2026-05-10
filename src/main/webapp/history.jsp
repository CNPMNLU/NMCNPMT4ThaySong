<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="model.GameRecord, java.util.List, java.time.format.DateTimeFormatter" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Lịch sử đấu — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<nav class="navbar">
  <span class="logo">⚓ BATTLESHIP</span>
  <nav>
    <a href="${pageContext.request.contextPath}/setup">🎮 Chơi</a>
    <a href="${pageContext.request.contextPath}/leaderboard">🏆 BXH</a>
    <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
  </nav>
</nav>

<div class="page-wrapper">
  <h1>📋 Lịch sử đấu</h1>

  <% if (request.getAttribute("error") != null) { %>
  <div class="alert alert-error">${error}</div>
  <% } %>

  <%
    List<GameRecord> records = (List<GameRecord>) request.getAttribute("records");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    String playerId = (String) session.getAttribute("playerId");
    String loginName = (String) session.getAttribute("playerName");
    if (loginName == null) loginName = "Bạn";
  %>

  <% if (records == null || records.isEmpty()) { %>
  <div style="text-align:center;padding:60px;color:var(--text-muted);">
    <div style="font-size:3rem;margin-bottom:12px">🎮</div>
    <p>Bạn chưa có trận đấu nào.<br><a href="${pageContext.request.contextPath}/setup">Bắt đầu chơi ngay!</a></p>
  </div>
  <% } else { %>
  <table>
    <thead>
      <tr>
        <th>Thời gian</th>
        <th>Chế độ</th>
        <th>Đối thủ</th>
        <th>Người thắng</th>
        <th>Kết quả</th>
        <th>Điểm</th>
        <th>Số lượt</th>
        <th>Thời gian trận</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
    <% for (GameRecord r : records) {
        // Đối thủ: dùng player2Name (đã lưu trong DB — "AI" hoặc tên nhập)
        String opponent = r.getDisplayPlayer2();
        if ("PvE".equals(r.getMode())) opponent = "🤖 AI";

        // Người thắng: lấy từ winner_name đã lưu
        String winnerDisplay = r.getDisplayWinner();

        // Kết quả: so tên đăng nhập với winner_name
        boolean won = loginName.equalsIgnoreCase(winnerDisplay)
                   || playerId.equals(r.getPlayer1Id()) && loginName.equalsIgnoreCase(r.getWinnerName());
        // Fallback: nếu winner_name = tên mình
        String resultLabel = won ? "✓ Thắng" : "✗ Thua";
        String resultClass = won ? "badge-win" : "badge-lose";

        // Điểm: player1 là người đang đăng nhập (always)
        int myScore = r.getPlayer1Score();
    %>
      <tr>
        <td style="color:var(--text-muted);font-size:0.85rem">
          <%= r.getPlayedAt() != null ? r.getPlayedAt().format(fmt) : "—" %>
        </td>
        <td><span class="badge <%= "PvE".equals(r.getMode()) ? "badge-pve" : "badge-pvp" %>"><%= r.getMode() %></span></td>
        <td><%= opponent %></td>
        <td style="font-weight:600"><%= winnerDisplay %></td>
        <td><span class="badge <%= resultClass %>"><%= resultLabel %></span></td>
        <td style="color:var(--warning);font-weight:600"><%= myScore %></td>
        <td><%= r.getTotalShots() %> lượt</td>
        <td style="color:var(--text-muted)"><%= r.getDurationSeconds() %>s</td>
        <td>
          <a href="${pageContext.request.contextPath}/history?id=<%= r.getId() %>"
             class="btn btn-secondary btn-sm">Chi tiết</a>
        </td>
      </tr>
    <% } %>
    </tbody>
  </table>
  <% } %>
</div>
</body>
</html>
