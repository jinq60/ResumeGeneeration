# 后端工程 — Claude 约束

> 作用：为后端开发 Agent 提供全局后端上下文、模块地图、接口约定与开发红线。
> 范围：`backend/` 目录下所有代码。
> 必读：根目录 `../AGENTS.md` + 本文件 + 你目标子模块的 `CLAUDE.md`。

---

## 1. 后端定位

后端是单 Maven 模块的 Spring Boot 单体应用，按 Java 包划分逻辑模块：

- `common`：全局基础设施
- `user`：用户与认证
- `resume`：简历核心（CRUD、预览、富文本内容、分享、Word/Markdown 导出）
- `template`：模板查询与后台管理
- `avatar`：头像上传与一寸照优化任务
- `pdf`：PDF 导出任务
- `ai`：多厂商 AI 路由、点评、JD 优化、头像优化与行内写作

---

## 2. 技术栈与版本

| 技术 | 版本 | 说明 |
|---|---|---|
| Java | 17 | 不得降级到 8/11 |
| Spring Boot | 3.2.5 | 单体应用 |
| MyBatis-Plus | 3.5.5 | 数据访问 |
| MySQL | 8.0+ | 业务数据库 |
| Flyway | 随 Spring Boot | 数据库迁移 |
| JWT | JJWT 0.12.5 | Access + Refresh Token |
| MinIO Client | 8.5.7 | 对象存储 |
| Playwright | 1.43.0 | PDF 渲染 |
| Lombok | 随 Spring Boot | 代码简化 |

---

## 3. 包结构约定

```
com.resume.{module}/
├── controller/          # REST API 入口，只负责接收请求、调用 Service、返回 R<T>
├── service/             # 业务逻辑，可跨本模块 Mapper，禁止跨模块直接调用 Mapper
├── mapper/              # MyBatis-Plus Mapper 接口
├── entity/              # 数据库实体类
├── dto/                 # 请求/响应 DTO
├── config/              # 模块级配置（common 可放全局配置）
└── security/            # 模块级安全组件
```

**强制规则**：

- Controller 不得直接调用 Mapper。
- Service 不得直接调用其他模块的 Mapper（应调用其他模块的 Service）。
- DTO 与 Entity 分离；Entity 只用于持久层，DTO 用于接口层。

---

## 4. 全局接口约定

### 4.1 Base URL

- 应用上下文：`server.servlet.context-path=/api`
- 本地 Base URL：`http://localhost:8080/api`

### 4.2 统一响应 `R<T>`

文件：`common/entity/R.java`

- 成功：`code = 200`，`message = "ok"`，`data` 为业务数据
- 失败：`code` 为业务错误码（见 `common/constant/ResultCode.java`），`data` 通常为 `null`
- HTTP 状态码：
  - 200：正常到达
  - 400：参数校验失败
  - 401：未认证
  - 403：无权限 / 越权
  - 404：资源不存在
  - 500：服务器内部错误

### 4.3 业务异常

- 统一使用 `BusinessException(int errorCode, String message)`。
- 由 `GlobalExceptionHandler` 自动转换为 `R`。

### 4.4 认证

- Token 通过 `Authorization: Bearer {accessToken}` 传递。
- 当前用户 ID 通过 `@AuthenticationPrincipal String userId` 注入。
- 白名单（permitAll）：`/auth/**`、`/templates`、`/templates/**`、`/share/**`、`/actuator/health`。
- 其余接口需认证。

### 4.5 分页

- 列表接口接收 `page`（默认 1）、`size`（默认 20，最大 100）。
- 返回 `R<Page<T>>`。

### 4.6 逻辑删除

- 全局配置：`logic-delete-field: deleted`，`logic-delete-value: 1`，`logic-not-delete-value: 0`。
- 所有业务表必须有 `deleted` 字段。
- 删除接口只能做逻辑删除。

---

## 5. 模块依赖图

```
common
  ├── user
  ├── template
  ├── resume ── template (校验/预览，内部包含分享与多格式导出)
  ├── avatar ── resume (可选回填)
  ├── pdf ───── resume, template
  └── ai ────── resume
```

**规则**：

- `common` 不依赖任何业务模块。
- 业务模块可依赖 `common`。
- `resume` 可依赖 `template`（查模板是否存在）。
- `pdf` 可依赖 `resume`、`template`（读简历和模板数据）。
- `avatar` 可依赖 `resume`（将优化结果回填到简历 profile.avatarUrl）。
- `ai` 可依赖 `resume`（读取简历内容并回写 AI 任务结果）。
- 禁止循环依赖。

---

## 6. 数据库与对象存储

### 6.1 数据库

- 开发环境 MySQL：`resume_generation`
- 迁移脚本：`backend/src/main/resources/db/migration/V1__init.sql` 至 `V9__resume_render_settings.sql`
- 列名使用下划线，Java 实体使用驼峰（MyBatis-Plus 自动映射）。
- 主键策略：`assign_id`（Snowflake）。

### 6.2 对象存储

- 统一使用 MinIO。
- Bucket：
  - `resume-avatars`：头像原图与一寸照
  - `resume-pdfs`：导出的 PDF
  - `resume-templates`：模板缩略图与 HTML 模板文件
- 数据库只存 URL，禁止存 BLOB。

---

## 7. 配置项速查

| 配置 | 文件 | 说明 |
|---|---|---|
| `app.jwt.secret` | `application.yml` | Base64 编码，≥ 256 bit |
| `app.jwt.access-token-expiration` | `application.yml` | 默认 1 小时 |
| `app.jwt.refresh-token-expiration` | `application.yml` | 默认 7 天 |
| `app.minio.*` | `application-dev.yml` | MinIO 连接与 Bucket |
| `spring.datasource.*` | `application-dev.yml` | MySQL 连接 |
| `mybatis-plus.global-config.db-config.logic-delete-field` | `application.yml` | `deleted` |

---

## 8. 开发红线

- 禁止物理删除业务数据。
- 禁止将文件内容以 BLOB 存入数据库。
- 禁止绕过 `R<T>` 自定义返回结构。
- 禁止 Controller 直接调用 Mapper。
- 禁止跨模块直接调用 Mapper。
- 禁止手动拼接/解析 `resume.sections` JSON。
- 禁止在代码中硬编码密钥、密码、Bucket 名。
- 禁止返回 `passwordHash` 等敏感字段给前端。

---

## 9. 测试要求

- 后端采用 TDD：新增功能前先补充测试。
- 单元测试使用 H2：`backend/src/test/resources/application.yml`
- 集成测试使用 `ops/docker-compose.test.yml` 提供的 MySQL/MinIO。
- 运行命令：使用 JDK 17 执行 `mvn clean test`。
- 集成测试：先启动 `ops/docker-compose.test.yml`，设置 `RUN_INTEGRATION_TESTS=true` 后运行 `ResumeServiceIntegrationTest`。

---

## 10. 子模块 CLAUDE.md 索引

| 模块 | 文件 |
|---|---|
| common | `backend/src/main/java/com/resume/common/CLAUDE.md` |
| user | `backend/src/main/java/com/resume/user/CLAUDE.md` |
| resume | `backend/src/main/java/com/resume/resume/CLAUDE.md` |
| template | `backend/src/main/java/com/resume/template/CLAUDE.md` |
| avatar | `backend/src/main/java/com/resume/avatar/CLAUDE.md` |
| pdf | `backend/src/main/java/com/resume/pdf/CLAUDE.md` |
| ai | `backend/src/main/java/com/resume/ai/CLAUDE.md` |

---

## 11. 常用命令

```bash
cd backend
mvn clean test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 12. 关键设计文档

- `../docs/superpowers/specs/2026-07-03-api-spec.md`
- `../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- `../docs/superpowers/specs/2026-07-03-validation-rules.md`
- `../docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `../docs/superpowers/specs/2026-07-03-tdd-test-plan.md`
- `../docs/setup-guide.md`
- `../docs/environment.md`
- `../docs/security-guide.md`
