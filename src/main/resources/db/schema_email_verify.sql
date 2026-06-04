USE battleship;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified  TINYINT(1)   NOT NULL DEFAULT 0 AFTER email,
    ADD COLUMN IF NOT EXISTS verify_token    VARCHAR(64)  NULL AFTER email_verified,
    ADD COLUMN IF NOT EXISTS verify_sent_at  DATETIME     NULL AFTER verify_token;

ALTER TABLE users
    MODIFY COLUMN email VARCHAR(100) NOT NULL;
