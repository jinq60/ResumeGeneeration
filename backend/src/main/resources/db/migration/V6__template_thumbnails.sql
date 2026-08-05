-- V6: 内置模板缩略图指向 classpath 静态资源（static/templates/*.svg）
-- 旧地址 /templates/*-thumb.png 无对应文件且不受任何路由服务，改为 /templates/thumbs/*-thumb.svg
UPDATE `template`
SET `thumbnail_url` = '/templates/thumbs/classic-single-thumb.svg'
WHERE `code` = 'classic-single';

UPDATE `template`
SET `thumbnail_url` = '/templates/thumbs/classic-double-thumb.svg'
WHERE `code` = 'classic-double';

UPDATE `template`
SET `thumbnail_url` = '/templates/thumbs/tech-thumb.svg'
WHERE `code` = 'tech';

UPDATE `template`
SET `thumbnail_url` = '/templates/thumbs/fresh-thumb.svg'
WHERE `code` = 'fresh';

UPDATE `template`
SET `thumbnail_url` = '/templates/thumbs/business-thumb.svg'
WHERE `code` = 'business';

UPDATE `template`
SET `thumbnail_url` = '/templates/thumbs/postgraduate-thumb.svg'
WHERE `code` = 'postgraduate';
