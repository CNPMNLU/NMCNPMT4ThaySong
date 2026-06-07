-- Battleship Database Schema
-- Run this once to create all tables

CREATE DATABASE IF NOT EXISTS battleship CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE battleship;

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    email         VARCHAR(100),
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    last_login    DATETIME
);

CREATE TABLE IF NOT EXISTS leaderboard (
    id            VARCHAR(36)    PRIMARY KEY,
    user_id       VARCHAR(36)    NOT NULL UNIQUE,
    total_games   INT            NOT NULL DEFAULT 0,
    total_wins    INT            NOT NULL DEFAULT 0,
    total_losses  INT            NOT NULL DEFAULT 0,
    win_rate      DECIMAL(5,2)   NOT NULL DEFAULT 0,
    best_score    INT            NOT NULL DEFAULT 0,
    total_score   INT            NOT NULL DEFAULT 0,
    updated_at    DATETIME       NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- game_records: lưu lịch sử mỗi trận (PvE và PvP offline đều dùng)
-- player1_id  : UUID của user đang đăng nhập (FK bắt buộc)
-- player2_id  : NULL hoặc UUID user thứ 2 nếu cả 2 cùng có tài khoản (không bắt buộc)
-- player1_name: tên hiển thị của player 1 (lấy từ username)
-- player2_name: tên hiển thị của player 2 (nhập tay khi PvP offline, hoặc "AI" khi PvE)
-- winner_name : tên hiển thị của người thắng (không cần FK)
CREATE TABLE IF NOT EXISTS game_records (
    id                VARCHAR(36)  PRIMARY KEY,
    room_id           VARCHAR(36),
    player1_id        VARCHAR(36)  NOT NULL,
    player2_id        VARCHAR(36),                -- NULL khi PvE hoặc PvP offline 1 tài khoản
    player1_name      VARCHAR(100) NOT NULL DEFAULT '',
    player2_name      VARCHAR(100) NOT NULL DEFAULT 'AI',
    winner_name       VARCHAR(100) NOT NULL DEFAULT '',
    mode              VARCHAR(10)  NOT NULL DEFAULT 'PvE',
    player1_score     INT          NOT NULL DEFAULT 0,
    player2_score     INT          NOT NULL DEFAULT 0,
    total_shots       INT          NOT NULL DEFAULT 0,
    duration_seconds  INT          NOT NULL DEFAULT 0,
    played_at         DATETIME     NOT NULL DEFAULT NOW(),
    FOREIGN KEY (player1_id) REFERENCES users(id)
    -- player2_id và winner_name KHÔNG có FK vì có thể là tên offline
);
