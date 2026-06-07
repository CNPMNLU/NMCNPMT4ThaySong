USE battleship;

-- 1. Người dùng mẫu
INSERT INTO users (id, username, password_hash, email, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'admin',   SHA2('admin123',   256), 'admin@battleship.com',   NOW()),
('22222222-2222-2222-2222-222222222222', 'player1', SHA2('password123',256), 'player1@battleship.com', NOW()),
('33333333-3333-3333-3333-333333333333', 'player2', SHA2('123456',      256), 'player2@battleship.com', NOW())
ON DUPLICATE KEY UPDATE username = username;  -- bỏ qua nếu đã tồn tại

-- 2. Bảng xếp hạng mẫu
INSERT INTO leaderboard (id, user_id, total_wins, total_losses, total_games, win_rate, best_score, total_score, updated_at) VALUES
(UUID(), '11111111-1111-1111-1111-111111111111', 15, 5, 20, 75.00, 2500, 18500, NOW()),
(UUID(), '22222222-2222-2222-2222-222222222222',  8,12, 20, 40.00, 1200,  9500, NOW()),
(UUID(), '33333333-3333-3333-3333-333333333333',  2, 3,  5, 40.00,  800,  1600, NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 3. Trận mẫu PvE (player1 thắng AI)
INSERT INTO game_records
    (id, room_id, player1_id, player2_id, player1_name, player2_name, winner_name,
     mode, player1_score, player2_score, total_shots, duration_seconds, played_at)
VALUES
    (UUID(), UUID(),
     '22222222-2222-2222-2222-222222222222', NULL,
     'player1', 'AI', 'player1',
     'PvE', 1500, 0, 42, 180, NOW());

-- 4. Trận mẫu PvP offline (player1 vs nhập tên 'Minh')
INSERT INTO game_records
    (id, room_id, player1_id, player2_id, player1_name, player2_name, winner_name,
     mode, player1_score, player2_score, total_shots, duration_seconds, played_at)
VALUES
    (UUID(), UUID(),
     '22222222-2222-2222-2222-222222222222', NULL,
     'player1', 'Minh', 'Minh',
     'PvP', 800, 1200, 65, 320, NOW());
