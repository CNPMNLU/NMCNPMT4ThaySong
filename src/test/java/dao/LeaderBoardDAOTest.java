package dao;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * TEST: LeaderboardDAO
 * ============================================================
 * Test các method DAO: upsert, getTopPlayers, updateEloRating, etc.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class LeaderboardDAOTest {

    private LeaderboardDAO leaderboardDAO;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        leaderboardDAO = new LeaderboardDAO();
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: upsert() - Thêm hoặc cập nhật xếp hạng
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("upsert() - Cập nhật xếp hạng khi người chơi đã tồn tại (thắng)")
    void testUpsertUpdateWin() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true); // Player exists
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.upsert("alice", true, 95);

            // Assert
            verify(mockConnection, atLeast(2)).prepareStatement(anyString());
            verify(mockPreparedStatement, atLeast(2)).executeUpdate();
        }
    }

    @Test
    @DisplayName("upsert() - Cập nhật xếp hạng khi người chơi đã tồn tại (thua)")
    void testUpsertUpdateLoss() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.upsert("bob", false, 50);

            // Assert
            verify(mockPreparedStatement, times(2)).executeUpdate();
        }
    }

    @Test
    @DisplayName("upsert() - Thêm người chơi mới (INSERT)")
    void testUpsertInsertNewPlayer() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false); // Player doesn't exist
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.upsert("newplayer", true, 100);

            // Assert
            verify(mockPreparedStatement, atLeast(1)).executeUpdate();
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: getTopPlayers() - Lấy top 20 người chơi
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTopPlayers() - Trả về danh sách top players")
    void testGetTopPlayersSuccess() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            // Simulate 3 top players
            when(mockResultSet.next())
                    .thenReturn(true)   // Player 1
                    .thenReturn(true)   // Player 2
                    .thenReturn(true)   // Player 3
                    .thenReturn(false); // End

            // Mock player data
            setupMockTopPlayer(1, "Alice", 100, 20, 120, 83.33, 95, 950);
            when(mockResultSet.next()).thenReturn(true);
            setupMockTopPlayer(2, "Bob", 85, 30, 115, 73.91, 90, 850);
            when(mockResultSet.next()).thenReturn(true);
            setupMockTopPlayer(3, "Charlie", 70, 40, 110, 63.63, 85, 750);
            when(mockResultSet.next()).thenReturn(false);

            // Act
            List<Map<String, Object>> results = leaderboardDAO.getTopPlayers(20);

            // Assert
            assertNotNull(results);
            assertEquals(3, results.size());

            // Check first player
            assertEquals(1, results.get(0).get("rank"));
            assertEquals("Alice", results.get(0).get("username"));
            assertEquals(100, results.get(0).get("total_wins"));
            assertEquals(83.33, results.get(0).get("win_rate"));

            // Verify SQL parameter
            verify(mockPreparedStatement).setInt(1, 20);
        }
    }

    @Test
    @DisplayName("getTopPlayers() - Không có dữ liệu")
    void testGetTopPlayersEmpty() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act
            List<Map<String, Object>> results = leaderboardDAO.getTopPlayers(20);

            // Assert
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: getTopPlayersByElo() - Top players theo ELO rating
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTopPlayersByElo() - Trả về danh sách theo ELO")
    void testGetTopPlayersByEloSuccess() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);

            // First player
            when(mockResultSet.getString("username")).thenReturn("Alice");
            when(mockResultSet.getInt("total_wins")).thenReturn(100);
            when(mockResultSet.getInt("total_losses")).thenReturn(20);
            when(mockResultSet.getInt("total_games")).thenReturn(120);
            when(mockResultSet.getDouble("win_rate")).thenReturn(83.33);
            when(mockResultSet.getInt("elo_rating")).thenReturn(1800);
            when(mockResultSet.getInt("level")).thenReturn(5);
            when(mockResultSet.getString("user_id")).thenReturn("alice-id");

            // Act
            List<Map<String, Object>> results = leaderboardDAO.getTopPlayersByElo(20);

            // Assert
            assertNotNull(results);
            assertTrue(results.size() > 0);
            assertEquals(1, results.get(0).get("rank"));
            assertEquals("Alice", results.get(0).get("username"));
            assertEquals(1800, results.get(0).get("eloRating"));
            assertEquals(5, results.get(0).get("level"));
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: getPlayerRank() - Lấy xếp hạng của một người chơi
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPlayerRank() - Trả về thông tin xếp hạng")
    void testGetPlayerRankSuccess() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("rank")).thenReturn(5);
            when(mockResultSet.getString("username")).thenReturn("Alice");
            when(mockResultSet.getInt("total_wins")).thenReturn(100);
            when(mockResultSet.getInt("total_losses")).thenReturn(20);
            when(mockResultSet.getInt("total_games")).thenReturn(120);
            when(mockResultSet.getDouble("win_rate")).thenReturn(83.33);
            when(mockResultSet.getInt("elo_rating")).thenReturn(1750);
            when(mockResultSet.getInt("level")).thenReturn(5);

            // Act
            Map<String, Object> result = leaderboardDAO.getPlayerRank("alice-id");

            // Assert
            assertNotNull(result);
            assertEquals(5, result.get("rank"));
            assertEquals("Alice", result.get("username"));
            assertEquals(100, result.get("totalWins"));
            assertEquals(1750, result.get("eloRating"));
        }
    }

    @Test
    @DisplayName("getPlayerRank() - Người chơi không tồn tại")
    void testGetPlayerRankNotFound() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act
            Map<String, Object> result = leaderboardDAO.getPlayerRank("nonexistent-id");

            // Assert
            assertNull(result);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: updateEloRating() - Cập nhật ELO sau trận đấu
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEloRating() - Thắng: +32 ELO")
    void testUpdateEloRatingWin() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.updateEloRating("alice", true);

            // Assert
            verify(mockPreparedStatement).setInt(1, 32); // Win = +32
            verify(mockPreparedStatement).setString(2, "alice");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    @DisplayName("updateEloRating() - Thua: -16 ELO")
    void testUpdateEloRatingLoss() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.updateEloRating("bob", false);

            // Assert
            verify(mockPreparedStatement).setInt(1, -16); // Loss = -16
            verify(mockPreparedStatement).setString(2, "bob");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: updateStreak() - Cập nhật winning streak
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStreak() - Thắng: tăng streak")
    void testUpdateStreakWin() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.updateStreak("alice", true);

            // Assert
            verify(mockConnection, times(1)).prepareStatement(contains("UPDATE leaderboard SET current_streak"));
            verify(mockPreparedStatement).setString(1, "alice");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    @Test
    @DisplayName("updateStreak() - Thua: reset streak về 0")
    void testUpdateStreakLoss() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            leaderboardDAO.updateStreak("bob", false);

            // Assert
            verify(mockConnection, times(1)).prepareStatement(contains("UPDATE leaderboard SET current_streak = 0"));
            verify(mockPreparedStatement).setString(1, "bob");
            verify(mockPreparedStatement).executeUpdate();
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // TEST: getPlayerTrend() - Theo dõi xu hướng xếp hạng
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPlayerTrend() - Xu hướng 'up' (xếp hạng cao)")
    void testGetPlayerTrendUp() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("current_rank")).thenReturn(50); // < 100

            // Act
            String trend = leaderboardDAO.getPlayerTrend("alice");

            // Assert
            assertEquals("up", trend);
        }
    }

    @Test
    @DisplayName("getPlayerTrend() - Xu hướng 'down' (xếp hạng thấp)")
    void testGetPlayerTrendDown() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("current_rank")).thenReturn(150); // > 100

            // Act
            String trend = leaderboardDAO.getPlayerTrend("bob");

            // Assert
            assertEquals("down", trend);
        }
    }

    @Test
    @DisplayName("getPlayerTrend() - Xu hướng 'stable' (xếp hạng trung bình)")
    void testGetPlayerTrendStable() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("current_rank")).thenReturn(100); // == 100

            // Act
            String trend = leaderboardDAO.getPlayerTrend("charlie");

            // Assert
            assertEquals("stable", trend);
        }
    }

    @Test
    @DisplayName("getPlayerTrend() - Không tìm thấy người chơi")
    void testGetPlayerTrendNotFound() throws SQLException {
        try (MockedStatic<DBConnection> mockDB = mockStatic(DBConnection.class)) {
            mockDB.when(DBConnection::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act
            String trend = leaderboardDAO.getPlayerTrend("nonexistent");

            // Assert
            assertEquals("stable", trend); // Default to stable
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────────

    private void setupMockTopPlayer(int rank, String username,
                                    int wins, int losses, int totalGames,
                                    double winRate, int bestScore, int totalScore)
            throws SQLException {
        when(mockResultSet.getString("username")).thenReturn(username);
        when(mockResultSet.getInt("total_wins")).thenReturn(wins);
        when(mockResultSet.getInt("total_losses")).thenReturn(losses);
        when(mockResultSet.getInt("total_games")).thenReturn(totalGames);
        when(mockResultSet.getDouble("win_rate")).thenReturn(winRate);
        when(mockResultSet.getInt("best_score")).thenReturn(bestScore);
        when(mockResultSet.getInt("total_score")).thenReturn(totalScore);
    }
}