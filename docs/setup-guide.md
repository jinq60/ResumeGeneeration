# 环境搭建与运行指南

> 本地开发环境搭建步骤。

## 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+（开发环境）
- MinIO（开发环境，对象存储）

## 后端启动

```bash
cd backend
# 使用 Java 17
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run
```

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

## 运行测试

```bash
# 后端测试
cd backend
mvn test

# 前端单元测试
cd frontend
npm run test:unit

# 前端 E2E 测试
cd frontend
npm run test:e2e
```

## 待补充

- 数据库初始化脚本
- MinIO 配置说明
- 环境变量清单
