# resume 模块 — Claude 约束

> 作用：简历核心生命周期管理、AI 简历点评、服务端 HTML 预览。
> 范围：`backend/src/main/java/com/resume/resume/`。
> 必读：`backend/CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

`resume` 模块负责：

- 简历创建、列表、详情、更新/自动保存、删除
- 简历复制、重命名
- 简历 Section 结构校验
- 服务端 HTML 实时预览
- 富文本字段的服务端安全渲染
- AI 简历点评任务入口（真实 LLM 调用，未配置时回退占位结果）
- Word/Markdown 导出与公开分享子模块

---

## 2. 包目录结构

```
com.resume.resume/
├── controller/
│   ├── ResumeController.java     # /resumes REST API
│   └── PreviewController.java    # /resumes/{id}/preview HTML 预览
├── service/
│   ├── ResumeService.java        # 简历核心业务
│   ├── ResumeReviewService.java  # AI 点评任务入口
│   └── ResumeExportService.java  # Word/Markdown 导出
├── share/                         # 公开分享链接与只读 HTML
├── mapper/
│   ├── ResumeMapper.java
│   └── ResumeReviewMapper.java
├── entity/
│   ├── Resume.java               # 简历主表
│   ├── ResumeReview.java         # AI 点评记录
│   └── ResumeReviewSuggestion.java # 点评建议项
└── dto/
    ├── CreateResumeRequest.java
    ├── UpdateResumeRequest.java
    ├── RenameResumeRequest.java
    ├── ReviewResumeRequest.java
    ├── SectionDTO.java
    ├── ResumeDetailResponse.java
    ├── ResumeListItemResponse.java
    └── ResumeReviewResponse.java
```

---

## 3. HTTP API 端点

Base URL：`http://localhost:8080/api`

所有 `/resumes/**` 接口需 JWT 认证，当前用户 ID通过 `@AuthenticationPrincipal String userId` 注入。

| 方法 | 路径 | 请求 DTO | 响应 DTO | 说明 |
|---|---|---|---|---|
| POST | `/resumes` | `CreateResumeRequest` | `R<ResumeDetailResponse>` | 创建简历，默认生成 6 个 Section |
| GET | `/resumes` | `page`、`size` | `R<Page<ResumeListItemResponse>>` | 当前用户简历列表 |
| GET | `/resumes/{id}` | 路径参数 | `R<ResumeDetailResponse>` | 简历详情 |
| PUT | `/resumes/{id}` | `UpdateResumeRequest` | `R<{id, updatedAt}>` | 更新/自动保存 |
| DELETE | `/resumes/{id}` | 路径参数 | `R<Void>` | 逻辑删除，并清理关联任务与文件 |
| POST | `/resumes/{id}/duplicate` | 路径参数 | `R<{id, title, createdAt}>` | 复制简历 |
| PUT | `/resumes/{id}/title` | `RenameResumeRequest` | `R<{id, title, updatedAt}>` | 重命名 |
| POST | `/resumes/{id}/reviews` | `ReviewResumeRequest` | `R<ResumeReviewResponse>` | 创建 AI 点评任务，异步调用 LLM 或回退占位结果 |
| GET | `/resumes/{id}/reviews/latest` | 路径参数 | `R<ResumeReviewResponse>` | 获取最新点评 |
| GET | `/resumes/{resumeId}/preview` | `templateId`（可选） | `text/html` | 服务端渲染预览 |
| GET | `/resumes/{id}/export/markdown` | 路径参数 | Markdown 文件流 | 导出 Markdown |
| GET | `/resumes/{id}/export/word` | 路径参数 | DOCX 文件流 | 导出 Word |
| POST/GET/DELETE | `/resumes/{id}/share` | 路径参数 | `R<ShareResponse>` | 创建、查询、关闭分享 |

---

## 4. 关键实体

### 4.1 `Resume`

| 字段 | Java 类型 | 说明 |
|---|---|---|
| `id` | String | Snowflake ID |
| `userId` | String | 用户 ID |
| `title` | String | 简历标题，最大 128 字符 |
| `scene` | String | 使用场景 |
| `targetPosition` | String | 目标岗位 |
| `targetIndustry` | String | 目标行业（P1 预留） |
| `templateId` | String | 当前模板 ID |
| `sections` | `List<SectionDTO>` | JSON 数组，TypeHandler 自动映射 |
| `renderSettings` | `RenderSettings` | JSON 对象，用户排版与一页适配设置 |
| `status` | String | `active` / `deleted` |
| `exportCount` | Integer | 导出次数 |
| `deleted` | Integer | 逻辑删除 |
| `lastEditedAt` | LocalDateTime | 最近编辑时间 |
| `createdAt` / `updatedAt` | LocalDateTime | 时间戳 |

### 4.2 `ResumeReview`

| 字段 | Java 类型 | 说明 |
|---|---|---|
| `id` | String | 点评记录 ID |
| `resumeId` | String | 关联简历 ID |
| `userId` | String | 用户 ID |
| `overallScore` | Integer | 综合评分 0–100 |
| `dimensionScores` | `Map<String, Integer>` | 分项评分 JSON |
| `suggestions` | `List<ResumeReviewSuggestion>` | 建议列表 JSON |
| `highlights` | `List<String>` | 亮点 JSON |
| `jobDescription` | String | 目标岗位 JD |
| `modelName` | String | AI 模型名 |
| `modelVersion` | String | AI 模型版本 |
| `status` | String | `pending` / `success` / `failed` |
| `errorMsg` | String | 失败原因 |
| `deleted` | Integer | 逻辑删除 |
| `createdAt` / `updatedAt` | LocalDateTime | 时间戳 |

---

## 5. Section 数据结构

### 5.1 `SectionDTO`

| 字段 | 类型 | 约束 |
|---|---|---|
| `id` | String | 必填，模块唯一 ID |
| `type` | String | 必填，必须是 `BizConstant.SECTION_TYPES` 之一 |
| `title` | String | 必填 |
| `order` | Integer | 必填，排序序号 |
| `visible` | Boolean | 必填 |
| `data` | Object | 必填，模块业务数据 |

### 5.2 支持的 Section 类型

- `profile`：个人信息
- `education`：教育经历
- `project`：项目经历
- `work`：工作经历
- `skill`：技能 & 技术栈
- `introduction`：自我介绍
- `custom`：自定义模块

### 5.3 JSON 类型化约束

- `Resume.sections` 必须声明为 `List<SectionDTO>`。
- 必须使用 MyBatis-Plus TypeHandler 自动映射数据库 JSON 字段。
- 禁止以 `String` 手动拼接/解析 JSON。
- `SectionDTO.data` 当前为 `Object`，后续应进一步类型化为具体业务 DTO。

---

## 6. DTO 请求约束

### 6.1 `CreateResumeRequest`

| 字段 | 约束 |
|---|---|
| `title` | 可选，最大 128 字符；为空时默认生成 `我的简历 N` |
| `scene` | 必填，必须是 `BizConstant.SCENES` 之一 |
| `targetPosition` | 可选 |
| `templateId` | 必填，必须指向有效模板 |

### 6.2 `UpdateResumeRequest`

| 字段 | 约束 |
|---|---|
| `title` | 可选 |
| `scene` | 可选 |
| `targetPosition` | 可选 |
| `templateId` | 可选 |
| `sections` | 可选；提供时做结构校验 |
| `renderSettings` | 可选；包含一页适配、字体、字号、行高、边距、模块间距和主题色 |

### 6.3 `RenameResumeRequest`

| 字段 | 约束 |
|---|---|
| `title` | 必填，最大 128 字符 |

---

## 7. 业务规则

### 7.1 创建简历

- 根据 `templateId` 校验模板是否存在且 `status=active`。
- 默认生成 6 个 Section（profile、education、project、work、skill、introduction），`data` 初始为空。

### 7.2 更新简历

- 仅更新传入字段。
- `sections` 更新时做结构校验：`id` 不重复、`type` 合法、字段完整。
- 更新后刷新 `lastEditedAt`。

### 7.3 删除简历

- 逻辑删除 `deleted=1`。
- 同步清理关联的 `pdf_task`、`avatar_task` 记录及 MinIO 文件。

### 7.4 复制简历

- 复制标题加 ` 副本`。
- 复制 `sections` 内容。
- 不复制 PDF/头像任务记录。

### 7.5 AI 点评

- 优先异步调用已配置的 LLM；未配置供应商或调用失败时按降级策略返回占位结果。
- 简历内容字符数少于 20 时返回 `RESUME_CONTENT_TOO_SHORT`。
- 每次点评插入一条 `resume_review` 记录。

### 7.6 预览

- `GET /resumes/{resumeId}/preview` 直接返回 HTML。
- `templateId` 参数仅覆盖本次预览，不修改简历。

---

## 8. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant`、`MinioStorageService`、`ResumeRenderService` |
| `user` | 通过 Spring Security 获取当前 userId |
| `template` | 校验模板存在性、预览时读取模板 |
| `pdf` | 删除简历时清理 `pdf_task` 与 MinIO PDF 文件 |
| `avatar` | 删除简历时清理 `avatar_task` 与 MinIO 头像文件 |
| `ai` | 异步 AI 点评、优化任务与调用审计 |

---

## 9. 错误码

| 错误码 | 常量 | 含义 |
|---|---|---|
| 2000 | `RESUME_SCENE_INVALID` | 使用场景不合法 |
| 2001 | `RESUME_TEMPLATE_NOT_FOUND` | 模板不存在 |
| 2002 | `RESUME_NOT_FOUND` | 简历不存在 |
| 2003 | `RESUME_SECTION_INVALID` | Section 数据不合法 |
| 2004 | `RESUME_PROFILE_NAME_REQUIRED` | 导出/预览时缺少姓名 |
| 2005 | `RESUME_PROFILE_CONTACT_REQUIRED` | 导出/预览时缺少联系方式 |
| 2006 | `RESUME_CONTENT_TOO_SHORT` | 简历内容过少 |
| 2007 | `RESUME_PROFILE_PHONE_INVALID` | 手机号格式错误 |
| 2008 | `RESUME_PROFILE_EMAIL_INVALID` | 邮箱格式错误 |
| 2009 | `RESUME_PROFILE_URL_INVALID` | URL 格式错误 |
| 2010 | `RESUME_CONTENT_TOO_LONG` | 简历内容过长 |

---

## 10. 开发约束

- 所有查询必须校验 `resume.userId == 当前 userId`，否则抛 `ACCESS_DENIED`（403）。
- Controller 返回 `R<T>`；预览接口除外，返回 `text/html`。
- `sections` 必须使用 TypeHandler 强类型映射。
- 删除简历必须同步清理关联任务与对象存储文件。

---

## 11. 测试要求

- 测试目录：`backend/src/test/java/com/resume/resume/`
- 必须覆盖：
  - 创建/更新/删除/复制/重命名
  - Section 结构校验
  - 越权访问拦截
  - AI 点评真实调用与未配置供应商时的降级行为
- 运行：`mvn test -Dtest=com.resume.resume.**`

---

## 12. 相关文档

- `../../../docs/superpowers/specs/2026-07-03-api-spec.md` §7
- `../../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.2、§4.1、§4.4、§5
- `../../../docs/superpowers/specs/2026-07-03-validation-rules.md` §3–§8、§11、§12、§13
- `../../../docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `../../../docs/superpowers/specs/2026-07-03-tdd-test-plan.md` §2.1、§2.3、§3
- `../../../docs/adr/ADR-002-json-sections.md`
