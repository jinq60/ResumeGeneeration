# ADR-004：模板作为后台可管理资源

## 状态

已接受（Accepted）

## 背景

早期讨论中，模板数量存在纠结：是否需要固定 6 套内置模板？是否允许用户自定义？若模板写死在前端代码中，会带来：

- 新增模板需发版前端。
- 模板与简历数据耦合，后续修改模板可能影响已生成简历。
- 无法支持运营推荐、付费模板等扩展。

## 决策

将模板设计为 **后台可动态管理的资源**：

- `template` 表存储模板元数据、配置 JSON、HTML 模板路径。
- 提供 `/admin/templates` CRUD 接口供后台管理。
- 前台通过 `/templates` 获取可用模板列表。
- 简历中只保存 `templateId`，不保存模板完整内容，避免模板更新影响历史简历。

## 理由

1. **运营灵活**：可随时新增/上下架/推荐模板，无需发版。
2. **扩展性好**：P2 可自然支持模板市场、付费模板。
3. **解耦**：模板渲染逻辑与简历数据分离。
4. **一致性**：服务端与客户端从同一份 `template.config` 渲染，保证 PDF 与预览一致。

## 影响

- 需要开发后台管理接口。
- 模板表字段较丰富：`code`、`render_engine`、`is_builtin`、`is_premium`、`sort_order`、`version` 等。
- 系统内置模板通过初始化 SQL 插入，`is_builtin = 1` 表示不可物理删除。
- 模板渲染服务需读取 `template.config` 和 `html_template`。

## 相关文档

- `docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `docs/superpowers/specs/2026-07-03-api-spec.md` §8、§10
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.6
