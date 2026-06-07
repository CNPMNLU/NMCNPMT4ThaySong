<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="model.GameRecord, java.time.format.DateTimeFormatter" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Chi tiết trận — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<nav class="navbar">
  <span class="logo">⚓ BATTLESHIP</span>
  <nav>
    <a href="${pageContext.request.contextPath}/setup">🎮 Chơi</a>
    <a href="${pageContext.request.contextPath}/history">📋 Lịch sử</a>
    <a href="${pageContext.request.contextPath}/leaderboard">🏆 BXH</a>
    <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
  </nav>
</nav>

<div class="page-wrapper">
  <h1>🔍 Chi tiết trận đấu</h1>

  <%
    GameRecord r = (GameRecord) request.getAttribute("matchDetail");
    String loginName = (String) session.getAttribute("playerName");
    if (loginName == null) loginName = "Bạn";
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Dùng display helpers — không phụ thuộc vào UUID
    String opponent     = "PvE".equals(r.getMode()) ? "🤖 AI" : r.getDisplayPlayer2();
    String winnerDisplay = r.getDisplayWinner();
    boolean won = loginName.equalsIgnoreCase(winnerDisplay);

    int myScore  = r.getPlayer1Score();   // player1 luôn là người đăng nhập
    int oppScore = r.getPlayer2Score();

    int dur  = r.getDurationSeconds();
    int mins = dur / 60;
    int secs = dur % 60;
  %>

  <div style="max-width:560px;margin:0 auto;">
    <div style="background:var(--card-bg);border:1px solid var(--border);border-radius:12px;padding:32px;margin-bottom:24px;">

      <!-- Tiêu đề kết quả -->
      <div style="display:flex;align-items:center;gap:16px;margin-bottom:28px;">
        <span style="font-size:3rem"><%= won ? "🏆" : "💀" %></span>
        <div>
          <div style="font-size:1.5rem;font-weight:700;color:<%= won ? "var(--success)" : "var(--danger)" %>">
            <%= won ? "Chiến thắng!" : "Thất bại" %>
          </div>
          <div style="color:var(--text-muted);font-size:0.85rem">
            <%= r.getPlayedAt() != null ? r.getPlayedAt().format(fmt) : "—" %>
          </div>
        </div>
      </div>

      <!-- Chi tiết dạng bảng -->
      <table style="width:100%;border-collapse:collapse;">
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Chế độ</td>
          <td style="padding:11px 0;text-align:right;border-bottom:1px solid var(--border)">
            <span class="badge <%= "PvE".equals(r.getMode()) ? "badge-pve" : "badge-pvp" %>"><%= r.getMode() %></span>
          </td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Bạn</td>
          <td style="padding:11px 0;text-align:right;font-weight:600;border-bottom:1px solid var(--border)"><%= r.getDisplayPlayer1() %></td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Đối thủ</td>
          <td style="padding:11px 0;text-align:right;font-weight:600;border-bottom:1px solid var(--border)"><%= opponent %></td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Người thắng</td>
          <td style="padding:11px 0;text-align:right;font-weight:600;color:var(--success);border-bottom:1px solid var(--border)"><%= winnerDisplay %></td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Điểm của bạn</td>
          <td style="padding:11px 0;text-align:right;font-weight:700;color:var(--warning);border-bottom:1px solid var(--border)"><%= myScore %></td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Điểm đối thủ</td>
          <td style="padding:11px 0;text-align:right;border-bottom:1px solid var(--border)"><%= oppScore %></td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted);border-bottom:1px solid var(--border)">Tổng lượt bắn</td>
          <td style="padding:11px 0;text-align:right;border-bottom:1px solid var(--border)"><%= r.getTotalShots() %> lượt</td>
        </tr>
        <tr>
          <td style="padding:11px 0;color:var(--text-muted)">Thời gian trận</td>
          <td style="padding:11px 0;text-align:right">
            <%= mins > 0 ? mins + " phút " : "" %><%= secs %> giây
          </td>
        </tr>
      </table>
    </div>

    <div style="display:flex;gap:12px;justify-content:center;">
      <a href="${pageContext.request.contextPath}/history" class="btn btn-secondary">← Lịch sử</a>
      <a href="${pageContext.request.contextPath}/setup"   class="btn btn-primary">🎮 Chơi ván mới</a>
    </div>
  </div>
</div>
</body>
</html>
