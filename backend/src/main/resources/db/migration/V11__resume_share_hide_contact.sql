-- V11: 分享隐私增强 - 隐藏联系方式开关
ALTER TABLE resume_share
  ADD COLUMN `hide_contact` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '分享页是否隐藏联系方式' AFTER `status`;
