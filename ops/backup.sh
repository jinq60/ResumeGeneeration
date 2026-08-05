#!/usr/bin/env bash
# ============================================================
# 智能简历生成工具 - 备份脚本
# 用法: ./backup.sh [备份目录]
#   默认备份目录: /opt/resume-generation/backups
# 说明:
#   - MySQL: 逻辑备份 (mysqldump), 保留最近 N 份
#   - MinIO: 使用 mc 镜像数据到备份目录 (对象级拷贝)
#   - 建议配合 crontab 每日执行: 0 2 * * * /opt/resume-generation/app/ops/backup.sh
# 恢复:
#   - MySQL:  mysql -u<user> -p resume_generation < resume_YYYYmmdd_HHMMSS.sql
#   - MinIO:  mc mirror --overwrite resume-backup/resume-minio-data  / 见 maintenance-guide.md
# ============================================================
set -euo pipefail

BACKUP_ROOT="${1:-/opt/resume-generation/backups}"
DATE=$(date +%Y%m%d_%H%M%S)
KEEP_N=7
COMPOSE_DIR="/opt/resume-generation/app/ops"

MYSQL_CONTAINER="resume-mysql"
MYSQL_USER="${MYSQL_USER:-resume}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-resume_generation}"

MINIO_SRC="resume-minio-data"
MINIO_ALIAS="resume-local"

mkdir -p "${BACKUP_ROOT}/mysql" "${BACKUP_ROOT}/minio"

log() { echo "[$(date '+%F %T')] $*"; }

# 1. MySQL 备份
log "Backing up MySQL database: ${MYSQL_DATABASE}"
docker exec "${MYSQL_CONTAINER}" sh -c \
  "exec mysqldump --single-transaction --routines --triggers \
     -u\"${MYSQL_USER}\" -p\"${MYSQL_PASSWORD}\" \"${MYSQL_DATABASE}\"" \
  > "${BACKUP_ROOT}/mysql/${MYSQL_DATABASE}_${DATE}.sql"
log "MySQL backup done: ${BACKUP_ROOT}/mysql/${MYSQL_DATABASE}_${DATE}.sql"

# 2. MinIO 备份（对象镜像到本地目录）
log "Backing up MinIO data"
# 宿主机需安装 mc:  https://min.io/docs/minio/linux/reference/minio-mc.html
if command -v mc >/dev/null 2>&1; then
  MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
  MINIO_ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
  MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-}"
  mc alias set "${MINIO_ALIAS}" "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null
  mc mirror --overwrite "${MINIO_ALIAS}" "${BACKUP_ROOT}/minio/${DATE}"
  log "MinIO backup done: ${BACKUP_ROOT}/minio/${DATE}"
else
  log "WARN: 'mc' not found, skipping MinIO backup. Install mc or back up the docker volume directly."
fi

# 3. 清理过期备份（保留最近 KEEP_N 份）
log "Cleaning up backups older than ${KEEP_N} copies"
find "${BACKUP_ROOT}/mysql" -name "*.sql" -type f | sort | head -n -${KEEP_N} | xargs -r rm -f
find "${BACKUP_ROOT}/minio" -maxdepth 1 -mindepth 1 -type d | sort | head -n -${KEEP_N} | xargs -r rm -rf

log "Backup completed: ${BACKUP_ROOT}"
