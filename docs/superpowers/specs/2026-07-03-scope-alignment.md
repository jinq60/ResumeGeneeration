# 简历生成工具 P0 范围与模型对齐书

> 版本：v1.1  
> 日期：2026-07-07  
> 作用：记录初始 P0 范围，消除 `docs/需求PRD-v1.md` 与 `docs/superpowers/specs/2026-07-03-resume-generation-design.md` 之间的冲突。后续 v1.4 功能扩展以 `docs/2026-08-05-business-line-roadmap.md`、`docs/api-changelog.md` 和当前 API 规范为准。

---

## 1. 对齐原则

1. **MVP（P0）只包含闭环所需的最小能力集**：用户能注册/登录、创建简历、填写核心模块、上传头像、实时预览、导出 PDF。
2. **P1/P2 能力仅预留接口与数据结构扩展点**：不在 P0 中实现真实 AI 模型、模板市场、Word 导出、在线分享等。
3. **PRD 与设计文档冲突时，以“不增加 P0 风险”为优先**：范围宁可收缩，也不盲目扩张。
4. **所有文档使用同一套术语表**：`scene`、`templateId`、`sectionType`、错误码等必须统一。
5. **历史版本可追溯**：本对齐书记录相对于 PRD-v1 和设计文档 v1 的变更，不直接修改原文件。

---

## 2. P0 范围最终清单

### 2.1 包含项（P0）

| 模块 | 包含功能 | 说明 |
|---|---|---|
| 用户认证 | 手机号/邮箱/验证码注册登录、游客模式、JWT 鉴权、Token 刷新 | 微信/Google/GitHub 登录为 P2 |
| 简历管理 | 新建简历、简历列表、获取详情、更新、删除（逻辑删除）、重命名、复制 | 列表分页、设为常用版本为 P1 |
| 简历编辑器 | 左侧表单、右侧实时预览、模块增删改、模块排序 | 拖拽排序为 P1，P0 用按钮排序 |
| 个人信息模块 | 姓名、性别、出生年月、手机号、邮箱、城市、目标岗位、期望薪资、到岗时间、个人网站、GitHub、作品集、头像 | age 由 birthDate 计算，不独立存储 |
| 教育经历模块 | 学校、学历、专业、学院、时间、GPA、排名、荣誉、课程 | 支持多段、倒序 |
| 项目经历模块 | 项目名称、角色、类型、时间、技术栈、描述、背景、职责、成果、链接 | 描述数组为必填展示形式 |
| 工作经历模块 | 公司、部门、职位、类型、城市、时间、描述、成果、技术栈、离职原因 | 离职原因默认不展示 |
| 技能技术栈模块 | 分类、技能名称、熟练程度、是否重点展示 | 支持标签/分类/等级展示 |
| 自我介绍模块 | 正文、关键词标签、风格选择、字数限制 | AI 润色为 P2 |
| 头像上传 | 上传、裁剪、原图应用、删除 | 格式 JPG/PNG/WEBP，≤10MB |
| 头像优化（占位） | 创建白底/蓝底/红底/职业照/自然清爽/高清修复等优化任务 | MVP 返回占位结果，真实模型 P1 接入 |
| 模板系统 | 至少 4 套内置模板、模板切换、模板配置 | 模板市场为 P2 |
| PDF 导出 | A4 尺寸、中文字体、高清头像、多页、文件命名 | 服务端 Playwright 渲染 |
| 自动保存 | 输入停止 2 秒后自动保存、保存状态提示、离开确认 | 游客优先 localStorage |
| 数据安全 | HTTPS、数据隔离、XSS 防护、文件校验、隐私删除 | 按 PRD 13 节执行 |

### 2.2 不包含项（P1/P2）

| 功能 | 阶段 | 说明 |
|---|---|---|
| AI 简历内容润色 | P2 | 自我介绍、项目经历、工作经历润色 |
| 岗位 JD 匹配分析 | P2 | 根据岗位描述优化简历 |
| 简历评分 | P2 | 完成度、质量评分 |
| 多语言简历 | P2 | 中英等多语言 |
| Word 导出 | P2 | 仅 P0 支持 PDF |
| 在线分享链接 | P2 | 公开/私密/密码访问 |
| 模板市场 | P2 | 用户可下载更多模板 |
| 批量简历生成 | P2 | 机构版功能 |
| 真实头像 AI 优化模型 | P1 | P0 仅占位实现，接口已预留 |
| 微信/Google/GitHub 登录 | P2 | P0 仅手机/邮箱/游客 |
| 模块拖拽排序 | P1 | P0 用上下按钮排序 |
| 简历投递记录管理 | P2 | 商业化运营功能 |

---

## 3. 数据模型冲突决策表

### 3.1 个人信息 Profile

| 字段 | PRD-v1 | 设计文档 v1 | 最终决策 | 备注 |
|---|---|---|---|---|
| `age` | 有 | 无 | 不存储，前端根据 `birthDate` 计算 | 由 `showAge` 控制展示 |
| `jobIntention` | 有（求职意向） | 无，合并到 `targetPosition` | 不保留独立字段，`targetPosition` 即目标岗位/求职意向 | 避免冗余 |
| `gender` | 有 | 有 | 保留 | 枚举值：`male` / `female` / `other` |
| `expectedSalary` | 有 | 有 | 保留 | 字符串，如 `15k-20k` |
| `availability` | 有（到岗时间） | 有 | 保留 | 字符串，如 `一周内到岗` |
| `personalWebsite` | 有 | 有（personalWebsite） | 保留 | URL 校验 |
| `github` | 有 | 有 | 保留 | URL 校验 |
| `portfolio` | 有 | 有 | 保留 | URL 校验 |
| `showGender` / `showAge` / `showSalary` / `showAvatar` | 有 | 有 | 保留 | 控制展示 |

### 3.2 项目经历 Project

| 字段 | PRD-v1 | 设计文档 v1 | 最终决策 | 备注 |
|---|---|---|---|---|
| `description` | `string[]`，3–5 条 | 无 | 保留为必填展示字段 | 每条建议 ≤40 字 |
| `background` | 无 | 有 | 保留为可选引导字段 | 用于填写提示，不强制展示 |
| `responsibility` | 无 | 有 | 保留为可选引导字段 | 同上 |
| `achievements` | 无 | `string[]` | 保留为可选数组 | 可合并到 description 展示 |
| `techStack` | 有 | 有 | 保留 | `string[]` |
| `link` / `github` | 有 | 有 | 保留 | URL 校验 |
| `demoLink` | PRD 有“作品展示链接” | 设计文档无 | 不保留 | 与 `link` 合并 |

**最终 JSON 结构示例**：

```json
{
  "name": "智能简历生成系统",
  "role": "前端开发",
  "type": "course_project",
  "startDate": "2026-03",
  "endDate": "2026-06",
  "techStack": ["Vue", "Spring Boot", "MySQL"],
  "background": "解决学生简历排版难问题",
  "responsibility": "负责编辑器页面开发",
  "achievements": ["实现实时预览", "导出 PDF"],
  "description": [
    "负责简历编辑器页面开发，实现左侧表单与右侧预览的实时联动。",
    "封装项目经历、教育经历、技能模块等动态表单组件。",
    "实现 PDF 导出功能，保证导出效果与页面预览一致。"
  ],
  "link": "https://example.com",
  "github": "https://github.com/example"
}
```

### 3.3 工作经历 WorkExperience

| 字段 | PRD-v1 | 设计文档 v1 | 最终决策 | 备注 |
|---|---|---|---|---|
| `description` | `string[]` | `string[]` | 保留 | 必填展示 |
| `achievements` | 有 | `string[]` | 保留 | 可选 |
| `leaveReason` | 有，默认不展示 | 无 | 保留 | 字符串，默认不展示 |
| `techStack` | 有 | 有 | 保留 | `string[]` |
| `workType` / `type` | 工作类型 | 有 `type` | 统一为 `type` | 枚举见术语表 |
| `city` | 有 | 有 | 保留 | 可隐藏 |

### 3.4 Section 统一模块模型

所有简历内容模块使用统一结构，便于排序、显隐和扩展。

```json
{
  "id": "sec_001",
  "type": "education",
  "title": "教育经历",
  "order": 1,
  "visible": true,
  "data": { }
}
```

`type` 枚举（P0）：

| 枚举值 | 含义 | 备注 |
|---|---|---|
| `profile` | 个人信息 | 每个简历唯一，通常置顶 |
| `education` | 教育经历 | 支持多段 |
| `project` | 项目经历 | 支持多段 |
| `work` | 工作经历 | 支持多段 |
| `skill` | 技能技术栈 | 支持多分类 |
| `introduction` | 自我介绍 | 单段文本 |

P1/P2 预留：`course`、`certificate`、`custom`、`campus`。

### 3.5 AvatarTask 头像优化任务

| 字段 | PRD-v1 | 设计文档 v1 | 最终决策 | 备注 |
|---|---|---|---|---|
| `backgroundType` | white / blue / red | white / blue / red / transparent | 保留 PRD 三种 + transparent | P0 仅 white/blue/red 生效 |
| `style` | 职业商务 / 自然清爽 | formal / natural / professional | 统一为：`formal`、`natural`、`professional` | 与 PRD 含义映射 |
| `options` | 多项勾选 | JSON | 保留 JSON options | 包含 keepIdentity / enhanceQuality / removeBackground / brightenSkin |
| `status` | 未明确 | pending/processing/success/failed | 采用设计文档状态机 | 异步任务 |

### 3.6 Resume 简历主表

| 字段 | PRD-v1 | 设计文档 v1 | 最终决策 | 备注 |
|---|---|---|---|---|
| `sections` | JSON | JSON | 保留 | Section 数组 |
| `scene` | 有 | 有 | 保留 | 枚举见术语表 |
| `targetPosition` | 有 | 有 | 保留 | 目标岗位 |
| `templateId` | 有 | 有 | 保留 | 当前模板 |
| `status` | 无 | active / deleted | 保留 | 逻辑删除标记 |

---

## 4. 接口路径决策

### 4.1 最终路径规范

统一采用 **复数资源路径**，符合 RESTful 习惯。

| 功能 | 旧路径（PRD） | 旧路径（设计文档） | 最终路径 |
|---|---|---|---|
| 上传头像 | `/api/avatar/upload` | `/api/avatars/upload` | `/api/avatars/upload` |
| 创建头像优化任务 | `/api/avatar/optimize` | `/api/avatars/optimize` | `/api/avatars/optimize` |
| 查询头像优化任务 | `/api/avatar/tasks/{taskId}` | `/api/avatars/tasks/{taskId}` | `/api/avatars/tasks/{taskId}` |
| 删除头像 | 无 | `/api/avatars/{id}` | `/api/avatars/{id}` |

### 4.2 其他接口路径确认

| 模块 | 路径前缀 | 说明 |
|---|---|---|
| 认证 | `/api/auth/*` | register / login / guest / refresh |
| 用户 | `/api/users/*` | me |
| 简历 | `/api/resumes/*` | CRUD + duplicate + title |
| 模板 | `/api/templates/*` | list + detail |
| PDF | `/api/pdf/*` | export + tasks + download |

---

## 5. 关键术语表

### 5.1 使用场景 `scene`

| 枚举值 | 中文 | 默认模块顺序 |
|---|---|---|
| `campus_recruitment` | 校招简历 | profile → education → project → work → skill → introduction |
| `internship` | 实习简历 | profile → education → project → work → skill → introduction |
| `social_recruitment` | 社招简历 | profile → work → project → education → skill → introduction |
| `postgraduate_reexam` | 考研复试 | profile → education → project → introduction → skill |
| `project_application` | 项目申报 | profile → education → project → introduction → skill |
| `custom` | 自定义 | profile → education → project → work → skill → introduction |

### 5.2 Section 类型 `sectionType`

见 3.4 节。

### 5.3 工作类型 `workType`

| 枚举值 | 中文 |
|---|---|
| `full_time` | 全职 |
| `internship` | 实习 |
| `part_time` | 兼职 |
| `campus_job` | 校内岗位 |
| `research_assistant` | 实验室助研 |
| `volunteer` | 志愿服务 |

### 5.4 项目类型 `projectType`

| 枚举值 | 中文 |
|---|---|
| `research` | 科研项目 |
| `course` | 课程项目 |
| `enterprise` | 企业项目 |
| `competition` | 竞赛项目 |
| `open_source` | 开源项目 |
| `personal` | 个人作品 |
| `other` | 其他 |

### 5.5 技能熟练程度 `skillLevel`

| 枚举值 | 中文 |
|---|---|
| `beginner` | 了解 |
| `familiar` | 熟悉 |
| `proficient` | 熟练 |
| `expert` | 精通 |

### 5.6 头像背景类型 `avatarBackgroundType`

| 枚举值 | 中文 |
|---|---|
| `white` | 白底 |
| `blue` | 蓝底 |
| `red` | 红底 |
| `transparent` | 透明底（P1） |

### 5.7 头像风格 `avatarStyle`

| 枚举值 | 中文 |
|---|---|
| `formal` | 正式证件照 |
| `natural` | 自然清爽 |
| `professional` | 职业商务 |

### 5.8 头像优化任务状态 `avatarTaskStatus`

| 枚举值 | 含义 |
|---|---|
| `pending` | 待处理 |
| `processing` | 处理中 |
| `success` | 成功 |
| `failed` | 失败 |

### 5.9 PDF 任务状态 `pdfTaskStatus`

| 枚举值 | 含义 |
|---|---|
| `pending` | 待处理 |
| `processing` | 处理中 |
| `success` | 成功 |
| `failed` | 失败 |

### 5.10 自我介绍风格 `introductionStyle`

| 枚举值 | 中文 |
|---|---|
| `concise_formal` | 简洁正式 |
| `tech_oriented` | 技术导向 |
| `student` | 学生求职 |
| `senior` | 社招成熟 |
| `postgraduate` | 考研复试 |
| `project` | 项目申报 |

---

## 6. 变更日志

### 6.1 相对于 PRD-v1 的变更

1. **P0 范围收缩**：
   - 模板切换从 P1 提前到 P0，但模板市场仍为 P2。
   - 头像白底/蓝底/红底优化改为 P0 占位实现，真实 AI 模型 P1 接入。
   - 模块拖拽排序保持 P1，P0 使用按钮排序。
2. **数据模型调整**：
   - 不存储 `age`，由 `birthDate` 计算。
   - `jobIntention` 合并到 `targetPosition`。
   - 项目经历采用 `description` 数组为主，增加可选的 `background/responsibility/achievements`。
   - 工作经历增加 `leaveReason` 字段。
3. **接口路径**：头像相关接口统一为 `/api/avatars/*`（复数）。
4. **术语统一**：`scene`、`sectionType`、`workType`、`projectType` 等使用下划线英文枚举值。

### 6.2 相对于设计文档 v1 的变更

1. **P0 范围收缩**：
   - 头像 AI 优化不实现真实模型，改为占位任务。
   - 模块拖拽排序从 P0 移到 P1。
2. **数据模型调整**：
   - 项目经历保留 `description` 数组，不完全替换为 `background/responsibility/achievements`。
   - 工作经历增加 `leaveReason` 字段。
   - `AvatarTask.backgroundType` 采用 PRD 的 white/blue/red，transparent 为 P1 预留。
3. **文档引用路径**：设计文档第 1 行 PRD 引用路径 `../prd/需求PRD-v1.md` 错误，正确路径为 `../../需求PRD-v1.md`。

### 6.3 v1.1 文档对齐变更（2026-07-07）

1. **JSON 字段类型映射**：明确 `resume.sections`、`resume_review` 各 JSON 字段在 Java/TypeScript 中的强类型映射。
2. **校验规则分层**：明确草稿保存、手动保存、导出/PDF 预检查三层校验强度。
3. **错误码映射**：`validation-rules.md` 增加数字错误码列，与 `ResultCode.java` 对齐。
4. **PDF 文件命名**：明确无姓名时文件名为 `我的简历_yyyyMMdd.pdf`。
5. **头像优化选项映射**：`api-spec.md` 增加 PRD 10 个选项与 API 字段的映射表。

---

## 7. 参考文档

- `docs/需求PRD-v1.md`
- `docs/superpowers/specs/2026-07-03-resume-generation-design.md`
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`（下游文档）
- `docs/superpowers/specs/2026-07-03-api-spec.md`（下游文档）
