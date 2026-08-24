$env:JAVA_HOME = "D:\Java\jdk-17.0.12"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:MYSQL_USERNAME = "resume"
$env:MYSQL_PASSWORD = "ResumeDev123"
$env:JWT_SECRET = "5fzzGoz+qpaerr2vZtQ3JTokEkMk7hXAiXAbZC2wa7LzONMYAoEYY4FvKTEe46ZB"
$env:MINIO_ACCESS_KEY = "minioadmin"
$env:MINIO_SECRET_KEY = "minioadmin123"
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
