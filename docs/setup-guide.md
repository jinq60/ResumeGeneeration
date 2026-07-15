# 环境搭建与运行指南

> 版本：v1.0  
> 日期：2026-07-07  
> 作用：帮助新开发者或运维人员在一台新机器上快速跑通本项目。

---

## 1. 环境要求

### 1.1 必需软件

| 软件 | 版本 | 用途 | 下载/安装建议 |
|---|---|---|---|
| Java JDK | 17+ | 后端编译运行 | Oracle JDK 17 或 Eclipse Temurin 17 |
| Maven | 3.8+ | 后端构建 | 随 IDE 安装或官网下载 |
| Node.js | 18+ | 前端构建运行 | 使用 nvm 或官网 LTS 安装包 |
| MySQL | 8.0+ | 业务数据库 | Docker 或本地安装 |
| MinIO | 最新稳定版 | 对象存储（头像/PDF/模板缩略图） | Docker 单节点即可 |

### 1.2 推荐工具

- **IDE**：IntelliJ IDEA（后端）、VS Code（前端）。
- **API 调试**：Postman、Apifox 或 IDEA HTTP Client。
- **数据库客户端**：DataGrip、Navicat、DBeaver。

---

## 2. 项目获取

```bash
git clone <仓库地址> ResumeGeneeration
cd ResumeGeneeration
```

---

## 3. 后端启动

### 3.1 配置数据库

1. 创建 MySQL 数据库：

```sql
CREATE DATABASE resume_generation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 创建用户并授权（可选，也可使用 root）：

```sql
CREATE USER 'resume'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON resume_generation.* TO 'resume'@'%';
FLUSH PRIVILEGES;
```

### 3.2 配置 MinIO

使用 Docker 快速启动：

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name resume-minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

访问 `http://localhost:9001`，创建以下 Bucket（访问策略设为公开或配置预签名 URL）：

- `resume-avatars`
- `resume-pdfs`
- `resume-templates`

### 3.3 配置应用

复制 `backend/src/main/resources/application-dev.yml` 中的示例，修改为你的本地配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/resume_generation?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: resume
    password: your_password

jwt:
  secret: your-base64-encoded-secret-at-least-256-bits
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  buckets:
    avatars: resume-avatars
    pdfs: resume-pdfs
    templates: resume-templates
```

> **注意**：`jwt.secret` 必须是 Base64 编码，长度建议 ≥ 256 bit。生产环境务必使用随机生成的强密钥。

### 3.4 启动后端

```bash
cd backend
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后访问：

- 健康检查：`http://localhost:8080/actuator/health`
- API Base URL：`http://localhost:8080/api`

---

## 4. 前端启动

### 4.1 安装依赖

```bash
cd frontend
npm install
```

### 4.2 配置代理（开发环境）

`vite.config.ts` 已配置开发代理，将 `/api` 转发到 `http://localhost:8080`。如需修改后端地址，编辑：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

### 4.3 启动前端

```bash
npm run dev
```

默认访问：`http://localhost:5173`

---

## 5. 数据库初始化

项目使用 Flyway 管理数据库迁移。首次启动时，Flyway 会自动执行 `backend/src/main/resources/db/migration/V1__init_schema.sql` 建表并插入系统内置模板。

如需手动初始化，可执行 `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §7 中的 DDL。

---

## 6. 运行测试

### 6.1 后端测试

```bash
cd backend
export JAVA_HOME=/path/to/jdk-17
mvn test
```

### 6.2 前端单元测试

```bash
cd frontend
npm run test:unit
```

### 6.3 前端 E2E 测试

```bash
cd frontend
npm run test:e2e
```

> E2E 测试需要后端服务已启动。

---

## 7. 常见问题

### 7.1 `mvn` 提示 “无效的发行版: --release”

当前系统默认 Java 版本不是 17。请设置 `JAVA_HOME`：

```bash
export JAVA_HOME=/d/Java/jdk-17.0.12  # 根据实际路径调整
export PATH=$JAVA_HOME/bin:$PATH
```

### 7.2 前端请求报 401

确认 `Authorization` Header 中携带了有效的 `accessToken`。登录/注册/游客接口会返回 Token。

### 7.3 MinIO 文件访问 403

开发环境可将 Bucket 访问策略设为 `public`；生产环境应使用预签名 URL 或 CDN。

### 7.4 数据库表未创建

检查 `application-dev.yml` 中 MySQL 连接配置是否正确；确认 Flyway 已启用且 `db/migration/` 目录存在迁移脚本。

---

## 8. 参考文档

- `docs/superpowers/specs/2026-07-03-resume-generation-design.md`
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- `docs/superpowers/specs/2026-07-03-api-spec.md`
- `docs/environment.md`
