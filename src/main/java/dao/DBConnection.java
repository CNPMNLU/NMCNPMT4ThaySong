package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Ưu tiên biến môi trường, fallback về giá trị mặc định
    private static final String URL;
    private static final String USER;
    private static final String PASS;

    static {
        String envUrl  = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASS");

        URL  = (envUrl  != null && !envUrl.isEmpty())  ? envUrl  : "jdbc:mysql://localhost:3306/battleship?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true";
        USER = (envUser != null && !envUser.isEmpty()) ? envUser : "root";
        PASS = (envPass != null)                       ? envPass : "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found. Hãy thêm mysql-connector-java vào classpath.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            // Ném ra lỗi rõ ràng hơn để dễ debug
            throw new SQLException(
                "Không thể kết nối DB. URL=" + URL + " USER=" + USER +
                " | Lỗi gốc: " + e.getMessage(), e.getSQLState(), e.getErrorCode(), e
            );
        }
    }
}
