# API 变更日志

> 记录 `docs/superpowers/specs/2026-07-03-api-spec.md` 的所有变更，便于前后端联调与版本管理。

## v1.5（2026-08-06）

### 新增

- 简历详情与更新接口新增 `renderSettings`，支持一页纸适配、字体、字号、行高、页面边距、模块间距和主题色。
- 实时预览、PDF 和 Word 导出统一应用简历排版设置。

---

## v1.4（2026-08-05）

### 新增

- `POST /resumes/{id}/ai/write`：行内 AI 写作（generate/polish/shorten/expand/translate），字段白名单 + 每用户并发限制，复用多厂商 LLM 路由与 `ai_call_log` 审计。
- `POST /resumes/{id}/ai/write/stream`：行内 AI 写作 SSE 流式接口，发送 `delta`、`done`、`error` 事件；复用同步接口校验和并发限制。
- `POST /resumes/{id}/grammar-check`：AI 语法检查，返回字段级问题、严重程度、修改建议和解释。
- 富文本字段：自我介绍使用 `contentHtml`，工作/项目描述与成就使用 `descriptionHtml`；服务端渲染与导出统一执行 HTML 白名单清洗。
- `POST /resumes/{id}/share` / `GET /resumes/{id}/share` / `DELETE /resumes/{id}/share`：简历公开分享（创建/轮换、查询、关闭）。
- `GET /share/{token}`：公开只读分享页（HTML，CSP/noindex/no-store）。
- `GET /resumes/{id}/export/markdown`：导出 Markdown 文件。
- `GET /resumes/{id}/export/word`：导出 Word（docx）文件。
- `resume_share` 表（V8 迁移）。
- 错误码：`AI_WRITING_FIELD_INVALID`(6007)、`AI_WRITING_CONTENT_TOO_LONG`(6008)。

### 调整

- 前端视觉简洁化：设计令牌收敛（单一强调色 + 中性灰）、编辑器两栏布局、深色侧栏浅色化；移除 Google Fonts 在线依赖（修复生产字体加载）。

---

## v1.3（2026-08-05）

### 新增

- `audit_log` 表（V7 迁移）：敏感操作审计（登录/注册/删除/导出/后台管理操作等）。
- 首个管理员引导：`ADMIN_BOOTSTRAP_PHONE` / `ADMIN_BOOTSTRAP_PASSWORD` 环境变量，仅当系统无任何 ADMIN 账号时生效。
- 安全风控：
  - 登录连续失败锁定：`LOGIN_MAX_FAILURES`（默认 5）/ `LOGIN_LOCKOUT_MINUTES`（默认 15）。
  - 游客会话限额：`GUEST_MAX_PER_IP_PER_DAY`（默认 50），超限返回 429。
  - 验证码模式：`VERIFY_CODE_MODE=placeholder|strict`；生产（strict）未接入真实验证码服务时注册被安全拒绝。
- `/templates/thumbs/**`：内置模板缩略图静态资源（classpath:static/templates/*.svg），V6 迁移更新 6 套内置模板缩略图地址。
- 模板 HTML 骨架新增 5 套（classic-double 双栏 / tech / fresh / business / postgraduate），由 `template.html_template` 指定加载。
- 监控：`/actuator/prometheus` 指标端点（micrometer-registry-prometheus）。

### 调整

- 注册验证码校验改为通过 `VerifyCodeService` 抽象执行（不再固定"任意 6 位数字"）；dev/test 默认 placeholder 行为不变。
- `/auth/guest` 增加每 IP 每日限额（GuestAccountGuard）。

---

## v1.2（2026-08-05）

### 新增

- `PUT /users/me/password`：修改当前用户密码（校验旧密码，成功后吊销全部刷新令牌，需重新登录）。
- `app.render.public-base-url` 配置：PDF 渲染时把 `/uploads/**` 相对路径拼为绝对地址，保证导出的 PDF 中头像可加载。

### 调整

- `/uploads/**` 静态资源实际访问路径为 `/api/uploads/**`（后端 context-path 为 `/api`），由网关（Vite 代理 / nginx）将 `/uploads/*` 改写为 `/api/uploads/*`。

---

## v1.1（2026-07-07）

### 新增

- `POST /avatars/optimize` 增加 PRD 选项与 API 字段映射说明。
- `PUT /resumes/{id}` 明确返回 `{ id, updatedAt }` 最小对象。
- 全局错误码表增加数字错误码映射。

### 调整

- `GET /resumes/{resumeId}/preview` 由匿名访问改为需 JWT 认证并校验简历所有者，防止个人简历信息泄露。
- PDF 导出无姓名时文件名统一为 `我的简历_yyyyMMdd.pdf`（例如 `我的简历_20260707.pdf`）。
- `UpdateResumeRequest.sections` 由必填改为可选。
- 头像优化任务明确为 P0 占位实现，P1 接入真实 AI。

### 修复

- 修复 `ResumePreview.vue` 引用的未定义端点问题（客户端预览应使用模板组件或 PDF 导出流程）。

---

## v1.0（2026-07-03）

### 初始版本

- 定义认证模块：`/auth/register`、`/auth/login`、`/auth/guest`、`/auth/refresh`。
- 定义用户模块：`GET /users/me`。
- 定义简历模块：`POST /resumes`、`GET /resumes`、`GET /resumes/{id}`、`PUT /resumes/{id}`、`DELETE /resumes/{id}`、`POST /resumes/{id}/duplicate`、`PUT /resumes/{id}/title`。
- 定义 AI 点评模块（P1）：`POST /resumes/{id}/reviews`、`GET /resumes/{id}/reviews/latest`。
- 定义模板模块：`GET /templates`、`GET /templates/{id}`。
- 定义后台模板管理模块：`/admin/templates/*`。
- 定义头像模块：`POST /avatars/upload`、`POST /avatars/optimize`、`GET /avatars/tasks/{taskId}`、`DELETE /avatars/{id}`。
- 定义 PDF 模块：`POST /pdf/export`、`GET /pdf/tasks/{taskId}`、`GET /pdf/download/{taskId}`。
- 定义统一响应格式 `R<T>`、认证方式、分页规范、幂等性建议。

---

## 版本策略

- 当前 Base URL：`/api`。
- 未来如需 breaking change，统一迁移到 `/api/v2/`。
- 非 breaking 变更通过本文档版本号管理，接口路径保持不变。

## 参考文档

- `docs/superpowers/specs/2026-07-03-api-spec.md`
- `docs/adr/ADR-004-template-as-managed-resource.md`
