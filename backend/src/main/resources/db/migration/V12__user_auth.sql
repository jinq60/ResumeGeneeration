-- V12: 多方式认证绑定表（第三方登录 / 验证码登录凭据）
CREATE TABLE user_auth (
  id          VARCHAR(64) NOT NULL PRIMARY KEY,
  user_id     VARCHAR(64) NOT NULL,
  provider    VARCHAR(32) NOT NULL COMMENT 'password/email_code/google/github/qq/sms_code',
  account     VARCHAR(128) NOT NULL COMMENT '第三方唯一标识（openid/sub/邮箱）',
  credential  VARCHAR(255) DEFAULT NULL COMMENT '附加凭据（预留）',
  deleted     TINYINT(1) NOT NULL DEFAULT 0,
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user_auth (provider, account, deleted),
  KEY idx_user_auth_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
