package dao;

import model.GameRecord;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * TEST: GameHistoryDAO
 * ============================================================
 * Test các method DAO: insert, findByUserId, findById, findByMode, etc.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class GameHistoryDAOTest {

    private GameHistoryDAO gameHistoryDAO;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        gameHistoryDAO = new GameHistoryDAO();
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: insert() - Lưu trận đấu vào DB
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("insert() - Thêm trận đấu PvE thành công")
    void testInsertPvEGameSuccess() throws SQLException {
        // Arrange
        GameRecord record = new GameRecord();
        record.setId("game-001");
        record.setRoomId("room-001");
        record.setPlayer1Id("player1");
        record.setPlayer2Id(null); // PvE -> player2 = null
        record.setPlayer1Name("Alice");
        record.setPlayer2Name("AI");
        record.setWinnerName("Alice");
        record.setMode("PvE");
        record.setPlayer1Score(100);
        record.setPlayer2Score(50);
        record.setTotalShots(25);
        record.setDurationSeconds(300);

        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            gameHistoryDAO.insert(record);

            // Assert
            verify(mockConnection).prepareStatement(contains("INSERT INTO game_records"));
            verify(mockPreparedStatement).setString(1, "game-001");
            verify(mockPreparedStatement).setString(3, "player1");
            verify(mockPreparedStatement).setNull(4, Types.VARCHAR); // player2_id = null
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    @DisplayName("insert() - Thêm trận PvP thành công")
    void testInsertPvPGameSuccess() throws SQLException {
        // Arrange
        GameRecord record = new GameRecord();
        record.setId("game-002");
        record.setRoomId("room-002");
        record.setPlayer1Id("alice");
        record.setPlayer2Id("bob");
        record.setPlayer1Name("Alice");
        record.setPlayer2Name("Bob");
        record.setWinnerName("Alice");
        record.setMode("PvP");
        record.setPlayer1Score(80);
        record.setPlayer2Score(40);
        record.setTotalShots(35);
        record.setDurationSeconds(450);

        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            gameHistoryDAO.insert(record);

            // Assert
            verify(mockPreparedStatement).setString(4, "bob"); // player2_id
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: findByUserId() - Lấy toàn bộ lịch sử
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId() - Trả về danh sách trận đấu")
    void testFindByUserIdSuccess() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simulate 2 game records
            when(mockResultSet.next())
                    .thenReturn(true)   // record 1
                    .thenReturn(true)   // record 2
                    .thenReturn(false); // no more records

            // Record 1
            when(mockResultSet.getString("id")).thenReturn("game-001");
            when(mockResultSet.getString("room_id")).thenReturn("room-001");
            when(mockResultSet.getString("player1_id")).thenReturn("alice");
            when(mockResultSet.getString("player2_id")).thenReturn(null);
            when(mockResultSet.getString("mode")).thenReturn("PvE");
            when(mockResultSet.getInt("player1_score")).thenReturn(100);
            when(mockResultSet.getInt("player2_score")).thenReturn(50);
            when(mockResultSet.getInt("total_shots")).thenReturn(25);
            when(mockResultSet.getInt("duration_seconds")).thenReturn(300);
            when(mockResultSet.getTimestamp("played_at")).thenReturn(
                    Timestamp.valueOf(LocalDateTime.now())
            );
            when(mockResultSet.getString("player1_name")).thenReturn("Alice");
            when(mockResultSet.getString("player2_name")).thenReturn("AI");
            when(mockResultSet.getString("winner_name")).thenReturn("Alice");
            when(mockResultSet.getString("u1name")).thenReturn("alice_user");
            when(mockResultSet.getString("u2name")).thenReturn(null);

            // Act
            List<GameRecord> results = gameHistoryDAO.findByUserId("alice");

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("game-001", results.get(0).getId());
            assertEquals("PvE", results.get(0).getMode());
            verify(mockPreparedStatement).setString(1, "alice");
        }
    }

    @Test
    @DisplayName("findByUserId() - Người chơi chưa có trận nào")
    void testFindByUserIdNoRecords() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act
            List<GameRecord> results = gameHistoryDAO.findByUserId("newplayer");

            // Assert
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: findById() - Lấy chi tiết một trận
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() - Tìm thấy trận đấu")
    void testFindByIdSuccess() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("id")).thenReturn("game-001");
            when(mockResultSet.getString("player1_id")).thenReturn("alice");
            when(mockResultSet.getString("player2_id")).thenReturn("bob");
            when(mockResultSet.getRow()).thenReturn(Integer.valueOf("PvP"));
            when(mockResultSet.getInt("player1_score")).thenReturn(100);
            when(mockResultSet.getInt("player2_score")).thenReturn(80);
            when(mockResultSet.getInt("total_shots")).thenReturn(50);
            when(mockResultSet.getInt("duration_seconds")).thenReturn(600);
            when(mockResultSet.getTimestamp("played_at")).thenReturn(
                    Timestamp.valueOf(LocalDateTime.now())
            );
            when(mockResultSet.getString("player1_name")).thenReturn("Alice");
            when(mockResultSet.getString("player2_name")).thenReturn("Bob");
            when(mockResultSet.getString("winner_name")).thenReturn("Alice");
            when(mockResultSet.getString("u1name")).thenReturn("alice_user");
            when(mockResultSet.getString("u2name")).thenReturn("bob_user");

            // Act
            GameRecord result = gameHistoryDAO.findById("game-001");

            // Assert
            assertNotNull(result);
            assertEquals("game-001", result.getId());
            assertEquals("alice", result.getPlayer1Id());
            assertEquals("bob", result.getPlayer2Id());
            assertEquals("PvP", result.getMode());
        }
    }

    @Test
    @DisplayName("findById() - Trận không tồn tại")
    void testFindByIdNotFound() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act
            GameRecord result = gameHistoryDAO.findById("nonexistent");

            // Assert
            assertNull(result);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: findByMode() - Lọc theo chế độ (PvE, PvP)
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByMode() - Lọc trận PvE")
    void testFindByModePvE() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);
            setupMockGameRecord("game-pve-001", "PvE");

            // Act
            List<GameRecord> results = gameHistoryDAO.findByMode("alice", "PvE");

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("PvE", results.get(0).getMode());
            verify(mockPreparedStatement).setString(2, "PvE");
        }
    }

    @Test
    @DisplayName("findByMode() - Lọc trận PvP")
    void testFindByModePvP() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);
            setupMockGameRecord("game-pvp-001", "PvP");

            // Act
            List<GameRecord> results = gameHistoryDAO.findByMode("alice", "PvP");

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("PvP", results.get(0).getMode());
            verify(mockPreparedStatement).setString(2, "PvP");
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: findByDateRange() - Lọc theo kỳ hạn
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByDateRange() - Lấy trận 7 ngày gần đây")
    void testFindByDateRangeWeek() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);
            setupMockGameRecord("game-week-001", "PvE");

            // Act
            List<GameRecord> results = gameHistoryDAO.findByDateRange("alice", "week");

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
            verify(mockPreparedStatement).setString(1, "alice");
        }
    }

    @Test
    @DisplayName("findByDateRange() - Lấy trận 30 ngày gần đây")
    void testFindByDateRangeMonth() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);
            setupMockGameRecord("game-month-001", "PvP");

            // Act
            List<GameRecord> results = gameHistoryDAO.findByDateRange("alice", "month");

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: countByUserId() - Đếm tổng trận
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByUserId() - Trả về số lượng trận")
    void testCountByUserId() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("cnt")).thenReturn(42);

            // Act
            int count = gameHistoryDAO.countByUserId("alice");

            // Assert
            assertEquals(42, count);
            verify(mockPreparedStatement).setString(1, "alice");
        }
    }

    @Test
    @DisplayName("countByUserId() - Người chơi mới (0 trận)")
    void testCountByUserIdZero() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("cnt")).thenReturn(0);

            // Act
            int count = gameHistoryDAO.countByUserId("newplayer");

            // Assert
            assertEquals(0, count);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: getPlayerStats() - Thống kê người chơi
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPlayerStats() - Trả về thống kê đầy đủ")
    void testGetPlayerStats() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("total_games")).thenReturn(50);
            when(mockResultSet.getInt("wins")).thenReturn(35);
            when(mockResultSet.getInt("avg_duration")).thenReturn(450);
            when(mockResultSet.getInt("total_shots")).thenReturn(1250);
            when(mockResultSet.getDouble("avg_score")).thenReturn(87.5);

            // Act
            Map<String, Object> stats = gameHistoryDAO.getPlayerStats("alice");

            // Assert
            assertNotNull(stats);
            assertEquals(50, stats.get("totalGames"));
            assertEquals(35, stats.get("wins"));
            assertEquals(450, stats.get("avgDuration"));
            assertEquals(1250, stats.get("totalShots"));
            assertEquals(87.5, stats.get("avgScore"));
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────────

    private void setupMockGameRecord(String id, String mode) throws SQLException {
        when(mockResultSet.getString("id")).thenReturn(id);
        when(mockResultSet.getString("room_id")).thenReturn("room-001");
        when(mockResultSet.getString("player1_id")).thenReturn("alice");
        when(mockResultSet.getString("player2_id")).thenReturn("bob");
        when(mockResultSet.getString("mode")).thenReturn(mode);
        when(mockResultSet.getInt("player1_score")).thenReturn(100);
        when(mockResultSet.getInt("player2_score")).thenReturn(50);
        when(mockResultSet.getInt("total_shots")).thenReturn(30);
        when(mockResultSet.getInt("duration_seconds")).thenReturn(300);
        when(mockResultSet.getTimestamp("played_at")).thenReturn(
                Timestamp.valueOf(LocalDateTime.now())
        );
        when(mockResultSet.getString("player1_name")).thenReturn("Alice");
        when(mockResultSet.getString("player2_name")).thenReturn("Bob");
        when(mockResultSet.getString("winner_name")).thenReturn("Alice");
        when(mockResultSet.getString("u1name")).thenReturn("alice_user");
        when(mockResultSet.getString("u2name")).thenReturn("bob_user");
    }
}