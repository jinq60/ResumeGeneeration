# notification 模块 — Claude 约束

> 作用：用户通知中心的写入与查询。
> 范围：`backend/src/main/java/com/resume/notification/`。
> 必读：`backend/CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

- 系统事件通知写入（PDF 导出完成、AI 点评完成、头像优化完成）
- 通知分页查询、未读数、标记已读 / 全部已读、逻辑删除

---

## 2. 包目录结构

```
com.resume.notification/
├── controller/
│   └── NotificationController.java # /notifications
├── service/
│   └── NotificationService.java
├── mapper/
│   └── NotificationMapper.java
├── entity/
│   └── Notification.java
└── dto/
    └── NotificationResponse.java
```

---

## 3. HTTP API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/notifications` | 分页查询（`unreadOnly` 可选） |
| GET | `/notifications/unread-count` | 未读数 |
| PUT | `/notifications/{id}/read` | 标记已读 |
| PUT | `/notifications/read-all` | 全部已读 |
| DELETE | `/notifications/{id}` | 逻辑删除 |

---

## 4. 事件接入点

| 事件 | 位置 | 类型 |
|---|---|---|
| PDF 导出完成 | `pdf/service/PdfService.generatePdf` 成功分支 | `pdf` |
| AI 点评完成 | `ai/service/AiResumeReviewService.executeReview` finally 成功分支 | `ai` |
| 头像优化完成 | `ai/service/AiAvatarService.executeOptimize` finally 成功分支 | `avatar` |

接入时调用 `NotificationService.notify(userId, type, title, content)`，写入失败不影响主流程。

---

## 5. 开发约束

- `notify` 为 `@Transactional` 写入，user_id 为空时直接忽略。
- 所有用户端操作必须校验 `notification.userId == 当前 userId`。
- 删除仅逻辑删除。

---

## 6. 测试要求

- 测试目录：`backend/src/test/java/com/resume/notification/`
- 必须覆盖：写入（含空 userId 忽略）、分页、未读数、标记已读（含越权）、全部已读、逻辑删除。
- 运行：`mvn test -Dtest=com.resume.notification.**`