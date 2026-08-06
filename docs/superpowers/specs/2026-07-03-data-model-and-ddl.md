# 简历生成工具数据模型与 DDL

> 版本：v1.2
> 日期：2026-08-06
> 基于：`docs/superpowers/specs/2026-07-03-scope-alignment.md`

---

## 1. 数据模型设计原则

1. **JSON 字段存储 section 数组**：简历内容模块多变，使用 JSON 可灵活扩展，避免频繁 DDL 变更。
2. **游客模式 user_id 设计**：游客使用 `guest_{snowflake_id}` 作为临时用户 ID，与正式用户统一存储；登录后按同步策略迁移数据。
3. **逻辑删除**：所有表统一使用 `deleted` 字段（TINYINT(1)，0 未删除 / 1 已删除）实现逻辑删除；`status` 字段仅表示业务状态，避免 MyBatis-Plus 全局逻辑删除配置与任务状态枚举冲突。
4. **ID 统一使用 Snowflake**：所有主键为 `VARCHAR(64)`，由后端 Snowflake 生成，对外均为字符串。
5. **无外键约束**：表之间通过 `user_id`、`resume_id`、`template_id` 等字段做逻辑关联，不设置 FOREIGN KEY。避免分布式/大数据量场景下的锁竞争与级联删除复杂性，数据一致性由业务层与定时清理任务保障。
6. **对象存储存放文件，数据库存 URL**：头像原图、优化结果图、PDF 文件、模板缩略图均存放于 MinIO（或兼容 S3 的对象存储），数据库仅保存可访问的 URL。禁止将二进制文件直接写入数据库。
6. **时间与审计字段**：所有表包含 `created_at`、`updated_at`，必要时增加 `completed_at`。

---

## 2. 数据库表结构

### 2.1 用户表 `user`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `phone` | VARCHAR(20) | UNIQUE, NULLABLE | 手机号，已脱敏存储建议加密 |
| `email` | VARCHAR(128) | UNIQUE, NULLABLE | 邮箱 |
| `password_hash` | VARCHAR(256) | NULLABLE | 密码哈希，游客为空 |
| `nickname` | VARCHAR(64) | NULLABLE | 昵称 |
| `avatar_url` | VARCHAR(512) | NULLABLE | 用户账号头像 URL，存储于 MinIO；用户可自由选择任意图片，非必须为真人照片；为空时不展示 |
| `is_guest` | TINYINT(1) | NOT NULL DEFAULT 1 | 是否游客：1 是，0 否 |
| `status` | VARCHAR(16) | NOT NULL DEFAULT 'active' | active / disabled / deleted（业务状态，deleted 仍由 `deleted` 字段统一控制逻辑删除） |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |

### 2.2 简历表 `resume`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `user_id` | VARCHAR(64) | NOT NULL, INDEX | 用户 ID，游客为 guest_id |
| `title` | VARCHAR(128) | NOT NULL | 简历名称 |
| `scene` | VARCHAR(32) | NOT NULL | 使用场景枚举 |
| `target_position` | VARCHAR(128) | NULLABLE | 目标岗位 |
| `target_industry` | VARCHAR(128) | NULLABLE | 目标行业（P1） |
| `template_id` | VARCHAR(64) | NOT NULL | 当前模板 ID |
| `sections` | JSON | NOT NULL | Section 数组 |
| `render_settings` | JSON | NULLABLE | 用户排版设置：一页适配、字体、字号、行高、边距、模块间距、主题色 |
| `status` | VARCHAR(16) | NOT NULL DEFAULT 'active' | active / deleted（业务状态） |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `export_count` | INT | NOT NULL DEFAULT 0 | 导出次数（P1 统计） |
| `last_edited_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 最近编辑时间 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |

### 2.3 头像任务表 `avatar_task`

简历一寸照片优化任务。用户在简历编辑器上传自拍照，后端按 `background_type`、`style`、`options` 等参数生成标准一寸照片并镶嵌到简历个人信息中。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `user_id` | VARCHAR(64) | NOT NULL, INDEX | 用户 ID |
| `resume_id` | VARCHAR(64) | NULLABLE, INDEX | 关联简历 ID，用于将结果图自动回填到对应简历 |
| `source_image_url` | VARCHAR(512) | NOT NULL | 用户上传的自拍照原图地址 |
| `result_image_url` | VARCHAR(512) | NULLABLE | 一寸照片优化结果图地址 |
| `background_type` | VARCHAR(16) | NOT NULL | 一寸照背景色：white / blue / red（P0）；transparent 为 P2 扩展预留 |
| `style` | VARCHAR(16) | NOT NULL | 照片风格：formal / natural / professional |
| `options` | JSON | NOT NULL | 优化选项，如美颜、磨皮、智能换底、人脸居中裁剪等 |
| `status` | VARCHAR(16) | NOT NULL DEFAULT 'pending' | pending / processing / success / failed（任务状态） |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `error_msg` | VARCHAR(512) | NULLABLE | 失败原因 |
| `completed_at` | DATETIME(3) | NULLABLE | 完成时间 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |

### 2.4 PDF 任务表 `pdf_task`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `user_id` | VARCHAR(64) | NOT NULL, INDEX | 用户 ID |
| `resume_id` | VARCHAR(64) | NOT NULL, INDEX | 关联简历 ID |
| `template_id` | VARCHAR(64) | NOT NULL | 模板 ID |
| `file_path` | VARCHAR(512) | NULLABLE | 生成文件路径 |
| `file_name` | VARCHAR(128) | NULLABLE | 下载文件名 |
| `file_size` | BIGINT | NULLABLE | 文件大小（字节） |
| `status` | VARCHAR(16) | NOT NULL DEFAULT 'pending' | pending / processing / success / failed（任务状态） |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `error_msg` | VARCHAR(512) | NULLABLE | 失败原因 |
| `completed_at` | DATETIME(3) | NULLABLE | 完成时间 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |

### 2.5 AI 点评记录表 `resume_review`

P1 功能：对用户简历进行 AI 点评并保存最近一次结果。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `resume_id` | VARCHAR(64) | NOT NULL, INDEX | 关联简历 ID |
| `user_id` | VARCHAR(64) | NOT NULL, INDEX | 用户 ID |
| `overall_score` | INT | NOT NULL | 综合评分 0–100 |
| `dimension_scores` | JSON | NOT NULL | 分项评分 |
| `suggestions` | JSON | NOT NULL | 修改建议列表 |
| `highlights` | JSON | NOT NULL | 亮点总结列表 |
| `job_description` | TEXT | NULLABLE | 用户粘贴的目标岗位 JD |
| `model_name` | VARCHAR(64) | NULLABLE | 使用的 AI 模型名称 |
| `model_version` | VARCHAR(32) | NULLABLE | 模型版本 |
| `status` | VARCHAR(16) | NOT NULL DEFAULT 'pending' | pending / success / failed（任务状态） |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `error_msg` | VARCHAR(512) | NULLABLE | 失败原因 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |

### 2.6 模板表 `template`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `code` | VARCHAR(64) | NOT NULL, UNIQUE | 模板唯一编码，用于文件命名与前端识别 |
| `name` | VARCHAR(64) | NOT NULL | 模板名称 |
| `category` | VARCHAR(32) | NOT NULL, INDEX | 分类：classic / tech / fresh / business / postgraduate |
| `thumbnail_url` | VARCHAR(512) | NULLABLE | 缩略图地址 |
| `description` | VARCHAR(512) | NULLABLE | 模板描述 |
| `config` | JSON | NOT NULL | 模板配置 |
| `html_template` | VARCHAR(128) | NOT NULL | HTML 模板文件名或路径 |
| `render_engine` | VARCHAR(32) | NOT NULL DEFAULT 'server' | 渲染引擎：server / client / hybrid |
| `is_builtin` | TINYINT(1) | NOT NULL DEFAULT 0 | 是否系统内置（后台不可删除） |
| `is_premium` | TINYINT(1) | NOT NULL DEFAULT 0 | 是否付费（P2） |
| `is_recommended` | TINYINT(1) | NOT NULL DEFAULT 0 | 是否首页推荐 |
| `sort_order` | INT | NOT NULL DEFAULT 0 | 排序权重，越小越靠前 |
| `status` | VARCHAR(16) | NOT NULL DEFAULT 'active' | active / inactive / deleted（业务状态） |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `created_by` | VARCHAR(64) | NULLABLE, INDEX | 创建人 ID，系统内置为 NULL |
| `version` | INT | NOT NULL DEFAULT 1 | 模板版本号，用于缓存控制 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |
| `updated_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |

### 2.7 刷新令牌表 `refresh_token`（可选，若使用数据库存储）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | VARCHAR(64) | PRIMARY KEY | Snowflake ID |
| `user_id` | VARCHAR(64) | NOT NULL, INDEX | 用户 ID |
| `token_hash` | VARCHAR(256) | NOT NULL, UNIQUE | Token 哈希 |
| `expires_at` | DATETIME(3) | NOT NULL | 过期时间 |
| `deleted` | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除：0 未删除，1 已删除 |
| `created_at` | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 创建时间 |

---

## 3. 头像与文件存储策略

### 3.1 两种头像

| 头像类型 | 用途 | 入口 | 是否必须真人 |
|---|---|---|---|
| **用户头像** | 账号维度的展示头像 | 个人中心/设置 | 否，用户可自由选择任意图片 |
| **简历头像** | 简历个人信息区域的一寸照片 | 简历编辑器头像入口 | 建议真人自拍，系统优化为一寸照 |

### 3.2 一寸照生成流程

1. 用户在简历编辑器点击「上传头像」/「优化头像」。
2. 前端调用 `POST /avatars/upload` 将自拍照上传至 MinIO。
3. 前端调用 `POST /avatars/optimize` 提交背景色、风格、美颜等选项，创建 `avatar_task`。
4. 后端（P0 为占位实现，P1 接入 AI）按一寸照规格处理：人脸检测、居中裁剪、背景替换、美颜。
5. 任务完成后，`avatar_task.result_image_url` 指向一寸照结果图。
6. 前端将 `result_image_url` 回填到 `resume.sections.profile.avatarUrl`，完成镶嵌。

### 3.3 存储方案

- **对象存储**：所有图片、PDF、模板缩略图等文件统一存放于 MinIO（或兼容 S3 协议的对象存储）。
- **数据库存 URL**：`user.avatar_url`、`avatar_task.source_image_url`、`avatar_task.result_image_url`、`resume.sections.profile.avatarUrl`、`template.thumbnail_url`、`pdf_task.file_path` 均保存对象存储上的可访问 URL。
- **Bucket 规划**：
  - `resume-avatars`：头像原图与一寸照优化结果图
  - `resume-pdfs`：导出的 PDF 文件
  - `resume-templates`：模板缩略图与 HTML 模板静态资源
- **路径组织**：`{bucket}/{user_id}/{resource_type}/{resource_id}.{ext}`，例如 `resume-avatars/user_xxx/avatars/avatar_xxx_source.png`。
- **访问方式**：
  - 开发环境：后端提供预签名 URL 或公开 Bucket。
  - 生产环境：通过 CDN + 预签名 URL 访问，Bucket 不直接对外公开。

### 3.4 头像未上传时的占位行为

- **用户头像**：`user.avatar_url` 为空时，前端不展示头像；可显示默认占位图标。
- **简历头像**：`profile.avatarUrl` 为空时，模板渲染不显示一寸照片；PDF 导出同样不显示。
- **默认占位图**：如需统一占位，可在对象存储上传一张默认头像，由前端/模板在 URL 为空时兜底使用，但数据库中仍保持 `NULL`。

---

## 4. 字段详细说明

### 4.1 resume.sections

JSON 数组，每个元素为 Section 统一结构：

```json
{
  "id": "sec_1720000000001",
  "type": "education",
  "title": "教育经历",
  "order": 1,
  "visible": true,
  "data": {}
}
```

### 4.2 avatar_task.options

```json
{
  "keepIdentity": true,
  "enhanceQuality": true,
  "removeBackground": true,
  "brightenSkin": false
}
```

### 4.3 template.config

见 `docs/superpowers/specs/2026-07-03-template-system-spec.md`。

### 4.4 JSON 字段与代码类型映射

数据库中的 JSON 字段在代码层必须映射为**强类型集合/对象**，避免使用 `String` 或 `Object` 做二次解析。当前规范与推荐类型如下：

| 数据库字段 | 推荐 Java 类型 | 推荐 TypeScript 类型 | 说明 |
|---|---|---|---|
| `resume.sections` | `List<Section>` | `Section[]` | Section 统一结构数组，见 §5.6 |
| `resume_review.dimension_scores` | `Map<String, Integer>` | `Record<string, number>` | 分项评分，键为维度编码 |
| `resume_review.suggestions` | `List<Suggestion>` | `Suggestion[]` | 建议对象数组，见 §5.9 |
| `resume_review.highlights` | `List<String>` | `string[]` | 亮点字符串数组 |
| `avatar_task.options` | `Map<String, Boolean>` | `Record<string, boolean>` | 优化选项键值对 |
| `template.config` | `Map<String, Object>` | `Record<string, any>` | 模板配置，结构由模板规范定义 |
| `resume.render_settings` | `RenderSettings` | `RenderSettings` | 用户排版覆盖设置，使用 TypeHandler 自动映射 |

**实现要求**：
- Java 实体使用 MyBatis-Plus `JacksonTypeHandler` 或自定义 `TypeHandler` 将 JSON 自动映射到上述类型；禁止在 Service 中手动 `ObjectMapper` 解析。
- TypeScript 前端类型定义必须保持与上表一致，禁止在 API 调用中使用 `any[]` 传递 `sections`。

---

## 5. JSON Schema 定义

### 5.1 Profile

```json
{
  "name": "张三",
  "gender": "male",
  "birthDate": "2000-01",
  "phone": "13800000000",
  "email": "zhangsan@example.com",
  "city": "西安",
  "targetPosition": "Java后端开发工程师",
  "expectedSalary": "15k-20k",
  "availability": "一周内到岗",
  "personalWebsite": "https://example.com",
  "github": "https://github.com/example",
  "portfolio": "https://example.com",
  "avatarUrl": "https://example.com/avatar.png",
  "showGender": true,
  "showAge": false,
  "showSalary": false,
  "showAvatar": true
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 姓名，1–50 字符 |
| `gender` | string | 否 | male / female / other |
| `birthDate` | string | 否 | 格式 `YYYY-MM` |
| `phone` | string | 条件 | 与 email 至少填一个 |
| `email` | string | 条件 | 与 phone 至少填一个 |
| `city` | string | 否 | 所在城市 |
| `targetPosition` | string | 否 | 目标岗位 |
| `expectedSalary` | string | 否 | 期望薪资 |
| `availability` | string | 否 | 到岗时间 |
| `personalWebsite` | string | 否 | URL |
| `github` | string | 否 | URL |
| `portfolio` | string | 否 | URL |
| `avatarUrl` | string | 否 | 头像 URL |
| `showGender` | boolean | 否 | 默认 true |
| `showAge` | boolean | 否 | 默认 false |
| `showSalary` | boolean | 否 | 默认 false |
| `showAvatar` | boolean | 否 | 默认 true |

### 5.2 Education

```json
{
  "school": "某某大学",
  "degree": "本科",
  "major": "计算机科学与技术",
  "college": "信息工程学院",
  "startDate": "2022-09",
  "endDate": "2026-06",
  "gpa": "3.7/4.0",
  "rank": "前10%",
  "honors": ["国家奖学金"],
  "courses": ["数据结构", "操作系统"]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `school` | string | 是 | 学校名称 |
| `degree` | string | 是 | 学历 |
| `major` | string | 是 | 专业 |
| `college` | string | 否 | 学院 |
| `startDate` | string | 是 | 格式 `YYYY-MM` |
| `endDate` | string | 否 | 格式 `YYYY-MM`，至今用 `present` |
| `gpa` | string | 否 | GPA |
| `rank` | string | 否 | 排名 |
| `honors` | string[] | 否 | 荣誉 |
| `courses` | string[] | 否 | 课程 |

### 5.3 Project

```json
{
  "name": "智能简历生成系统",
  "role": "前端开发",
  "type": "course_project",
  "startDate": "2026-03",
  "endDate": "2026-06",
  "techStack": ["Vue", "Spring Boot", "MySQL"],
  "background": "解决学生简历排版难问题",
  "responsibility": "负责编辑器页面开发",
  "achievements": ["实现实时预览", "导出 PDF"],
  "description": [
    "负责简历编辑器页面开发，实现左侧表单与右侧预览的实时联动。",
    "封装项目经历、教育经历、技能模块等动态表单组件。",
    "实现 PDF 导出功能，保证导出效果与页面预览一致。"
  ],
  "link": "https://example.com",
  "github": "https://github.com/example"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 项目名称 |
| `role` | string | 否 | 项目角色 |
| `type` | string | 否 | 项目类型枚举 |
| `startDate` | string | 否 | 格式 `YYYY-MM` |
| `endDate` | string | 否 | 格式 `YYYY-MM` |
| `techStack` | string[] | 否 | 技术栈 |
| `background` | string | 否 | 项目背景 |
| `responsibility` | string | 否 | 个人职责 |
| `achievements` | string[] | 否 | 项目成果 |
| `description` | string[] | 是 | 展示用描述，3–5 条 |
| `link` | string | 否 | URL |
| `github` | string | 否 | URL |

### 5.4 WorkExperience

```json
{
  "company": "某某科技有限公司",
  "department": "研发部",
  "position": "前端实习生",
  "type": "internship",
  "city": "上海",
  "startDate": "2025-07",
  "endDate": "2025-10",
  "description": [
    "参与后台管理系统页面开发，完成数据表格、筛选表单和权限页面。",
    "配合后端完成接口联调，优化页面加载速度和交互体验。"
  ],
  "achievements": ["优化页面加载速度 30%"],
  "techStack": ["Vue", "Element Plus"],
  "leaveReason": "实习结束",
  "showLeaveReason": false
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `company` | string | 是 | 公司/机构名称 |
| `department` | string | 否 | 部门 |
| `position` | string | 是 | 职位 |
| `type` | string | 否 | 工作类型枚举 |
| `city` | string | 否 | 工作城市 |
| `startDate` | string | 是 | 格式 `YYYY-MM` |
| `endDate` | string | 否 | 格式 `YYYY-MM`，至今用 `present` |
| `description` | string[] | 是 | 工作内容 |
| `achievements` | string[] | 否 | 工作成果 |
| `techStack` | string[] | 否 | 技术栈 |
| `leaveReason` | string | 否 | 离职原因，默认不展示 |
| `showLeaveReason` | boolean | 否 | 默认 false |

### 5.5 Skill

```json
{
  "category": "frontend",
  "items": [
    { "name": "Vue", "level": "proficient", "highlight": true },
    { "name": "JavaScript", "level": "familiar", "highlight": false }
  ]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `category` | string | 是 | 技能分类枚举 |
| `items` | object[] | 是 | 技能项数组 |
| `items[].name` | string | 是 | 技能名称 |
| `items[].level` | string | 否 | 熟练程度枚举 |
| `items[].highlight` | boolean | 否 | 是否重点展示 |

### 5.6 Section 统一结构

```json
{
  "id": "sec_1720000000001",
  "type": "education",
  "title": "教育经历",
  "order": 1,
  "visible": true,
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | string | 是 | Section 唯一 ID，前端生成或后端生成 |
| `type` | string | 是 | sectionType 枚举 |
| `title` | string | 是 | 模块标题 |
| `order` | integer | 是 | 排序序号，从 0 开始 |
| `visible` | boolean | 是 | 是否展示 |
| `data` | object | 是 | 模块数据，根据 type 变化 |

### 5.7 AvatarTask.options

```json
{
  "keepIdentity": true,
  "enhanceQuality": true,
  "removeBackground": true,
  "brightenSkin": false
}
```

### 5.8 Resume 简历主表 JSON

```json
{
  "id": "resume_1720000000001",
  "userId": "user_1720000000001",
  "title": "我的Java后端开发简历",
  "scene": "campus_recruitment",
  "targetPosition": "Java后端开发工程师",
  "templateId": "template_1720000000001",
  "sections": [],
  "createdAt": "2026-07-03T10:00:00",
  "updatedAt": "2026-07-03T12:00:00"
}
```

### 5.9 ResumeReview AI 点评结果 JSON

```json
{
  "overallScore": 78,
  "dimensionScores": {
    "completeness": 85,
    "structure": 80,
    "content": 70,
    "match": 75,
    "expression": 82
  },
  "suggestions": [
    {
      "sectionType": "project",
      "title": "项目经历描述不够量化",
      "problem": "缺少具体数据和成果，招聘方难以评估贡献度。",
      "advice": "建议使用 STAR 法则，补充 QPS、用户数、性能提升百分比等指标。",
      "priority": "high"
    }
  ],
  "highlights": ["教育背景与目标岗位匹配度高"]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `overallScore` | integer | 是 | 综合评分，0–100 |
| `dimensionScores` | object | 是 | 分项评分，键为维度编码，值为 0–100 |
| `dimensionScores.completeness` | integer | 是 | 信息完整度 |
| `dimensionScores.structure` | integer | 是 | 结构清晰度 |
| `dimensionScores.content` | integer | 是 | 内容充实度 |
| `dimensionScores.match` | integer | 是 | 岗位匹配度 |
| `dimensionScores.expression` | integer | 是 | 表达专业性 |
| `suggestions` | object[] | 是 | 修改建议列表 |
| `suggestions[].sectionType` | string | 否 | 关联的 Section 类型 |
| `suggestions[].title` | string | 是 | 建议标题 |
| `suggestions[].problem` | string | 是 | 问题描述 |
| `suggestions[].advice` | string | 是 | 具体修改建议 |
| `suggestions[].priority` | string | 是 | `high` / `medium` / `low` |
| `highlights` | string[] | 是 | 亮点总结 |

---

## 6. 索引设计

设计原则：索引优先覆盖高频查询与数据隔离条件；复合索引把区分度高的 `status` 放在后面，便于范围扫描与排序；任务表额外增加“待处理队列”索引用于异步消费。

### 6.1 user 表

```sql
-- 手机号、邮箱唯一（MySQL 中 NULL 值可重复，业务层补充校验）
CREATE UNIQUE INDEX idx_user_phone ON user(phone);
CREATE UNIQUE INDEX idx_user_email ON user(email);
CREATE INDEX idx_user_is_guest_status ON user(is_guest, status);
```

### 6.2 resume 表

```sql
-- 用户简历列表：按最近编辑倒序
CREATE INDEX idx_resume_user_status_edited ON resume(user_id, status, last_edited_at DESC);
-- 模板使用统计与管理
CREATE INDEX idx_resume_template_status ON resume(template_id, status);
-- 场景推荐
CREATE INDEX idx_resume_scene_status ON resume(scene, status);
```

### 6.3 avatar_task 表

```sql
-- 用户查询自己的头像任务
CREATE INDEX idx_avatar_task_user_status ON avatar_task(user_id, status);
-- 按简历查询
CREATE INDEX idx_avatar_task_resume_id ON avatar_task(resume_id);
-- 异步任务消费队列
CREATE INDEX idx_avatar_task_status_created ON avatar_task(status, created_at);
-- 过期/清理
CREATE INDEX idx_avatar_task_completed_at ON avatar_task(completed_at);
```

### 6.4 pdf_task 表

```sql
-- 用户查询自己的 PDF 任务
CREATE INDEX idx_pdf_task_user_status ON pdf_task(user_id, status);
-- 按简历查询
CREATE INDEX idx_pdf_task_resume_id ON pdf_task(resume_id);
-- 异步任务消费队列
CREATE INDEX idx_pdf_task_status_created ON pdf_task(status, created_at);
-- 过期/清理
CREATE INDEX idx_pdf_task_completed_at ON pdf_task(completed_at);
```

### 6.5 resume_review 表

```sql
-- 按简历查询最新点评
CREATE INDEX idx_resume_review_resume_id ON resume_review(resume_id);
-- 按用户查询点评历史
CREATE INDEX idx_resume_review_user_id ON resume_review(user_id);
-- 按状态查询失败/待处理记录
CREATE INDEX idx_resume_review_status ON resume_review(status);
```

### 6.6 template 表

```sql
-- 前台模板列表：状态 + 排序
CREATE INDEX idx_template_status_sort ON template(status, sort_order);
-- 推荐位
CREATE INDEX idx_template_status_recommended ON template(status, is_recommended);
-- 按分类筛选
CREATE INDEX idx_template_status_category ON template(status, category);
-- 创建人管理后台
CREATE INDEX idx_template_created_by ON template(created_by);
```

### 6.7 refresh_token 表

```sql
-- 按用户清理
CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);
-- 过期令牌批量清理
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
```

---

## 7. DDL 脚本

### 7.1 MySQL 8 建表语句

```sql
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
```

### 7.2 初始化数据（系统内置模板示例）

```sql
INSERT INTO `template` (`id`, `code`, `name`, `category`, `thumbnail_url`, `description`, `config`, `html_template`, `render_engine`, `is_builtin`, `is_premium`, `is_recommended`, `sort_order`, `status`, `created_by`, `version`) VALUES
('template_classic_single', 'classic-single', '经典单栏', 'classic', '/templates/classic-single-thumb.png', '传统单栏布局，适合大多数岗位', '{"page":{"width":"210mm","height":"297mm","margin":"20mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#333333","secondary":"#666666","accent":"#1a5276","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"25mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#1a5276","borderBottom":"1px solid #1a5276"},"skill":{"displayStyle":"tag"}}', 'classic-single', 'server', 1, 0, 1, 10, 'active', NULL, 1),
('template_classic_double', 'classic-double', '经典双栏', 'classic', '/templates/classic-double-thumb.png', '左侧 sidebar 展示个人信息与技能，右侧展示经历', '{"page":{"width":"210mm","height":"297mm","margin":"15mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10pt","titleSize":"13pt","smallSize":"9pt"},"color":{"primary":"#2c3e50","secondary":"#7f8c8d","accent":"#2c3e50","background":"#ffffff","sidebar":"#f8f9fa"},"layout":{"singleColumn":false,"sidebarWidth":"30%","avatar":{"visible":true,"shape":"circle","size":"20mm"}},"sectionTitle":{"fontSize":"11pt","fontWeight":"bold","color":"#2c3e50"},"skill":{"displayStyle":"tag"}}', 'classic-double', 'server', 1, 0, 0, 20, 'active', NULL, 1),
('template_tech', 'tech', '技术岗模板', 'tech', '/templates/tech-thumb.png', '突出技术栈与项目经历，适合研发岗位', '{"page":{"width":"210mm","height":"297mm","margin":"18mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10pt","titleSize":"13pt","smallSize":"9pt"},"color":{"primary":"#1e1e1e","secondary":"#5f6368","accent":"#2563eb","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"22mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#2563eb","borderBottom":"2px solid #2563eb"},"skill":{"displayStyle":"category"}}', 'tech', 'server', 1, 0, 1, 30, 'active', NULL, 1),
('template_fresh', 'fresh', '应届生模板', 'fresh', '/templates/fresh-thumb.png', '清新简洁，突出教育背景与校园经历', '{"page":{"width":"210mm","height":"297mm","margin":"20mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#34495e","secondary":"#7f8c8d","accent":"#27ae60","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"circle","size":"24mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#27ae60","borderBottom":"1px solid #27ae60"},"skill":{"displayStyle":"tag"}}', 'fresh', 'server', 1, 0, 1, 40, 'active', NULL, 1),
('template_business', 'business', '简洁商务模板', 'business', '/templates/business-thumb.png', '商务稳重风格，适合社招与管理层', '{"page":{"width":"210mm","height":"297mm","margin":"20mm"},"font":{"family":"\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#2c3e50","secondary":"#7f8c8d","accent":"#c0392b","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"22mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#c0392b","borderBottom":"1px solid #c0392b"},"skill":{"displayStyle":"level"}}', 'business', 'server', 1, 0, 0, 50, 'active', NULL, 1),
('template_postgraduate', 'postgraduate', '考研复试模板', 'postgraduate', '/templates/postgraduate-thumb.png', '学术风格，突出科研项目与教育背景', '{"page":{"width":"210mm","height":"297mm","margin":"22mm"},"font":{"family":"\"Noto Sans SC\", \"SimSun\", serif","mainSize":"10.5pt","titleSize":"14pt","smallSize":"9pt"},"color":{"primary":"#000000","secondary":"#333333","accent":"#000000","background":"#ffffff"},"layout":{"singleColumn":true,"avatar":{"visible":true,"shape":"square","size":"20mm"}},"sectionTitle":{"fontSize":"12pt","fontWeight":"bold","color":"#000000","borderBottom":"1px solid #000000"},"skill":{"displayStyle":"tag"}}', 'postgraduate', 'server', 1, 0, 0, 60, 'active', NULL, 1);
```

> 说明：以上为系统内置模板示例，生产环境可通过后台管理接口继续新增/编辑/下架模板。`is_builtin = 1` 表示系统模板，后台不可物理删除，仅可上下架。

---

## 8. 版本兼容性

### 8.1 未来扩展字段策略

1. **JSON 字段扩展**：`resume.sections`、`avatar_task.options`、`template.config`、`resume_review.dimension_scores`、`resume_review.suggestions`、`resume_review.highlights` 均为 JSON，新增字段无需修改表结构。
2. **新 Section 类型**：P1/P2 新增 `course`、`certificate`、`custom` 等 type 时，只需在前后端枚举中注册，无需 DDL。
3. **新模板字段**：在 `template.config` 中增加，老模板可保持默认配置。
4. **AI 点评维度扩展**：新增评分维度时，在 `dimension_scores` JSON 中增加字段，老记录可保持默认或回追。
5. **字段弃用**：旧字段保留在 JSON 中，前端忽略即可，避免数据迁移。

### 8.2 数据迁移考虑

- 游客转正时：创建新 user 记录，将 `resume.user_id` 从 guest_id 更新为 user_id，同时迁移 `avatar_task.user_id` 和 `pdf_task.user_id`。
- 头像/PDF 文件路径：游客与正式用户统一按 `uploads/{user_id}/...` 组织，用户 ID 变化时需要移动文件或更新路径。

---

## 9. 参考文档

- `docs/superpowers/specs/2026-07-03-scope-alignment.md`
- `docs/superpowers/specs/2026-07-03-validation-rules.md`（下游文档）
- `docs/superpowers/specs/2026-07-03-api-spec.md`（下游文档）
- `docs/superpowers/specs/2026-07-03-template-system-spec.md`（下游文档）
