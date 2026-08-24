-- V13: 简历乐观锁版本号（防止自动保存并发丢失更新）
ALTER TABLE resume
  ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
