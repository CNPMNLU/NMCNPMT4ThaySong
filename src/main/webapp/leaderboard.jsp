<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, java.util.Map" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Bảng xếp hạng — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<nav class="navbar">
  <span class="logo">⚓ BATTLESHIP</span>
  <nav>
    <a href="${pageContext.request.contextPath}/setup">🎮 Chơi</a>
    <a href="${pageContext.request.contextPath}/history">📋 Lịch sử</a>
    <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
  </nav>
</nav>

<div class="page-wrapper">
  <h1>🏆 Bảng xếp hạng</h1>

  <% List<Map<String,Object>> top = (List<Map<String,Object>>) request.getAttribute("topPlayers");
     String myName = (String) session.getAttribute("playerName");
  %>

  <% if (top == null || top.isEmpty()) { %>
  <div style="text-align:center;padding:60px;color:var(--text-muted);">
    <div style="font-size:3rem;margin-bottom:12px">🏆</div>
    <p>Chưa có dữ liệu xếp hạng.<br><a href="${pageContext.request.contextPath}/setup">Hãy chơi và ghi điểm!</a></p>
  </div>
  <% } else { %>
  <table>
    <thead>
      <tr>
        <th>Hạng</th>
        <th>Người chơi</th>
        <th>Thắng</th>
        <th>Thua</th>
        <th>Tổng trận</th>
        <th>Tỉ lệ thắng</th>
        <th>Điểm cao nhất</th>
        <th>Tổng điểm</th>
      </tr>
    </thead>
    <tbody>
    <% for (Map<String,Object> row : top) {
        int rank = (int) row.get("rank");
        String username = (String) row.get("username");
        boolean isMe = username != null && username.equals(myName);
        String rankClass = rank == 1 ? "rank-1" : rank == 2 ? "rank-2" : rank == 3 ? "rank-3" : "";
        String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "";
    %>
      <tr style="<%= isMe ? "background:rgba(59,130,246,0.08)" : "" %>">
        <td class="<%= rankClass %>"><%= medal %> #<%= rank %></td>
        <td style="font-weight:<%= isMe ? "700" : "400" %>">
          <%= username %> <%= isMe ? "<span style='color:var(--accent);font-size:0.8rem'>(Bạn)</span>" : "" %>
        </td>
        <td style="color:var(--success)"><%= row.get("total_wins") %></td>
        <td style="color:var(--danger)"><%= row.get("total_losses") %></td>
        <td><%= row.get("total_games") %></td>
        <td style="color:var(--accent)"><%= row.get("win_rate") %>%</td>
        <td style="color:var(--warning);font-weight:600"><%= row.get("best_score") %></td>
        <td style="color:var(--text-muted)"><%= row.get("total_score") %></td>
      </tr>
    <% } %>
    </tbody>
  </table>
  <% } %>
</div>
</body>
</html>
