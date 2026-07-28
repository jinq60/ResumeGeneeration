-- V5: 统一字段长度，让 ai_call_log / resume_optimize_task / idempotency_record 三张表的
-- ID 与 user_id 字段长度对齐 V1 的 VARCHAR(64)。
-- DATETIME 精度的差异对业务无影响，留给后续专门迁移处理，避免 H2 测试环境不兼容。

-- ai_call_log
ALTER TABLE `ai_call_log` MODIFY COLUMN `id` VARCHAR(64) NOT NULL;
ALTER TABLE `ai_call_log` MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL;

-- resume_optimize_task
ALTER TABLE `resume_optimize_task` MODIFY COLUMN `id` VARCHAR(64) NOT NULL;
ALTER TABLE `resume_optimize_task` MODIFY COLUMN `resume_id` VARCHAR(64) NOT NULL;
ALTER TABLE `resume_optimize_task` MODIFY COLUMN `user_id` VARCHAR(64) NOT NULL;

-- idempotency_record
ALTER TABLE `idempotency_record` MODIFY COLUMN `user_id` VARCHAR(64) DEFAULT NULL;