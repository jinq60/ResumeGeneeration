-- V3: 幂等性记录表
CREATE TABLE IF NOT EXISTS idempotency_record (
    idempotency_key        VARCHAR(64)   NOT NULL PRIMARY KEY COMMENT '客户端生成的幂等键',
    user_id                VARCHAR(24)   DEFAULT NULL COMMENT '用户ID（未认证请求为NULL）',
    http_method            VARCHAR(8)    NOT NULL COMMENT 'HTTP方法',
    request_path           VARCHAR(256)  NOT NULL COMMENT '请求路径',
    response_status        INT           NOT NULL COMMENT '首次响应状态码',
    response_body          MEDIUMTEXT    NOT NULL COMMENT '首次响应体',
    response_content_type  VARCHAR(128)  NOT NULL DEFAULT 'application/json' COMMENT '响应Content-Type',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at             DATETIME      NOT NULL COMMENT '过期时间（24h后）',
    INDEX idx_idempotency_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等性记录';
