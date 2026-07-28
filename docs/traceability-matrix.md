# 需求追溯矩阵

> 版本：v1.1  
> 日期：2026-07-26  
> 作用：将 PRD 需求追溯到设计文档、数据模型、API 规范、校验规则、测试计划与代码，便于审计与迭代。

---

## 1. 认证与用户模块

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 手机号/邮箱注册 | `scope-alignment.md` §2.1 | `user` 表 | `api-spec.md` §5.1 | `validation-rules.md` §2、§11 | `tdd-test-plan.md` §2.1 `UserServiceTest` | `backend/src/main/java/com/resume/user/service/UserService.java` |
| 登录 | `scope-alignment.md` §2.1 | `user` 表 | `api-spec.md` §5.2 | `validation-rules.md` §2 | `tdd-test-plan.md` §2.3 | `AuthController`, `UserService` |
| 游客模式 | `scope-alignment.md` §2.1 | `user` 表 `is_guest` | `api-spec.md` §5.3 | — | `tdd-test-plan.md` §2.3 | `AuthController`, `UserService` |
| JWT 鉴权 | — | — | `api-spec.md` §3 | — | `tdd-test-plan.md` §2.1 `JwtTokenProviderTest` | `JwtTokenProvider`, `JwtAuthenticationFilter` |
| Token 刷新 | `scope-alignment.md` §2.1 | `refresh_token` 表（可选） | `api-spec.md` §5.4 | — | `tdd-test-plan.md` §2.3 | `AuthController`, `UserService` |
| 获取当前用户 | — | `user` 表 | `api-spec.md` §6 | — | `tdd-test-plan.md` §2.3 | `UserController` |

## 2. 简历管理模块

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 新建简历 | `scope-alignment.md` §2.1 | `resume` 表 | `api-spec.md` §7.1 | `validation-rules.md` §11.1 | `tdd-test-plan.md` §2.1 `ResumeServiceTest` | `ResumeController`, `ResumeService` |
| 简历列表 | `scope-alignment.md` §2.1 | `resume` 表 | `api-spec.md` §7.2 | — | `tdd-test-plan.md` §2.3 | `ResumeController`, `ResumeService` |
| 获取详情 | `scope-alignment.md` §2.1 | `resume` 表 | `api-spec.md` §7.3 | — | `tdd-test-plan.md` §2.3 | `ResumeController`, `ResumeService` |
| 更新简历 | `scope-alignment.md` §2.1 | `resume` 表 `sections` | `api-spec.md` §7.4 | `validation-rules.md` §11.2、§12 | `tdd-test-plan.md` §2.1 | `ResumeController`, `ResumeService` |
| 删除简历 | `scope-alignment.md` §2.1 | `resume` 表 `deleted` | `api-spec.md` §7.5 | — | `tdd-test-plan.md` §2.1 | `ResumeController`, `ResumeService` |
| 复制简历 | `scope-alignment.md` §2.1 | `resume` 表 | `api-spec.md` §7.6 | — | `tdd-test-plan.md` §2.1 | `ResumeController`, `ResumeService` |
| 重命名简历 | `scope-alignment.md` §2.1 | `resume` 表 `title` | `api-spec.md` §7.7 | — | `tdd-test-plan.md` §2.3 | `ResumeController`, `ResumeService` |

## 3. 简历编辑器模块

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 左侧表单 + 右侧实时预览 | `scope-alignment.md` §2.1 | `resume.sections` | `api-spec.md` §7 | `validation-rules.md` §3–§8 | `tdd-test-plan.md` §3 | `frontend/src/views/EditorView.vue` |
| 模块增删改 | `scope-alignment.md` §2.1 | `resume.sections` | `api-spec.md` §7.4 | `validation-rules.md` §12 | `tdd-test-plan.md` §3 | `ResumeService` |
| 模块排序（按钮） | `scope-alignment.md` §2.1、§2.2 | `resume.sections.order` | `api-spec.md` §7.4 | — | `tdd-test-plan.md` §2.1 `SectionSorterTest` | 待实现 |
| 自动保存 | `scope-alignment.md` §2.1 | `resume.sections` | `api-spec.md` §7.4 | `validation-rules.md` §1.3 | `tdd-test-plan.md` §3 | `frontend/src/composables/useAutoSave.ts`（待连接） |

## 4. 内容模块

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 个人信息 Profile | `scope-alignment.md` §3.1 | `data-model-and-ddl.md` §5.1 | `api-spec.md` §7 | `validation-rules.md` §3 | `tdd-test-plan.md` §2.1 `ProfileValidatorTest` | `SectionDTO`, `ResumeService.validateSectionData` |
| 教育经历 Education | `scope-alignment.md` §3.2 | `data-model-and-ddl.md` §5.2 | `api-spec.md` §7 | `validation-rules.md` §4 | `tdd-test-plan.md` §2.1 `EducationValidatorTest` | `SectionDTO`, `ResumeService.validateSectionData` |
| 项目经历 Project | `scope-alignment.md` §3.2 | `data-model-and-ddl.md` §5.3 | `api-spec.md` §7 | `validation-rules.md` §5 | `tdd-test-plan.md` §2.1 `ProjectValidatorTest` | `SectionDTO`, `ResumeService.validateSectionData` |
| 工作经历 WorkExperience | `scope-alignment.md` §3.3 | `data-model-and-ddl.md` §5.4 | `api-spec.md` §7 | `validation-rules.md` §6 | `tdd-test-plan.md` §2.1 `WorkValidatorTest` | `SectionDTO`, `ResumeService.validateSectionData` |
| 技能 Skill | `scope-alignment.md` §2.1 | `data-model-and-ddl.md` §5.5 | `api-spec.md` §7 | `validation-rules.md` §7 | `tdd-test-plan.md` §2.1 `SkillValidatorTest` | `SectionDTO`, `ResumeService.validateSectionData` |
| 自我介绍 Introduction | `scope-alignment.md` §2.1 | `data-model-and-ddl.md` §5.6 | `api-spec.md` §7 | `validation-rules.md` §8 | `tdd-test-plan.md` §2.1 | `SectionDTO`, `ResumeService.validateSectionData` |

## 5. 头像模块

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 头像上传 | `scope-alignment.md` §2.1 | `avatar_task` 表 | `api-spec.md` §9.1 | `validation-rules.md` §9 | `tdd-test-plan.md` §2.1 `AvatarServiceTest` | `AvatarController`, `AvatarService` |
| 一寸照优化（P0 占位） | `scope-alignment.md` §2.1、§3.5 | `avatar_task` 表 | `api-spec.md` §9.2 | — | `tdd-test-plan.md` §2.1 `AvatarServiceTest` | `AvatarService.optimize`（占位） |
| 查询优化任务 | `scope-alignment.md` §2.1 | `avatar_task` 表 | `api-spec.md` §9.3 | — | `tdd-test-plan.md` §2.3 | `AvatarController`, `AvatarService` |
| 删除头像 | `scope-alignment.md` §2.1 | `avatar_task` 表 `deleted` | `api-spec.md` §9.4 | — | `tdd-test-plan.md` §2.1 | `AvatarController`, `AvatarService` |

## 6. 模板系统

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 模板列表 | `scope-alignment.md` §2.1 | `template` 表 | `api-spec.md` §8 | — | `tdd-test-plan.md` §2.1 `TemplateServiceTest` | `TemplateController`, `TemplateService` |
| 模板详情 | `scope-alignment.md` §2.1 | `template` 表 | `api-spec.md` §8 | — | `tdd-test-plan.md` §2.3 | `TemplateController`, `TemplateService` |
| 后台模板 CRUD | `scope-alignment.md` §2.1 | `template` 表 | `api-spec.md` §10 | — | `tdd-test-plan.md` §2.1 `TemplateServiceTest` | `AdminTemplateController`, `TemplateService` |
| 模板渲染 | `scope-alignment.md` §2.1 | `template.config` | `template-system-spec.md` | — | `tdd-test-plan.md` §2.1 `ResumeRenderServiceTest` | `ResumeRenderService` |

## 7. PDF 导出

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 导出 PDF | `scope-alignment.md` §2.1 | `pdf_task` 表 | `api-spec.md` §11.1 | `validation-rules.md` §10 | `tdd-test-plan.md` §2.1 `PdfServiceTest` | `PdfController`, `PdfService` |
| 查询 PDF 任务 | `scope-alignment.md` §2.1 | `pdf_task` 表 | `api-spec.md` §11.2 | — | `tdd-test-plan.md` §2.3 | `PdfController`, `PdfService` |
| 下载 PDF | `scope-alignment.md` §2.1 | `pdf_task` 表 | `api-spec.md` §11.3 | — | `tdd-test-plan.md` §2.3 | `PdfController`, `PdfService` |

## 8. AI 简历点评（P1）

| PRD 需求 | 范围对齐 | 数据模型 | API 规范 | 校验规则 | TDD 计划 | 代码位置 |
|---|---|---|---|---|---|---|
| 创建 AI 点评 | `scope-alignment.md` §2.1 | `resume_review` 表 | `api-spec.md` §7.8 | — | `tdd-test-plan.md` §2.1 `ResumeReviewServiceTest` | `ResumeController`, `ResumeReviewService`（stub） |
| 获取最新点评 | `scope-alignment.md` §2.1 | `resume_review` 表 | `api-spec.md` §7.9 | — | `tdd-test-plan.md` §2.3 | `ResumeController`, `ResumeReviewService` |

---

## 9. 通用与基础设施

| 需求/决策 | 文档 | 代码位置 |
|---|---|---|
| 对象存储方案 | `adr/ADR-001-minio-over-localfs.md` | `MinioConfig`, `MinioStorageService` |
| JSON Section 存储 | `adr/ADR-002-json-sections.md` | `Resume` entity, `SectionDTO` |
| P0 头像占位 AI | `adr/ADR-003-placeholder-avatar-ai.md` | `AvatarService.optimize` |
| 模板后台化管理 | `adr/ADR-004-template-as-managed-resource.md` | `Template` entity, `AdminTemplateController` |
| 安全合规 | `security-guide.md` | `SecurityConfig`, `GlobalExceptionHandler` |

---

## 10. 前端页面实现

| 页面/能力 | 设计来源 | 代码位置 |
|---|---|---|
| 用户工作台（Dashboard） | Google Stitch 项目 `workbench-overview` | `frontend/src/views/DashboardView.vue` |
| 简历列表 | Google Stitch 项目 `my-resumes` | `frontend/src/views/ResumeListView.vue` |
| 简历编辑器 | Google Stitch 项目 `resume-editor` | `frontend/src/views/EditorView.vue` |
| 官网首页 | Google Stitch 项目 `landing-page` | `frontend/src/views/LandingView.vue` |
| 投递管理（用户侧） | Google Stitch 项目 `delivery-management` | `frontend/src/views/DeliveryManagementView.vue` |
| 后台总览 | Google Stitch 项目 `admin-overview` | `frontend/src/views/admin/Dashboard.vue` |
| 用户管理 | Google Stitch 项目 `user-management` | `frontend/src/views/admin/UserManagement.vue` |
| 模板管理 | Google Stitch 项目 `template-management` | `frontend/src/views/admin/TemplateManagement.vue` |
| 简历管理 | Google Stitch 项目 `resume-management` | `frontend/src/views/admin/ResumeManagement.vue` |
| AI 规则管理 | Google Stitch 项目 `ai-rules` | `frontend/src/views/admin/SystemSettings.vue` |
| 内容审核 | Google Stitch 项目 `content-audit` | `frontend/src/views/admin/ContentAudit.vue` |
| 投递数据 | Google Stitch 项目 `delivery-data` | `frontend/src/views/admin/DeliveryData.vue` |
| 用户端布局与导航 | Google Stitch 设计系统 | `frontend/src/components/layout/MainLayout.vue`, `TopNavbar.vue` |
| 管理端布局 | Google Stitch 设计系统 | `frontend/src/components/admin/AdminLayout.vue` |
| 设计系统/主题 | Google Stitch 设计令牌 | `frontend/src/assets/styles/design-system.scss`, `tailwind.css` |
| 管理员登录 | 设计系统 | `frontend/src/views/admin/Login.vue` |
| 用户账号设置 | 设计系统 | `frontend/src/views/SettingsView.vue` |
| 模板中心 | 设计系统 | `frontend/src/views/TemplateCenterView.vue` |
| AI 点评入口 | 设计系统 | `frontend/src/views/AIReviewCenterView.vue` |
| 404 页面 | 设计系统 | `frontend/src/views/NotFoundView.vue` |
| 模板详情页 | 设计系统 | `frontend/src/views/TemplateDetailView.vue` |
| 下载中心 | 设计系统 | `frontend/src/views/DownloadCenterView.vue` |
| 通知中心 | 设计系统 | `frontend/src/views/NotificationCenterView.vue` |
| 简历详情/预览页 | 设计系统 | `frontend/src/views/ResumeDetailView.vue` |
| 路由与鉴权守卫 | `security-guide.md` | `frontend/src/router/index.ts` |
| 导出/头像任务本地存储 | 设计系统 | `frontend/src/utils/download.ts` |

---

## 11. 说明

- “代码位置”列标记为“待实现”或“（待完善）”的项，是下一阶段开发重点。
- 本矩阵随需求变更和功能实现同步更新。
- 新增 PRD 需求时，必须在本矩阵中补充对应的设计、API、校验、测试、代码位置。
