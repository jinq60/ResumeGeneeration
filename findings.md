# Findings & Decisions

> 版本：v1.1  
> 日期：2026-07-07  
> 状态：文档审计与对齐完成，待代码对齐后进入开发

---

## Requirements

基于用户请求与现有文档，本次工作的具体需求如下：

- 审查当前项目的设计文档是否与 PRD 对应，并评估是否达到可开发状态。
- 补充缺失的关键设计文档，使项目达到按 SDD 开发、TDD 验证的状态。
- 初始化前后端工程脚手架，提供可运行的项目基础结构。
- **新增（2026-07-07）**：在开发前完成文档审计、一致性修正与缺失文档补全，确保文档与代码一致、进度可追溯。

## Research Findings

### 项目现状（v1.1 对齐后）

- 项目目录：`E:\javaProject\ResumeGeneeration`
- 当前状态：设计文档已补齐并经过一致性审计，前后端工程脚手架已初始化，后端可通过 `mvn clean test` 编译测试。
- 已补充/更新文档：
  - `docs/superpowers/specs/2026-07-03-api-spec.md`（API 规范，v1.1）
  - `docs/superpowers/specs/2026-07-03-template-system-spec.md`（模板系统规范）
  - `docs/superpowers/specs/2026-07-03-tdd-test-plan.md`（TDD 测试计划）
  - `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`（数据模型，新增 JSON 类型映射）
  - `docs/superpowers/specs/2026-07-03-validation-rules.md`（校验规则，新增错误码映射与校验分层）
  - `docs/superpowers/specs/2026-07-03-scope-alignment.md`（范围对齐，新增 v1.1 变更记录）
  - `docs/需求PRD-v1.md`（PDF 命名规则明确为 `yyyyMMdd`）
- 已初始化工程：
  - `backend/`：Spring Boot 项目，含 common/user/resume/template/avatar/pdf 模块包，新增 `ResumeReview` 实体、`JwtTokenProvider`、`JwtAuthenticationFilter`、CORS 配置、Actuator 健康检查
  - `frontend/`：Vue 3 + Vite 项目，含 api/components/composables/router/stores/utils/views/tests，Section TS 类型已补全为 union 类型
- **待完成**：
  - 后端实体 JSON 字段类型化（`Resume.sections`、`ResumeReview` JSON 字段）。
  - 后端缺失枚举常量补全（`workType`、`projectType`、`skillCategory`、`skillLevel`、`introductionStyle`）。
  - `PUT /resumes/{id}` 返回结构、`UpdateResumeRequest.sections` 可选性、简历渲染器字段名等代码与规范对齐。
  - 缺失项目文档：环境搭建指南、架构决策记录（ADR）、API 变更日志、环境变量说明、安全指南。
  - 缺失可执行 Flyway 迁移脚本。

### 审计结果修补

针对 `项目审计结果.md` 中的合理发现，已完成一轮修补：

| 原问题 | 修补内容 |
|---|---|
| MyBatis-Plus 全局逻辑删除字段 `status` 与任务表状态枚举冲突 | 所有表新增 `deleted` 字段（TINYINT），全局逻辑删除配置改为 `deleted` / 1 / 0；实体类同步新增 `deleted` 字段；DDL 与数据模型文档已更新 |
| 索引设计章节使用 PostgreSQL partial index 语法 | `idx_user_phone` / `idx_user_email` 改为 MySQL 兼容的 `CREATE UNIQUE INDEX ...`，并补充 NULL 值业务层校验说明 |
| SecurityConfig 缺少 JWT 过滤器与 CORS | 新增 `JwtTokenProvider`、`JwtAuthenticationFilter`，在 `SecurityConfig` 中注册并配置 CORS（开发环境放行 `localhost:5173`） |
| `application.yml` 缺少 MinIO 配置 | 在 `application-dev.yml` 中补充 MinIO endpoint、access-key、secret-key、bucket 规划 |
| 前端 `Section.data` 为 `Record<string, any>` | `resume.ts` 重构为 union 类型，为 profile/education/work/project/skill/summary/custom 定义 data 接口 |
| SDD §2 文件存储描述仍为本地文件系统 | 已更新为 MinIO（兼容 S3），并修正架构图与技术栈表格 |
| 模板渲染前端/服务端一致性无保障机制 | `template-system-spec.md` 新增 §4.3 一致性保障：同源配置、同源 Section 规则、预览回源、回归验证 |
| 简历/模板删除逻辑与新的 `deleted` 字段不一致 | `api-spec.md` 中删除业务规则更新为设置 `deleted = 1`；PDF 导出 `templateId` 含义补充说明；admin 模板查询参数说明更新 |
| 缺少健康检查端点 | `pom.xml` 增加 `spring-boot-starter-actuator`，`application.yml` 暴露 `/actuator/health` |
| 头像背景 `transparent` 在 P0 枚举中存在但不实现 | `data-model-and-ddl.md` 中 `background_type` 说明更新为 white/blue/red（P0），transparent 为 P2 预留 |

> 未在本次修补的合理建议（可在开发阶段逐步落地）：
> - DDL 无外键约束已补充设计原则说明；
> - 游客数据迁移的事务边界与失败回滚策略需在 Service 实现时细化；
> - 其余 5 套模板 Vue 组件需在开发阶段补充；
> - API 版本控制（`/api/v1/`）如未来需要 breaking change 再统一迁移。
- 已修正：SDD 中 PRD 引用路径错误。

### 开发就绪度（v1.1 对齐后）

| 维度 | 就绪度 | 说明 |
|---|---|---|
| 需求冻结 | ⭐⭐⭐⭐⭐ | PRD 详尽，范围已对齐 |
| 架构设计 | ⭐⭐⭐⭐⭐ | 技术栈、模块划分、数据流清晰 |
| 数据模型 | ⭐⭐⭐⭐☆ | DDL、JSON Schema、初始化数据完整；**待代码层 JSON 字段类型化** |
| 接口契约 | ⭐⭐⭐⭐☆ | API 规范已补齐；**待 `PUT /resumes/{id}` 返回结构等代码对齐** |
| 模板系统 | ⭐⭐⭐⭐⭐ | 模板已设计为后台可管理资源，配置 Schema、渲染规则、索引已定义 |
| 校验规则 | ⭐⭐⭐⭐☆ | 前后端规则、错误码完整；**待代码层激活校验逻辑** |
| 对象存储 | ⭐⭐⭐⭐⭐ | 头像/PDF/模板缩略图统一使用 MinIO；数据库只存 URL |
| 头像设计 | ⭐⭐⭐⭐⭐ | 用户头像任意上传存 user.avatar_url；简历头像走自拍 → 一寸照优化 → 镶嵌到简历 |
| AI 点评 | ⭐⭐⭐⭐☆ | P1 功能：LLM 点评、评分、建议，数据模型与 API 已定义；**待接入真实 LLM** |
| 上线策略 | ⭐⭐⭐⭐⭐ | P0 + P1 全部完成后统一上线 |
| 测试准备 | ⭐⭐⭐☆☆ | TDD 测试计划已补齐；**实际测试覆盖不足，待补全** |
| 工程初始化 | ⭐⭐⭐⭐☆ | 前后端脚手架已搭建，后端可编译测试；**待补全 Flyway 迁移、环境文档** |
| 文档一致性 | ⭐⭐⭐⭐☆ | 核心文档已对齐；**待补全 ADR、setup-guide、environment.md** |

**综合评估：约 75% 就绪（P0+P1 设计与文档层面），核心设计文档已可用于指导开发。** 剩余工作分为两类：
1. **文档补全**：setup-guide、ADR、environment.md、api-changelog、security-guide、Flyway 迁移脚本。
2. **代码对齐**：JSON 字段类型化、枚举常量、`PUT /resumes/{id}` 返回结构、渲染器字段名、校验逻辑激活、测试补全。

> 用户明确：P0 + P1 所有需求一次性完成后统一上线，后续版本修 BUG；**开发前先完成文档补全与代码对齐**。

### PRD 与 SDD 对应性结论

- P0 需求基本被 SDD、范围对齐书、数据模型、校验规则覆盖。
- 已解决的不一致集中在 `scope-alignment.md` 中：`age` 计算、`jobIntention` 合并、接口路径复数化、P0 范围调整等。
- 头像占位行为已明确：用户头像（`user.avatar_url`）可任意上传、非必须真人；简历头像为自拍照经一寸照优化后镶嵌到简历，`profile.avatarUrl` 为空时不展示头像。
- （SDD 第 1 行 PRD 引用路径错误已修正）

### 验证结果

- 后端项目可通过 `mvn clean test` 编译并运行测试（使用 Java 17 + H2 内存数据库），**44 个用例全部通过**。
- 前端单元测试可执行（`npm run test:unit -- --run`），**8 个用例全部通过**。
- 前端 `package.json` 为有效 JSON，目录结构与 SDD §5 一致。
- 后端模块包结构与 SDD §4 一致。
- API 规范、模板规范、TDD 测试计划、数据模型、校验规则已完成一致性对齐。
- 已知限制：
  - 后端测试使用 H2 内存数据库，未接入真实 MySQL/Testcontainers。
  - 后端实体 JSON 字段当前为 `String` 类型，需按数据模型 v1.1 改造为强类型集合。
  - 后端校验逻辑未完全激活，与 `validation-rules.md` v1.1 存在差距。
  - 前端核心页面（简历列表、编辑器、个人表单）仍为占位实现。
  - 缺少可执行 Flyway 数据库迁移脚本。

## Technical Decisions

| Decision | Rationale |
|----------|-----------|
| 沿用 Spring Boot 3.x + Java 17 + MySQL 8 + MyBatis-Plus | SDD 已确定，避免重新选型 |
| 沿用 Vue 3 + Vite + Element Plus + Pinia | SDD 已确定 |
| 模板作为后台可管理资源 | 避免纠结内置模板数量，支持动态新增/上下架，不影响已生成简历 |
| 模板表增加 `code`、`sort_order`、`is_recommended`、`render_engine`、`version`、`created_by` | 支撑后台管理、排序、推荐、缓存控制、权限追溯 |
| 对象存储采用 MinIO（兼容 S3） | 你惯用且成熟的方案：数据库只存 URL，文件存对象存储，便于扩展与 CDN 接入 |
| 用户头像与简历一寸照职责分离 | 用户头像任意上传；简历头像走自拍 → 一寸照优化（avatar_task） → 镶嵌到简历 |
| AI 简历点评纳入 P1 | 用户明确要求：生成/上传完整简历后 AI 打分并给出修改建议；P0+P1 完成后统一上线 |
| 后端模块包结构为 `com.resume.{user,resume,template,avatar,pdf,common}` | 与 SDD §4 一致 |
| 前端目录结构为 `frontend/src/{api,assets,components,composables,router,stores,utils,views}` | 与 SDD §5 一致 |
| API 规范采用 RESTful + 统一响应 `R<T>` | 与 SDD §7/§9 一致 |
| 模板渲染采用服务端 HTML + CSS 变量注入，前端 Vue 组件同步预览 | 保证 PDF 与预览一致 |
| TDD 测试分层：后端单元/集成/接口测试 + 前端单元/组件/E2E 测试 | 与 SDD §11 一致 |

## Issues Encountered

| Issue | Resolution |
|-------|------------|
| 现有任务列表属于审计阶段，与补充工作阶段不同 | 完成审计任务后新建补充阶段任务 |
| 用户要求“继续补充”，未明确范围边界 | 按审计报告建议的最小阻塞集执行：API 规范、模板规范、TDD 计划、脚手架 |

## Resources

- PRD：`docs/需求PRD-v1.md`
- SDD：`docs/superpowers/specs/2026-07-03-resume-generation-design.md`
- 范围对齐：`docs/superpowers/specs/2026-07-03-scope-alignment.md`
- 数据模型：`docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- 校验规则：`docs/superpowers/specs/2026-07-03-validation-rules.md`

## Visual/Browser Findings

无。
