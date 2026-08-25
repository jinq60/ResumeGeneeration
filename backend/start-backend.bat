@echo off
set "JAVA_HOME=D:\Java\jdk-17.0.12"
set "Path=%JAVA_HOME%\bin;%Path%"

set "MYSQL_USERNAME=resume"
set "MYSQL_PASSWORD=ResumeDev123"
REM MinIO 凭据禁止硬编码（AGENTS.md §9）：从环境变量 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 读取，
REM 缺失时跳过导出（后端将回退到 application-dev.yml 配置的本地默认值）
if defined MINIO_ROOT_USER if defined MINIO_ROOT_PASSWORD (
    set "MINIO_ACCESS_KEY=%MINIO_ROOT_USER%"
    set "MINIO_SECRET_KEY=%MINIO_ROOT_PASSWORD%"
) else (
    echo [WARN] 未设置 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 环境变量，跳过 MinIO 凭据导出（使用配置文件默认值）。
)

set "SMTP_HOST="
set "SMTP_PORT="
set "SMTP_USERNAME="
set "SMTP_PASSWORD="
set "SMTP_SSL_ENABLE=false"

set "AUTH_OAUTH_BASE_URL=http://localhost:8080/api"
set "AUTH_OAUTH_FRONTEND_REDIRECT=http://localhost:5173/login"
set "CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost"

cd /d "E:\javaProject\ResumeGeneeration\backend"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
