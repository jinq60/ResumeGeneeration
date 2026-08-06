# pdf 模块 — Claude 约束

> 作用：简历 PDF 导出任务。
> 范围：`backend/src/main/java/com/resume/pdf/`。
> 必读：`backend/CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

`pdf` 模块负责：

- 接收 PDF 导出请求
- 校验简历数据完整性
- 使用模板渲染 HTML
- 通过 Playwright 将 HTML 转换为 A4 PDF
- 上传 PDF 到 MinIO
- 提供任务查询与 PDF 下载

---

## 2. 包目录结构

```
com.resume.pdf/
├── controller/
│   └── PdfController.java
├── dto/
│   ├── ExportPdfRequest.java
│   ├── PdfTaskResponse.java
│   └── SectionDTO.java
├── entity/
│   └── PdfTask.java
├── mapper/
│   └── PdfTaskMapper.java
└── service/
    └── PdfService.java
```

---

## 3. HTTP API 端点

Base URL：`http://localhost:8080/api`

所有 `/pdf/**` 接口需 JWT 认证。

| 方法 | 路径 | 请求 | 响应 | 说明 |
|---|---|---|---|---|
| POST | `/pdf/export` | `ExportPdfRequest` | `R<{id, status}>` | 创建 PDF 导出任务 |
| GET | `/pdf/tasks/{taskId}` | 路径参数 | `R<PdfTaskResponse>` | 查询任务状态 |
| GET | `/pdf/download/{taskId}` | 路径参数 | PDF 二进制流 | 下载 PDF |

---

## 4. 关键实体 `PdfTask`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | String | 任务 ID |
| `userId` | String | 用户 ID |
| `resumeId` | String | 关联简历 ID |
| `templateId` | String | 本次导出使用的模板 ID |
| `filePath` | String | MinIO 对象路径 |
| `fileName` | String | 下载文件名 |
| `fileSize` | Long | 文件大小（字节） |
| `status` | String | `pending` / `processing` / `success` / `failed` |
| `errorMsg` | String | 失败原因 |
| `completedAt` | LocalDateTime | 完成时间 |
| `deleted` | Integer | 逻辑删除 |
| `createdAt` / `updatedAt` | LocalDateTime | 时间戳 |

---

## 5. DTO 请求约束

### 5.1 `ExportPdfRequest`

| 字段 | 约束 |
|---|---|
| `resumeId` | 必填 |
| `templateId` | 可选；不传则使用简历当前 `templateId` |

### 5.2 `PdfTaskResponse`

| 字段 | 说明 |
|---|---|
| `id` | 任务 ID |
| `resumeId` | 简历 ID |
| `templateId` | 模板 ID |
| `fileName` | 文件名 |
| `fileSize` | 文件大小 |
| `status` | 任务状态 |
| `errorMsg` | 失败原因 |
| `createdAt` / `completedAt` | 时间戳 |

---

## 6. 业务规则

### 6.1 导出流程

1. 校验简历存在、用户有权限。
2. 校验简历包含姓名和至少一个联系方式（手机/邮箱）。
3. 确定模板：`request.templateId` 优先，否则 `resume.templateId`。
4. 调用 `ResumeRenderService.render(resume, template)` 生成 HTML。
5. 使用 Playwright Chromium 导出 A4 PDF（`setFormat("A4")`、`setPrintBackground(true)`）。
6. 上传 PDF 到 MinIO `resume-pdfs` Bucket。
7. 更新 `pdf_task` 状态为 `success`，记录文件路径、大小。

### 6.2 命名规则

| 条件 | 文件名示例 |
|---|---|
| 有姓名 + 有目标岗位 | `张三_Java后端开发_简历.pdf` |
| 有姓名 + 无目标岗位 | `张三_简历.pdf` |
| 无姓名 | `我的简历_yyyyMMdd.pdf` |

- 姓名从 `resume.sections.profile.name` 读取。
- 目标岗位从 `resume.targetPosition` 读取。

### 6.3 文件路径

- Bucket：`resume-pdfs`
- 路径：`{userId}/pdfs/{pdfTaskId}/{fileName}`

### 6.4 下载

- 校验任务属于当前用户。
- 校验任务状态为 `success`。
- 从 MinIO 读取 PDF 二进制流返回。

---

## 7. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant`、`MinioStorageService`、`ResumeRenderService` |
| `user` | 获取当前 userId |
| `resume` | 读取简历数据 |
| `template` | 读取模板配置与 HTML 模板 |

---

## 8. 错误码

| 错误码 | 常量 | 含义 |
|---|---|---|
| 5000 | `PDF_TASK_NOT_FOUND` | 任务不存在 |
| 5001 | `PDF_FILE_NOT_READY` | 文件未生成 |
| 5002 | `PDF_EXPORT_NAME_REQUIRED` | 缺少姓名 |
| 5003 | `PDF_EXPORT_CONTACT_REQUIRED` | 缺少联系方式 |
| 5004 | `PDF_EXPORT_FAILED` | 导出失败 |

---

## 9. 开发约束

- PDF 导出必须校验简历完整性（姓名 + 联系方式）。
- 渲染 HTML 与预览接口共用 `ResumeRenderService`，保证一致性。
- Playwright Chromium 参数需包含 `--no-sandbox`、`--disable-setuid-sandbox`。
- 任务状态机：`pending → processing → success / failed`。
- 下载接口返回二进制流，不包装 `R<T>`。

---

## 10. 测试要求

- 测试目录：`backend/src/test/java/com/resume/pdf/`
- 必须覆盖：
  - 导出流程（Mock MinIO、Mock Playwright）
  - 缺少姓名/联系方式的校验
  - 越权下载
  - 任务状态查询
- 运行：`mvn test -Dtest=com.resume.pdf.**`

---

## 11. 相关文档

- `../../../docs/superpowers/specs/2026-07-03-api-spec.md` §11
- `../../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.5
- `../../../docs/superpowers/specs/2026-07-03-validation-rules.md` §10
- `../../../docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `../../../docs/superpowers/specs/2026-07-03-tdd-test-plan.md` §2.1
