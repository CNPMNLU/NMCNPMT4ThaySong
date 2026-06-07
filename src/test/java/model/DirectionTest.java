package model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test COMMIT-06: Direction enum — parsing, toCode, Ship integration.
 *
 * Commit: test(model): verify Direction enum parsing, toCode, and Ship integration
 *
 * Căn cứ code thực tế Direction.java:
 *   - fromString(null) → H
 *   - fromString("V") / fromString("v") → V
 *   - default case → H
 *   - toCode() → "H" / "V"
 */
@DisplayName("COMMIT-06 — Direction enum")
class DirectionTest {

    @Nested @DisplayName("1. fromString() — parsing")
    class FromString {

        @Test @DisplayName("'H' → Direction.H")
        void uppercase_H() { assertEquals(Direction.H, Direction.fromString("H")); }

        @Test @DisplayName("'V' → Direction.V")
        void uppercase_V() { assertEquals(Direction.V, Direction.fromString("V")); }

        @Test @DisplayName("'h' lowercase → Direction.H (equalsIgnoreCase)")
        void lowercase_h() { assertEquals(Direction.H, Direction.fromString("h")); }

        @Test @DisplayName("'v' lowercase → Direction.V (equalsIgnoreCase)")
        void lowercase_v() { assertEquals(Direction.V, Direction.fromString("v")); }

        @Test @DisplayName("null → fallback Direction.H, không throw")
        void null_fallbackH() {
            assertDoesNotThrow(() -> assertEquals(Direction.H, Direction.fromString(null)));
        }

        @Test @DisplayName("empty string → fallback Direction.H")
        void empty_fallbackH() { assertEquals(Direction.H, Direction.fromString("")); }

        @Test @DisplayName("typo 'Horizontal' → fallback Direction.H")
        void typo_fallbackH() { assertEquals(Direction.H, Direction.fromString("Horizontal")); }

        @Test @DisplayName("typo 'vertical' → Direction.V (equalsIgnoreCase)")
        void typo_vertical() { assertEquals(Direction.V, Direction.fromString("Vertical")); }
    }

    @Nested @DisplayName("2. toCode()")
    class ToCode {

        @Test @DisplayName("Direction.H.toCode() == 'H'")
        void h_toCode() { assertEquals("H", Direction.H.toCode()); }

        @Test @DisplayName("Direction.V.toCode() == 'V'")
        void v_toCode() { assertEquals("V", Direction.V.toCode()); }

        @Test @DisplayName("Roundtrip: fromString(toCode()) trả về enum gốc")
        void roundtrip_H() { assertEquals(Direction.H, Direction.fromString(Direction.H.toCode())); }

        @Test @DisplayName("Roundtrip: fromString(toCode()) cho V")
        void roundtrip_V() { assertEquals(Direction.V, Direction.fromString(Direction.V.toCode())); }
    }

    @Nested @DisplayName("3. Ship tích hợp với Direction")
    class ShipIntegration {

        @Test @DisplayName("setDirectionFromString('V') → getDirection() == V")
        void setFromString_V() {
            Ship s = new Ship();
            s.setDirectionFromString("V");
            assertEquals(Direction.V, s.getDirection());
        }

        @Test @DisplayName("setDirectionFromString('H') → getDirection() == H")
        void setFromString_H() {
            Ship s = new Ship();
            s.setDirectionFromString("H");
            assertEquals(Direction.H, s.getDirection());
        }

        @Test @DisplayName("setDirectionFromString(null) → fallback H, không throw")
        void setFromString_null() {
            Ship s = new Ship();
            assertDoesNotThrow(() -> s.setDirectionFromString(null));
            assertEquals(Direction.H, s.getDirection());
        }

        @Test @DisplayName("Ship chưa setDirection → getDirection() fallback H, không null")
        void defaultDirection_notNull() {
            Ship s = new Ship();
            assertNotNull(s.getDirection());
            assertEquals(Direction.H, s.getDirection());
        }

        @Test @DisplayName("setDirection(Direction.V) → getDirectionCode() == 'V'")
        void setEnum_getCode() {
            Ship s = new Ship();
            s.setDirection(Direction.V);
            assertEquals("V", s.getDirectionCode());
        }

        @Test @DisplayName("getDirectionCode() trả 'H'/'V' đúng cho cả 2 giá trị")
        void getDirectionCode_bothValues() {
            Ship h = new Ship(); h.setDirection(Direction.H);
            Ship v = new Ship(); v.setDirection(Direction.V);
            assertEquals("H", h.getDirectionCode());
            assertEquals("V", v.getDirectionCode());
        }
    }
}