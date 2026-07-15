# 服务器部署运维说明

该目录包含在 `101.43.117.17`（或任意 Ubuntu 24.04 服务器）上运行 `ResumeGeneeration` 的运维文件。

## 文件说明

- `docker-compose.server.yml`：运行 MySQL 8.0 与 MinIO。
- `.env.example`：环境变量示例，复制到服务器 `/opt/resume-generation/.env` 并替换真实值。
- `run-backend.example.sh`：启动后端的示例脚本。

## 快速启动

```bash
# 在服务器上
mkdir -p /opt/resume-generation
cp docker-compose.server.yml /opt/resume-generation/docker-compose.yml
cp .env.example /opt/resume-generation/.env   # 编辑并设置真实密码和 JWT_SECRET

cd /opt/resume-generation
docker compose up -d

# 构建后端（首次）
cd /opt/resume-generation/app/backend
mvn clean package -DskipTests

# 启动后端
nohup /opt/resume-generation/run-backend.sh > /opt/resume-generation/backend.log 2>&1 &

# 启动前端开发服务器（仅开发/测试）
cd /opt/resume-generation/app/frontend
nohup npx vite --host > /opt/resume-generation/frontend.log 2>&1 &
```

## 访问地址

| 服务 | URL |
|---|---|
| 前端（开发服务器） | http://<服务器IP>:5173 |
| 后端 API | http://<服务器IP>:8080/api |
| MinIO 控制台 | http://<服务器IP>:9001 |

> 注意：生产部署应使用 `nginx` 反向代理，并通过 HTTPS + 域名访问。本示例仅用于开发/测试。
