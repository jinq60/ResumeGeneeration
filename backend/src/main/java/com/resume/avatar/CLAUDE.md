# avatar 模块 — Claude 约束

> 作用：头像上传与一寸照优化任务。
> 范围：`backend/src/main/java/com/resume/avatar/`。
> 必读：`../CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

`avatar` 模块负责：

- 自拍照上传至 MinIO
- 创建一寸照优化任务
- 查询优化任务状态与结果
- 删除头像原图与优化结果

**P0 占位策略**：优化任务不接入真实 AI，仅复制原图作为结果；P1 替换 `AvatarService.optimize` 内部实现即可。

---

## 2. 包目录结构

```
com.resume.avatar/
├── controller/
│   └── AvatarController.java
├── dto/
│   ├── AvatarUploadResponse.java
│   ├── AvatarTaskResponse.java
│   └── OptimizeAvatarRequest.java
├── entity/
│   └── AvatarTask.java
├── mapper/
│   └── AvatarTaskMapper.java
└── service/
    └── AvatarService.java
```

---

## 3. HTTP API 端点

Base URL：`http://localhost:8080/api`

所有 `/avatars/**` 接口需 JWT 认证。

| 方法 | 路径 | 请求 | 响应 | 说明 |
|---|---|---|---|---|
| POST | `/avatars/upload` | `MultipartFile file` + 可选 `resumeId` | `R<AvatarUploadResponse>` | 上传自拍照 |
| POST | `/avatars/optimize` | `OptimizeAvatarRequest` | `R<{id, status}>` | 创建一寸照优化任务 |
| GET | `/avatars/tasks/{taskId}` | 路径参数 | `R<AvatarTaskResponse>` | 查询任务 |
| DELETE | `/avatars/{id}` | 路径参数 | `R<Void>` | 删除头像与任务 |

---

## 4. 关键实体 `AvatarTask`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | String | 任务 ID |
| `userId` | String | 用户 ID |
| `resumeId` | String | 关联简历 ID（可空） |
| `sourceImageUrl` | String | 自拍照原图 URL |
| `resultImageUrl` | String | 一寸照结果 URL |
| `backgroundType` | String | 背景：`white` / `blue` / `red` |
| `style` | String | 风格：`formal` / `natural` / `professional` |
| `options` | String（JSON） | 优化选项 |
| `status` | String | `pending` / `processing` / `success` / `failed` |
| `errorMsg` | String | 失败原因 |
| `completedAt` | LocalDateTime | 完成时间 |
| `deleted` | Integer | 逻辑删除 |
| `createdAt` / `updatedAt` | LocalDateTime | 时间戳 |

---

## 5. DTO 请求约束

### 5.1 `OptimizeAvatarRequest`

| 字段 | 约束 |
|---|---|
| `sourceImageUrl` / `avatarTaskId` | 必填其一，标识原图 |
| `backgroundType` | 必填，枚举 `white` / `blue` / `red` |
| `style` | 必填，枚举 `formal` / `natural` / `professional` |
| `options` | 可选，优化选项对象 |
| `resumeId` | 可选，优化成功后回填到简历 `profile.avatarUrl` |

### 5.2 文件上传约束

- 格式：JPG、PNG、WEBP
- 大小：≤ 10MB
- 文件名重命名为 UUID
- 校验 MIME 类型与扩展名

---

## 6. 业务规则

### 6.1 上传

- 文件写入 MinIO `resume-avatars` Bucket。
- 路径：`{userId}/avatars/{avatarTaskId}_source.{ext}`
- 创建 `avatar_task` 记录，状态初始化为 `pending`。
- 若传入 `resumeId`，建立关联但不自动回填到简历。

### 6.2 优化（P0 占位）

- 校验参数合法。
- 将任务状态从 `pending` → `processing` → `success`。
- `resultImageUrl` 直接设置为 `sourceImageUrl`。
- 若传入 `resumeId`，将 `resultImageUrl` 回填到对应简历 `profile.avatarUrl`。

### 6.3 查询任务

- 校验任务属于当前用户。
- 返回任务状态、结果 URL、错误信息。

### 6.4 删除

- 逻辑删除 `avatar_task`。
- 同步删除 MinIO 中的 `sourceImageUrl` 和 `resultImageUrl` 对应对象。

---

## 7. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant`、`MinioStorageService` |
| `user` | 获取当前 userId |
| `resume`（可选） | 回填 `profile.avatarUrl` |

---

## 8. 错误码

| 错误码 | 常量 | 含义 |
|---|---|---|
| 4000 | `AVATAR_FILE_EMPTY` | 文件为空 |
| 4001 | `AVATAR_FORMAT_UNSUPPORTED` | 不支持的格式 |
| 4002 | `AVATAR_FILE_TOO_LARGE` | 文件过大 |
| 4003 | `AVATAR_SOURCE_NOT_FOUND` | 原图不存在 |
| 4004 | `AVATAR_BACKGROUND_TYPE_INVALID` | 背景类型不合法 |
| 4005 | `AVATAR_STYLE_INVALID` | 风格不合法 |
| 4006 | `AVATAR_TASK_NOT_FOUND` | 任务不存在 |
| 4007 | `AVATAR_OPTIMIZE_FAILED` | 优化失败 |

---

## 9. 开发约束

- P0 优化为占位实现，保持接口契约不变。
- 所有任务必须校验 `userId`，防止越权。
- 文件路径使用 UUID 重命名，禁止暴露原始文件名。
- 删除时必须同步清理 MinIO。

---

## 10. 测试要求

- 测试目录：`backend/src/test/java/com/resume/avatar/`
- 必须覆盖：
  - 上传文件格式/大小校验
  - 优化任务状态机
  - 越权查询/删除
  - 删除时 MinIO 清理
- 运行：`mvn test -Dtest=com.resume.avatar.**`

---

## 11. 相关文档

- `../../../docs/superpowers/specs/2026-07-03-api-spec.md` §9
- `../../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.4、§3
- `../../../docs/superpowers/specs/2026-07-03-validation-rules.md` §9
- `../../../docs/superpowers/specs/2026-07-03-tdd-test-plan.md` §2.1
- `../../../docs/adr/ADR-003-placeholder-avatar-ai.md`
