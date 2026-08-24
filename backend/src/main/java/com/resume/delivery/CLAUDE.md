# delivery 模块 — Claude 约束

> 作用：用户投递记录管理与管理端投递数据统计。
> 范围：`backend/src/main/java/com/resume/delivery/`。
> 必读：`backend/CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

- 投递记录 CRUD（创建 / 分页查询 / 详情 / 更新进度 / 逻辑删除）
- 投递状态机：`delivered` / `written` / `interview1` / `interview2` / `hr` / `offer` / `rejected` / `withdrawn`
- 管理端全平台投递列表、状态统计、热门岗位 TOP5、CSV 导出

---

## 2. 包目录结构

```
com.resume.delivery/
├── controller/
│   ├── DeliveryController.java      # 用户端 /deliveries
│   └── AdminDeliveryController.java # 管理端 /admin/deliveries
├── service/
│   └── DeliveryService.java
├── mapper/
│   └── DeliveryRecordMapper.java
├── entity/
│   └── DeliveryRecord.java
└── dto/
    ├── DeliveryRecordRequest.java
    ├── DeliveryRecordResponse.java
    └── DeliveryStatsResponse.java
```

---

## 3. HTTP API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/deliveries` | 创建投递记录（校验简历归属） |
| GET | `/deliveries` | 分页查询（keyword/company/position/status/date 筛选） |
| GET | `/deliveries/{id}` | 详情（校验归属） |
| PUT | `/deliveries/{id}` | 更新（进度 / 面试安排 / 备注） |
| DELETE | `/deliveries/{id}` | 逻辑删除 |
| GET | `/admin/deliveries` | 管理端全平台分页列表 |
| GET | `/admin/deliveries/stats` | 总数 + 状态分布 + TOP5 岗位 |
| GET | `/admin/deliveries/export` | CSV 导出（UTF-8 BOM） |

---

## 4. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant` |
| `resume` | 校验投递记录关联的简历存在且归属当前用户 |

---

## 5. 开发约束

- 创建 / 更新必须校验 `DeliveryRecordRequest.status` 属于状态机枚举。
- 所有用户端查询必须校验 `record.userId == 当前 userId`。
- 删除仅逻辑删除（`deleted = 1`）。
- CSV 导出加 UTF-8 BOM，便于 Excel 识别中文。

---

## 6. 测试要求

- 测试目录：`backend/src/test/java/com/resume/delivery/`
- 必须覆盖：创建（含越权简历 / 非法状态）、详情越权、更新、逻辑删除、分页、统计聚合。
- 运行：`mvn test -Dtest=com.resume.delivery.**`