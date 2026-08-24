@echo off
set "JAVA_HOME=D:\Java\jdk-17.0.12"
set "Path=%JAVA_HOME%\bin;%Path%"

set "MYSQL_USERNAME=resume"
set "MYSQL_PASSWORD=ResumeDev123"
set "JWT_SECRET=5fzzGoz+qpaerr2vZtQ3JTokEkMk7hXAiXAbZC2wa7LzONMYAoEYY4FvKTEe46ZB"
set "MINIO_ACCESS_KEY=minioadmin"
set "MINIO_SECRET_KEY=minioadmin123"

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
