# Cấu hình Database — Battleship

## 1. Tạo Database

Chạy file `schema.sql` trong MySQL:
```sql
source schema.sql;
```

## 2. Cấu hình kết nối

### Cách A: Sửa trực tiếp trong code (DBConnection.java)
Mở `java/dao/DBConnection.java` và sửa URL/USER/PASS:
```java
URL  = "jdbc:mysql://localhost:3306/battleship?useSSL=false&serverTimezone=UTC&...";
USER = "root";
PASS = "your_password";
```

### Cách B: Dùng biến môi trường (khuyến nghị)
Set trước khi khởi động Tomcat:
```bash
export DB_URL=jdbc:mysql://localhost:3306/battleship?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true
export DB_USER=root
export DB_PASS=your_password
```

## 3. Mật khẩu được hash SHA-256

Khi đăng ký, mật khẩu được hash SHA-256 trước khi lưu vào cột `password_hash`.  
Khi đăng nhập, hệ thống hash mật khẩu nhập vào rồi so sánh với DB — không lưu plaintext.

## 4. Lỗi kết nối thường gặp

| Lỗi | Nguyên nhân | Giải pháp |
|-----|------------|-----------|
| MySQL Driver not found | Thiếu JAR | Thêm `mysql-connector-java-8.x.jar` vào `WEB-INF/lib` |
| Access denied for user 'root' | Sai mật khẩu | Sửa DB_PASS |
| Unknown database 'battleship' | Chưa tạo DB | Chạy `schema.sql` |
| Connection refused | MySQL chưa chạy | `sudo systemctl start mysql` |
