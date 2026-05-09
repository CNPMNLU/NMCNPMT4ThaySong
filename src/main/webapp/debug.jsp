<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
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
