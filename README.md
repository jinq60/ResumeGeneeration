# 智能简历生成工具

基于 Vue 3 + Spring Boot 的在线简历生成工具。

## 项目状态

- **设计文档**：v1.1 已对齐，见 [设计文档](#设计文档)。
- **后端完成度**：认证、简历、模板、PDF 导出等 P0 核心模块已实现，`mvn test` 74 个用例通过；AI 点评/优化、后台管理为占位或部分实现。
- **前端完成度**：P0 约 25%，核心页面待实现；lint / build / unit tests 已通过。
- **CI/CD**：GitHub Actions 已启用，覆盖 `production`、`develop`、`stable`。
- **分支模型**：`production`（默认）/ `develop` / `stable`，详见 `docs/development-workflow.md`。
- **下一步**：基于开发流程进入功能开发 / 服务器部署初始化。

## 项目结构

```
ResumeGeneeration/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── docs/             # 需求与设计文档
├── task_plan.md      # 任务计划
├── findings.md       # 研究发现与决策
└── progress.md       # 进度日志
```

## 设计文档

- `docs/需求PRD-v1.md`：产品需求文档
- `docs/superpowers/specs/2026-07-03-resume-generation-design.md`：系统设计文档（SDD）
- `docs/superpowers/specs/2026-07-03-scope-alignment.md`：P0 范围与模型对齐书
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`：数据模型与 DDL（v1.1）
- `docs/superpowers/specs/2026-07-03-validation-rules.md`：字段校验规则（v1.1）
- `docs/superpowers/specs/2026-07-03-api-spec.md`：API 接口规范（v1.1）
- `docs/superpowers/specs/2026-07-03-template-system-spec.md`：模板系统规范
- `docs/superpowers/specs/2026-07-03-tdd-test-plan.md`：TDD 测试计划
- `docs/setup-guide.md`：环境搭建与运行指南
- `docs/environment.md`：环境变量说明
- `docs/security-guide.md`：安全与隐私合规指南
- `docs/api-changelog.md`：API 变更日志
- `docs/development-workflow.md`：分支模型、CI/CD 与开发流程
- `docs/adr/`：架构决策记录（ADR）
- `docs/traceability-matrix.md`：需求追溯矩阵

## 快速开始

详见 [`docs/setup-guide.md`](docs/setup-guide.md)。

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+（开发环境）
- MinIO（开发环境，对象存储）

### 后端启动

```bash
cd backend
# 使用 Java 17
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 运行测试

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

## 开发规范

- 后端采用 Java 17 + Spring Boot 3.x，模块包结构为 `com.resume.{module}`。
- 前端采用 Vue 3 Composition API + TypeScript + Pinia。
- 所有接口遵循 `docs/superpowers/specs/2026-07-03-api-spec.md`。
- 新增功能需先补充测试用例，遵循 TDD 流程。
