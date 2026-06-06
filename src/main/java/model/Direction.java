package model;

/**
 * Enum thay thế String "H" / "V" trong Ship.direction.
 * Lý do dùng enum (COMMIT-06):
 *   - Loại bỏ rủi ro typo ("h", "Horizontal", null) dẫn đến checkSunk() tính sai
 *   - Compiler kiểm tra tại compile-time thay vì fail silently lúc runtime
 *   - switch/case exhaustive — IDE cảnh báo khi thiếu nhánh
 * Tương thích ngược:
 *   - fromString() dùng khi đọc từ DB hoặc JSON vẫn dùng "H"/"V"
 *   - toCode() dùng khi ghi xuống DB hoặc serialize JSON
 */
public enum Direction {

    H("H"),   // Horizontal — tàu trải dọc theo trục x (startX tăng)
    V("V");   // Vertical   — tàu trải dọc theo trục y (startY tăng)

    private final String code;

    Direction(String code) {
        this.code = code;
    }

    /** Trả về "H" hoặc "V" — dùng khi ghi DB / serialize JSON. */
    public String toCode() {
        return code;
    }

    /**
     * Parse từ String "H" / "V" (không phân biệt hoa thường).
     * Trả về H nếu input không hợp lệ hoặc null để tránh NullPointerException
     * trong các chỗ dùng cũ chưa kịp migrate.
     */
    public static Direction fromString(String s) {
        if (s == null) return H;
        if (s.equalsIgnoreCase("V")) {
            return V;
        }
        return H;
    }
}