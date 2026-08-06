-- V9: 简历排版与一页适配设置
ALTER TABLE resume
  ADD COLUMN `render_settings` JSON NULL COMMENT '简历渲染设置';

UPDATE resume
SET `render_settings` = '{"autoOnePage":false}'
WHERE `render_settings` IS NULL;
