-- V7: 审计日志表（敏感操作留痕，配合 AuditLogService 写入）
CREATE TABLE IF NOT EXISTS audit_log (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID（未登录为NULL）',
  `action` VARCHAR(64) NOT NULL COMMENT '操作动作：login/register/logout/change_password/delete_resume/duplicate_resume/pdf_export/admin_update_role/admin_update_status/admin_reset_password/admin_delete_user/guest_create',
  `target_id` VARCHAR(64) DEFAULT NULL COMMENT '操作对象ID（简历ID/用户ID等）',
  `detail` VARCHAR(512) DEFAULT NULL COMMENT '补充描述',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  KEY `idx_audit_user_created` (`user_id`, `created_at`),
  KEY `idx_audit_action_created` (`action`, `created_at`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';
