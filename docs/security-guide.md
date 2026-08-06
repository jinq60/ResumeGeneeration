# 安全与隐私合规指南

> 版本：v1.1
> 日期：2026-08-05
> 作用：明确项目在认证、数据保护、文件上传、XSS/CSRF、隐私删除等方面的安全要求。

---

## 1. 认证与授权

### 1.1 JWT 安全

- Access Token 有效期：1 小时；Refresh Token 有效期：7 天。
- `app.jwt.secret` 必须是 Base64 编码且长度 ≥ 256 bit，生产环境严禁使用默认密钥。
- Token 只通过 HTTPS 传输，禁止在 URL 中携带 Token。
- 前端 `accessToken` 可存内存或短期 localStorage；`refreshToken` 建议存 httpOnly cookie 或安全存储。

### 1.2 接口权限

- 除注册、登录、游客、刷新 Token、健康检查外，所有接口需 JWT 认证。
- 简历、头像、PDF 任务等数据必须校验 `user_id`，防止越权访问。
- 后台管理接口 `/admin/**` 已通过 `ROLE_ADMIN` 校验，普通用户返回 403。
- 分享页 `GET /share/{token}` 可匿名访问，但 token 必须为有效、未撤销且未过期的分享记录；响应使用 CSP、`no-store` 和 `noindex`。

---

## 2. 数据保护

### 2.1 敏感字段

- 手机号、邮箱、密码哈希等敏感数据禁止在日志中明文输出。
- 返回给前端的用户信息中不包含 `password_hash`。
- 游客数据与正式用户数据隔离，游客转正时按业务规则迁移。

### 2.2 传输安全

- 生产环境全站 HTTPS。
- 后端配置 HSTS、Secure Cookie、SameSite Cookie。

### 2.3 数据库安全

- 数据库不直接暴露公网。
- 使用最小权限原则的数据库账号。
- 密码使用 BCrypt 等强哈希算法，禁止明文存储。

---

## 3. 文件上传安全

### 3.1 头像上传

- 仅允许 JPG、PNG、WEBP 格式。
- 单文件大小 ≤ 10MB。
- 后端校验 MIME 类型和文件扩展名，防止伪装上传。
- 文件重命名存储（UUID），避免原始文件名安全风险。
- 图片存储于 MinIO，数据库只存 URL。

### 3.2 文件访问

- 开发环境 Bucket 可公开；生产环境使用预签名 URL 或 CDN + 鉴权。
- 禁止用户通过构造 URL 访问他人文件。

---

## 4. XSS 与 CSRF 防护

### 4.1 XSS

- 后端返回的文本内容需做 HTML 转义，防止反射型 XSS。
- 简历内容渲染时使用 `textContent` 或 Vue 自动转义，避免插入未过滤 HTML。
- 模板 HTML 由服务端控制，禁止用户注入脚本。

### 4.2 CSRF

- 前后端分离架构下，CSRF 风险较低；但仍建议：
  - 使用 `SameSite=Lax/Strict` Cookie。
  - 敏感操作增加验证码或二次确认。

---

## 5. 隐私删除

- 用户删除简历、头像、PDF 任务时执行逻辑删除（`deleted = 1`）。
- 逻辑删除后，数据对前端不可见，但保留在数据库中便于审计与恢复。
- 对象存储中的文件可延迟清理，由定时任务处理。
- 如需彻底删除（GDPR/个人信息保护法要求），提供物理删除接口并记录日志。

---

## 6. 安全审计

- 登录、注册、删除、导出等敏感操作记录审计日志。
- 定期轮换 JWT Secret 和数据库密码。
- 上线前进行安全扫描（依赖漏洞、越权测试、文件上传测试）。

### 6.1 审计日志（已实现）

- `audit_log` 表记录敏感操作（V7 迁移）：login / register / guest_create / logout / change_password / delete_resume / duplicate_resume / pdf_export / admin_update_role / admin_update_status / admin_reset_password / admin_delete_user。
- 写入失败不影响主业务流程（AuditLogService 内部 try/catch）。

### 6.2 登录防爆破与游客风控（已实现）

- 连续登录失败达到 `LOGIN_MAX_FAILURES`（默认 5）次后，账号临时锁定 `LOGIN_LOCKOUT_MINUTES`（默认 15）分钟（LoginAttemptGuard）。
- 每 IP 每日游客会话上限 `GUEST_MAX_PER_IP_PER_DAY`（默认 50），防止 /auth/guest 刷号（GuestAccountGuard）。

---

## 7. 已知待办（P1 排期）

- **分享隐私控制**：当前分享页默认展示简历联系方式，后续增加隐藏联系方式和自定义过期时间。
- **对象存储访问控制**：`/uploads/avatars/**` 目前为公开可访问（URL 可预测）。生产建议迁移为 MinIO 预签名 URL（短时效）或 CDN + 鉴权后公开缓存；前端需相应改造头像加载方式。
- **用户数据物理删除**：当前仅逻辑删除，数据永久保留。GDPR /《个人信息保护法》合规需要提供"注销账号 + 彻底删除"流程（含 MinIO 对象清理），需产品与法务确认删除范围后实现。
- **真实验证码服务**：`VERIFY_CODE_MODE=strict` 下注册被安全拒绝；接入短信/邮件服务实现 `VerifyCodeService` 后开放注册。

---

## 8. 参考文档

- `docs/需求PRD-v1.md` §13
- `docs/superpowers/specs/2026-07-03-api-spec.md` §3、§4
- `docs/superpowers/specs/2026-07-03-validation-rules.md` §9
- `docs/environment.md`
