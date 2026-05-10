<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Thiết lập trận đấu — Battleship</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <style>
    @keyframes shake {
      0%,100%{transform:translateX(0)} 20%,60%{transform:translateX(-6px)} 40%,80%{transform:translateX(6px)}
    }
    .setup-controls { display:flex; gap:10px; margin-bottom:16px; flex-wrap:wrap; }
  </style>
</head>
<body>
<nav class="navbar">
  <span class="logo">⚓ BATTLESHIP</span>
  <nav>
    <a href="${pageContext.request.contextPath}/history">Lịch sử</a>
    <a href="${pageContext.request.contextPath}/leaderboard">BXH</a>
    <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
  </nav>
</nav>

<div class="setup-wrapper">
  <h1>⚙ Thiết lập trận đấu</h1>

  <% if (request.getAttribute("error") != null) { %>
  <div class="alert alert-error">${error}</div>
  <% } %>

  <!-- Mode selection -->
  <p style="color:var(--text-muted);margin-bottom:12px;font-size:0.9rem;">Chọn chế độ chơi:</p>
  <div class="mode-cards" id="mode-cards">
    <div class="mode-card selected" onclick="selectMode('PvE',this)">
      <div class="mode-icon">🤖</div>
      <h3>Chơi với AI</h3>
      <p>Thách đấu máy tính với 2 cấp độ</p>
    </div>
    <div class="mode-card" onclick="selectMode('PvP',this)">
      <div class="mode-icon">👥</div>
      <h3>Chơi 2 người</h3>
      <p>Luân phiên trên cùng máy</p>
    </div>
  </div>

  <div id="p2name-row" style="display:none;margin-bottom:16px">
    <p style="color:var(--text-muted);margin-bottom:8px;font-size:0.9rem;">Tên Người chơi 2:</p>
    <input type="text" id="p2name-input" class="input-field" placeholder="Người chơi 2" style="max-width:260px">
  </div>

  <div id="difficulty-row" style="margin-bottom:20px;">
    <p style="color:var(--text-muted);margin-bottom:8px;font-size:0.9rem;">Độ khó AI:</p>
    <div style="display:flex;gap:10px;">
      <button class="btn btn-secondary selected-diff" id="diff-easy" onclick="selectDiff('Easy')" style="border-color:var(--accent)">🟢 Dễ</button>
      <button class="btn btn-secondary" id="diff-hard" onclick="selectDiff('Hard')">🔴 Khó (Hunt-Target)</button>
    </div>
  </div>

  <div class="setup-grid-area">
    <div class="ship-panel">
      <h3>📦 Thuyền của bạn</h3>
      <div id="ship-list">
        <div class="ship-item" data-idx="0" onclick="selectShip(0)">
          <div class="ship-dots" id="dots-0"></div>
          <span>Carrier (5)</span>
        </div>
        <div class="ship-item" data-idx="1" onclick="selectShip(1)">
          <div class="ship-dots" id="dots-1"></div>
          <span>Battleship (4)</span>
        </div>
        <div class="ship-item" data-idx="2" onclick="selectShip(2)">
          <div class="ship-dots" id="dots-2"></div>
          <span>Cruiser (3)</span>
        </div>
        <div class="ship-item" data-idx="3" onclick="selectShip(3)">
          <div class="ship-dots" id="dots-3"></div>
          <span>Submarine (3)</span>
        </div>
        <div class="ship-item" data-idx="4" onclick="selectShip(4)">
          <div class="ship-dots" id="dots-4"></div>
          <span>Destroyer (2)</span>
        </div>
      </div>
      <div style="margin-top:16px;">
        <button id="dir-btn" class="btn btn-secondary btn-sm" onclick="toggleDir()" style="width:100%;margin-bottom:8px">↔ Ngang</button>
        <button class="btn btn-secondary btn-sm" onclick="resetSetup()" style="width:100%">🔄 Đặt lại</button>
      </div>
    </div>

    <div>
      <div class="setup-controls">
        <button class="btn btn-secondary btn-sm" onclick="doAutoPlace()">⚡ Tự động đặt</button>
        <button class="btn btn-success btn-sm" onclick="submitSetup()">▶ Bắt đầu trận đấu</button>
      </div>
      <div class="grid-container" id="setup-grid">
        <div class="grid-header">
          <% String[] cols = {"A","B","C","D","E","F","G","H","I","J"}; %>
          <% for(String c : cols) { %><div class="grid-label"><%=c%></div><% } %>
        </div>
        <% for(int y=0;y<10;y++) { %>
        <div class="grid-row">
          <div class="grid-row-label"><%=y+1%></div>
          <% for(int x=0;x<10;x++) { %>
          <div class="grid-cell" id="setup-<%=x%>-<%=y%>"
               onclick="setupCellClick(<%=x%>,<%=y%>)"
               onmouseenter="setupCellHover(<%=x%>,<%=y%>)"
               onmouseleave="clearHoverHighlight()"></div>
          <% } %>
        </div>
        <% } %>
      </div>
    </div>
  </div>

  <form id="setup-form" action="${pageContext.request.contextPath}/setup" method="post" style="display:none">
    <input type="hidden" name="action" value="manual" id="action-input">
    <input type="hidden" name="mode" value="PvE" id="mode-input">
    <input type="hidden" name="difficulty" value="Easy" id="diff-input">
    <input type="hidden" name="player2Name" id="p2name-hidden">
    <input type="hidden" name="ships" id="ships-input">
  </form>
</div>

<script src="${pageContext.request.contextPath}/js/game.js"></script>
<script>
  // Render ship dots
  const DOTS_COUNT = [5,4,3,3,2];
  DOTS_COUNT.forEach((len, i) => {
    const el = document.getElementById('dots-' + i);
    if (el) for(let d=0;d<len;d++) {
      const dot = document.createElement('div');
      dot.className = 'ship-dot';
      el.appendChild(dot);
    }
  });

  function selectMode(mode, el) {
    document.querySelectorAll('.mode-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    document.getElementById('mode-input').value = mode;
    document.getElementById('difficulty-row').style.display = mode === 'PvE' ? '' : 'none';
    document.getElementById('p2name-row').style.display = mode === 'PvP' ? '' : 'none';
  }

  function selectDiff(d) {
    document.getElementById('diff-input').value = d;
    document.getElementById('diff-easy').style.borderColor = d==='Easy' ? 'var(--accent)' : '';
    document.getElementById('diff-hard').style.borderColor = d==='Hard' ? 'var(--accent)' : '';
  }

  function doAutoPlace() {
    resetSetup();
    // Auto-place using JS for preview
    const SHIPS = [{type:'Carrier',length:5},{type:'Battleship',length:4},{type:'Cruiser',length:3},{type:'Submarine',length:3},{type:'Destroyer',length:2}];
    SHIPS.forEach((ship, idx) => {
      let placed = false, tries = 0;
      while (!placed && tries++ < 1000) {
        const x = Math.floor(Math.random()*10);
        const y = Math.floor(Math.random()*10);
        const dir = Math.random() < 0.5 ? 'H' : 'V';
        const prevDir = currentDir;
        currentDir = dir;
        if (canPlace(x, y, ship.length, dir)) {
          placeShipOnSetup(x, y, ship, idx);
          placed = true;
        } else {
          currentDir = prevDir;
        }
      }
    });
  }
</script>
</body>
</html>
