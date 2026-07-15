-- V2: AI 模块表
-- AI 调用日志表
CREATE TABLE IF NOT EXISTS ai_call_log (
    id                VARCHAR(24)  NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
    user_id           VARCHAR(24)  NOT NULL COMMENT '用户ID',
    feature_key       VARCHAR(64)  NOT NULL COMMENT '功能标识: resume-review / resume-optimize / avatar-optimize',
    provider_name     VARCHAR(64)  NOT NULL COMMENT '厂商: openai / qwen / ernie',
    model_name        VARCHAR(128) NOT NULL COMMENT '模型名',
    request_hash      VARCHAR(128) NOT NULL COMMENT '请求内容去重哈希',
    prompt_tokens     INT          NOT NULL DEFAULT 0 COMMENT '输入token数',
    completion_tokens INT          NOT NULL DEFAULT 0 COMMENT '输出token数',
    total_tokens      INT          NOT NULL DEFAULT 0 COMMENT '总token数',
    latency_ms        BIGINT       NOT NULL DEFAULT 0 COMMENT '调用耗时(ms)',
    success           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否成功',
    error_msg         VARCHAR(1024)         DEFAULT NULL COMMENT '失败信息',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ai_call_log_user (user_id),
    INDEX idx_ai_call_log_feature (feature_key),
    INDEX idx_ai_call_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用日志';

-- JD 匹配优化任务表
CREATE TABLE IF NOT EXISTS resume_optimize_task (
    id                VARCHAR(24)   NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
    resume_id         VARCHAR(24)   NOT NULL COMMENT '简历ID',
    user_id           VARCHAR(24)   NOT NULL COMMENT '用户ID',
    job_description   TEXT          NOT NULL COMMENT '目标岗位JD',
    match_score       INT           DEFAULT NULL COMMENT 'JD匹配评分 0-100',
    dimension_scores  JSON          DEFAULT NULL COMMENT '分项评分 JSON',
    optimizations     JSON          DEFAULT NULL COMMENT '逐模块优化建议 JSON',
    missing_skills    JSON          DEFAULT NULL COMMENT '缺失技能 JSON',
    recommendations   JSON          DEFAULT NULL COMMENT '整体建议 JSON',
    model_name        VARCHAR(128)  DEFAULT NULL COMMENT 'AI模型名',
    status            VARCHAR(32)   NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/success/failed',
    error_msg         VARCHAR(1024) DEFAULT NULL COMMENT '失败信息',
    completed_at      DATETIME      DEFAULT NULL COMMENT '完成时间',
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_optimize_user (user_id),
    INDEX idx_optimize_resume (resume_id),
    INDEX idx_optimize_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JD匹配优化任务';
