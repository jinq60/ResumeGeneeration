# 简历生成工具 TDD 测试计划

> 版本：v1.0  
> 日期：2026-07-03  > 基于：`docs/superpowers/specs/2026-07-03-scope-alignment.md`、`docs/superpowers/specs/2026-07-03-api-spec.md`、`docs/superpowers/specs/2026-07-03-template-system-spec.md`、`docs/superpowers/specs/2026-07-03-validation-rules.md`

---

## 1. TDD 策略概述

本项目采用 **SDD + TDD**：先确定设计文档与接口契约，再编写测试，最后实现代码。

### 1.1 测试分层

| 层级 | 后端 | 前端 | 作用 |
|---|---|---|---|
| 单元测试 | JUnit 5 + Mockito | Vitest | 验证纯业务逻辑、工具函数 |
| 组件/集成测试 | Spring Boot Test + Testcontainers | Vue Test Utils | 验证组件渲染、模块集成 |
| 接口/E2E 测试 | MockMvc / RestAssured | Playwright | 验证端到端链路 |

### 1.2 TDD 节奏

1. **红**：根据需求/接口契约编写失败的测试。
2. **绿**：编写最小实现使测试通过。
3. **重构**：优化代码，保持测试通过。

### 1.3 测试目录

后端：

```
backend/src/test/java/com/resume/
├── common/
├── user/
├── resume/
├── template/
├── avatar/
└── pdf/
```

前端：

```
frontend/tests/
├── unit/
├── component/
└── e2e/
```

---

## 2. 后端测试计划

### 2.1 单元测试

#### common 模块

| 测试类 | 测试内容 |
|---|---|
| `RTest` | 统一响应 `R.success()` / `R.error()` 构建 |
| `GlobalExceptionHandlerTest` | 各类异常转换为统一响应 |
| `ValidationUtilsTest` | 手机号、邮箱、URL、日期正则校验 |
| `SnowflakeIdGeneratorTest` | ID 唯一性与单调性 |

#### user 模块

| 测试类 | 测试内容 |
|---|---|
| `PasswordEncoderTest` | 密码加密与校验 |
| `JwtTokenProviderTest` | Token 生成、解析、过期判断 |
| `AuthServiceTest` | 注册、登录、游客、刷新逻辑 |

#### resume 模块

| 测试类 | 测试内容 |
|---|---|
| `ResumeServiceTest` | 创建默认标题、复制、重命名、逻辑删除 |
| `SectionSorterTest` | sections 按 order 排序 |
| `SectionValidatorTest` | Section 结构与字段校验 |
| `ProfileValidatorTest` | 姓名必填、手机/邮箱至少一个、URL 格式 |
| `EducationValidatorTest` | 时间范围、必填字段 |
| `ProjectValidatorTest` | description 数组必填、时间范围 |
| `WorkValidatorTest` | description 数组必填、时间范围 |
| `SkillValidatorTest` | 分类必填、items 非空 |
| `ResumeReviewServiceTest` | 调用 LLM、解析结果、保存点评记录 |
| `ResumeReviewPromptBuilderTest` | Prompt 组装包含 sections、岗位 JD、scene |
| `ResumeReviewScoreValidatorTest` | 评分范围 0–100、维度完整性校验 |
| `ResumeReviewResultParserTest` | JSON 结果解析、字段缺失兜底 |

#### avatar 模块

| 测试类 | 测试内容 |
|---|---|
| `AvatarFileValidatorTest` | 文件格式、大小、扩展名校验 |
| `AvatarTaskServiceTest` | 任务创建、状态流转、占位实现 |

#### pdf 模块

| 测试类 | 测试内容 |
|---|---|
| `PdfFileNameGeneratorTest` | 文件名生成规则 |
| `PdfExportPreCheckTest` | 导出前校验（姓名、联系方式） |
| `PdfTaskServiceTest` | 任务创建、状态流转 |

### 2.2 集成测试

使用 `@SpringBootTest` + Testcontainers MySQL。

| 测试类 | 测试内容 |
|---|---|
| `AuthControllerIntegrationTest` | 注册、登录、游客、Token 刷新 |
| `ResumeControllerIntegrationTest` | CRUD、复制、重命名、越权 |
| `TemplateControllerIntegrationTest` | 列表、详情 |
| `AvatarControllerIntegrationTest` | 上传、优化、查询、删除 |
| `PdfControllerIntegrationTest` | 导出、查询、下载 |
| `ResumeReviewControllerIntegrationTest` | 点评成功、保存最新记录、越权、内容过少 |

### 2.3 接口测试

使用 RestAssured 或 MockMvc，覆盖 API 规范中的所有接口。

| 接口 | 测试场景 |
|---|---|
| `POST /api/auth/register` | 成功注册、手机号已注册、邮箱已注册、验证码错误、密码过短 |
| `POST /api/auth/login` | 成功登录、账号不存在、密码错误、账号锁定 |
| `POST /api/auth/guest` | 成功创建游客 |
| `POST /api/auth/refresh` | 成功刷新、Token 无效 |
| `GET /api/users/me` | 获取当前用户、Token 过期 |
| `POST /api/resumes` | 创建成功、默认标题、场景无效、模板不存在 |
| `GET /api/resumes` | 列表分页、游客只能看自己的 |
| `GET /api/resumes/{id}` | 获取成功、不存在、越权 |
| `PUT /api/resumes/{id}` | 更新成功、Section 校验失败、越权 |
| `DELETE /api/resumes/{id}` | 删除成功、越权、关联文件清理 |
| `POST /api/resumes/{id}/duplicate` | 复制成功、标题加“副本” |
| `PUT /api/resumes/{id}/title` | 重命名成功、标题为空 |
| `POST /api/resumes/{id}/reviews` | 点评成功、LLM 失败、内容过少、越权 |
| `GET /api/resumes/{id}/reviews/latest` | 获取最新成功记录、无记录返回 null、越权 |
| `GET /api/templates` | 列表返回内置模板 |
| `GET /api/templates/{id}` | 详情成功、不存在 |
| `POST /api/avatars/upload` | 上传成功、格式错误、过大 |
| `POST /api/avatars/optimize` | 创建任务成功、参数无效 |
| `GET /api/avatars/tasks/{taskId}` | 查询成功、不存在、越权 |
| `DELETE /api/avatars/{id}` | 删除成功、越权 |
| `POST /api/pdf/export` | 导出成功、未填姓名、未填联系方式、越权 |
| `GET /api/pdf/tasks/{taskId}` | 查询成功、不存在、越权 |
| `GET /api/pdf/download/{taskId}` | 下载成功、文件未就绪、越权 |

---

## 3. 前端测试计划

### 3.1 单元测试

| 测试文件 | 测试内容 |
|---|---|
| `utils/validation.test.ts` | 手机号、邮箱、URL、日期校验函数 |
| `utils/date.test.ts` | 出生年月计算年龄、时间格式化 |
| `utils/file.test.ts` | 头像文件类型与大小校验 |
| `composables/useResumeStore.test.ts` | Pinia store 状态变更、自动保存逻辑 |
| `composables/useSectionOrder.test.ts` | Section 排序、显隐切换 |

### 3.2 组件测试

| 测试文件 | 测试内容 |
|---|---|
| `ProfileForm.test.ts` | 个人信息表单渲染、校验、提交 |
| `EducationForm.test.ts` | 教育经历增删改、时间校验 |
| `ProjectForm.test.ts` | 项目经历 description 数组渲染 |
| `SkillForm.test.ts` | 技能分类与等级展示 |
| `ResumePreview.test.ts` | 预览组件根据 sections 渲染 |
| `TemplateSelector.test.ts` | 模板切换、配置应用 |
| `AvatarUploader.test.ts` | 文件选择、格式校验、裁剪交互 |
| `ResumeReviewPanel.test.ts` | 评分展示、建议列表、重新点评 |

### 3.3 E2E 测试

使用 Playwright。

| 测试用例 | 场景 |
|---|---|
| `auth.spec.ts` | 游客进入编辑器、注册、登录、数据同步 |
| `editor.spec.ts` | 创建简历、填写个人信息、实时预览更新 |
| `template.spec.ts` | 切换模板、内容不丢失 |
| `avatar.spec.ts` | 上传头像、裁剪、应用到简历 |
| `pdf.spec.ts` | 点击导出 PDF、下载文件 |
| `review.spec.ts` | 打开 AI 点评、查看评分与建议、重新点评 |

---

## 4. 测试用例与验收标准映射

| 验收标准（PRD §18） | 后端测试 | 前端测试 |
|---|---|---|
| 用户可以成功创建简历 | `ResumeControllerIntegrationTest.createResume` | `editor.spec.ts` |
| 用户可以填写所有核心模块 | `SectionValidatorTest` | `editor.spec.ts` |
| 用户填写内容后，右侧预览实时更新 | — | `ResumePreview.test.ts` |
| 用户可以新增、删除、排序模块 | `SectionSorterTest` | `useSectionOrder.test.ts` |
| 用户刷新页面后，已保存内容不丢失 | `ResumeControllerIntegrationTest.getResume` | `editor.spec.ts` |
| 用户可以切换模板，内容不丢失 | — | `template.spec.ts` |
| 用户可以上传头像 | `AvatarControllerIntegrationTest.upload` | `avatar.spec.ts` |
| 用户可以裁剪头像 | — | `avatar.spec.ts` |
| 用户可以选择白底/蓝底/红底优化版本 | `AvatarTaskServiceTest` | `avatar.spec.ts` |
| 系统可以生成优化后的头像 | `AvatarTaskServiceTest` | `avatar.spec.ts` |
| 用户可以将优化头像应用到简历 | — | `avatar.spec.ts` |
| 头像在预览区和 PDF 中展示一致 | — | `avatar.spec.ts` + `pdf.spec.ts` |
| 用户可以导出 PDF | `PdfControllerIntegrationTest` | `pdf.spec.ts` |
| PDF 页面为 A4 | `PdfTaskServiceTest` | `pdf.spec.ts` |
| PDF 内容与预览一致 | `PdfControllerIntegrationTest` | `pdf.spec.ts` |
| 中文字体显示正常 | — | `pdf.spec.ts` |
| AI 可以点评简历并给出修改建议 | `ResumeReviewControllerIntegrationTest` | `review.spec.ts` |
| AI 点评结果包含综合评分与分项评分 | `ResumeReviewScoreValidatorTest` | `ResumeReviewPanel.test.ts` |
| AI 点评可以基于目标岗位 JD 进行匹配评分 | `ResumeReviewPromptBuilderTest` | `review.spec.ts` |
| 头像显示正常 | — | `pdf.spec.ts` |
| 多页内容导出正常 | — | `pdf.spec.ts` |
| 导出失败时有明确提示 | `PdfExportPreCheckTest` | `pdf.spec.ts` |
| 系统至少提供 4 套模板 | `TemplateControllerIntegrationTest` | `template.spec.ts` |
| 模板中的字体、间距、颜色统一 | — | `TemplateSelector.test.ts` |

---

## 5. 核心测试用例详述

### 5.1 用户注册/登录/游客模式 Token 获取

**测试目标**：验证三种认证方式都能获得有效 Token。

**步骤**：
1. 调用 `POST /api/auth/guest`，断言返回 `userId` 以 `guest_` 开头。
2. 调用 `POST /api/auth/register`，断言返回 `accessToken`。
3. 使用注册账号调用 `POST /api/auth/login`，断言返回相同 `userId`。
4. 使用过期 Token 访问 `GET /api/users/me`，断言返回 401。
5. 使用 Refresh Token 调用 `POST /api/auth/refresh`，断言获取新 Token。

### 5.2 创建简历默认标题为“我的简历 1”

**测试目标**：验证不传 title 时使用默认标题。

**步骤**：
1. 以游客身份调用 `POST /api/resumes`，请求体不含 `title`。
2. 断言响应 `data.title` 为 `我的简历 1`。
3. 再次创建，断言标题自动递增或保持默认（根据产品决策）。

### 5.3 个人信息校验

**测试目标**：验证姓名为必填，手机和邮箱至少填一个，邮箱格式校验。

**步骤**：
1. 创建简历后更新 sections，profile.name 为空。
2. 调用 `PUT /api/resumes/{id}`，断言返回 `RESUME_PROFILE_NAME_REQUIRED`。
3. profile.phone 和 profile.email 同时为空，断言返回 `RESUME_PROFILE_CONTACT_REQUIRED`。
4. profile.email 格式错误，断言返回 `RESUME_PROFILE_EMAIL_INVALID`。

### 5.4 教育经历按时间倒序排列

**测试目标**：验证后端返回的教育经历按结束时间倒序。

**步骤**：
1. 创建包含两段教育经历的简历，结束时间分别为 2026-06 和 2024-06。
2. 调用 `GET /api/resumes/{id}`。
3. 断言第一段结束时间为 2026-06，第二段为 2024-06。

### 5.5 模块增删改排序后 section order 正确

**测试目标**：验证 section order 在编辑后保持正确。

**步骤**：
1. 创建简历，默认 sections order 为 0,1,2,3,4,5。
2. 将 order=5 的 section 移动到 order=2。
3. 更新简历，断言所有 section 的 order 重新计算为 0,1,2,3,4,5。

### 5.6 切换模板不影响已填内容

**测试目标**：验证修改 templateId 不改变 sections。

**步骤**：
1. 创建简历并填写完整个人信息。
2. 调用 `PUT /api/resumes/{id}` 只修改 `templateId`。
3. 断言 `sections` 内容不变。

### 5.7 PDF 导出文件名

**测试目标**：验证文件名生成规则。

**步骤**：
1. 简历包含姓名和目标岗位，调用 `POST /api/pdf/export`。
2. 断言任务记录 file_name 为 `张三_Java后端开发_简历.pdf`。
3. 简历未填姓名，断言 file_name 为 `我的简历_20260703.pdf`。

### 5.8 头像上传超过 10MB 失败

**测试目标**：验证文件大小限制。

**步骤**：
1. 构造 11MB 图片文件。
2. 调用 `POST /api/avatars/upload`。
3. 断言返回 `AVATAR_FILE_TOO_LARGE`。

### 5.9 头像上传非 JPG/PNG/WEBP 格式失败

**测试目标**：验证文件格式限制。

**步骤**：
1. 上传 `.gif` 文件。
2. 断言返回 `AVATAR_FORMAT_UNSUPPORTED`。

### 5.10 越权访问他人简历返回 403

**测试目标**：验证数据隔离。

**步骤**：
1. 用户 A 创建简历，记录 ID。
2. 用户 B 登录，调用 `GET /api/resumes/{id}`。
3. 断言返回 403 `ACCESS_DENIED`。

### 5.11 游客模式本地保存与登录后同步

**测试目标**：验证游客数据可迁移到正式用户。

**步骤**：
1. 游客创建简历，数据存于 localStorage（前端）。
2. 游客注册/登录。
3. 断言原简历的 `user_id` 已更新为正式用户 ID。

### 5.12 删除简历后关联头像和 PDF 被清理

**测试目标**：验证隐私删除。

**步骤**：
1. 创建简历并上传头像。
2. 导出 PDF。
3. 删除简历。
4. 断言头像原图、优化图、PDF 文件已从存储中删除。

---

## 6. 测试数据策略

### 6.1 后端测试数据

- 使用 Testcontainers 启动 MySQL 容器。
- 每个测试类独立初始化数据，测试结束后清理。
- 使用 `@Sql` 或 Flyway 初始化模板数据。

### 6.2 前端测试数据

- 单元测试使用 mock 数据。
- 组件测试使用 factory 函数生成简历数据。
- E2E 测试通过 UI 交互生成真实数据。

---

## 7. 测试执行策略

### 7.1 本地开发

```bash
# 后端
./mvnw test

# 前端
npm run test:unit
npm run test:e2e
```

### 7.2 CI/CD

1. 每次提交触发单元测试与组件测试。
2. PR 合并前触发集成测试与 E2E 测试。
3. 测试覆盖率阈值：
   - 后端行覆盖率 ≥ 70%
   - 核心 Service 覆盖率 ≥ 80%
   - 前端组件覆盖率 ≥ 60%

---

## 8. 测试环境

| 环境 | 数据库 | 文件存储 | 用途 |
|---|---|---|---|
| 本地单元测试 | H2 / Mockito | Mockito Mock | 快速验证逻辑 |
| 本地集成测试 | Testcontainers MySQL | 临时目录 | 验证持久化与文件操作 |
| CI | Testcontainers MySQL | 临时目录 | 自动化回归 |
| E2E | 开发后端 | 临时目录 | 端到端验证 |

---

## 9. 缺陷管理

1. 测试失败时，先确认是测试问题还是代码问题。
2. 代码问题：修复后重新运行测试。
3. 测试问题：更新测试以匹配最新需求。
4. 所有 flaky test 必须标记并修复。

---

## 10. 参考文档

- `docs/superpowers/specs/2026-07-03-resume-generation-design.md` §11
- `docs/superpowers/specs/2026-07-03-api-spec.md`
- `docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `docs/superpowers/specs/2026-07-03-validation-rules.md`
- `docs/需求PRD-v1.md` §18
