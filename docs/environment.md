# 环境变量说明

> 版本：v1.1
> 日期：2026-08-05
> 作用：列出项目运行所需的全部环境变量与配置文件项，便于部署和排查。

---

## 1. 后端环境变量

后端配置统一通过 `application.yml` / `application-dev.yml` / `application-prod.yml` 管理，支持 Spring Boot 标准外部化配置。

### 1.1 数据库

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/resume_generation` | MySQL 连接串 |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `root` | 数据库用户名 |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | — | 数据库密码 |
| `spring.datasource.driver-class-name` | — | `com.mysql.cj.jdbc.Driver` | 驱动类 |

### 1.2 JWT

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `app.jwt.secret` | `JWT_SECRET`（dev）/ `APP_JWT_SECRET`（prod） | — | Base64 编码密钥，≥ 256 bit |
| `app.jwt.access-token-expiration` | — | `3600000` | Access Token 有效期（毫秒） |
| `app.jwt.refresh-token-expiration` | — | `604800000` | Refresh Token 有效期（毫秒） |

### 1.3 MinIO（对象存储）

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `app.minio.endpoint` | `MINIO_ENDPOINT`（dev）/ `APP_MINIO_ENDPOINT`（prod） | `http://localhost:9000` | MinIO 服务端点 |
| `app.minio.access-key` | `MINIO_ACCESS_KEY`（dev）/ `APP_MINIO_ACCESS_KEY`（prod） | — | Access Key |
| `app.minio.secret-key` | `MINIO_SECRET_KEY`（dev）/ `APP_MINIO_SECRET_KEY`（prod） | — | Secret Key |
| `app.minio.buckets.avatars` | `APP_MINIO_BUCKET_AVATARS`（prod） | `resume-avatars` | 头像 Bucket |
| `app.minio.buckets.pdfs` | `APP_MINIO_BUCKET_PDFS`（prod） | `resume-pdfs` | PDF Bucket |
| `app.minio.buckets.templates` | `APP_MINIO_BUCKET_TEMPLATES`（prod） | `resume-templates` | 模板 Bucket |

### 1.4 服务端口号

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` | HTTP 端口 |
| `management.server.port` | `MANAGEMENT_SERVER_PORT` | `8080` | Actuator 端口（与主端口一致） |

### 1.5 日志级别

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `logging.level.com.resume` | — | dev `DEBUG` / prod `INFO` | 应用日志级别 |

### 1.6 认证、AI 与跨域配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS`（dev）/ `APP_CORS_ALLOWED_ORIGINS`（prod） | `http://localhost:5173` | 允许的前端来源，逗号分隔 |
| `app.auth.verify-code.mode` | `VERIFY_CODE_MODE` | `placeholder` | 已随注册功能移除，保留配置项向后兼容 |
| `app.admin.bootstrap.phone` | `ADMIN_BOOTSTRAP_PHONE` | 空 | 首个管理员引导手机号 |
| `app.admin.bootstrap.password` | `ADMIN_BOOTSTRAP_PASSWORD` | 空 | 首个管理员引导密码 |
| `app.render.public-base-url` | `APP_PUBLIC_BASE_URL` | 空 | PDF/分享渲染时的外部访问前缀 |
| `app.ai.*` | `OPENAI_API_KEY`、`DASHSCOPE_API_KEY`、`ERNIE_API_KEY` 等 | 空 | AI 供应商配置；未配置时按功能降级 |

### 1.7 多方式登录（邮箱验证码 / 第三方 OAuth / SMTP）

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `app.auth.email-code.ttl-minutes` | — | `5` | 邮箱验证码有效期（分钟） |
| `app.auth.email-code.resend-interval-seconds` | — | `60` | 重发间隔（秒） |
| `app.auth.oauth.base-url` | `AUTH_OAUTH_BASE_URL` | `http://localhost:8080/api` | 后端对外地址，回调 = base-url + `/auth/oauth/{provider}/callback` |
| `app.auth.oauth.frontend-redirect` | `AUTH_OAUTH_FRONTEND_REDIRECT` | `http://localhost:5173/login` | OAuth 登录成功后的前端跳转地址 |
| `app.auth.oauth.google.client-id` | `GOOGLE_CLIENT_ID` | 空 | Google OAuth 客户端 ID |
| `app.auth.oauth.google.client-secret` | `GOOGLE_CLIENT_SECRET` | 空 | Google OAuth 客户端密钥 |
| `app.auth.oauth.github.client-id` | `GITHUB_CLIENT_ID` | 空 | GitHub OAuth 客户端 ID |
| `app.auth.oauth.github.client-secret` | `GITHUB_CLIENT_SECRET` | 空 | GitHub OAuth 客户端密钥 |
| `app.auth.oauth.qq.app-id` | `QQ_APP_ID` | 空 | QQ 互联 APP ID |
| `app.auth.oauth.qq.app-key` | `QQ_APP_KEY` | 空 | QQ 互联 APP KEY |
| `app.auth.smtp.host` | `SMTP_HOST` | 空 | SMTP 服务器；留空时验证码降级为日志输出（仅限开发） |
| `app.auth.smtp.port` | `SMTP_PORT` | `587` | SMTP 端口 |
| `app.auth.smtp.username` | `SMTP_USERNAME` | 空 | SMTP 账号（通常是邮箱） |
| `app.auth.smtp.password` | `SMTP_PASSWORD` | 空 | SMTP 密码/授权码 |
| `app.auth.smtp.from` | `SMTP_FROM` | 空 | 发件人地址，缺省用 username |

#### 需要获取的密钥/API 配置单

| 用途 | 需要获取 | 获取入口 | 回调地址（回填到平台） | 填到环境变量 |
|---|---|---|---|---|
| 邮箱验证码发送 | 任意邮箱的 SMTP 授权码（如 QQ 邮箱/163/Outlook 开启 SMTP 服务） | 邮箱服务商设置页 | — | `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_FROM` |
| Google 登录 | OAuth 客户端 ID + 密钥 | https://console.cloud.google.com → 凭据 → OAuth 客户端 ID（Web 应用） | `http://localhost:8080/api/auth/oauth/google/callback`（生产换成域名） | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` |
| GitHub 登录 | OAuth App Client ID + Secret | https://github.com/settings/developers → OAuth Apps | `http://localhost:8080/api/auth/oauth/github/callback` | `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` |
| QQ 扫码登录 | APP ID + APP Key（需实名开发者 + 应用审核） | https://connect.qq.com | `http://localhost:8080/api/auth/oauth/qq/callback` | `QQ_APP_ID` / `QQ_APP_KEY` |

> 部署提示：回调地址中的 `localhost:8080` 需替换为生产环境对外域名（与 `AUTH_OAUTH_BASE_URL` 一致）。

---

## 2. 前端环境变量

前端使用 Vite 构建，环境变量以 `VITE_` 开头，通过 `.env` 文件配置。

| 变量名 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` | 后端 API 基础路径（开发环境通过 Vite 代理） |
| `VITE_APP_TITLE` | `智能简历生成工具` | 页面标题 |

### 2.1 环境文件示例

`.env.development`：

```bash
VITE_API_BASE_URL=/api
VITE_APP_TITLE=智能简历生成工具(开发)
```

`.env.production`：

```bash
VITE_API_BASE_URL=https://api.yourdomain.com/api
VITE_APP_TITLE=智能简历生成工具
```

---

## 3. 安全配置检查清单

- [ ] 生产环境 `app.jwt.secret` 已替换为随机强密钥。
- [ ] 数据库密码不为默认弱密码。
- [ ] MinIO Bucket 在生产环境不公开，使用预签名 URL。
- [ ] MySQL 不对外暴露 3306 端口。
- [ ] 后端 `application-prod.yml` 中关闭详细错误堆栈。
- [ ] HTTPS 证书已配置。

---

## 4. 参考文档

- `docs/setup-guide.md`
- `docs/superpowers/specs/2026-07-03-api-spec.md`
- `backend/src/main/resources/application-dev.yml`
