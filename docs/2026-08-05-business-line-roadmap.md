# 业务线调整方案：对标 magic-resume 补齐核心亮点

> 版本：v1.1（2026-08-05 实施完成）
> 日期：2026-08-05
> 参考：https://github.com/JOYCEQL/magic-resume（免费在线 AI 简历编辑器，9.6k star）
> 定位：借鉴其产品打法与功能设计（Apache-2.0 商用限制，仅参考设计不复制代码/素材），用本项目自有架构实现
> 目标：形成差异化闭环 —— **AI 写作 → 一键导出（PDF/Word/Markdown）→ 分享展示**

> ✅ **实施状态（v1.1）**：Phase 1（AI 写作）、Phase 2（分享链接）、Phase 3（多格式导出）、步骤 A（令牌收敛）、步骤 B（编辑器两栏）、步骤 C/D（组件与官网简洁化）**功能全部完成**；后端 174 测试、前端 36 测试全通过。分享/导出控制层与分享页测试仍需补齐，P1 后续项见 §5。

---

## 0. 背景与选型

magic-resume 的三个核心业务亮点及其在本项目的落地路径：

| magic-resume 亮点 | 我们的承接方案 | 依据 |
|---|---|---|
| AI 辅助写作（Gemini 服务端代理） | 行内 AI 写作助手，复用已有 LlmProvider 多厂商路由 | 我们已有 OpenAI/Qwen/Ernie 路由 + AiCallLog 审计 + 并发配额 |
| 在线简历托管（roadmap） | 简历公开分享链接（token 鉴权只读页） | 已有 ResumeRenderService 服务端渲染，可复用 |
| 多格式导出（roadmap） | Word（docx4j）+ Markdown 导出 | 已有 PDF 异步任务体系，Markdown 零依赖 |

原则：**同步接口优先、不建多余任务表、全部复用现有基础设施**。

---

## 1. 行内 AI 写作助手

### 1.1 业务设计

- 在编辑器各文本字段旁提供 AI 操作入口（生成 / 润色 / 缩短 / 扩写 / 翻译）。
- 支持字段（白名单，随 Section 类型扩展）：
  - `introduction.content`（自我介绍正文）
  - `project[].description`、`work[].description`（经历描述数组）
  - `profile`：`name`（生成英文名/花名）、`targetPosition`、`expectedSalary`、`availability`
  - `education`/`project`/`work` 的 `achievements`（成果量化改写）
- 调用时携带简历整体内容 + 当前字段原文 + 用户已填写的目标岗位 JD（`resume.targetPosition` + 可选 `ReviewResumeRequest` 式 JD 输入），让 AI 输出贴合岗位。
- 结果以"预览 → 应用/放弃"交互落回表单，走现有自动保存链路。
- 配额（P0 最小集）：复用每用户并发 3 个 AI 任务限制；按日配额（游客 3 次/天、登录 30 次/天）作为 P1 项记录在案。

### 1.2 后端设计

**新接口**：`POST /resumes/{id}/ai/write`（需认证 + 归属校验）

```json
// 请求 ResumeAiWriteRequest
{
  "sectionType": "project",          // profile/education/work/project/skill/introduction
  "field": "description",            // 字段名（白名单校验）
  "action": "polish",                // generate | polish | shorten | expand | translate
  "originalText": "负责会员系统开发",
  "targetLang": "en"                 // 仅 translate 必填，如 en/ja
}

// 响应 R<ResumeAiWriteResponse>
{ "content": "主导会员系统的架构设计与开发，覆盖 50 万用户..." }
```

- 同步执行（用户等待结果，WebClient 120s 超时，前端 loading）。
- 服务端从简历实体读取目标字段原文（不信任客户端原文，防止 prompt 注入与越权）。
- 新增 feature key：`resume-writing`，配置于 `app.ai.providers` 与 `app.ai.prompts`。
  - Prompt 模板变量：`{resumeContent}` `{sectionType}` `{field}` `{action}` `{originalText}` `{targetLang}` `{jobDescription}`。
- 复用 `AiCallLog`（feature_key=resume-writing）、`ProviderRouter`、`ProviderRouter.resolveModel`。
- 失败语义：复用 `AI_MODEL_CALL_FAILED`(6003) / `AI_RESPONSE_PARSE_FAILED`(6005)；新增错误码 `AI_WRITING_FIELD_INVALID`(6007) 与 `AI_WRITING_CONTENT_TOO_LONG`(6008)（超 2000 字符拒绝）。
- 并发配额：复用 AiResumeOptimizeService 的"每用户进行中任务数"查询模式（同步接口下即进行中请求计数，用内存计数守卫即可，P0 用简单 `AtomicInteger` 每用户计数，10 并发上限）。

**新增文件**：
- `ai/dto/ResumeAiWriteRequest.java`、`ai/dto/ResumeAiWriteResponse.java`
- `ai/service/AiWritingService.java`
- `ai/controller/AiWritingController.java`（挂 `/resumes/{id}/ai/write`）
- `ai/config`：prompt 模板加入 `application.yml`（`app.ai.prompts.resume-writing`）

**测试**：
- `AiWritingServiceTest`：生成成功（mock provider）、provider 失败、越权简历、字段白名单外拒绝、原文超长拒绝、翻译缺 targetLang 拒绝
- `AiWritingControllerTest`：200 成功、404 简历不存在、403 越权、400 参数错误

### 1.3 前端设计

- 通用组件 `components/editor/AiWriterButton.vue`：
  - Props：`resumeId`、`sectionType`、`field`、`originalText`（函数/响应式）
  - 交互：点击 → 弹出操作菜单 → 选中后调用 `POST /resumes/{id}/ai/write` → 结果 dialog（可编辑文本域 + 重新生成 + 应用）
  - Emits：`apply(content)` 把结果写回表单本地状态
- 接入点：
  - `IntroductionForm`：content 文本框旁
  - `ProjectForm`/`WorkForm`：每条 description 文本域旁（按行）
  - `ProfileForm`：姓名/期望薪资旁（P0 只接 introduction 与 description，其余留扩展）
- `api/resume.ts` 新增 `aiWrite(resumeId, payload)`。
- 编辑态"应用"后自动触发既有 `triggerAutoSave`。

**测试**：`AiWriterButton.test.ts`（mock api：成功应用、失败提示、loading 态）

### 1.4 工作量

后端约 1 人日，前端约 1 人日。

---

## 2. 简历公开分享链接

### 2.1 业务设计

- 用户可对某份简历开启"分享"，得到只读公开链接：`https://<host>/share/{token}`。
- 一份简历最多一条有效分享记录；重新开启则轮换 token。
- 关闭分享后 token 立即失效。
- 分享页为纯服务端渲染 HTML（复用 ResumeRenderService + 简洁展示壳），无登录、无脚本；页面带 `noindex`、`Cache-Control: no-store`、CSP。
- 打开分享页时提示"简历包含联系方式"（展示页脚文案）。
- 简历逻辑删除时级联关闭分享。

### 2.2 后端设计

**数据模型（V8 迁移 `resume_share`）**：

```sql
CREATE TABLE resume_share (
  id          VARCHAR(64) NOT NULL PRIMARY KEY,
  resume_id   VARCHAR(64) NOT NULL,
  user_id     VARCHAR(64) NOT NULL,
  token       VARCHAR(64) NOT NULL COMMENT '随机 32 字节 token，唯一',
  status      VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/revoked',
  expires_at  DATETIME(3) DEFAULT NULL COMMENT '过期时间（NULL=永久）',
  revoked_at  DATETIME(3) DEFAULT NULL,
  deleted     TINYINT(1) NOT NULL DEFAULT 0,
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_resume_share_token (token),
  UNIQUE KEY uk_resume_share_resume (resume_id, user_id, deleted),
  KEY idx_resume_share_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**接口**：

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/resumes/{id}/share` | 是 | 创建/轮换分享，返回 `{token, url, expiresAt}` |
| GET | `/resumes/{id}/share` | 是 | 查询当前分享状态（无则 404/返回 null） |
| DELETE | `/resumes/{id}/share` | 是 | 关闭分享 |
| GET | `/share/{token}` | 匿名 | 公开只读渲染页（text/html，CSP/no-store/noindex） |

- token：`SecureRandom` 32 字节 Base64URL，不可枚举。
- 匿名访问：`SecurityConfig` 放行 `/share/**`；分享接口自身校验 token 对应记录 `status=active`、`deleted=0`、未过期，且简历未删除。
- 渲染：`ResumeRenderService.render` + 分享页壳（含页脚"由智能简历生成"，链接官网首页）。
- 删除简历：`ResumeService.deleteResume` 级联 `resume_share → revoked`。
- 代理：vite `rewrite /share → /api/share`；nginx `location /share` 同 /templates 模式（P0 分享链接直连 `/share/{token}`，由前端路由 ShareView iframe 加载 `/api/share/{token}`，避免新增代理。**选型：走前端路由 + iframe**，改动最小）。

**新增文件**：
- `share/entity/ResumeShare.java`、`share/mapper/ResumeShareMapper.java`、`share/service/ShareService.java`、`share/controller/ShareController.java`
- `resources/db/migration/V8__resume_share.sql`
- `share/dto/ShareResponse.java`

**测试**：
- `ShareServiceTest`：创建/轮换/查询/关闭、越权（他人简历）、token 无效、过期、简历已删、删除简历级联
- 分享控制层测试待补齐

### 2.3 前端设计

- `ResumeDetailView.vue` 与 `ResumeListView` 卡片菜单增加"分享"入口。
- 分享 dialog：开关、链接展示 + 复制按钮、关闭分享、隐私提示。
- 新路由 `/share/:token`（public，无需登录）→ `ShareView.vue`：全屏 iframe 加载 `/api/share/{token}` + 顶部"由智能简历生成"页脚。
- `api/share.ts` 封装三个认证接口。

**测试**：分享页 iframe 与无效 token 场景测试待补齐。

### 2.4 工作量

后端约 1 人日，前端约 0.5 人日。

---

## 3. 多格式导出（Word / Markdown）

### 3.1 业务设计

- ExportView 增加导出格式选择：PDF（现有异步任务）/ Word / Markdown。
- Word 与 Markdown 同步生成直接下载（文件小、秒级），P0 不落任务表（P1 统一到下载中心任务体系）。
- 导出前复用 `validateForExport`（姓名 + 联系方式必填）与现有命名规则（`姓名_岗位_简历.docx/.md`）。
- Word 为**可编辑文档**：使用简化版"Word 友好" HTML（标题/段落/列表 + 行内样式）经 docx4j 转 docx；PDF 仍走原 Playwright 渲染（视觉版）。

### 3.2 后端设计

**接口**：

| 方法 | 路径 | 认证 | 返回 |
|---|---|---|---|
| GET | `/resumes/{id}/export/markdown` | 是 | `text/markdown` 文件流（Content-Disposition 下载） |
| GET | `/resumes/{id}/export/word` | 是 | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` 二进制流 |

- `ResumeExportService`：
  - `buildMarkdown(Resume)`：按 Section 顺序拼接（# 标题 / 列表 / 表格），字段规则与渲染器一致（visible=false 跳过）。
  - `buildWord(Resume)`：先生成"Word 友好" HTML（新方法 `ResumeRenderService.renderWordHtml`，纯段落+无序列表+行内 strong，无 CSS 依赖），再经 docx4j `XHTMLImporter` 转换（docx4j 11.4.x，Java 17 兼容）。
  - 中文：docx4j 输出 docx 内含字体声明（默认宋体/微软雅黑 fallback），Word/WPS 打开正常。
- 依赖：pom 新增 `org.docx4j:docx4j-JAXB-ReferenceImpl:11.4.9`（docx4j 11 系 JAXB 引用实现，避免 JDK 模块问题）。
- 命名与 PDF 一致（`truncateFileName` 复用到 100 字符，扩展名 `.docx`/`.md`）。
- 下载头：`Content-Disposition: attachment; filename*=UTF-8''...`（复用 PdfController 模式）。
- 错误码：复用 `PDF_EXPORT_NAME_REQUIRED`/`PDF_EXPORT_CONTACT_REQUIRED`（语义通用，文档注明也用于 Word/Markdown）。

**新增文件**：
- `resume/service/ResumeExportService.java`
- `resume/controller/ResumeExportController.java`（或并入 ResumeController，独立文件更清晰）
- `ResumeRenderService` 增加 `renderWordHtml(Resume)` 方法

**测试**：
- `ResumeExportServiceTest`：markdown 内容包含姓名/联系方式/各 section 标题、visible=false 跳过、无姓名拒绝（复用校验）、word 字节非空且以 `PK`（docx zip 头）开头
- 导出控制层测试待补齐

### 3.3 前端设计

- `ExportView.vue`：导出设置区增加格式单选（PDF/Word/Markdown）；PDF 走现有任务轮询；Word/Markdown 走 `pdfApi` 之外的 `exportApi`（新建 `api/export.ts`，blob 下载 + 文件名从 Content-Disposition 解析）。
- 下载中心暂不展示 Word/Markdown 记录（P0 直接下载）。

**测试**：`ExportView` 现有组件测试补格式切换用例（mock exportApi）。

### 3.4 工作量

后端约 1.5 人日（含 docx4j 依赖与中文验证），前端约 0.5 人日。

---

## 4. 实施计划

| 阶段 | 内容 | 工作量 | 验收 |
|---|---|---|---|
| Phase 1 | AI 写作助手（后端 + 前端 + 测试） | ~2 人日 | 编辑器字段可生成/润色并应用回简历 |
| Phase 2 | 分享链接（表 + 接口 + 展示页 + 前端） | ~1.5 人日 | 匿名可打开只读页；关闭后 404 |
| Phase 3 | Word/Markdown 导出（+ 前端格式切换） | ~2 人日 | 三种格式均可下载，Word 可编辑、中文正常 |

总预估：约 5.5~6 人日。每阶段结束运行 `mvn test`（后端）与 `npm run test:unit && npm run build`（前端）并提交。

## 5. 风险与后续项（P1）

- ~~**AI 写作配额**：按日配额（游客 3/天、登录 30/天）需新增配额表或复用 Redis 计数~~ ✅ 已完成（2026-08-06）：`ai_daily_quota` 表（V10）+ 原子计数，游客 3/天、登录 30/天，见 `api-changelog.md` v1.6。
- **Word 转换质量**：docx4j 对复杂 CSS 支持有限；若效果不达标，P1 评估容器内 pandoc 方案（apt 安装 pandoc，HTML→docx 质量更高）。
- **分享页隐私**：~~默认展示联系方式；后续提供"隐藏联系方式"开关与自定义过期时间~~ ✅ 已完成（2026-08-06）：`hide_contact` 开关 + `expiresAt` 自定义过期（V11），见 `api-changelog.md` v1.7。
- **导入功能**（Markdown/JSON 简历解析）✅ 已完成（2026-08-06）：`POST /resumes/import`，JSON/Markdown 双格式，见 `api-spec.md` §7.14。编辑器富文本（Tiptap 直编）不在本次范围，列入后续业务迭代。

---

## 6. 相关文档联动

- 实施后更新：`docs/api-changelog.md`、`docs/traceability-matrix.md`、`docs/superpowers/specs/2026-07-03-api-spec.md`（新接口章节）、`docs/security-guide.md`（分享安全说明）。

---

## 7. 前端布局与排版简洁化（对标 magic-resume 视觉风格）

### 7.1 参考风格画像（magic-resume）

| 特征 | 做法 |
|---|---|
| 用色克制 | 中性灰底 + 单一强调色（indigo），无多色系、无渐变装饰 |
| 结构简约 | 编辑器 = 顶栏 + 编辑面板 + 预览 两栏；浮动工具坞（PreviewDock）在底部 |
| 组件轻盈 | 1px 细边框、小圆角、无常态阴影（hover 才有）、输入框无重背景 |
| 内容优先 | 减少卡片化、减少图标色彩，让简历内容本身成为视觉主体 |

### 7.2 现状问题诊断

1. **色板过重**：Material 3 三色系 × 6 变体 ≈ 30+ 颜色变量，页面大面积使用 primary-fixed / tertiary 等，视觉嘈杂。
2. **编辑器四栏过重**：深色 tab 侧栏 + 480px 表单面板 + 中间画布 + 360px AI 评估栏（约 60% 屏宽被工具占掉）。
3. **装饰过重**：玻璃拟态卡（st-glass-card）、投影卡片、旋转微件、多级阴影。
4. **在线字体不可达（生产隐患）**：`design-system.scss` 引入 `fonts.googleapis.com`，国内访问会超时/白屏等待。
5. **组件风格不统一**：Element Plus 默认样式与自定义样式混用。

### 7.3 改造方案

**A. 设计令牌收敛（0.5 人日）**——只改值、不改名，保证 60+ 组件零破坏：

| 令牌 | 调整 |
|---|---|
| 颜色 | `--st-tertiary*`、`--st-secondary*` 收敛为中性/主色映射；`--st-primary*` 保留单一强调色；surface 8 级收敛为 4 级（背景/卡片/浮层/边框） |
| 字体 | 移除 Google Fonts 在线引入，改用系统字体栈（Inter 降级为 Segoe/雅黑/PingFang）——**修复生产字体加载问题** |
| 阴影 | `--st-shadow-*` 收敛为 2 级；`--st-shadow-card` 取消彩色投影 |
| 圆角 | 统一 `--st-radius-md`（8px）为主，减少 full/2xl 使用 |

**B. 编辑器重构为简洁两栏（1 人日）**：

```
┌─ EditorHeader：返回 | 标题/重命名 | 保存状态 | 模块管理 | 模板 | 导出 ─────────────┐
├─ 编辑面板（左，可折叠，400px）              │  预览画布（右，自适应）                │
│  顶部：模块 Tab（图标+文字，单行）            │  A4 页面居中                          │
│  下方：当前模块表单（Field 化）              │                                      │
├─────────────────────────────────────────────┴──────────────────────────────────────┤
└─ FloatingToolbar：缩放 / 页码 / AI 评估入口 ────────────────────────────────────────┘
```

- 删除深色左侧 tab 栏（合并为编辑面板顶部单行 Tab）与右侧 AI 评估栏（改为浮动工具栏入口 / 抽屉）。
- 表单 Field 化：标签 12px 灰色 + 细边框输入框 + 紧凑间距（参考 `Field.tsx` 模式）。
- 预览区获得约 55% 屏宽，所见即所得体验对齐 magic-resume。

**C. 全局组件统一（1 人日）**：

- 卡片：去常态阴影 → 1px `outline-variant` 边框。
- 按钮：主按钮实色 + 次按钮描边，统一 8px 圆角，去掉 scale 动效的过度使用。
- 表格/列表：细分割线、去斑马纹。
- 对话框：收敛为 Element Plus 默认 + 主题桥接。

**D. 官网/工作台去装饰（0.5 人日）**：

- 官网首页：移除旋转简历微件、玻璃拟态卡片，改为静态 A4 缩略 + 浅色区块。
- 工作台侧栏：深色 `#001a43` 改为浅色/白色细边框（对齐整体中性风格）。

### 7.4 风险与约束

- 只改 CSS 变量值、不删变量名 → 全部现有页面视觉自动收敛，无编译破坏风险。
- Element Plus 主题桥接保留（改值即可联动）。
- ~~暗色模式不在本次范围（magic-resume 有，但需系统化改造，列入 P1）~~ ✅ 已完成（2026-08-06）：`html.dark` 令牌覆盖 + Element Plus dark 主题 + 浅色/深色/跟随系统三档切换（工作台顶栏快捷开关 + 设置页偏好），存储于 `localStorage`。

### 7.5 工作量汇总

| 模块 | 工作量 |
|---|---|
| A 设计令牌收敛 | 0.5 人日 |
| B 编辑器两栏重构 | 1 人日 |
| C 全局组件统一 | 1 人日 |
| D 官网/工作台去装饰 | 0.5 人日 |
| **合计** | **3 人日** |

### 7.6 实施顺序

业务功能（Phase 1-3）与前端简洁化（A-D）可并行或穿插执行；建议先做 **A（令牌收敛）** 让全局观感统一，再做 **B（编辑器）**，随后 C/D 与 Phase 3 并行。每步跑 `npm run test:unit && npm run build` 验证。

---

## 8. 编辑器体验 2.0（2026-08-05）

### 已实现

- 撤销/重做历史栈，连续输入按字段合并，最多保留 50 个编辑快照。
- `Ctrl/Cmd + Z`、`Ctrl/Cmd + Shift + Z`、`Ctrl/Cmd + S` 快捷键，编辑输入框保留浏览器原生撤销行为。
- 预览缩放真实作用于 A4 画布，预览加载后计算页数并支持上一页/下一页导航。
- 编辑过程写入按简历隔离的本地草稿；重新进入时检测未同步草稿并提供恢复选择。
- 工作台窄屏侧栏改为抽屉，编辑器移动端提供编辑/预览 Tab，桌面端支持收起编辑面板进入专注预览。
- 编辑器工具栏接入 AI 语法检查，问题按模块/字段展示，点击问题可回到对应编辑模块。

### 下一步

- ~~一页纸自动适配、字体/间距/主题色编辑~~ ✅ 已完成（2026-08-06）：`renderSettings`（V9）支持一页适配、字体、字号、行高、边距、模块间距和主题色，同步作用于实时预览、PDF 与 Word 导出。

---

## 9. 暗色模式（2026-08-06）

### 已实现

- 设计令牌暗色覆盖：`html.dark` 下仅覆盖颜色类变量（surface 灰阶、文字、描边、错误、阴影、Element Plus 桥接），结构与尺寸令牌不变，60+ 组件自动生效。
- 三档主题：浅色 / 深色 / 跟随系统（`matchMedia` 实时响应系统切换），偏好持久化到 `localStorage`（`resume_theme`）。
- 入口：工作台顶栏月亮/太阳快捷切换 + 设置页「偏好设置」外观主题单选。
- 防闪烁：`main.ts` 在应用挂载前调用 `initTheme()`。
- Element Plus 引入官方暗色变量集 `dark/css-vars.css`，与 `html.dark` class 对齐。

### 测试

- `useTheme.test.ts`：默认跟随系统、持久化、切换、非法值回退、系统暗色偏好跟随。
