# 智能简历生成工具

基于 Vue 3 + Spring Boot 的在线简历生成工具。

## 项目状态

- **当前基线**：`develop` 分支已完成业务线 v1.1，包含 AI 行内写作、简历公开分享、PDF/Word/Markdown 多格式导出，以及前端设计令牌和编辑器两栏布局收敛。
- **后端**：核心 P0 能力与 v1.1 接口已实现，覆盖认证、简历 CRUD、模板、头像、PDF 导出、AI 点评/优化/写作/语法检查、富文本渲染、分享、Word/Markdown 导出和后台管理。JDK 17 下 `174` 个测试总数 0 失败，其中 5 个外部 MySQL 集成测试默认受环境变量控制；启用测试环境后集成测试 `5/5` 通过。
- **前端**：官网、用户工作台、编辑器、模板中心、导出、AI、分享、头像和后台管理页面已接入真实接口；编辑器已集成表单、富文本、流式 AI 写作、自动保存、撤销/重做、快捷键、真实缩放、分页导航和本地草稿恢复。当前单元/组件测试 `36/36` 通过，TypeScript 检查、ESLint 和生产构建均通过。
- **CI/CD**：GitHub Actions 已启用，覆盖 `production`、`develop`、`stable`。
- **分支模型**：`production`（默认）/ `develop` / `stable`，详见 `docs/development-workflow.md`。
- **测试环境**：`docker compose -f ops/docker-compose.test.yml -p resume-test up -d`，MySQL 暴露 `3307`，MinIO API 暴露 `9002`。
- **当前边界**：头像真实 AI 优化、真实短信/邮件验证码、分享隐私字段控制和导入/富文本编辑仍属于后续迭代；未配置 AI Key 时按设计回退到占位结果。AI 写作按日配额（游客 3/天、登录 30/天）已启用。

## 项目结构

```
ResumeGeneeration/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── docs/             # 需求与设计文档
├── ops/              # Docker Compose 与部署脚本
├── AGENTS.md         # 项目级开发约束
└── README.md
```

## 快速开始

详见 [`docs/setup-guide.md`](docs/setup-guide.md)。

### 环境要求

- Java 17+
- Node.js 20.19+
- MySQL 8.0+ / Docker
- MinIO / Docker

### 测试环境（Docker）

```bash
docker compose -f ops/docker-compose.test.yml -p resume-test up -d
```

### 后端启动

```bash
cd backend
$env:JAVA_HOME="D:\Java\jdk-17.0.12"  # 必须使用 JDK 17+
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:JWT_SECRET="your-base64-secret"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

开发配置默认连接 MySQL `localhost:3306`、MinIO `localhost:9000`；若使用测试 Docker Compose，请改用 `integration` 配置，避免端口不一致。

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 运行测试

```bash
# 后端（必须使用 JDK 17）
cd backend
$env:JAVA_HOME="D:\Java\jdk-17.0.12"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn clean test

# 后端集成测试（需先启动 Docker）
$env:RUN_INTEGRATION_TESTS="true"
mvn test -Dtest="com.resume.resume.service.ResumeServiceIntegrationTest"

# 前端
cd ..\frontend
npm run test:unit
npm run build
```

## 设计文档

- `docs/需求PRD-v1.md`：产品需求文档
- `docs/superpowers/specs/2026-07-03-resume-generation-design.md`：系统设计文档（SDD）
- `docs/superpowers/specs/2026-07-03-scope-alignment.md`：P0 范围与模型对齐书
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`：数据模型与 DDL（v1.1）
- `docs/superpowers/specs/2026-07-03-validation-rules.md`：字段校验规则（v1.1）
- `docs/superpowers/specs/2026-07-03-api-spec.md`：API 接口规范（v1.4）
- `docs/superpowers/specs/2026-07-03-template-system-spec.md`：模板系统规范
- `docs/superpowers/specs/2026-07-03-tdd-test-plan.md`：TDD 测试计划
- `docs/setup-guide.md`：环境搭建与运行指南
- `docs/environment.md`：环境变量说明
- `docs/security-guide.md`：安全与隐私合规指南
- `docs/api-changelog.md`：API 变更日志
- `docs/development-workflow.md`：分支模型、CI/CD 与开发流程
- `docs/adr/`：架构决策记录（ADR）
- `docs/traceability-matrix.md`：需求追溯矩阵

## 开发规范

- 后端采用 Java 17 + Spring Boot 3.x，模块包结构为 `com.resume.{module}`
- 前端采用 Vue 3 Composition API + TypeScript + Pinia + Tailwind CSS
- 所有接口遵循 `docs/superpowers/specs/2026-07-03-api-spec.md`
- 新增功能需先补充测试用例，遵循 TDD 流程
