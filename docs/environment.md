# 环境变量说明

> 记录后端、前端及基础设施所需的环境变量。

## 后端

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `JAVA_HOME` | JDK 17 安装路径 | `/usr/lib/jvm/java-17` |
| `SPRING_PROFILES_ACTIVE` | Spring 运行环境 | `dev` |

## 数据库与对象存储

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/resume` |
| `MINIO_ENDPOINT` | MinIO 服务地址 | `http://localhost:9000` |

## 前端

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `VITE_API_BASE_URL` | 后端 API 地址 | `http://localhost:8080` |

## 待补充

- 具体配置项与默认值
- 敏感变量（密码、密钥）管理规范
