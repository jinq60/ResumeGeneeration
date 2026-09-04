-- 补齐简历列表查询索引：user_id+deleted+last_edited_at 覆盖前台列表过滤+排序
CREATE INDEX idx_resume_user_deleted_edited ON resume(user_id, deleted, last_edited_at DESC);
-- 后台/统计常用 deleted+created_at
CREATE INDEX idx_resume_deleted_created ON resume(deleted, created_at DESC);
-- 任务 sweep 复合索引
CREATE INDEX idx_optimize_user_status_updated ON resume_optimize_task(user_id, status, updated_at);
CREATE INDEX idx_optimize_status_updated ON resume_optimize_task(status, updated_at);
CREATE INDEX idx_pdf_status_updated ON pdf_task(status, updated_at);
CREATE INDEX idx_avatar_status_updated ON avatar_task(status, updated_at);
