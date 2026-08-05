# API 变更日志

> 记录 `docs/superpowers/specs/2026-07-03-api-spec.md` 的所有变更，便于前后端联调与版本管理。

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
