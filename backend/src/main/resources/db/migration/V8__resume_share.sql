-- V8: 简历公开分享
CREATE TABLE IF NOT EXISTS resume_share (
  `id`         VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `resume_id`  VARCHAR(64) NOT NULL COMMENT '简历ID',
  `user_id`    VARCHAR(64) NOT NULL COMMENT '分享者ID',
  `token`      VARCHAR(64) NOT NULL COMMENT '随机 token（32 字节 Base64URL），唯一',
  `status`     VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/revoked',
  `expires_at` DATETIME(3) DEFAULT NULL COMMENT '过期时间（NULL=永久）',
  `revoked_at` DATETIME(3) DEFAULT NULL COMMENT '关闭时间',
  `deleted`    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY `uk_resume_share_token` (`token`),
  KEY `idx_resume_share_user` (`user_id`),
  KEY `idx_resume_share_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历公开分享';
