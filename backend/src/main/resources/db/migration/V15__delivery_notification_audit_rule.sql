-- V15: 投递管理 / 通知中心 / 内容审核 / AI 规则 / 用户偏好
CREATE TABLE IF NOT EXISTS delivery_record (
  `id`                VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id`           VARCHAR(64)  NOT NULL COMMENT '用户ID',
  `resume_id`         VARCHAR(64)  NOT NULL COMMENT '关联简历ID',
  `company`           VARCHAR(128) NOT NULL COMMENT '公司名称',
  `position`          VARCHAR(128) NOT NULL COMMENT '职位名称',
  `channel`           VARCHAR(32)  DEFAULT NULL COMMENT '投递渠道',
  `status`            VARCHAR(32)  NOT NULL DEFAULT 'delivered' COMMENT '进度状态',
  `apply_date`        DATE         DEFAULT NULL COMMENT '投递日期',
  `jd_content`        TEXT         DEFAULT NULL COMMENT 'JD 内容',
  `note`              VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `interview_time`    DATETIME(3)  DEFAULT NULL COMMENT '面试时间',
  `interview_location` VARCHAR(255) DEFAULT NULL COMMENT '面试地点',
  `deleted`           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_delivery_user` (`user_id`, `deleted`),
  KEY `idx_delivery_status` (`status`),
  KEY `idx_delivery_apply_date` (`apply_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递记录';

CREATE TABLE IF NOT EXISTS notification (
  `id`         VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id`    VARCHAR(64)  NOT NULL COMMENT '接收用户ID',
  `type`       VARCHAR(32)  NOT NULL COMMENT 'pdf/avatar/ai/system',
  `title`      VARCHAR(255) NOT NULL COMMENT '标题',
  `content`    VARCHAR(1024) NOT NULL COMMENT '内容',
  `read_flag`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
  `deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_notification_user` (`user_id`, `deleted`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知';

CREATE TABLE IF NOT EXISTS content_audit (
  `id`           VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `target_type`  VARCHAR(32)  NOT NULL DEFAULT 'resume' COMMENT '审核对象类型',
  `target_id`    VARCHAR(64)  NOT NULL COMMENT '审核对象ID',
  `target_title` VARCHAR(255) DEFAULT NULL COMMENT '对象标题',
  `user_id`      VARCHAR(64)  NOT NULL COMMENT '内容所有者ID',
  `risk_level`   VARCHAR(16)  NOT NULL DEFAULT 'low' COMMENT 'low/medium/high',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/warning',
  `reviewer_id`  VARCHAR(64)  DEFAULT NULL COMMENT '审核人ID',
  `review_note`  VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  `reviewed_at`  DATETIME(3)  DEFAULT NULL COMMENT '审核时间',
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_audit_status` (`status`, `deleted`),
  KEY `idx_audit_target` (`target_id`),
  KEY `idx_audit_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核记录';

CREATE TABLE IF NOT EXISTS ai_rule (
  `id`            VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `family_id`     VARCHAR(64)  NOT NULL COMMENT '版本族 ID，首个版本为自身 ID',
  `name`          VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_type`     VARCHAR(32)  NOT NULL COMMENT 'score/prompt/security/dict/match/risk',
  `description`   VARCHAR(512) DEFAULT NULL COMMENT '规则说明',
  `system_prompt` TEXT         DEFAULT NULL COMMENT '系统提示词',
  `user_prompt`   TEXT         DEFAULT NULL COMMENT '用户提示词模板',
  `params`        JSON         DEFAULT NULL COMMENT '参数配置',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/active/disabled',
  `version`       INT          NOT NULL DEFAULT 1 COMMENT '版本号',
  `published_at`  DATETIME(3)  DEFAULT NULL COMMENT '发布时间',
  `created_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人ID',
  `deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_ai_rule_status` (`status`, `deleted`),
  KEY `idx_ai_rule_family` (`family_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 规则';

CREATE TABLE IF NOT EXISTS user_preference (
  `user_id`     VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '用户ID',
  `preferences` JSON        NOT NULL COMMENT '偏好配置 JSON',
  `updated_at`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置';