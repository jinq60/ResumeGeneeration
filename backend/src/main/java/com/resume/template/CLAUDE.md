# template 模块 — Claude 约束

> 作用：简历模板的后台管理与前台查询。
> 范围：`backend/src/main/java/com/resume/template/`。
> 必读：`../CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

`template` 模块负责：

- 前台模板列表与详情查询（无需认证）
- 后台模板 CRUD、上下架、排序、推荐管理
- 模板配置 JSON 的序列化/反序列化
- 为 `resume`、`pdf`、`ResumeRenderService` 提供模板数据

---

## 2. 包目录结构

```
com.resume.template/
├── controller/
│   ├── TemplateController.java      # 前台 /templates
│   └── AdminTemplateController.java # 后台 /admin/templates
├── service/
│   └── TemplateService.java
├── mapper/
│   └── TemplateMapper.java
├── entity/
│   └── Template.java
└── dto/
    ├── TemplateDTO.java
    ├── AdminTemplateRequest.java
    └── AdminTemplateListItemResponse.java
```

---

## 3. HTTP API 端点

Base URL：`http://localhost:8080/api`

### 3.1 前台接口（permitAll）

| 方法 | 路径 | 请求 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/templates` | 无 | `R<List<TemplateDTO>>` | 已上架模板列表，按 sortOrder 升序 |
| GET | `/templates/{id}` | 路径参数 | `R<TemplateDTO>` | 模板详情 |

### 3.2 后台接口（需认证，P0 未限制管理员角色）

| 方法 | 路径 | 请求 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/admin/templates` | `page`、`size`、`status`、`category` | `R<Page<AdminTemplateListItemResponse>>` | 后台模板列表 |
| GET | `/admin/templates/{id}` | 路径参数 | `R<TemplateDTO>` | 后台模板详情 |
| POST | `/admin/templates` | `AdminTemplateRequest` | `R<TemplateDTO>` | 创建模板 |
| PUT | `/admin/templates/{id}` | `AdminTemplateRequest` | `R<TemplateDTO>` | 更新模板 |
| PATCH | `/admin/templates/{id}/status` | `{"status": "active|inactive"}` | `R<Void>` | 上下架 |
| DELETE | `/admin/templates/{id}` | 路径参数 | `R<Void>` | 逻辑删除；仅非内置模板可删 |

---

## 4. 关键实体 `Template`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | String | Snowflake ID |
| `code` | String | 唯一编码，如 `classic-single` |
| `name` | String | 模板名称 |
| `category` | String | 分类：classic/tech/fresh/business/postgraduate |
| `thumbnailUrl` | String | 缩略图 URL |
| `description` | String | 描述 |
| `config` | Object（JSON） | 模板配置：页面、字体、颜色、布局等；数据库以 JSON 对象存储，实体通过 JacksonTypeHandler 直接映射为 Object |
| `htmlTemplate` | String | HTML 模板文件名或路径 |
| `renderEngine` | String | `server` / `client` / `hybrid`，默认 `server` |
| `isBuiltin` | Integer | `1` 系统内置，不可删除 |
| `isPremium` | Integer | `1` 付费模板（P2 预留，当前固定 0） |
| `isRecommended` | Integer | `1` 首页推荐 |
| `sortOrder` | Integer | 排序权重，越小越靠前 |
| `status` | String | `active` / `inactive` |
| `createdBy` | String | 创建人 ID |
| `version` | Integer | 版本号，更新自动 +1 |
| `deleted` | Integer | 逻辑删除 |
| `createdAt` / `updatedAt` | LocalDateTime | 时间戳 |

---

## 5. DTO 请求约束

### 5.1 `AdminTemplateRequest`

| 字段 | 约束 |
|---|---|
| `code` | 必填，`@NotBlank`，最大 64 字符 |
| `name` | 必填，最大 64 字符 |
| `category` | 必填，最大 32 字符 |
| `thumbnailUrl` | 可选，最大 512 字符 |
| `description` | 可选，最大 512 字符 |
| `config` | 必填，`@NotNull`，任意 JSON 对象 |
| `htmlTemplate` | 必填，最大 128 字符 |
| `renderEngine` | 可选，默认 `server` |
| `sortOrder` | 可选，默认 0 |
| `isRecommended` | 可选，默认 false |

---

## 6. 业务规则

### 6.1 创建模板

- `code` 全局唯一。
- `isBuiltin` 固定为 0，`isPremium` 固定为 0。
- `createdBy` 写入当前用户 ID。

### 6.2 更新模板

- `code` 不可修改。
- `version` 自动 +1。
- 内置模板（`isBuiltin=1`）允许更新配置，但不允许删除。

### 6.3 删除模板

- 仅允许删除非内置模板。
- 逻辑删除，不物理删除。

### 6.4 上下架

- 仅支持 `active` / `inactive` 两种状态。

### 6.5 前台列表

- 只返回 `status=active` 且 `deleted=0` 的模板。
- 按 `sortOrder` 升序、`createdAt` 降序排列。

---

## 7. 模板 `config` JSON 关键字段

| 字段路径 | 说明 |
|---|---|
| `page.width` / `page.height` / `page.margin` / `page.background` | A4 页面配置 |
| `font.family` / `font.mainSize` / `font.titleSize` / `font.smallSize` / `font.lineHeight` | 字体与字号 |
| `color.primary` / `color.secondary` / `color.accent` / `color.background` / `color.sidebar` | 配色 |
| `layout.singleColumn` / `layout.sidebarWidth` / `layout.avatar.*` | 布局与头像 |
| `sectionTitle.*` | 模块标题样式 |
| `skill.displayStyle` | `tag` / `category` / `level` |
| `moduleSpacing` | 模块间距 |

---

## 8. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant` |
| `user`（认证上下文） | 获取当前用户 ID 写入 `createdBy` |

**注意**：`template` 不直接依赖 `resume`、`pdf`、`avatar`。

---

## 9. 错误码

| 错误码 | 常量 | 含义 |
|---|---|---|
| 3000 | `TEMPLATE_NOT_FOUND` | 模板不存在 |
| 3001 | `TEMPLATE_CODE_EXISTS` | 编码已存在 |
| 3002 | `TEMPLATE_CONFIG_INVALID` | 配置 JSON 不合法 |
| 3003 | `TEMPLATE_CODE_IMMUTABLE` | 编码不可修改 |
| 3004 | `TEMPLATE_BUILTIN_PROTECTED` | 内置模板不可删除 |

---

## 10. 开发约束

- `config` 在实体中为 `Object`（通过 `JacksonTypeHandler` 映射为 MySQL JSON 对象），DTO 中为 `Object`；Service 层在写入前使用 `ObjectMapper` 校验其可序列化。
- 模板更新必须自动递增 `version`，用于缓存控制。
- 内置模板受保护，不可删除。
- 前台接口允许匿名访问。

---

## 11. 测试要求

- 测试目录：`backend/src/test/java/com/resume/template/`
- 必须覆盖：
  - 前台列表/详情
  - 后台 CRUD、上下架、删除保护
  - `code` 唯一性
  - 内置模板保护
- 运行：`mvn test -Dtest=com.resume.template.**`

---

## 12. 相关文档

- `../../../docs/superpowers/specs/2026-07-03-api-spec.md` §8、§10
- `../../../docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `../../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.6、§4.3、§6.6
- `../../../docs/superpowers/specs/2026-07-03-tdd-test-plan.md` §2.1
- `../../../docs/adr/ADR-004-template-as-managed-resource.md`
