# 智能简历生成工具 — 项目级 Codex 约束

> 作用：为进入本项目的所有开发 Agent 提供全局上下文、模块地图、开发流程与不可违背的约定。
> 范围：根目录 / 全项目。
> 必读：本文件 + 你将要修改的模块的 `CLAUDE.md`（如存在）。

---

## 1. 项目定位

**智能简历生成工具**是一个前后端分离的 Web 应用：

- 用户可在线创建、编辑、预览、导出 PDF 简历。
- 支持手机号/邮箱注册、游客模式、JWT 鉴权。
- 支持头像上传与一寸照优化（当前为 P0 占位实现）。
- 支持模板后台化管理。
- 支持 AI 简历点评、JD 优化和编辑器内 AI 写作，未配置供应商时回退到占位结果。
- 支持简历公开分享，以及 PDF、Word、Markdown 多格式导出。

---

## 2. 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| 对象存储 | MinIO（兼容 S3） |
| 安全 | Spring Security + JWT（JJWT 0.12.5） |
| PDF 生成 | Playwright 1.43.0 |
| 前端 | Vue 3 + Vite + TypeScript + Pinia + Element Plus + Tailwind CSS |
| 前端运行时 | Node.js 20.19+ |
| 测试 | 后端：JUnit 5 + H2，集成测试使用 Docker Compose MySQL/MinIO；前端：Vitest + Playwright |

---

## 3. 项目结构

```
ResumeGeneeration/
├── backend/                    # Spring Boot 单体后端
│   ├── src/main/java/com/resume/
│   │   ├── common/             # 全局基础设施、异常、响应、安全、MinIO
│   │   ├── user/               # 用户、认证、JWT
│   │   ├── resume/             # 简历 CRUD、预览、分享、Word/Markdown 导出
│   │   ├── template/           # 模板查询、后台模板管理
│   │   ├── avatar/             # 头像上传、一寸照优化任务
│   │   ├── pdf/                # PDF 导出任务
│   │   └── ai/                 # 多厂商 AI 路由、点评、优化与写作
│   └── src/main/resources/db/migration/
│       ├── V1__init.sql        # Flyway 初始迁移脚本
│       ├── V8__resume_share.sql # 分享功能迁移
│       └── V9__resume_render_settings.sql # 排版与一页适配设置迁移
├── frontend/                   # Vue 3 前端
│   ├── src/api/                # 按模块封装的 Axios 接口
│   ├── src/components/         # 公共组件、编辑器组件、预览组件
│   ├── src/composables/        # 组合式函数（如 useAutoSave）
│   ├── src/router/             # Vue Router
│   ├── src/stores/             # Pinia stores
│   ├── src/types/              # TypeScript 类型
│   ├── src/utils/              # 工具函数
│   └── src/views/              # 页面视图
└── docs/                       # 需求、设计、API、ADR、指南
    ├── 需求PRD-v1.md
    ├── setup-guide.md
    ├── environment.md
    ├── security-guide.md
    ├── api-changelog.md
    ├── traceability-matrix.md
    ├── adr/
    └── superpowers/specs/      # SDD、API 规范、数据模型、校验规则等
```

---

## 4. 全局约定（任何模块都必须遵守）

### 4.1 接口响应格式

后端所有 HTTP 接口统一返回 `R<T>`：

```json
{
  "code": 200,
  "message": "ok",
  "data": { ... }
}
```

- `code = 200` 表示成功；非 200 表示业务错误，详见 `docs/superpowers/specs/2026-07-03-validation-rules.md` §13。
- HTTP 状态码与业务错误码（`R.code`）均返回：200 表示成功；参数校验失败 400；未认证 401；无权限 403；未找到 404；资源冲突 409；限流 429；服务器内部错误 500。具体映射见 `GlobalExceptionHandler.resolveHttpStatus`。

### 4.2 认证

- 使用 JWT access token，前端通过 `Authorization: Bearer <token>` 传递。
- Token 由 `/auth/login`、`/auth/register`、`/auth/guest` 返回。
- `/auth/**`、`/actuator/health`、`GET /templates`、`GET /templates/{id}`、`GET /share/{token}` 可匿名访问，其余接口需认证；`/admin/**` 需要管理员角色。

### 4.3 逻辑删除

- 所有业务表必须包含 `deleted` 字段（TINYINT）。
- MyBatis-Plus 全局逻辑删除配置：`logic-delete-field: deleted`，`logic-delete-value: 1`，`logic-not-delete-value: 0`。
- 删除接口不得物理删除，只能设置 `deleted = 1`。

### 4.4 数据库存储

- 文件（头像、PDF、模板缩略图）只存 URL，实际文件存 MinIO。
- 简历内容 `resume.sections` 以 JSON 数组存储，必须通过 TypeHandler 映射为强类型集合，禁止以 `String` 手动拼接/解析。

### 4.5 错误处理

- 业务异常统一抛出 `BusinessException`，由 `GlobalExceptionHandler` 转换为 `R`。
- 不得在 Controller 中直接返回 `new ResponseEntity(...)` 等自定义结构。

### 4.6 测试

- 后端新增功能必须先写测试（TDD）。
- 单元测试使用 H2 内存数据库；集成测试使用 `ops/docker-compose.test.yml` 提供的 MySQL/MinIO。
- 前端新增组件/页面需补充 Vitest 单元/组件测试。

---

## 5. 模块间依赖关系

```
common
├── 被 user、resume、template、avatar、pdf、ai、audit、delivery、notification 依赖
├── 提供：R、异常、JWT 过滤器、SecurityConfig、MinIO 服务、全局异常处理

user
├── 被 resume、avatar、pdf、delivery、notification 依赖（通过 userId 关联、JWT 解析）
├── 提供：User 实体、JWT Token 生成、当前用户上下文

resume
├── 依赖：common、user、template
├── 被 pdf、avatar、ai、audit、delivery 依赖；内部包含 share 子包
├── 提供：Resume 实体、简历 CRUD、Section 校验、预览、分享和多格式导出

template
├── 依赖：common
├── 被 resume（简历引用 templateId）、pdf（渲染使用模板）依赖
├── 提供：Template 实体、模板列表、后台模板 CRUD

avatar
├── 依赖：common、user、resume（可选回填）、ai（异步任务）
├── 提供：头像上传、一寸照优化任务

pdf
├── 依赖：common、user、resume、template、notification（导出完成事件）
├── 提供：PDF 导出任务、下载

ai
├── 依赖：common、resume、notification（任务完成事件）
├── 提供：多厂商 LLM 路由、AI 点评、JD 优化、头像优化和行内写作

audit
├── 依赖：common
├── 被 resume 依赖（创建/导入简历时自动生成审核记录）
├── 提供：内容审核服务（admin 端审核 / 通过 / 标记警告）

delivery
├── 依赖：common、user、resume（校验简历归属）
├── 提供：投递记录管理、admin 端统计与 CSV 导出

notification
├── 依赖：common
├── 被 pdf、ai 依赖（事件接收方，无下游业务依赖）
├── 提供：通知中心（未读数 / 已读 / 删除）
```

**依赖原则**：

- 不允许循环依赖。
- 下层模块（common）不得依赖上层模块（user/resume 等）。
- 跨模块调用优先通过 Service 层接口，禁止直接操作其他模块的 Mapper。

---

## 6. 开发流程

1. **读文档**：先读本文件 + 目标模块 `CLAUDE.md`（如存在）+ 相关 `docs/superpowers/specs/*.md`。
2. **读代码**：了解目标模块现有 Controller / Service / Mapper / DTO / Entity 骨架。
3. **写测试**：按 `docs/superpowers/specs/2026-07-03-tdd-test-plan.md` 补充测试用例。
4. **实现功能**：遵循模块约束与全局约定。
5. **运行测试**：
   - 后端：使用 JDK 17 执行 `mvn test`
   - 后端集成测试：设置 `RUN_INTEGRATION_TESTS=true`，并先启动 `ops/docker-compose.test.yml`
   - 前端：`npm run test:unit`
6. **更新追溯矩阵**：如修改 `docs/traceability-matrix.md` 中对应行的代码位置。

---

## 7. 关键设计文档速查

| 主题 | 文档 |
|---|---|
| 产品需求 | `docs/需求PRD-v1.md` |
| 系统设计 | `docs/superpowers/specs/2026-07-03-resume-generation-design.md` |
| 范围对齐 | `docs/superpowers/specs/2026-07-03-scope-alignment.md` |
| 数据模型与 DDL | `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` |
| 校验规则与错误码 | `docs/superpowers/specs/2026-07-03-validation-rules.md` |
| API 规范 | `docs/superpowers/specs/2026-07-03-api-spec.md` |
| 模板系统 | `docs/superpowers/specs/2026-07-03-template-system-spec.md` |
| TDD 测试计划 | `docs/superpowers/specs/2026-07-03-tdd-test-plan.md` |
| 环境搭建 | `docs/setup-guide.md` |
| 环境变量 | `docs/environment.md` |
| 安全合规 | `docs/security-guide.md` |
| API 变更日志 | `docs/api-changelog.md` |
| 需求追溯矩阵 | `docs/traceability-matrix.md` |

---

## 8. 常用命令

```bash
# 后端
mvn clean test                  # 运行后端测试
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd frontend
npm install
npm run dev                     # 开发服务器
npm run test:unit               # 单元/组件测试
npm run test:e2e                # E2E 测试
npm run build                   # 生产构建
```

---

## 9. 开发红线

- 禁止物理删除业务数据。
- 禁止在业务代码中硬编码密钥、密码、Bucket 名（使用配置）。
- 禁止将文件内容以 BLOB 存入数据库。
- 禁止绕过统一响应 `R<T>` 自定义返回结构。
- 禁止跨模块直接调用 Mapper。
- 禁止手动拼接/解析 `resume.sections` JSON。

---

## 10. 相关工程约束文件

- `backend/CLAUDE.md`
- `frontend/CLAUDE.md`
- `backend/src/main/java/com/resume/{module}/CLAUDE.md`（按你工作的模块选择）
