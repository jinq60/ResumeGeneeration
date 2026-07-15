# 简历生成工具模板系统规范

> 版本：v1.0  
> 日期：2026-07-03  
> 基于：`docs/superpowers/specs/2026-07-03-scope-alignment.md`、`docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`

---

## 1. 设计目标

1. **模板后台化管理**：模板作为数据库资源，管理员可在后台创建、编辑、上下架，前端动态加载，无需发版即可新增模板。
2. **预览与导出一致**：前端实时预览与服务端 PDF 导出使用同一套模板配置和渲染规则。
3. **模板可配置**：通过 JSON 配置控制字体、颜色、间距、布局、模块样式等，无需修改代码即可调整模板。
4. **模块可扩展**：新增 Section 类型时，模板系统通过统一渲染接口自动适配。
5. **切换无损失**：用户切换模板不影响已填写内容。

---

## 2. 总体架构

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Template      │────▶│  Template Engine │────▶│   HTML + CSS    │
│   Config (JSON) │     │  (Server/Client) │     │   (Render Out)  │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                ▲
                                │
┌─────────────────┐     ┌──────────────────┐
│   Resume Data   │────▶│   Section Data   │
│   (Sections)    │     │   (Unified)      │
└─────────────────┘     └──────────────────┘
```

- **服务端渲染（PDF）**：后端读取模板 HTML 文件与配置，注入简历数据，生成完整 HTML，再由 Playwright 导出 PDF。
- **客户端渲染（预览）**：前端使用 Vue 组件解析同一套模板配置，实时渲染预览。

---

## 3. 模板配置 Schema

### 3.1 顶层结构

```json
{
  "page": {
    "width": "210mm",
    "height": "297mm",
    "margin": "20mm",
    "background": "#ffffff"
  },
  "font": {
    "family": "\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif",
    "mainSize": "10.5pt",
    "titleSize": "14pt",
    "smallSize": "9pt",
    "lineHeight": 1.5
  },
  "color": {
    "primary": "#333333",
    "secondary": "#666666",
    "accent": "#1a5276",
    "background": "#ffffff",
    "sidebar": "#f8f9fa"
  },
  "layout": {
    "singleColumn": true,
    "sidebarWidth": "30%",
    "avatar": {
      "visible": true,
      "shape": "square",
      "size": "25mm",
      "position": "top-right"
    }
  },
  "sectionTitle": {
    "fontSize": "12pt",
    "fontWeight": "bold",
    "color": "#1a5276",
    "borderBottom": "1px solid #1a5276",
    "marginBottom": "8mm"
  },
  "skill": {
    "displayStyle": "tag"
  },
  "moduleSpacing": "6mm"
}
```

### 3.2 字段说明

| 字段路径 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `page.width` | string | 是 | 页面宽度，默认 `210mm`（A4） |
| `page.height` | string | 是 | 页面高度，默认 `297mm`（A4） |
| `page.margin` | string | 是 | 页边距 |
| `page.background` | string | 是 | 页面背景色 |
| `font.family` | string | 是 | 字体栈，需包含中文字体回退 |
| `font.mainSize` | string | 是 | 正文字号 |
| `font.titleSize` | string | 是 | 姓名/标题字号 |
| `font.smallSize` | string | 是 | 辅助字号 |
| `font.lineHeight` | number | 是 | 行高倍数 |
| `color.primary` | string | 是 | 主文字颜色 |
| `color.secondary` | string | 是 | 辅助文字颜色 |
| `color.accent` | string | 是 | 强调色（模块标题、链接等） |
| `color.background` | string | 是 | 背景色 |
| `color.sidebar` | string | 否 | 双栏布局侧边栏背景色 |
| `layout.singleColumn` | boolean | 是 | 是否单栏 |
| `layout.sidebarWidth` | string | 否 | 双栏时侧边栏宽度 |
| `layout.avatar.visible` | boolean | 是 | 是否展示头像 |
| `layout.avatar.shape` | string | 是 | `square` / `circle` |
| `layout.avatar.size` | string | 是 | 头像尺寸 |
| `layout.avatar.position` | string | 是 | 头像位置 |
| `sectionTitle.fontSize` | string | 是 | 模块标题字号 |
| `sectionTitle.fontWeight` | string | 是 | 模块标题字重 |
| `sectionTitle.color` | string | 是 | 模块标题颜色 |
| `sectionTitle.borderBottom` | string | 否 | 模块标题下边框 |
| `sectionTitle.marginBottom` | string | 是 | 模块标题下边距 |
| `skill.displayStyle` | string | 是 | `tag` / `category` / `level` |
| `moduleSpacing` | string | 是 | 模块之间间距 |

---

## 4. 模板渲染流程

### 4.1 服务端 PDF 渲染流程

1. 接收 `POST /pdf/export` 请求。
2. 读取简历数据 `resume.sections` 与 `template.config`。
3. 加载对应 `htmlTemplate` 文件（如 `classic-single.html`）。
4. 将 `template.config` 转换为 CSS 变量注入到 HTML `<style>` 中。
5. 将简历数据按 Section 类型渲染为 HTML 片段。
6. 合并生成完整 HTML 字符串。
7. 调用 Playwright 将 HTML 渲染为 A4 PDF。

### 4.2 客户端预览渲染流程

1. 前端编辑器加载简历数据与模板配置。
2. 使用 Vue 组件 `<ResumePreview :sections="sections" :config="config" :template="templateId" />`。
3. 组件根据 `templateId` 动态加载模板配置与样式。
4. 每个 Section 类型对应一个渲染子组件（`ProfileSection`、`EducationSection` 等）。
5. 表单数据变更后，Pinia store 更新 sections，预览组件响应式重新渲染。

### 4.3 预览与导出一致性保障

为避免「前端 Vue 组件预览」与「服务端 Playwright PDF 渲染」出现样式/排版差异，采用以下机制：

1. **同源配置**：两端必须读取同一份 `template.config`（CSS 变量、字体、间距、颜色），禁止在 Vue 组件中硬编码与 HTML 模板不一致的样式。
2. **同源 Section 渲染规则**：同一 Section 类型的 HTML 结构与 Vue 组件结构保持一致；`section.data` 的字段命名与取值逻辑两端统一。
3. **预览回源（推荐）**：前端预览区通过 iframe 加载后端渲染的 HTML URL（`/pdf/preview` 或模板预览接口），使预览与 PDF 使用同一份 HTML/CSS。P0 可先用 Vue 组件预览，但需通过 E2E 截图对比做回归验证。
4. **回归验证**：TDD 测试计划中增加「PDF 内容与预览一致」的验收测试，通过 Playwright 对比导出 PDF 与前端预览截图的关键区域。

---

## 5. HTML 模板规范

### 5.1 模板文件位置

后端模板文件存放于 `backend/src/main/resources/templates/resume/`：

```
templates/resume/
├── classic-single.html
├── classic-double.html
├── tech.html
├── fresh.html
├── business.html
└── postgraduate.html
```

前端模板组件存放于 `frontend/src/components/preview/templates/`：

```
frontend/src/components/preview/templates/
├── ClassicSingleTemplate.vue
├── ClassicDoubleTemplate.vue
├── TechTemplate.vue
├── FreshTemplate.vue
├── BusinessTemplate.vue
└── PostgraduateTemplate.vue
```

### 5.2 模板 HTML 结构

每个模板 HTML 必须包含以下结构：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <style>
    :root {
      /* CSS 变量由模板引擎注入 */
      --page-width: {{page.width}};
      --page-height: {{page.height}};
      --page-margin: {{page.margin}};
      --font-family: {{font.family}};
      --font-main-size: {{font.mainSize}};
      --color-primary: {{color.primary}};
      --color-accent: {{color.accent}};
      /* ... */
    }
    /* 模板基础样式 */
  </style>
</head>
<body>
  <div class="resume-page">
    <!-- 动态渲染 sections -->
    {{sections}}
  </div>
</body>
</html>
```

### 5.3 CSS 变量约定

| CSS 变量 | 来源配置 |
|---|---|
| `--page-width` | `page.width` |
| `--page-height` | `page.height` |
| `--page-margin` | `page.margin` |
| `--page-background` | `page.background` |
| `--font-family` | `font.family` |
| `--font-main-size` | `font.mainSize` |
| `--font-title-size` | `font.titleSize` |
| `--font-small-size` | `font.smallSize` |
| `--line-height` | `font.lineHeight` |
| `--color-primary` | `color.primary` |
| `--color-secondary` | `color.secondary` |
| `--color-accent` | `color.accent` |
| `--color-sidebar` | `color.sidebar` |
| `--section-title-font-size` | `sectionTitle.fontSize` |
| `--section-title-color` | `sectionTitle.color` |
| `--module-spacing` | `moduleSpacing` |

---

## 6. Section 渲染规则

### 6.1 通用规则

1. 按 `section.order` 升序渲染。
2. 仅渲染 `visible = true` 的 Section。
3. 空 Section（无有效数据）不渲染，但保留在数据中。
4. 模块标题使用 `section.title`，可被模板覆盖样式。

### 6.2 个人信息 `profile`

- 每个简历唯一，通常置顶（order = 0）。
- 展示字段受 `showGender`、`showAge`、`showSalary`、`showAvatar` 控制。
- `age` 由 `birthDate` 计算，不独立存储。
- `avatarUrl` 为简历一寸照片地址，由 `avatar_task` 优化生成；为空时不展示头像。
- 头像展示受模板 `layout.avatar` 配置控制（可见性、形状、尺寸、位置）。

### 6.3 教育经历 `education`

- 支持多段，按 `endDate` 倒序展示。
- `endDate` 为 `present` 时展示“至今”。
- `honors` 与 `courses` 可选展示。

### 6.4 项目经历 `project`

- 支持多段，按 `endDate` 倒序展示。
- `description` 数组必填，渲染为无序列表。
- `techStack` 渲染为标签。
- `background`、`responsibility`、`achievements` 可选展示。

### 6.5 工作经历 `work`

- 支持多段，按 `endDate` 倒序展示。
- `description` 数组必填，渲染为无序列表。
- `leaveReason` 默认不展示。

### 6.6 技能 `skill`

- 展示方式由 `template.config.skill.displayStyle` 决定：
  - `tag`：平铺标签
  - `category`：按分类分组
  - `level`：显示熟练程度

### 6.7 自我介绍 `introduction`

- 单段文本，按 `style` 控制语气（仅前端提示，不影响渲染）。
- 字数超出 `maxWords` 时前端提示，后端限制 500 字符。

---

## 7. 系统内置模板示例

> 以下为初始化时内置的模板示例，后续可通过后台管理接口继续添加、编辑或上下架。

### 7.1 经典单栏 `classic-single`

- **编码**：`classic-single`
- **分类**：classic
- **布局**：单栏
- **头像**：右上角，方形，25mm
- **配色**：深蓝强调 `#1a5276`
- **适用**：通用岗位
- **skill.displayStyle**：tag

### 7.2 经典双栏 `classic-double`

- **编码**：`classic-double`
- **分类**：classic
- **布局**：双栏，左侧 sidebar 宽度 30%
- **头像**：左侧顶部，圆形，20mm
- **配色**：深蓝 + 浅灰 sidebar `#f8f9fa`
- **适用**：通用岗位
- **skill.displayStyle**：tag

### 7.3 技术岗模板 `tech`

- **编码**：`tech`
- **分类**：tech
- **布局**：单栏
- **头像**：右上角，方形，22mm
- **配色**：蓝色强调 `#2563eb`
- **适用**：研发岗位
- **skill.displayStyle**：category

### 7.4 应届生模板 `fresh`

- **编码**：`fresh`
- **分类**：fresh
- **布局**：单栏
- **头像**：顶部居中，圆形，24mm
- **配色**：绿色强调 `#27ae60`
- **适用**：学生、应届生
- **skill.displayStyle**：tag

### 7.5 简洁商务模板 `business`

- **编码**：`business`
- **分类**：business
- **布局**：单栏
- **头像**：右上角，方形，22mm
- **配色**：暗红强调 `#c0392b`
- **适用**：社招、管理层
- **skill.displayStyle**：level

### 7.6 考研复试模板 `postgraduate`

- **编码**：`postgraduate`
- **分类**：postgraduate
- **布局**：单栏
- **头像**：右上角，方形，20mm
- **配色**：黑色强调 `#000000`，宋体回退
- **适用**：考研复试、学术场景
- **skill.displayStyle**：tag

---

## 8. 模板切换规则

1. **内容不丢失**：切换模板仅修改 `resume.templateId`，不动 `sections`。
2. **即时预览**：前端切换模板后立即重新渲染预览。
3. **模块兼容提示**：如果目标模板不支持某 Section 类型（P0 所有模板均支持 P0 Section 类型），提示用户。
4. **默认模板**：新建简历时根据 `scene` 推荐默认模板：
   - `campus_recruitment` → `template_tech`
   - `internship` → `template_fresh`
   - `social_recruitment` → `template_business`
   - `postgraduate_reexam` → `template_postgraduate`
   - `project_application` → `template_classic_single`
   - `custom` → `template_classic_single`

---

## 9. 分页与溢出处理

1. **PDF 导出**：Playwright 按 A4 尺寸自动分页。
2. **内容溢出提示**：导出前若内容明显超出一页，提示用户“可能导出为多页”。
3. **预览区分页**：前端预览区不强制分页，但提供分页指示线。

---

## 10. 中文字体处理

1. **字体栈**：所有模板字体栈必须包含中文字体回退，例如：
   - `"Noto Sans SC", "Microsoft YaHei", "PingFang SC", sans-serif`
   - 考研复试模板可增加 `"SimSun", serif` 回退。
2. **PDF 字体**：服务端需预装中文字体或使用 web fonts，确保 PDF 导出中文正常显示。

---

## 11. 模板管理规范

### 11.1 后台管理模板生命周期

模板作为后台资源，管理员可通过 `/admin/templates` 接口完成以下操作：

1. **创建模板**：上传/填写 `code`、`name`、`category`、`config`、`html_template`、`thumbnail_url`、`sort_order` 等字段，存入 `template` 表。
2. **编辑模板**：修改模板配置、排序、推荐位；修改后 `version` 自增，前端与 CDN 缓存失效。
3. **上下架模板**：修改 `status` 为 `active` / `inactive`，下架后前台不可见但已使用该模板的简历不受影响。
4. **删除模板**：仅允许删除 `is_builtin = 0` 的模板；删除为逻辑删除（`status = deleted`）。
5. **系统内置模板保护**：`is_builtin = 1` 的模板不可删除，仅可上下架，保证基础模板始终可用。

### 11.2 新增模板流程

1. 在后台创建模板记录，填写 `code`、`name`、`category`、`config`、`html_template`、`render_engine`。
2. 将 HTML 模板文件上传/部署到 `backend/src/main/resources/templates/resume/`（服务端渲染）或静态资源服务器（客户端渲染）。
3. 如使用客户端渲染，前端需按 `code` 动态注册对应的 Vue 组件；如使用服务端渲染，前端通过 iframe 或统一预览组件加载后端渲染结果。
4. 调整 `sort_order` 与 `is_recommended`，前台列表即时生效。

### 11.3 新增 Section 类型

1. 在后端模板引擎中新增该 Section 的渲染逻辑。
2. 在前端新增对应的 Section 渲染子组件。
3. 在模板 `config` 中决定该 Section 的默认展示方式；老模板可保持默认样式。

---

## 12. 参考文档

- `docs/需求PRD-v1.md` §8 模板系统
- `docs/superpowers/specs/2026-07-03-resume-generation-design.md` §5、§8
- `docs/superpowers/specs/2026-07-03-scope-alignment.md` §3.4、§5.1
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.5、§6.2
- `docs/superpowers/specs/2026-07-03-api-spec.md`
- `docs/superpowers/specs/2026-07-03-tdd-test-plan.md`
