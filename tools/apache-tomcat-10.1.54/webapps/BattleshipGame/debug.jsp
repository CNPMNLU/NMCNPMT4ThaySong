<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<%@ page import="java.security.MessageDigest" %>
<%@ page import="java.sql.*" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>🔧 Debug — Battleship</title>
<style>
  body { font-family: monospace; background: #0f172a; color: #e2e8f0; padding: 24px; }
  h2 { color: #38bdf8; border-bottom: 1px solid #334155; padding-bottom: 8px; }
  .box { background: #1e293b; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
  .ok  { color: #4ade80; } .err { color: #f87171; } .warn { color: #fbbf24; }
  table { border-collapse: collapse; width: 100%; }
  td, th { border: 1px solid #334155; padding: 6px 12px; text-align: left; }
  th { background: #0f172a; color: #94a3b8; }
  input[type=text], input[type=password] {
    background:#0f172a; color:#e2e8f0; border:1px solid #475569;
    padding:8px; border-radius:4px; width:220px; margin-right:8px;
  }
  button { background:#2563eb; color:white; border:none; padding:8px 18px; border-radius:4px; cursor:pointer; }
  pre { background:#0f172a; padding:12px; border-radius:4px; overflow-x:auto; color:#f87171; font-size:0.85rem; }
  .fix { background: rgba(234,179,8,0.1); border:1px solid rgba(234,179,8,0.4); border-radius:6px; padding:10px 14px; margin-top:10px; color:#fbbf24; }
</style>
</head>
<body>
<h1>🔧 Debug Panel — Battleship</h1>

<%!
  static String sha256(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) { return "ERROR: " + e.getMessage(); }
  }
%>

<%
  String dbUrl  = "jdbc:mysql://localhost:3306/battleship?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
  String dbUser = "root";
  String dbPass = "";
  Connection conn = null;
  String connError = null;
  try {
    Class.forName("com.mysql.cj.jdbc.Driver");
    conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
  } catch (Exception e) {
    connError = e.getClass().getSimpleName() + ": " + e.getMessage();
  }
%>

<!-- 1. DB CONNECTION -->
<h2>1. Kết nối Database</h2>
<div class="box">
<% if (conn != null) { %>
  <p class="ok">✅ Kết nối thành công tới <b>localhost:3306/battleship</b></p>
<% } else { %>
  <p class="err">❌ Không kết nối được DB</p>
  <pre><%= connError %></pre>
  <% if (connError != null && connError.contains("Access denied")) { %>
    <div class="fix">🔧 Sửa: Kiểm tra USER/PASS trong <b>DBConnection.java</b></div>
  <% } else if (connError != null && connError.toLowerCase().contains("unknown database")) { %>
    <div class="fix">🔧 Sửa: Chạy <code>CREATE DATABASE battleship;</code> trong MySQL</div>
  <% } else if (connError != null && connError.contains("refused")) { %>
    <div class="fix">🔧 Sửa: MySQL chưa chạy. Khởi động MySQL service.</div>
  <% } %>
<% } %>
</div>

<!-- 2. KIỂM TRA CẤU TRÚC BẢNG USERS -->
<h2>2. Cấu trúc bảng users (Nguyên nhân chính!)</h2>
<div class="box">
<% if (conn != null) {
  try {
    DatabaseMetaData meta = conn.getMetaData();
    ResultSet cols = meta.getColumns(null, null, "users", null);
    boolean hasLastLogin = false;
    boolean hasTable = false;
%>
  <table>
    <tr><th>Cột</th><th>Kiểu</th><th>Nullable</th><th>Trạng thái</th></tr>
<%
    while (cols.next()) {
      hasTable = true;
      String colName = cols.getString("COLUMN_NAME");
      String colType = cols.getString("TYPE_NAME");
      String nullable = cols.getString("IS_NULLABLE");
      if (colName.equals("last_login")) hasLastLogin = true;
      String status = "";
      if (colName.equals("last_login")) {
        status = "<span class='ok'>✅ Có cột này (cần thiết)</span>";
      } else {
        status = "<span style='color:#94a3b8'>—</span>";
      }
%>
    <tr>
      <td><b><%= colName %></b></td>
      <td><%= colType %></td>
      <td><%= nullable %></td>
      <td><%= status %></td>
    </tr>
<%  }
    if (!hasTable) { %>
      <tr><td colspan="4" class="err">❌ Bảng users KHÔNG TỒN TẠI</td></tr>
<%  } %>
  </table>

  <% if (hasTable && !hasLastLogin) { %>
  <div class="fix">
    ⚠️ <b>ĐÂY LÀ NGUYÊN NHÂN LOGIN KHÔNG ĐƯỢC!</b><br><br>
    Bảng <b>users</b> thiếu cột <b>last_login</b>.<br>
    Khi đăng nhập đúng, code gọi <code>updateLastLogin()</code> → lỗi SQL → bị catch → reload lại trang login.<br><br>
    <b>Chạy lệnh này trong MySQL để fix:</b><br>
    <code style="background:#0f172a;padding:6px 10px;border-radius:4px;display:inline-block;margin-top:6px;color:#4ade80">
      ALTER TABLE users ADD COLUMN last_login DATETIME NULL;
    </code>
  </div>
  <% } else if (hasTable && hasLastLogin) { %>
  <p class="ok">✅ Bảng users có đủ cột <b>last_login</b></p>
  <% } %>

<% } catch (Exception e) { %>
  <p class="err">Lỗi đọc schema: <%= e.getMessage() %></p>
<% } } else { %>
  <p class="warn">⚠ Bỏ qua — DB chưa kết nối</p>
<% } %>
</div>

<!-- 3. DỮ LIỆU USERS -->
<h2>3. Dữ liệu bảng users</h2>
<div class="box">
<% if (conn != null) {
  try {
    Statement st = conn.createStatement();
    ResultSet rs = st.executeQuery("SELECT username, password_hash, email, created_at, last_login FROM users LIMIT 10");
    boolean hasRows = false;
%>
  <table>
    <tr><th>username</th><th>hash (6 ký tự đầu...)</th><th>email</th><th>last_login</th></tr>
<% while (rs.next()) {
     hasRows = true;
     String h = rs.getString("password_hash");
     String shortHash = (h != null && h.length() > 6) ? h.substring(0,6) + "..." : h;
%>
    <tr>
      <td><b><%= rs.getString("username") %></b></td>
      <td style="color:#94a3b8"><%= shortHash %></td>
      <td><%= rs.getString("email") %></td>
      <td><%= rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login") : "<span style='color:#475569'>null</span>" %></td>
    </tr>
<% }
   if (!hasRows) { %>
    <tr><td colspan="4" class="warn">⚠ Bảng trống — chưa INSERT data</td></tr>
<% } %>
  </table>
<% } catch (Exception e) { %>
  <p class="err">Lỗi: <%= e.getMessage() %></p>
<% } } else { %>
  <p class="warn">⚠ Bỏ qua — DB chưa kết nối</p>
<% } %>
</div>

<!-- 4. TEST LOGIN TRỰC TIẾP -->
<h2>4. Test Đăng Nhập Trực Tiếp</h2>
<div class="box">
<%
  String testUser = request.getParameter("testUser");
  String testPass = request.getParameter("testPass");
  if (testUser != null && !testUser.isEmpty() && testPass != null) {
    if (conn != null) {
      try {
        // Step 1: tìm user
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
        ps.setString(1, testUser);
        ResultSet r = ps.executeQuery();
        if (!r.next()) {
%>
          <p class="err">❌ Không tìm thấy username: <b><%= testUser %></b></p>
<%      } else {
          String storedHash = r.getString("password_hash");
          String inputHash  = sha256(testPass);
          if (!storedHash.equals(inputHash)) {
%>
            <p class="err">❌ Sai mật khẩu</p>
            <p style="font-size:0.8rem;color:#94a3b8">Input hash: <%= inputHash %></p>
            <p style="font-size:0.8rem;color:#94a3b8">DB hash:    <%= storedHash %></p>
<%          } else {
              // Step 2: thử updateLastLogin
              String userId = r.getString("id");
              String updateError = null;
              try {
                PreparedStatement upd = conn.prepareStatement("UPDATE users SET last_login = NOW() WHERE id = ?");
                upd.setString(1, userId);
                upd.executeUpdate();
              } catch (Exception ue) {
                updateError = ue.getMessage();
              }
              if (updateError != null) {
%>
                <p class="warn">⚠️ Hash KHỚP nhưng <b>updateLastLogin thất bại!</b></p>
                <p class="err">→ Đây chính là lý do login không chuyển trang!</p>
                <pre><%= updateError %></pre>
                <div class="fix">
                  🔧 Chạy lệnh SQL này để fix:<br>
                  <code style="background:#0f172a;padding:6px 10px;border-radius:4px;display:inline-block;margin-top:6px;color:#4ade80">
                    ALTER TABLE users ADD COLUMN last_login DATETIME NULL;
                  </code>
                </div>
<%            } else { %>
                <p class="ok">✅ Đăng nhập OK! Hash khớp + updateLastLogin thành công.</p>
                <p style="color:#94a3b8">Nếu vẫn không vào được setup → kiểm tra SetupServlet hoặc lỗi trong Tomcat log.</p>
<%            }
          }
        }
      } catch (Exception e) { %>
        <p class="err">Lỗi query: <%= e.getMessage() %></p>
<%    }
    } else { %>
      <p class="err">❌ DB chưa kết nối</p>
<%  }
  }
%>
<form method="get" style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
  <input type="text"     name="testUser" placeholder="Username" value="<%= testUser != null ? testUser : "admin" %>">
  <input type="password" name="testPass" placeholder="Password">
  <button type="submit">🔍 Test Login</button>
</form>
</div>

<!-- 5. SESSION -->
<h2>5. Session hiện tại</h2>
<div class="box">
<%
  HttpSession sess = request.getSession(false);
  if (sess != null && sess.getAttribute("playerId") != null) { %>
    <p class="ok">✅ Session hợp lệ: <b><%= sess.getAttribute("playerName") %></b></p>
    <a href="<%= request.getContextPath() %>/setup" style="color:#38bdf8">→ Vào trang Setup</a>
<% } else { %>
    <p class="warn">⚠ Chưa có session</p>
    <a href="<%= request.getContextPath() %>/login" style="color:#38bdf8">→ Tới trang Login</a>
<% }
   if (conn != null) { try { conn.close(); } catch(Exception e){} }
%>
</div>
<p style="color:#475569;font-size:0.8rem;margin-top:24px">⚠ Xóa file debug.jsp trước khi deploy production!</p>
</body>
</html>
<!-- ============================================================ -->
<!-- PHẦN MỞ RỘNG: DEBUG GAME_RECORDS & TEST INSERT              -->
<!-- ============================================================ -->
<%
  // Mở lại kết nối cho phần mới
  Connection conn2 = null;
  String connError2 = null;
  try {
    Class.forName("com.mysql.cj.jdbc.Driver");
    conn2 = DriverManager.getConnection(
      "jdbc:mysql://localhost:3306/battleship?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true",
      "root", "");
  } catch (Exception e2) {
    connError2 = e2.getMessage();
  }
%>

<!-- 6. KIỂM TRA CẤU TRÚC BẢNG GAME_RECORDS -->
<h2>6. Cấu trúc bảng game_records</h2>
<div class="box">
<% if (conn2 != null) {
  try {
    DatabaseMetaData meta2 = conn2.getMetaData();
    ResultSet cols2 = meta2.getColumns(null, null, "game_records", null);
    boolean hasTable2 = false;
%>
  <table>
    <tr><th>Cột</th><th>Kiểu</th><th>Nullable</th></tr>
<%  while (cols2.next()) {
      hasTable2 = true;
%>
    <tr>
      <td><b><%= cols2.getString("COLUMN_NAME") %></b></td>
      <td><%= cols2.getString("TYPE_NAME") %></td>
      <td><%= cols2.getString("IS_NULLABLE") %></td>
    </tr>
<%  }
    if (!hasTable2) { %>
      <tr><td colspan="3" class="err">❌ Bảng game_records KHÔNG TỒN TẠI — chạy schema.sql!</td></tr>
<%  } %>
  </table>
<% } catch (Exception e2) { %>
  <p class="err">Lỗi: <%= e2.getMessage() %></p>
<% } } else { %>
  <p class="warn">⚠ DB chưa kết nối</p>
<% } %>
</div>

<!-- 7. DỮ LIỆU GAME_RECORDS -->
<h2>7. Dữ liệu bảng game_records (10 gần nhất)</h2>
<div class="box">
<% if (conn2 != null) {
  try {
    Statement st2 = conn2.createStatement();
    ResultSet rs2 = st2.executeQuery(
      "SELECT gr.id, gr.mode, gr.winner_id, gr.player1_score, gr.total_shots, gr.played_at, " +
      "  u1.username as p1name " +
      "FROM game_records gr " +
      "LEFT JOIN users u1 ON gr.player1_id = u1.id " +
      "ORDER BY gr.played_at DESC LIMIT 10"
    );
    boolean hasRows2 = false;
%>
  <table>
    <tr><th>ID (6 ký tự)</th><th>Mode</th><th>Winner</th><th>Player1</th><th>Score</th><th>Shots</th><th>Played At</th></tr>
<% while (rs2.next()) {
     hasRows2 = true;
     String shortId = rs2.getString("id");
     if (shortId != null && shortId.length() > 8) shortId = shortId.substring(0, 8) + "...";
%>
    <tr>
      <td style="font-size:0.8rem;color:#94a3b8"><%= shortId %></td>
      <td><%= rs2.getString("mode") %></td>
      <td><%= rs2.getString("winner_id") %></td>
      <td><%= rs2.getString("p1name") != null ? rs2.getString("p1name") : "<span style='color:#f87171'>null/not found</span>" %></td>
      <td><%= rs2.getInt("player1_score") %></td>
      <td><%= rs2.getInt("total_shots") %></td>
      <td style="font-size:0.8rem"><%= rs2.getTimestamp("played_at") %></td>
    </tr>
<% }
   if (!hasRows2) { %>
    <tr><td colspan="7" class="warn">⚠ Bảng game_records TRỐNG — chưa có trận đấu nào được lưu</td></tr>
<% } %>
  </table>
<% } catch (Exception e2) { %>
  <p class="err">Lỗi đọc game_records: <%= e2.getMessage() %></p>
  <pre><%= e2.getClass().getName() %>: <%= e2.getMessage() %></pre>
<% } } else { %>
  <p class="warn">⚠ DB chưa kết nối</p>
<% } %>
</div>

<!-- 8. TEST INSERT GAME_RECORD -->
<h2>8. Test Insert game_record trực tiếp</h2>
<div class="box">
<%
  String doTestInsert = request.getParameter("testInsert");
  if ("1".equals(doTestInsert) && conn2 != null) {
    try {
      // Lấy một user bất kỳ để test
      Statement stUser = conn2.createStatement();
      ResultSet rsUser = stUser.executeQuery("SELECT id FROM users LIMIT 1");
      if (!rsUser.next()) {
%>
        <p class="err">❌ Không có user nào trong DB — tạo tài khoản trước!</p>
<%    } else {
        String testUserId = rsUser.getString("id");
        String testId = java.util.UUID.randomUUID().toString();
        PreparedStatement psTest = conn2.prepareStatement(
          "INSERT INTO game_records (id,room_id,player1_id,player2_id,winner_id,mode,player1_score,player2_score,total_shots,duration_seconds,played_at) " +
          "VALUES (?,?,?,?,?,?,?,?,?,?,?)"
        );
        psTest.setString(1, testId);
        psTest.setString(2, "test-room");
        psTest.setString(3, testUserId);
        psTest.setNull(4, java.sql.Types.VARCHAR);   // PvE: player2 = null
        psTest.setString(5, "AI_PLAYER");
        psTest.setString(6, "PvE");
        psTest.setInt(7, 100);
        psTest.setInt(8, 0);
        psTest.setInt(9, 30);
        psTest.setInt(10, 60);
        psTest.setTimestamp(11, new java.sql.Timestamp(System.currentTimeMillis()));
        psTest.executeUpdate();
%>
        <p class="ok">✅ INSERT thành công! Record ID: <%= testId.substring(0,8) %>...</p>
        <p style="color:#94a3b8">→ Bây giờ bấm F5 để thấy record ở mục 7 ở trên</p>
<%    }
    } catch (Exception e2) {
%>
        <p class="err">❌ INSERT THẤT BẠI!</p>
        <pre><%= e2.getClass().getName() %>: <%= e2.getMessage() %></pre>
        <div class="fix">
          ⚠️ Đây là lỗi gốc khiến lịch sử không được lưu.<br>
          Sao chép lỗi trên và báo lại để được hỗ trợ.
        </div>
<%  }
  } else {
%>
  <p style="color:#94a3b8">Bấm nút bên dưới để chạy INSERT thử vào bảng game_records:</p>
  <a href="?testInsert=1" class="btn" style="display:inline-block;margin-top:8px">
    🧪 Test INSERT game_record ngay
  </a>
<% } %>
<% if (conn2 != null) { try { conn2.close(); } catch(Exception e){} } %>
</div>

<style>
.btn { background:#2563eb; color:white; border:none; padding:8px 18px; border-radius:4px; cursor:pointer; text-decoration:none; }
</style>
