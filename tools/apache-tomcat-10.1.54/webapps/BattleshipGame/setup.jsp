<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Thiết lập trận đấu — Battleship</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap" rel="stylesheet">
  <style>
    :root {
      --primary-gradient: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
      --accent-gradient: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
      --glass-bg: rgba(255, 255, 255, 0.04);
      --glass-border: rgba(255, 255, 255, 0.08);
      --glow-shadow: 0 0 15px rgba(59, 130, 246, 0.35);
    }

    body {
      background: radial-gradient(circle at 50% 50%, #0f172a 0%, #020617 100%) !important;
      color: #f8fafc !important;
      font-family: 'Outfit', system-ui, -apple-system, sans-serif !important;
    }

    .navbar {
      background: rgba(15, 23, 42, 0.75) !important;
      backdrop-filter: blur(12px) !important;
      -webkit-backdrop-filter: blur(12px) !important;
      border-bottom: 1px solid var(--glass-border) !important;
    }

    .setup-wrapper {
      background: rgba(15, 23, 42, 0.5);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border: 1px solid var(--glass-border);
      border-radius: 24px;
      padding: 35px;
      margin: 40px auto;
      max-width: 960px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
      animation: scaleUp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
    }

    @keyframes scaleUp {
      from { opacity: 0; transform: scale(0.96) translateY(10px); }
      to { opacity: 1; transform: scale(1) translateY(0); }
    }

    @keyframes shake {
      0%,100%{transform:translateX(0)} 20%,60%{transform:translateX(-6px)} 40%,80%{transform:translateX(6px)}
    }

    .setup-controls { display:flex; gap:12px; margin-bottom:18px; flex-wrap:wrap; }

    h1 {
      font-size: 2.3rem;
      background: linear-gradient(135deg, #93c5fd 0%, #3b82f6 50%, #1d4ed8 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: 24px;
      text-align: center;
      letter-spacing: -0.5px;
      font-weight: 800;
    }

    .alert-error {
      background: rgba(239, 68, 68, 0.12) !important;
      border: 1px solid rgba(239, 68, 68, 0.25) !important;
      color: #fca5a5 !important;
      border-radius: 14px !important;
      padding: 16px 20px !important;
      margin-bottom: 24px !important;
      font-size: 0.95rem !important;
      box-shadow: 0 8px 24px rgba(239, 68, 68, 0.08) !important;
      animation: shake 0.4s ease;
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .mode-cards {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 18px;
      margin-bottom: 24px;
    }

    .mode-card {
      background: var(--glass-bg);
      border: 1px solid var(--glass-border);
      border-radius: 18px;
      padding: 22px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
      position: relative;
      overflow: hidden;
    }

    .mode-card:hover {
      background: rgba(255, 255, 255, 0.07);
      border-color: rgba(59, 130, 246, 0.3);
      transform: translateY(-3px);
    }

    .mode-card.selected {
      background: rgba(59, 130, 246, 0.08);
      border-color: #3b82f6;
      box-shadow: var(--glow-shadow);
    }

    .mode-card.selected::after {
      content: '✓';
      position: absolute;
      top: 14px;
      right: 18px;
      color: #60a5fa;
      font-weight: 800;
      font-size: 1.2rem;
    }

    .mode-icon {
      font-size: 2.3rem;
      margin-bottom: 12px;
    }

    .mode-card h3 {
      margin: 0 0 6px 0;
      font-size: 1.25rem;
      color: #f1f5f9;
      font-weight: 600;
    }

    .mode-card p {
      margin: 0;
      font-size: 0.88rem;
      color: #94a3b8;
      line-height: 1.4;
    }

    .ship-panel {
      background: rgba(15, 23, 42, 0.3);
      border: 1px solid var(--glass-border);
      border-radius: 18px;
      padding: 22px;
    }

    .ship-item {
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--glass-border);
      border-radius: 14px;
      padding: 14px 18px;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
    }

    .ship-item:hover {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(59, 130, 246, 0.2);
      transform: translateX(3px);
    }

    .ship-item.placed {
      opacity: 0.45;
      background: rgba(16, 185, 129, 0.03) !important;
      border-color: rgba(16, 185, 129, 0.15) !important;
      cursor: not-allowed;
      transform: none !important;
    }

    .ship-dots {
      display: flex;
      gap: 5px;
    }

    .ship-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #475569;
      transition: background 0.3s ease;
    }

    .ship-item.placed .ship-dot {
      background: #10b981;
      box-shadow: 0 0 6px rgba(16, 185, 129, 0.5);
    }

    .grid-container {
      background: rgba(15, 23, 42, 0.4);
      border: 1px solid var(--glass-border);
      border-radius: 18px;
      padding: 22px;
      box-shadow: inset 0 2px 10px rgba(0,0,0,0.3);
    }

    .grid-cell {
      width: 38px;
      height: 38px;
      border: 1px solid rgba(255, 255, 255, 0.03) !important;
      background: rgba(255, 255, 255, 0.005);
      border-radius: 4px;
      transition: all 0.15s ease;
    }

    .grid-cell:hover {
      background: rgba(255, 255, 255, 0.06);
    }

    .grid-cell.has-ship {
      background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%) !important;
      box-shadow: inset 0 0 8px rgba(255, 255, 255, 0.3), 0 0 6px rgba(59, 130, 246, 0.4);
      border-color: #3b82f6 !important;
    }

    .btn {
      font-family: 'Outfit', sans-serif;
      font-weight: 600;
      border-radius: 12px;
      padding: 10px 18px;
      transition: all 0.25s ease;
      cursor: pointer;
    }

    .btn-secondary {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--glass-border);
      color: #e2e8f0;
    }

    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.2);
    }

    .btn-success {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      border: none;
      color: white;
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25);
    }

    .btn-success:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(16, 185, 129, 0.35);
    }
    
    .input-field {
      background: rgba(0, 0, 0, 0.25);
      border: 1px solid var(--glass-border);
      color: white;
      border-radius: 12px;
      padding: 10px 16px;
      font-family: 'Outfit', sans-serif;
      transition: all 0.3s ease;
    }
    
    .input-field:focus {
      border-color: #3b82f6;
      outline: none;
      box-shadow: var(--glow-shadow);
    }
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
