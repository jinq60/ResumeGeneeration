#!/bin/bash
set -a
source /opt/resume-generation/.env
set +a

export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT:-3306}/${MYSQL_DATABASE:-resume_generation}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai"
export SPRING_DATASOURCE_USERNAME="${MYSQL_USER:-resume}"
export SPRING_DATASOURCE_PASSWORD="${MYSQL_PASSWORD:-ResumeDev123}"

export APP_JWT_SECRET="${JWT_SECRET}"
export APP_CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:5173}"

export APP_MINIO_ENDPOINT="http://127.0.0.1:${MINIO_API_PORT:-9000}"
export APP_MINIO_ACCESS_KEY="${MINIO_ROOT_USER:-minioadmin}"
export APP_MINIO_SECRET_KEY="${MINIO_ROOT_PASSWORD:-minioadmin123}"
export APP_MINIO_BUCKET_AVATARS="${APP_MINIO_BUCKET_AVATARS:-resume-avatars}"
export APP_MINIO_BUCKET_PDFS="${APP_MINIO_BUCKET_PDFS:-resume-pdfs}"
export APP_MINIO_BUCKET_TEMPLATES="${APP_MINIO_BUCKET_TEMPLATES:-resume-templates}"

cd /opt/resume-generation/app/backend
exec java -jar target/resume-generation-0.0.1-SNAPSHOT.jar "$@"
