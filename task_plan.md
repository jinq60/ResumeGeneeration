# Task Plan: 补充简历生成工具缺失设计文档与工程脚手架

## Goal

补充 `ResumeGeneeration` 项目缺失的 API 规范、模板系统规范、TDD 测试计划，并初始化前后端工程脚手架，使项目达到可按 SDD 开发、TDD 验证的状态。

## Current Phase

Phase 8

## Phases

### Phase 1: 需求与现状发现

- [x] 阅读并理解 PRD、SDD、范围对齐书、数据模型、校验规则
- [x] 识别 PRD 与设计文档之间的不一致
- [x] 识别缺失的关键交付物（API 规范、模板规范、TDD 计划、脚手架）
- [x] 将发现记录到 `findings.md`
- **Status:** complete

### Phase 2: 规划与结构设计

- [x] 创建 `task_plan.md`、`findings.md`、`progress.md`
- [x] 确定待补充文档的目录结构与命名
- [x] 确定工程脚手架的技术版本与目录结构
- [x] 确定 TDD 测试策略与测试分层
- **Status:** complete

### Phase 3: 设计文档补充

- [x] 编写 `docs/superpowers/specs/2026-07-03-api-spec.md`
- [x] 编写 `docs/superpowers/specs/2026-07-03-template-system-spec.md`
- [x] 编写 `docs/superpowers/specs/2026-07-03-tdd-test-plan.md`
- [x] 修正 SDD 中 PRD 引用路径错误
- **Status:** complete

### Phase 4: 工程脚手架初始化

- [x] 初始化后端 Spring Boot 项目结构（含 common/user/resume/template/avatar/pdf 模块包）
- [x] 初始化前端 Vue 3 + Vite 项目结构（含 api/components/composables/router/stores/utils/views/tests）
- [x] 添加基础依赖配置（pom.xml / package.json）
- [x] 添加统一的代码规范配置（.editorconfig、.gitignore 等）
- **Status:** complete

### Phase 5: 测试与验证

- [x] 检查新增文档与 PRD/SDD/数据模型/校验规则的一致性
- [x] 检查工程脚手架目录与 SDD 设计是否匹配
- [x] 记录验证结果到 `progress.md`
- **Status:** complete

### Phase 6: 交付

- [x] 汇总所有新增/修改的文件清单
- [x] 向用户说明项目当前已达开发就绪状态
- [x] 提供后续开发建议
- **Status:** complete

### Phase 7: 设计反馈迭代

- [x] 将模板从固定内置改为后台可动态管理资源
- [x] 优化数据库表结构与索引设计
- [x] 更新相关设计文档与后端实体
- **Status:** complete

### Phase 8: P1 AI 简历点评设计与上线策略确认

- [x] 将 AI 简历点评纳入 P1 范围，明确 P0 + P1 全部完成后统一上线
- [x] 设计 `resume_review` 数据模型、索引与 JSON Schema
- [x] 补充 AI 点评相关 API（创建点评、获取最新点评）
- [x] 补充 AI 点评相关 TDD 测试计划（单元/集成/接口/前端/E2E）
- [x] 创建 `ResumeReview` 后端实体类
- [x] 更新 `findings.md` 与 `progress.md`
- **Status:** complete

## Key Questions

1. API 规范是否需要覆盖所有 P0 接口的完整请求/响应/错误码？（是）
2. 模板系统规范是否需要包含 6 套模板的 HTML 结构与 CSS 变量？（是，至少包含规范与示例）
3. TDD 测试计划是否需要按后端/前端分层并映射到验收标准？（是）
4. 工程脚手架是否需要包含可运行的最小依赖？（是，至少可启动）
5. 是否需要同时创建后端与前端的完整模块包？（是，按 SDD 目录结构）

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| 沿用 SDD 已确定的技术栈（Spring Boot 3.x + Vue 3） | 需求已冻结，避免重新选型 |
| 缺失文档统一放到 `docs/superpowers/specs/` | 与现有设计文档保持一致 |
| TDD 测试计划单独成文 | 作为 SDD §11 测试策略的展开，便于开发时对照 |
| 后端模块包按 SDD §4 划分 | 保持设计一致性 |
| 前端目录按 SDD §5 划分 | 保持设计一致性 |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| 无 | — | — |

## Notes

- 每次完成一个阶段后更新 `task_plan.md` 中的状态。
- 重大决策前重新阅读本计划。
- 所有发现写入 `findings.md`，所有操作写入 `progress.md`。
