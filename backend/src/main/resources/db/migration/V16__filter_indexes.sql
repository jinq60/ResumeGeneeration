-- V16: 过滤索引补丁
-- 为 v2.3 新表的列表/筛选接口补索引，避免 keyword/company/position/riskLevel/rule_type 触发全表扫描。
-- 与 docs/superpowers/specs/2026-07-03-data-model-and-ddl.md §6 保持一致。

-- delivery_record: GET /deliveries 支持 keyword/company/position/apply_date 筛选
CREATE INDEX idx_delivery_user_company ON delivery_record(user_id, deleted, company);
CREATE INDEX idx_delivery_user_position ON delivery_record(user_id, deleted, position);
CREATE INDEX idx_delivery_user_apply_date ON delivery_record(user_id, deleted, apply_date);

-- content_audit: GET /admin/audits 支持 keyword/status/riskLevel 筛选
CREATE INDEX idx_audit_target_title ON content_audit(target_type, deleted, target_title);
CREATE INDEX idx_audit_risk_level ON content_audit(deleted, risk_level);

-- ai_rule: GET /admin/ai-rules 支持 ruleType 筛选
CREATE INDEX idx_ai_rule_type ON ai_rule(deleted, rule_type);

-- idempotency_record: 用户级清理与查询（按 user_id 路径）
CREATE INDEX idx_idempotency_user ON idempotency_record(user_id);
