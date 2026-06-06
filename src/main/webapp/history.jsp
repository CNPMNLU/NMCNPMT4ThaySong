<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*, java.time.format.DateTimeFormatter" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>📋 Lịch Sử Đấu - Battleship Game</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/history.css">
  <script src="https://cdn.jsdelivr.net/npm/chart.js@3.9.1/dist/chart.min.js"></script>
</head>
<body>
<nav class="navbar">
  <span class="logo">⚓ BATTLESHIP</span>
  <div class="nav-links">
    <a href="${pageContext.request.contextPath}/setup" class="nav-link">🎮 Chơi</a>
    <a href="${pageContext.request.contextPath}/leaderboard" class="nav-link">🏆 BXH</a>
    <a href="${pageContext.request.contextPath}/profile" class="nav-link">👤 Hồ Sơ</a>
    <a href="${pageContext.request.contextPath}/logout" class="nav-link logout">Đăng xuất</a>
  </div>
</nav>

<div class="page-wrapper history-page">
  <header class="page-header">
    <h1>📋 Lịch Sử Đấu</h1>
    <p class="subtitle">Xem lại các trận đấu đã chơi và thống kê chi tiết</p>
  </header>

  <section class="stats-summary">
    <div class="stat-box stat-total">
      <div class="stat-icon">🎮</div>
      <div class="stat-content">
        <div class="stat-value">0</div>
        <div class="stat-label">Tổng Trận</div>
      </div>
    </div>
    <div class="stat-box stat-wins">
      <div class="stat-icon">✅</div>
      <div class="stat-content">
        <div class="stat-value">0</div>
        <div class="stat-label">Thắng</div>
      </div>
    </div>
    <div class="stat-box stat-losses">
      <div class="stat-icon">❌</div>
      <div class="stat-content">
        <div class="stat-value">0</div>
        <div class="stat-label">Thua</div>
      </div>
    </div>
    <div class="stat-box stat-winrate">
      <div class="stat-icon">📊</div>
      <div class="stat-content">
        <div class="stat-value">0%</div>
        <div class="stat-label">Tỷ Lệ Thắng</div>
      </div>
    </div>
  </section>

  <section class="filter-controls">
    <div class="filter-group">
      <label>Chế Độ:</label>
      <div class="filter-buttons">
        <button class="filter-btn active" data-mode="all">Tất Cả</button>
        <button class="filter-btn" data-mode="PvE">PvE (AI)</button>
        <button class="filter-btn" data-mode="PvP">PvP (Người)</button>
      </div>
    </div>

    <div class="filter-group">
      <label>Kết Quả:</label>
      <div class="filter-buttons">
        <button class="filter-btn active" data-result="all">Tất Cả</button>
        <button class="filter-btn" data-result="win">🟢 Thắng</button>
        <button class="filter-btn" data-result="loss">🔴 Thua</button>
      </div>
    </div>

    <div class="view-options">
      <button class="view-btn active" data-view="list">📋 Danh Sách</button>
      <button class="view-btn" data-view="chart">📊 Thống Kê</button>
      <button class="view-btn" data-view="timeline">📈 Tiến Độ</button>
    </div>
  </section>

  <section id="list-view" class="view-section active">
    <div class="matches-container">
      <div class="loading">⏳ Đang tải lịch sử...</div>
    </div>
    <div class="pagination">
      <button class="page-btn" id="prev-page">← Trước</button>
      <span class="page-info">Trang <span id="current-page">1</span></span>
      <button class="page-btn" id="next-page">Tiếp →</button>
    </div>
  </section>

  <section id="chart-view" class="view-section">
    <div class="chart-container">
      <canvas id="resultChart"></canvas>
    </div>
    <div class="chart-container">
      <canvas id="performanceChart"></canvas>
    </div>
  </section>

  <section id="timeline-view" class="view-section">
    <div class="timeline-container">
    </div>
  </section>
</div>

<div id="matchModal" class="modal">
  <div class="modal-content">
    <span class="modal-close">&times;</span>
    <div id="modalBody" class="modal-body">

    </div>
  </div>
</div>

<script src="${pageContext.request.contextPath}/js/api-client.js"></script>
<script src="${pageContext.request.contextPath}/js/charts.js"></script>
<script src="${pageContext.request.contextPath}/js/history.js"></script>
</body>
</html>