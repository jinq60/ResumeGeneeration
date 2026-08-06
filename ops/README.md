# 服务器部署运维说明

该目录包含在 `101.43.117.17`（或任意 Ubuntu 20.04+ 服务器）上以 **Docker 容器** 方式运行 `ResumeGeneeration` 的运维文件。

## 文件说明

| 文件 | 作用 |
|---|---|
| `docker-compose.server.yml` | 编排 MySQL 8.0、MinIO、后端、前端四个服务。 |
| `.env.example` | 环境变量示例，复制到 `/opt/resume-generation/.env` 并替换真实值。 |

## 前置条件

服务器需安装：
- Docker 24.x + Docker Compose V2
-（可选，仅用于本地构建验证）Node.js 20.19+

> 后端依赖 JDK 17 / Maven 已包含在 `backend/Dockerfile` 的构建镜像中，无需在宿主机安装。

## 本地/首次手动启动

```bash
# 1. 准备环境变量
mkdir -p /opt/resume-generation
cp ops/.env.example /opt/resume-generation/.env
# 编辑 /opt/resume-generation/.env，替换 JWT_SECRET、密码等

# 2. 构建前端 dist（后端 jar 由后端 Dockerfile 在容器内构建）
cd frontend && npm ci && npm run build

# 3. 启动所有服务（在仓库根目录或 ops 目录均可）
cd ops
docker compose -f docker-compose.server.yml up -d --build
```

## GitHub Actions 自动部署

`.github/workflows/deploy.yml` 会在 `production` 分支推送或手动触发时执行：

1. 前端 `npm ci && npm run build`
2. 通过 SCP 把后端源码、`dist` 与 Docker/Compose 文件上传到服务器
3. 后端 `Dockerfile` 在容器内执行 `mvn package` 并安装 Playwright Chromium
4. 在服务器上执行 `docker compose up -d --build`

需要先在仓库 `Settings → Secrets and variables → Actions` 中配置：

| Secret | 说明 |
|---|---|
| `SSH_HOST` | 服务器 IP 或域名 |
| `SSH_USERNAME` | 登录用户名（如 `ubuntu`） |
| `SSH_PRIVATE_KEY` | 服务器 SSH 私钥全文 |
| `SSH_PORT` | SSH 端口，默认 22 |

> 注意：服务器必须能从 GitHub Actions  runner 通过 SSH 访问；同时服务器上需提前放置好 `/opt/resume-generation/.env`。

## 访问地址

| 服务 | URL |
|---|---|
| 前端 | http://`<服务器IP>` |
| 后端 API | http://`<服务器IP>`/api 或 http://`<服务器IP>`:8080/api |
| MinIO 控制台 | http://`<服务器IP>`:9001 |
| MySQL | `<服务器IP>`:3306 |

## 常用运维命令

```bash
cd /opt/resume-generation/app/ops

# 查看状态
docker compose -f docker-compose.server.yml ps

# 查看日志
docker compose -f docker-compose.server.yml logs -f backend
docker compose -f docker-compose.server.yml logs -f frontend

# 重启后端/前端
docker compose -f docker-compose.server.yml up -d --build backend
docker compose -f docker-compose.server.yml up -d --build frontend

# 停止全部
docker compose -f docker-compose.server.yml down

# 停止并删除数据卷（危险）
docker compose -f docker-compose.server.yml down -v
```

## 生产建议

- 使用 HTTPS + 域名，并在 `nginx.conf` 中配置反向代理与 SSL。
- 将 `JWT_SECRET`、数据库密码等真实值存入服务器 `/opt/resume-generation/.env`，不要提交到仓库。
- 数据库和 MinIO 数据已挂载到命名卷 `resume-mysql-data`、`resume-minio-data`，`docker compose down` 不会丢失。
