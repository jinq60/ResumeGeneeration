# ADR-002：简历内容模块使用 JSON 数组而非关系型表

## 状态

已接受（Accepted）

## 背景

简历内容包含多种动态模块（个人信息、教育、项目、工作、技能、自我介绍等），每个模块的字段差异大，且未来可能新增模块类型。传统关系型设计需要为每种模块建表，导致：

- DDL 频繁变更。
- 模块排序、显隐、扩展需要跨表操作。
- 前端需要组装多表数据。

## 决策

将简历内容以 **JSON 数组** 形式存储在 `resume.sections` 字段中，统一 Section 结构：`{ id, type, title, order, visible, data }`。

## 理由

1. **灵活性**：新增模块类型只需扩展枚举和校验规则，无需改表结构。
2. **原子性**：一份简历的所有内容在一个字段中，读取/保存都是单行操作。
3. **排序与显隐简单**：`order` 和 `visible` 直接作为 JSON 字段维护。
4. **版本兼容**：旧数据可保留历史字段，新代码忽略即可。
5. **适合读多写少**：简历编辑和导出以读取整份简历为主，JSON 性能可接受。

## 影响

- 后端需使用 MyBatis-Plus TypeHandler 将 JSON 映射为强类型集合（`List<Section>`），禁止以 `String` 手动解析。
- 前端 `Section` 类型定义为 union type，`data` 随 `type` 变化。
- 业务校验必须在应用层完成，数据库约束较弱。
- 复杂查询（如“查找所有包含 Vue 技能的简历”）需借助 MySQL JSON 函数或搜索引擎，当前 P0 不涉及。

## 相关文档

- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.2、§4.1、§4.4
- `docs/superpowers/specs/2026-07-03-validation-rules.md`
- `frontend/src/types/resume.ts`
