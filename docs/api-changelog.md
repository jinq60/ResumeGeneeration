# API 变更日志

> 记录 `docs/superpowers/specs/2026-07-03-api-spec.md` 的所有变更，便于前后端联调与版本管理。

## v2.4（2026-08-24）— 安全加固

### 变更（破坏性）

- **OAuth 回调不再经 URL 传递 JWT**：`GET /auth/oauth/{provider}/callback` 成功后重定向为 `frontend?oauth_code={一次性授权码}`（旧 `token/refresh/guest` 参数废弃）。前端须以授权码调用新增的 `POST /auth/oauth/exchange` 换取令牌对。授权码 32 字节随机、单次消费、120 秒过期。
- **幂等冲突不再放行执行**：同一 `Idempotency-Key` 的并发重复请求在等待超时后返回 HTTP `425` + 业务码 `425`（`IDEMPOTENCY_CONFLICT`），而非降级直接执行业务。
- **邮箱验证码生产 fail-fast**：非 dev/test profile 下未配置 SMTP 时，`POST /auth/email-code/send` 返回 `1009 AUTH_EMAIL_CODE_SEND_FAILED`（此前降级将验证码写入日志）。

### 增强

- 刷新令牌 reuse detection 补全：refresh JWT 新增 `fid`（familyId）claim；签名合法但记录已被消费时按家族撤销全部令牌（覆盖失窃令牌顺序重放场景），返回 `1007`。
- 生产编排安全基线收紧：Redis 强制密码认证；MySQL/MinIO/Redis/后端端口默认仅绑定宿主 `127.0.0.1`。
- 限流与游客限额按真实客户端 IP 计数：启用 `server.forward-headers-strategy=native`（仅在直连方为可信内网代理时信任 X-Forwarded-For）。
- 管理员临时密码生成保证同时包含字母与数字（修复约 19% 概率被自身复杂度校验拒绝的问题）。
- 分享页 iframe 增加 `sandbox="allow-same-origin"`；前端登录态收敛为单一 localStorage 键并自动迁移历史双键数据。

## v2.3（2026-08-21）

### 新增（投递管理 / 通知中心 / 内容审核 / AI 规则 / 用户设置）

**投递管理（用户端）**
- `POST /deliveries`：创建投递记录（校验简历归属；状态机：delivered/written/interview1/interview2/hr/offer/rejected/withdrawn）。
- `GET /deliveries`：分页查询，支持 keyword/company/position/status/startDate/endDate 筛选。
- `GET /deliveries/{id}`、`PUT /deliveries/{id}`、`DELETE /deliveries/{id}`：详情 / 更新进度 / 逻辑删除。

**投递数据（管理端）**
- `GET /admin/deliveries`：全平台投递分页列表。
- `GET /admin/deliveries/stats`：总数 + 状态分布 + 热门岗位 TOP5。
- `GET /admin/deliveries/export`：CSV 导出（UTF-8 BOM）。

**通知中心（用户端）**
- `GET /notifications`：分页查询（`unreadOnly` 可选）。
- `GET /notifications/unread-count`：未读数。
- `PUT /notifications/{id}/read`、`PUT /notifications/read-all`、`DELETE /notifications/{id}`。
- 事件接入：PDF 导出完成、AI 点评完成、头像优化完成自动写入通知。

**内容审核（管理端）**
- `GET /admin/audits`：分页查询（keyword/status/riskLevel）。
- `GET /admin/audits/stats`：各状态统计 + 今日已审。
- `POST /admin/audits/{id}/approve`、`POST /admin/audits/{id}/reject`、`POST /admin/audits/{id}/mark-warning`（可选 riskLevel）。
- 简历创建 / 导入时自动生成 `pending` 审核记录。

**AI 规则管理（管理端）**
- `GET /admin/ai-rules`、`GET /admin/ai-rules/{id}`、`GET /admin/ai-rules/stats`。
- `POST /admin/ai-rules`：创建草稿（version=1，familyId=自身 ID）。
- `PUT /admin/ai-rules/{id}/draft`：保存草稿。
- `POST /admin/ai-rules/{id}/publish`：发布新版本（原生效版本停用，版本号 +1）。
- `PATCH /admin/ai-rules/{id}/status`：启停（active/disabled）。
- `GET /admin/ai-rules/{id}/versions`：版本列表。
- `POST /admin/ai-rules/{id}/rollback/{version}`：回滚到指定版本。
- `POST /admin/ai-rules/{id}/test`：测试运行（调用已配置 LLM，未配置时返回占位）。
- `DELETE /admin/ai-rules/{id}`：删除（生效中不可删）。

**用户设置**
- `PUT /users/me`：更新昵称 / 手机号 / 邮箱 / 头像（游客不可改；手机号、邮箱唯一校验）。
- `GET /users/me/preferences`、`PUT /users/me/preferences`：偏好设置读写（JSON 整体覆盖）。

**管理端用户 / 简历 / 模板增强**
- `POST /admin/users`：新增用户（邮箱必填；初始密码留空时返回一次性临时密码）。
- `GET /admin/users/export`：用户 CSV 导出（含状态 / 关键词筛选）。
- `GET /admin/resumes/{id}/preview`：管理端简历 HTML 预览（无所有权校验）。
- `GET /admin/resumes/{id}/export/markdown`、`GET /admin/resumes/{id}/export/word`：管理端导出。
- `POST /admin/resumes/{id}/export/pdf`：管理端创建 PDF 任务（任务归属简历所有者）。
- `GET /admin/pdf/tasks/{taskId}`、`GET /admin/pdf/download/{taskId}`：管理端查询 / 下载 PDF 任务。
- `GET /admin/templates/{id}/export`：模板 JSON 导出。

**数据库迁移**
- V15：`delivery_record` / `notification` / `content_audit` / `ai_rule`（familyId 版本族）/ `user_preference` 表。

---

## v2.2（2026-08-18）

### 后端加固与并发安全

- `PUT /resumes/{id}`、`PUT /resumes/{id}/title` 引入乐观锁（V13 迁移 `resume.version`）：响应新增 `version` 字段；并发编辑冲突返回 `RESUME_VERSION_CONFLICT`(2012)，HTTP 409。
- 限流策略调整：已认证请求按 userId 计数，匿名请求按 IP 计数（`RateLimitFilter`）。
- HTTP 状态映射补全：`AVATAR_SOURCE_NOT_FOUND`(4003)、`AI_TASK_NOT_FOUND`(6000) → 404；`AI_DAILY_QUOTA_EXCEEDED`(6009)、`AI_CONCURRENT_LIMIT_EXCEEDED`(6004) → 429。
- Refresh token 家族撤销（V14 迁移 `refresh_token.family_id`）：检测到令牌复用攻击时整家族失效。
- 简历删除/头像删除的 MinIO 文件清理延迟到事务提交后执行，避免回滚后 DB 与文件不一致。
- AI 异步线程池队列满时任务直接标记失败（不再降级同步执行占住请求线程）。
- PDF 导出复用共享 Chromium 实例，降低启动/销毁开销。
- CI 新增集成测试 job（MySQL/MinIO 容器 + `ResumeServiceIntegrationTest`）。

---

## v2.1（2026-08-06）

### 完善（多方式登录）

- 邮箱验证码存储升级：优先 Redis（`email-code:{email}`，多实例安全），未配置 Redis 时降级进程内存（单实例）；内存模式惰性清理过期条目。
- 验证码防爆破：连续 5 次校验失败后验证码作废。
- OAuth 回调响应增加 `Cache-Control: no-store`（防止 JWT 落入中间缓存）。
- OAuth state 存储惰性清理过期条目。
- `docs/environment.md` 补全认证配置项与密钥获取清单（Google/GitHub/QQ/SMTP）。

---

## v2.0（2026-08-06）

### 移除

- 删除 `POST /auth/register` 注册接口与 `RegisterRequest`、`VerifyCodeService`/`PlaceholderVerifyCodeService`（登录即注册，不再需要独立注册与占位验证码）。

### 调整

- `POST /auth/login`：邮箱账号不存在时自动创建（登录即注册，密码作为初始密码，需满足强度要求）；手机号不存在仍返回 `AUTH_ACCOUNT_NOT_FOUND`。
- 前端移除注册 Tab 与注册表单，登录页仅保留登录与第三方入口。

---

## v1.9（2026-08-06）

### 新增（多方式登录适配器）

- `POST /auth/login/{method}`：统一登录入口，适配器分发（`password` / `email_code` / `sms_code` 预留）。
- `GET /auth/methods`：可用登录方式与第三方配置状态（前端渲染入口）。
- `POST /auth/email-code/send`：发送邮箱登录验证码（5 分钟有效、60 秒重发间隔；未配置 SMTP 时降级日志输出验证码）。
- `POST /auth/email-code/login`：邮箱验证码免密登录，首次登录自动创建账号。
- `GET /auth/oauth/{provider}/authorize`：第三方授权页跳转（google / github / qq），state 防 CSRF。
- `GET /auth/oauth/{provider}/callback`：OAuth 回调，换 token → 拉取用户 → 登录/绑定 → 302 回前端携带 JWT。
- `user_auth` 表（V12 迁移）：第三方账号绑定（provider + account 唯一）。
- 错误码：`AUTH_EMAIL_CODE_INVALID`(1008)、`AUTH_EMAIL_CODE_SEND_FAILED`(1009)、`AUTH_OAUTH_NOT_CONFIGURED`(1010)、`AUTH_OAUTH_EXCHANGE_FAILED`(1011)、`AUTH_SMS_CODE_NOT_AVAILABLE`(1012)、`AUTH_EMAIL_CODE_TOO_FREQUENT`(1013)。
- 配置：`app.auth.email-code.*`、`app.auth.oauth.*`（GOOGLE_CLIENT_ID / GITHUB_CLIENT_ID / QQ_APP_ID 等环境变量）、`app.auth.smtp.*`。

---

## v1.8（2026-08-06）

### 新增

- `POST /resumes/import`：导入简历并直接创建。支持 `format=json`（模块数组或 `{sections:[...]}`）与 `format=markdown`（按 `##` 模块标题自动识别教育/工作/项目/技能/自我介绍等类型，未知标题映射为自定义模块）。
- 错误码：`RESUME_IMPORT_INVALID`(2011)。

---

## v1.7（2026-08-06）

### 新增

- `POST /resumes/{id}/share` 支持请求体 `hideContact`（分享页隐藏手机号/邮箱/个人链接）与 `expiresAt`（自定义过期时间，晚于当前时间，缺省永久）；响应新增 `hideContact` 字段。
- `resume_share` 表新增 `hide_contact` 字段（V11 迁移）；分享页渲染支持隐藏联系方式选项。

---

## v1.6（2026-08-06）

### 新增

- AI 写作按日配额：游客 3 次/天、登录用户 30 次/天（`app.ai.daily-quota.guest/user` 可配置），`ai_daily_quota` 表（V10 迁移）持久化计数，同步与流式接口均受配额约束。
- 错误码：`AI_DAILY_QUOTA_EXCEEDED`(6009)。
- `POST /resumes/{id}/ai/write` 与 `POST /resumes/{id}/ai/write/stream` 依据 JWT `guest` claim 区分配额上限。

---

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
