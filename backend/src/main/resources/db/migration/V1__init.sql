-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(256) DEFAULT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '用户头像',
  `is_guest` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否游客：1是0否',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active/disabled/deleted',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_is_guest` (`is_guest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 简历表
CREATE TABLE IF NOT EXISTS `resume` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `title` VARCHAR(128) NOT NULL COMMENT '简历名称',
  `scene` VARCHAR(32) NOT NULL COMMENT '使用场景枚举',
  `target_position` VARCHAR(128) DEFAULT NULL COMMENT '目标岗位',
  `target_industry` VARCHAR(128) DEFAULT NULL COMMENT '目标行业',
  `template_id` VARCHAR(64) NOT NULL COMMENT '当前模板ID',
  `sections` JSON NOT NULL COMMENT 'Section数组',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active/deleted',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `export_count` INT NOT NULL DEFAULT 0 COMMENT '导出次数',
  `last_edited_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近编辑时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_resume_user_status_edited` (`user_id`, `status`, `last_edited_at` DESC),
  KEY `idx_resume_template_status` (`template_id`, `status`),
  KEY `idx_resume_scene_status` (`scene`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';

-- 头像任务表
CREATE TABLE IF NOT EXISTS `avatar_task` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `resume_id` VARCHAR(64) DEFAULT NULL COMMENT '关联简历ID',
  `source_image_url` VARCHAR(512) NOT NULL COMMENT '原图地址',
  `result_image_url` VARCHAR(512) DEFAULT NULL COMMENT '优化结果图地址',
  `background_type` VARCHAR(16) NOT NULL COMMENT '背景类型：white/blue/red（P0），transparent为P2预留',
  `style` VARCHAR(32) NOT NULL COMMENT '风格：formal/natural/professional',
  `options` JSON NOT NULL COMMENT '优化选项',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/success/failed',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  `completed_at` DATETIME(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_avatar_task_user_status` (`user_id`, `status`),
  KEY `idx_avatar_task_resume_id` (`resume_id`),
  KEY `idx_avatar_task_status_created` (`status`, `created_at`),
  KEY `idx_avatar_task_completed_at` (`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='头像优化任务表';

-- PDF 任务表
CREATE TABLE IF NOT EXISTS `pdf_task` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `resume_id` VARCHAR(64) NOT NULL COMMENT '关联简历ID',
  `template_id` VARCHAR(64) NOT NULL COMMENT '模板ID',
  `file_path` VARCHAR(512) DEFAULT NULL COMMENT '生成文件路径',
  `file_name` VARCHAR(128) DEFAULT NULL COMMENT '下载文件名',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/success/failed',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  `completed_at` DATETIME(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_pdf_task_user_status` (`user_id`, `status`),
  KEY `idx_pdf_task_resume_id` (`resume_id`),
  KEY `idx_pdf_task_status_created` (`status`, `created_at`),
  KEY `idx_pdf_task_completed_at` (`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PDF导出任务表';

-- AI 点评记录表
CREATE TABLE IF NOT EXISTS `resume_review` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `resume_id` VARCHAR(64) NOT NULL COMMENT '关联简历ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `overall_score` INT NOT NULL COMMENT '综合评分0-100',
  `dimension_scores` JSON NOT NULL COMMENT '分项评分',
  `suggestions` JSON NOT NULL COMMENT '修改建议列表',
  `highlights` JSON NOT NULL COMMENT '亮点总结列表',
  `job_description` TEXT DEFAULT NULL COMMENT '目标岗位JD',
  `model_name` VARCHAR(64) DEFAULT NULL COMMENT 'AI模型名称',
  `model_version` VARCHAR(32) DEFAULT NULL COMMENT '模型版本',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/success/failed',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  KEY `idx_resume_review_resume_id` (`resume_id`),
  KEY `idx_resume_review_user_id` (`user_id`),
  KEY `idx_resume_review_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI简历点评记录表';

-- 模板表
CREATE TABLE IF NOT EXISTS `template` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `code` VARCHAR(64) NOT NULL COMMENT '模板唯一编码',
  `name` VARCHAR(64) NOT NULL COMMENT '模板名称',
  `category` VARCHAR(32) NOT NULL COMMENT '分类',
  `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图地址',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '模板描述',
  `config` JSON NOT NULL COMMENT '模板配置',
  `html_template` VARCHAR(128) NOT NULL COMMENT 'HTML模板文件名或路径',
  `render_engine` VARCHAR(32) NOT NULL DEFAULT 'server' COMMENT '渲染引擎：server/client/hybrid',
  `is_builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统内置',
  `is_premium` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否付费',
  `is_recommended` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首页推荐',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive/deleted',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人ID',
  `version` INT NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  UNIQUE KEY `uk_template_code` (`code`),
  KEY `idx_template_status_sort` (`status`, `sort_order`),
  KEY `idx_template_status_recommended` (`status`, `is_recommended`),
  KEY `idx_template_status_category` (`status`, `category`),
  KEY `idx_template_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板表';

-- 刷新令牌表（可选）
CREATE TABLE IF NOT EXISTS `refresh_token` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `token_hash` VARCHAR(256) NOT NULL COMMENT 'Token哈希',
  `expires_at` DATETIME(3) NOT NULL COMMENT '过期时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_token_user_id` (`user_id`),
  KEY `idx_refresh_token_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';

-- 系统内置模板
INSERT INTO `template` (`id`, `code`, `name`, `category`, `thumbnail_url`, `description`, `config`, `html_template`, `render_engine`, `is_builtin`, `is_premium`, `is_recommended`, `sort_order`, `status`, `created_by`, `version`) VALUES
('template_classic_single', 'classic-single', '经典单栏', 'classic', '/templates/classic-single-thumb.png', '传统单栏布局，适合大多数岗位', '{"page":{"width":"210mm","height":"297mm","margin":"20mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#333333","secondary":"#666666","accent":"#1a5276","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"25mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#1a5276","borderBottom":"1px solid #1a5276"},"skill":{"displayStyle":"tag"}}', 'classic-single', 'server', 1, 0, 1, 10, 'active', NULL, 1),
('template_classic_double', 'classic-double', '经典双栏', 'classic', '/templates/classic-double-thumb.png', '左侧 sidebar 展示个人信息与技能，右侧展示经历', '{"page":{"width":"210mm","height":"297mm","margin":"15mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10pt","titleSize":"13pt","smallSize":"9pt"},"color":{"primary":"#2c3e50","secondary":"#7f8c8d","accent":"#2c3e50","background":"#ffffff","sidebar":"#f8f9fa"},"layout":{"singleColumn":false,"sidebarWidth":"30%","avatar":{"visible":true,"shape":"circle","size":"20mm"}},"sectionTitle":{"fontSize":"11pt","fontWeight":"bold","color":"#2c3e50"},"skill":{"displayStyle":"tag"}}', 'classic-double', 'server', 1, 0, 0, 20, 'active', NULL, 1),
('template_tech', 'tech', '技术岗模板', 'tech', '/templates/tech-thumb.png', '突出技术栈与项目经历，适合研发岗位', '{"page":{"width":"210mm","height":"297mm","margin":"18mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10pt","titleSize":"13pt","smallSize":"9pt"},"color":{"primary":"#1e1e1e","secondary":"#5f6368","accent":"#2563eb","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"22mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#2563eb","borderBottom":"2px solid #2563eb"},"skill":{"displayStyle":"category"}}', 'tech', 'server', 1, 0, 1, 30, 'active', NULL, 1),
('template_fresh', 'fresh', '应届生模板', 'fresh', '/templates/fresh-thumb.png', '清新简洁，突出教育背景与校园经历', '{"page":{"width":"210mm","height":"297mm","margin":"20mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#34495e","secondary":"#7f8c8d","accent":"#27ae60","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"circle","size":"24mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#27ae60","borderBottom":"1px solid #27ae60"},"skill":{"displayStyle":"tag"}}', 'fresh', 'server', 1, 0, 1, 40, 'active', NULL, 1),
('template_business', 'business', '简洁商务模板', 'business', '/templates/business-thumb.png', '商务稳重风格，适合社招与管理层', '{"page":{"width":"210mm","height":"297mm","margin":"20mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#2c3e50","secondary":"#7f8c8d","accent":"#c0392b","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"22mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#c0392b","borderBottom":"1px solid #c0392b"},"skill":{"displayStyle":"level"}}', 'business', 'server', 1, 0, 0, 50, 'active', NULL, 1),
('template_postgraduate', 'postgraduate', '考研复试模板', 'postgraduate', '/templates/postgraduate-thumb.png', '学术风格，突出科研项目与教育背景', '{"page":{"width":"210mm","height":"297mm","margin":"22mm"},"font":{"family":"\"Noto Sans SC\", \"SimSun\", serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#000000","secondary":"#333333","accent":"#000000","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"20mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#000000","borderBottom":"1px solid #000000"},"skill":{"displayStyle":"tag"}}', 'postgraduate', 'server', 1, 0, 0, 60, 'active', NULL, 1);
