-- V10: AI 每日配额计数（按用户 × 功能 × 日期）
CREATE TABLE ai_daily_quota (
  id          VARCHAR(64) NOT NULL PRIMARY KEY,
  user_id     VARCHAR(64) NOT NULL,
  feature_key VARCHAR(64) NOT NULL COMMENT '功能标识，如 resume-writing',
  quota_date  CHAR(8) NOT NULL COMMENT '配额日期 yyyyMMdd',
  used_count  INT NOT NULL DEFAULT 0,
  deleted     TINYINT(1) NOT NULL DEFAULT 0,
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_ai_daily_quota (user_id, feature_key, quota_date, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
