-- V14: refresh token 家族 ID（同一登录会话的令牌链共享），检测到复用攻击时整家族撤销
ALTER TABLE refresh_token
  ADD COLUMN `family_id` VARCHAR(64) NULL COMMENT '令牌家族ID（同一次登录会话的令牌共享）' AFTER `user_id`;

CREATE INDEX `idx_refresh_token_family` ON refresh_token (`family_id`);
