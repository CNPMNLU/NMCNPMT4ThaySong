<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="model.*,java.util.*" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Trận đấu — Battleship</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<nav class="navbar">
  <span class="logo">⚓ BATTLESHIP</span>
  <nav>
    <span style="color:var(--text-muted);font-size:0.9rem">👤 ${sessionScope.playerName}</span>
    <a href="${pageContext.request.contextPath}/history">Lịch sử</a>
    <a href="${pageContext.request.contextPath}/leaderboard">BXH</a>
    <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
  </nav>
</nav>

<%
  Board playerBoard = (Board) session.getAttribute("board");
  GameState gs = (GameState) session.getAttribute("gameState");
  String mode = (String) session.getAttribute("mode");
  String p2Name = (String) session.getAttribute("player2Name");
  if (p2Name == null) p2Name = "Người chơi 2";
  String p1Name = (String) session.getAttribute("playerName");
  if (p1Name == null) p1Name = "Người chơi 1";
%>

<div class="game-wrapper">
  <div class="game-header">
    <h1>⚔ <%= "PvE".equals(mode) ? "Chơi với AI" : "PvP — 2 Người chơi" %></h1>
    <div style="display:flex;align-items:center;gap:12px">
      <div id="turn-indicator" class="turn-indicator">⚔ Lượt của bạn</div>
      <div id="turn-timer" class="turn-timer"></div>
    </div>
    <div style="display:flex;gap:8px">
      <a href="${pageContext.request.contextPath}/setup" class="btn btn-secondary btn-sm">🔄 Ván mới</a>
    </div>
  </div>

  <div class="boards-container">
    <!-- Enemy board -->
    <div class="board-area enemy-board">
      <h2>🎯 Lưới địch — bắn vào đây</h2>
      <div class="grid-container">
        <div class="grid-header">
          <% String[] c2 = {"A","B","C","D","E","F","G","H","I","J"};
             for(String c : c2) { %><div class="grid-label"><%=c%></div><% } %>
        </div>
        <% for(int y=0;y<10;y++) { %>
        <div class="grid-row">
          <div class="grid-row-label"><%=y+1%></div>
          <% for(int x=0;x<10;x++) { %>
          <div class="grid-cell" id="enemy-<%=x%>-<%=y%>"
               onclick="<%= "PvP".equals(mode) ? "fireShotPvP(" + x + "," + y + ")" : "fireShot(" + x + "," + y + ")" %>"
               title="<%=c2[x]%><%=y+1%>"></div>
          <% } %>
        </div>
        <% } %>
      </div>
    </div>

    <!-- Player board -->
    <div class="board-area my-board">
      <h2>🛡 Lưới của bạn</h2>
      <div class="grid-container">
        <div class="grid-header">
          <% for(String c : c2) { %><div class="grid-label"><%=c%></div><% } %>
        </div>
        <% for(int y=0;y<10;y++) { %>
        <div class="grid-row">
          <div class="grid-row-label"><%=y+1%></div>
          <% for(int x=0;x<10;x++) {
            String cls = "grid-cell";
            if (playerBoard != null && playerBoard.getCells() != null) {
              Cell cell = playerBoard.getCells()[x][y];
              if (cell != null) {
                if (cell.isHit() && cell.isHasShip()) cls += " hit-ship";
                else if (cell.isHit()) cls += " miss";
                else if (cell.isHasShip()) cls += " has-ship";
              }
            }
          %>
          <div class="<%=cls%>" id="my-<%=x%>-<%=y%>"></div>
          <% } %>
        </div>
        <% } %>
      </div>
    </div>
  </div>

  <!-- Log -->
  <div>
    <p style="color:var(--text-muted);font-size:0.85rem;margin-bottom:8px;text-transform:uppercase;letter-spacing:1px">📋 Nhật ký chiến đấu</p>
    <div class="game-log" id="game-log">
      <div class="log-entry" style="color:var(--text-muted)">Trận đấu bắt đầu. Chúc may mắn! 🚀</div>
    </div>
  </div>
</div>

<!-- PvP: Player 2 setup overlay -->
<% if ("PvP".equals(mode)) { %>
<div id="pvp-setup-overlay" class="modal-overlay" style="display:none">
  <div class="modal pvp-setup-modal">
    <h2 id="pvp-setup-title" style="margin-bottom:16px">Người chơi 2 — Đặt thuyền</h2>
    <div style="display:flex;gap:20px;align-items:flex-start;flex-wrap:wrap;justify-content:center">
      <!-- Ship list -->
      <div class="ship-panel" style="min-width:160px">
        <h3>📦 Thuyền</h3>
        <div id="pvp-ship-list">
          <div class="ship-item pvp-ship-item" data-idx="0" onclick="pvpSelectShip(0)"><div class="ship-dots" id="pvp-dots-0"></div><span>Carrier (5)</span></div>
          <div class="ship-item pvp-ship-item" data-idx="1" onclick="pvpSelectShip(1)"><div class="ship-dots" id="pvp-dots-1"></div><span>Battleship (4)</span></div>
          <div class="ship-item pvp-ship-item" data-idx="2" onclick="pvpSelectShip(2)"><div class="ship-dots" id="pvp-dots-2"></div><span>Cruiser (3)</span></div>
          <div class="ship-item pvp-ship-item" data-idx="3" onclick="pvpSelectShip(3)"><div class="ship-dots" id="pvp-dots-3"></div><span>Submarine (3)</span></div>
          <div class="ship-item pvp-ship-item" data-idx="4" onclick="pvpSelectShip(4)"><div class="ship-dots" id="pvp-dots-4"></div><span>Destroyer (2)</span></div>
        </div>
        <div style="margin-top:12px;display:flex;flex-direction:column;gap:6px">
          <button id="pvp-dir-btn" class="btn btn-secondary btn-sm" onclick="pvpToggleDir()">↔ Ngang</button>
          <button class="btn btn-secondary btn-sm" onclick="pvpResetSetup()">🔄 Đặt lại</button>
          <button class="btn btn-secondary btn-sm" onclick="pvpAutoPlace()">⚡ Tự động</button>
        </div>
      </div>
      <!-- Grid -->
      <div>
        <div class="grid-container" id="pvp-setup-grid">
          <div class="grid-header">
            <% String[] cols2 = {"A","B","C","D","E","F","G","H","I","J"};
               for(String c : cols2) { %><div class="grid-label"><%=c%></div><% } %>
          </div>
          <% for(int y=0;y<10;y++) { %>
          <div class="grid-row">
            <div class="grid-row-label"><%=y+1%></div>
            <% for(int x=0;x<10;x++) { %>
            <div class="grid-cell" id="pvp-setup-<%=x%>-<%=y%>"
                 onclick="pvpSetupCellClick(<%=x%>,<%=y%>)"
                 onmouseenter="pvpSetupCellHover(<%=x%>,<%=y%>)"
                 onmouseleave="pvpSetupCellHover(-1,-1)"></div>
            <% } %>
          </div>
          <% } %>
        </div>
        <div style="margin-top:12px;text-align:center">
          <button class="btn btn-success" onclick="submitPvPSetup2()">▶ Sẵn sàng chiến đấu!</button>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- PvP: Screen handover overlay -->
<div id="pvp-turn-overlay" class="modal-overlay" style="display:none">
  <div class="modal" style="text-align:center;max-width:400px">
    <div style="font-size:3rem;margin-bottom:12px">🎮</div>
    <h2 id="pvp-turn-name" style="margin-bottom:8px">Người chơi 1, đến lượt bạn!</h2>
    <p id="pvp-turn-hint" style="color:var(--text-muted);margin-bottom:24px">Đưa màn hình cho người chơi, rồi nhấn Sẵn sàng.</p>
    <button class="btn btn-primary" style="font-size:1.1rem;padding:12px 32px" onclick="pvpReady()">✅ Sẵn sàng</button>
  </div>
</div>
<% } %>

<!-- End game modal -->
<div id="end-modal" class="modal-overlay" style="display:none">
  <div class="modal">
    <div style="font-size:3.5rem;margin-bottom:12px">🎉</div>
    <h2 class="result-title win" id="modal-title">Bạn thắng!</h2>
    <p id="modal-subtitle" style="color:var(--text-muted);margin:8px 0 24px"></p>
    <div style="display:flex;gap:12px;justify-content:center">
      <a href="${pageContext.request.contextPath}/setup" class="btn btn-primary">🔄 Chơi lại</a>
      <a href="${pageContext.request.contextPath}/history" class="btn btn-secondary">📋 Lịch sử</a>
      <a href="${pageContext.request.contextPath}/leaderboard" class="btn btn-secondary">🏆 BXH</a>
    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/game.js"></script>
<script>
<% if ("PvP".equals(mode)) { %>
// Render pvp ship dots
[5,4,3,3,2].forEach((len,i) => {
  const el = document.getElementById('pvp-dots-' + i);
  if (el) for(let d=0;d<len;d++){const dot=document.createElement('div');dot.className='ship-dot';el.appendChild(dot);}
});
// Load P1 ships from session & init PvP
const p1Ships = <%= session.getAttribute("shipsJson") != null ? session.getAttribute("shipsJson") : "[]" %>;
initPvP('<%=p1Name%>', '<%=p2Name%>', p1Ships);
init('${pageContext.request.contextPath}', 'PvP');
<% } else { %>
init('${pageContext.request.contextPath}', 'PvE');
<% } %>
</script>
</body>
</html>
