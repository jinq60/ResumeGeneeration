# 环境变量说明

> 版本：v1.0  
> 日期：2026-07-07  
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
| `jwt.secret` | `JWT_SECRET` | — | Base64 编码密钥，≥ 256 bit |
| `jwt.access-token-expiration` | `JWT_ACCESS_TOKEN_EXPIRATION` | `3600000` | Access Token 有效期（毫秒） |
| `jwt.refresh-token-expiration` | `JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` | Refresh Token 有效期（毫秒） |

### 1.3 MinIO（对象存储）

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `minio.endpoint` | `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO 服务端点 |
| `minio.access-key` | `MINIO_ACCESS_KEY` | — | Access Key |
| `minio.secret-key` | `MINIO_SECRET_KEY` | — | Secret Key |
| `minio.buckets.avatars` | `MINIO_BUCKET_AVATARS` | `resume-avatars` | 头像 Bucket |
| `minio.buckets.pdfs` | `MINIO_BUCKET_PDFS` | `resume-pdfs` | PDF Bucket |
| `minio.buckets.templates` | `MINIO_BUCKET_TEMPLATES` | `resume-templates` | 模板 Bucket |

### 1.4 服务端口号

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` | HTTP 端口 |
| `management.server.port` | `MANAGEMENT_SERVER_PORT` | `8080` | Actuator 端口（与主端口一致） |

### 1.5 日志级别

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `logging.level.com.resume` | `LOGGING_LEVEL_COM_RESUME` | `INFO` | 应用日志级别 |

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

- [ ] 生产环境 `jwt.secret` 已替换为随机强密钥。
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
