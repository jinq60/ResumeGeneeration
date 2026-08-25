$env:JAVA_HOME = "D:\Java\jdk-17.0.12"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:MYSQL_USERNAME = "resume"
$env:MYSQL_PASSWORD = "ResumeDev123"
# MinIO 凭据禁止硬编码（AGENTS.md §9）：从环境变量 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 读取，
# 缺失时跳过导出（后端将回退到 application-dev.yml 配置的本地默认值）
if ($env:MINIO_ROOT_USER -and $env:MINIO_ROOT_PASSWORD) {
    $env:MINIO_ACCESS_KEY = $env:MINIO_ROOT_USER
    $env:MINIO_SECRET_KEY = $env:MINIO_ROOT_PASSWORD
} else {
    Write-Host "[WARN] 未设置 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 环境变量，跳过 MinIO 凭据导出（使用配置文件默认值）。"
}
# 本地开发：清空 SMTP 配置，让邮箱验证码降级为日志输出，便于前端联调
$env:SMTP_HOST = ""
$env:SMTP_PORT = ""
$env:SMTP_USERNAME = ""
$env:SMTP_PASSWORD = ""
$env:SMTP_SSL_ENABLE = "false"
$env:AUTH_OAUTH_BASE_URL = "http://localhost:8080/api"
$env:AUTH_OAUTH_FRONTEND_REDIRECT = "http://localhost:5173/login"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://localhost"

mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
