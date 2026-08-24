# audit 模块 — Claude 约束

> 作用：用户内容（简历）合规审核记录的管理。
> 范围：`backend/src/main/java/com/resume/audit/`。
> 必读：`backend/CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

- 内容创建时自动生成待审核记录（当前支持简历）
- 管理端审核列表、统计、通过 / 驳回 / 标记风险

---

## 2. 包目录结构

```
com.resume.audit/
├── controller/
│   └── AdminAuditController.java # /admin/audits
├── service/
│   └── ContentAuditService.java
├── mapper/
│   └── ContentAuditMapper.java
├── entity/
│   └── ContentAudit.java
└── dto/
    ├── AuditItemResponse.java
    ├── AuditStatsResponse.java
    └── AuditReviewRequest.java
```

---

## 3. HTTP API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/audits` | 分页查询（keyword/status/riskLevel） |
| GET | `/admin/audits/stats` | 各状态统计 + 今日已审 |
| POST | `/admin/audits/{id}/approve` | 通过 |
| POST | `/admin/audits/{id}/reject` | 驳回 |
| POST | `/admin/audits/{id}/mark-warning` | 标记风险（可选 riskLevel） |

## 4. 生成入口

- `resume/service/ResumeService.createResume` 与 `ResumeImportService.importResume`
  创建简历后调用 `ContentAuditService.createForResume(userId, resumeId, title)`，
  初始状态 `pending`、风险 `low`。

---

## 5. 开发约束

- 审核动作仅允许 `approve` / `reject` / `warning`；`warning` 时风险等级必须是 `low|medium|high`。
- 审核记录记录 `reviewerId` / `reviewNote` / `reviewedAt`。
- 无物理删除；保留历史审核记录。

---

## 6. 测试要求

- 测试目录：`backend/src/test/java/com/resume/audit/`
- 必须覆盖：生成记录、分页、统计、三种审核动作、非法动作 / 非法风险等级 / 记录不存在。
- 运行：`mvn test -Dtest=com.resume.audit.**`