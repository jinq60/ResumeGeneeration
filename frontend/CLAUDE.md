# 前端工程 — Claude 约束

> 作用：为前端开发 Agent 提供全局前端上下文、目录约定、状态管理、API 映射与开发红线。
> 范围：`frontend/` 目录下所有代码。
> 必读：根目录 `../AGENTS.md` + 本文件。

---

## 1. 前端定位

前端是 Vue 3 单页应用，负责：

- 用户登录、注册、游客模式
- 简历列表管理
- 简历编辑器（左侧表单 + 右侧实时预览）
- 模板选择与切换
- 头像上传与一寸照优化
- PDF、Word、Markdown 导出
- AI 简历点评、JD 优化和编辑器内 AI 写作
- 简历公开分享页

---

## 2. 技术栈

| 技术 | 版本 | 说明 |
|---|---|---|
| Vue | 3.4.21 | Composition API + `script setup` |
| Vite | 5.2.8 | 构建工具 |
| TypeScript | 5.4.5 | 全项目 TS |
| Vue Router | 4.3.0 | 路由 |
| Pinia | 2.1.7 | 状态管理 |
| Axios | 1.6.8 | HTTP 请求 |
| Element Plus | 2.6.3 | UI 组件库 |
| Element Plus Icons | 2.3.1 | 图标 |
| vue-cropper | 1.1.1 | 头像裁剪 |
| SCSS | 1.74.1 | 样式 |
| Tailwind CSS | 4.3.3 | 设计令牌与工具类样式 |
| Vitest | 1.4.0 | 单元/组件测试 |
| Playwright | 1.43.0 | E2E 测试 |
| ESLint / Prettier | 8 / 3 | 代码规范 |
| Node.js | >=20.19.0 | 前端运行时 |

---

## 3. 目录结构约定

```
frontend/src/
├── api/                # 按模块封装的 Axios 接口
├── assets/             # 静态资源（图片、字体、样式）
├── components/         # Vue 组件
│   ├── common/         # 通用组件
│   ├── editor/         # 编辑器表单组件
│   ├── preview/        # 预览相关组件
│   ├── website/        # 官网布局与组件
│   └── workbench/      # 用户工作台布局与组件
├── composables/        # 组合式函数
├── router/             # Vue Router 配置
├── stores/             # Pinia stores
├── types/              # TypeScript 类型
├── utils/              # 工具函数
├── views/              # 页面视图
│   ├── admin/          # 后台管理页面
│   ├── website/        # 官网页面
│   └── workbench/      # 用户工作台页面
├── App.vue             # 根组件
└── main.ts             # 应用入口
```

**强制规则**：

- 页面级组件放 `views/`。
- 可复用 UI 组件放 `components/`。
- 业务 API 调用放 `api/*.ts`。
- 状态放 `stores/*.ts`。
- 类型定义放 `types/*.ts`。
- 工具函数放 `utils/*.ts`。

---

## 4. 路由

文件：`src/router/index.ts`，按端拆分为：

- `src/router/website.ts`：官网路由（`/` 下无鉴权）
- `src/router/workbench.ts`：用户工作台路由（`/workbench/*` 需登录）
- `src/router/admin.ts`：后台管理路由（`/admin/*`）
- `/share/:token`：匿名简历分享页，内部通过 iframe 加载后端 HTML

```typescript
const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
  // 官网 /（Home / Features / TemplatesShowcase / Pricing / About / Contact / HelpDocs）
  ...websiteRoutes,
  // 工作台 /workbench/*
  ...workbenchRoutes,
  // 后台 /admin/*
  ...adminRoutes,
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue') }
]
```

工作台主要子路由示例：

```typescript
{ path: '/workbench/dashboard', name: 'Dashboard', component: () => import('@/views/workbench/DashboardView.vue') },
{ path: '/workbench/resumes', name: 'ResumeList', component: () => import('@/views/workbench/ResumeListView.vue') },
{ path: '/workbench/resumes/create', name: 'TemplateSelect', component: () => import('@/views/workbench/TemplateSelectView.vue') },
{ path: '/workbench/templates', name: 'TemplateCenter', component: () => import('@/views/workbench/TemplateCenterView.vue') },
{ path: '/workbench/templates/:id', name: 'TemplateDetail', component: () => import('@/views/workbench/TemplateDetailView.vue') },
{ path: '/workbench/editor/:id', name: 'Editor', component: () => import('@/views/workbench/EditorView.vue') },
{ path: '/workbench/resumes/:id/edit', name: 'ResumeEdit', component: () => import('@/views/workbench/EditorView.vue') },
{ path: '/workbench/resumes/:id', name: 'ResumeDetail', component: () => import('@/views/workbench/ResumeDetailView.vue') },
{ path: '/workbench/resumes/:id/export', name: 'Export', component: () => import('@/views/workbench/ExportView.vue') },
{ path: '/workbench/ai-review', name: 'AIReviewCenter', component: () => import('@/views/workbench/AIReviewCenterView.vue') },
{ path: '/workbench/avatar/upload', name: 'AvatarUpload', component: () => import('@/views/workbench/AvatarUploadView.vue') },
{ path: '/workbench/delivery', name: 'DeliveryManagement', component: () => import('@/views/workbench/DeliveryManagementView.vue') },
{ path: '/workbench/settings', name: 'Settings', component: () => import('@/views/workbench/SettingsView.vue') },
{ path: '/workbench/downloads', name: 'DownloadCenter', component: () => import('@/views/workbench/DownloadCenterView.vue') },
{ path: '/workbench/notifications', name: 'NotificationCenter', component: () => import('@/views/workbench/NotificationCenterView.vue') }
```

**路由守卫**（已实现在 `src/router/index.ts`）：

- 官网侧：`/` 下页面（首页、功能、模板、定价、关于、联系、帮助）无需登录。
- 工作台侧：访问 `/workbench/*` 需校验 `access_token`，未登录重定向到 `/login`；已登录用户访问 `/login` 时重定向到 `/workbench/dashboard`。
- 管理侧：访问 `/admin/*` 受保护页面时校验 `admin_token`，未登录则重定向到 `/admin/login`。管理员登录后自动进入 `/admin/dashboard`。

---

## 5. Pinia Stores

### 5.1 `user.ts`

- 状态：`userId`、`nickname`、`isGuest`、`accessToken`
- Getter：`isLoggedIn`
- Actions：
  - `setUser(info)`：写入状态并持久化到 `localStorage`
  - `clearUser()`：清空状态与 `localStorage`
  - `restoreFromStorage()`：从 `localStorage` 恢复
- 持久化键名：`resume_user_info`、`access_token`

### 5.2 `resume.ts`

- 状态：`currentResume`、`saveStatus`
- Getter：`sections`
- Actions：
  - `setResume(resume)`
  - `updateSections(newSections)`
  - `setSaveStatus(status)`

### 5.3 `ui.ts`

- 状态：`isMobile`、`activeTab`
- Actions：`setMobile(value)`、`setActiveTab(tab)`

---

## 6. API 封装

### 6.1 统一请求 `utils/request.ts`

- baseURL：`import.meta.env.VITE_API_BASE_URL || '/api'`
- 请求拦截器：附加 `Authorization: Bearer {accessToken}`
- 响应拦截器：
  - `code === 200` 时返回 `data`
  - 否则抛出 `Error(message)`
  - 网络异常统一提示

### 6.2 接口模块

| 文件 | 后端模块 | 主要接口 |
|---|---|---|
| `api/auth.ts` | user | 注册、登录、游客、刷新 |
| `api/resume.ts` | resume | 简历 CRUD、复制、重命名 |
| `api/template.ts` | template | 模板列表、详情 |
| `api/avatar.ts` | avatar | 头像上传、优化、查询、删除 |
| `api/pdf.ts` | pdf | PDF 导出、查询、下载 |
| `api/export.ts` | resume | Word/Markdown 文件下载 |
| `api/share.ts` | resume/share | 分享创建、查询、关闭 |
| `api/preview.ts` | resume | 预览地址与 HTML 预览 |
| `api/admin/*.ts` | user/template/resume | 后台登录与管理接口 |

### 6.3 后端接口映射

| 前端方法 | 后端接口 |
|---|---|
| `auth.register` | `POST /api/auth/register` |
| `auth.login` | `POST /api/auth/login` |
| `auth.guest` | `POST /api/auth/guest` |
| `auth.refresh` | `POST /api/auth/refresh` |
| `resume.create` | `POST /api/resumes` |
| `resume.list` | `GET /api/resumes` |
| `resume.get` | `GET /api/resumes/{id}` |
| `resume.update` | `PUT /api/resumes/{id}` |
| `resume.remove` | `DELETE /api/resumes/{id}` |
| `resume.duplicate` | `POST /api/resumes/{id}/duplicate` |
| `resume.rename` | `PUT /api/resumes/{id}/title` |
| `template.list` | `GET /api/templates` |
| `template.get` | `GET /api/templates/{id}` |
| `avatar.upload` | `POST /api/avatars/upload` |
| `avatar.optimize` | `POST /api/avatars/optimize` |
| `avatar.getTask` | `GET /api/avatars/tasks/{taskId}` |
| `avatar.remove` | `DELETE /api/avatars/{id}` |
| `pdf.export` | `POST /api/pdf/export` |
| `pdf.getTask` | `GET /api/pdf/tasks/{taskId}` |
| `pdf.download` | `GET /api/pdf/download/{taskId}` |
| `resume.aiWrite` | `POST /api/resumes/{id}/ai/write` |
| `resume.aiWriteStream` | `POST /api/resumes/{id}/ai/write/stream`（SSE） |
| `resume.grammarCheck` | `POST /api/resumes/{id}/grammar-check` |
| `export.downloadWord` | `GET /api/resumes/{id}/export/word` |
| `export.downloadMarkdown` | `GET /api/resumes/{id}/export/markdown` |
| `share.create` | `POST /api/resumes/{id}/share` |
| `share.get` | `GET /api/resumes/{id}/share` |
| `share.revoke` | `DELETE /api/resumes/{id}/share` |
| `share page` | `GET /api/share/{token}` |

---

## 7. 类型定义

文件：`src/types/resume.ts`

### 7.1 Section Union Type

```typescript
export type SectionType =
  | 'profile'
  | 'education'
  | 'work'
  | 'project'
  | 'skill'
  | 'introduction'
  | 'custom'

export interface BaseSection<T extends SectionType, D> {
  id: string
  type: T
  title: string
  order: number
  visible: boolean
  data: D
}

export type Section =
  | ProfileSection
  | EducationSection
  | WorkSection
  | ProjectSection
  | SkillSection
  | IntroductionSection
  | CustomSection
```

### 7.2 Resume 主类型

```typescript
export interface Resume {
  id: string
  userId: string
  title: string
  scene: string
  targetPosition?: string
  templateId: string
  sections: Section[]
  renderSettings?: RenderSettings | null
  createdAt: string
  updatedAt: string
}
```

### 7.3 约束

- `Section` 必须使用 union type，根据 `type` 确定 `data` 结构。
- 新增 Section 类型时，必须同步扩展 union type 和对应 data 接口。

---

## 8. 视图与组件现状

### 已完成

#### 官网（`views/website/`）
- `HomeView.vue`：官网首页（Hero / 合作伙伴 / 功能 / 演示 / CTA）
- `FeaturesView.vue`：功能特性页
- `TemplatesShowcaseView.vue`：模板展示页
- `PricingView.vue`：定价方案页
- `AboutView.vue`：关于我们页
- `ContactView.vue`：联系我们页
- `HelpDocsView.vue`：帮助文档页
- `WebsiteLayout.vue`：官网统一 Header / Footer 布局

#### 用户工作台（`views/workbench/`）
- `DashboardView.vue`：用户工作台
- `ResumeListView.vue`：简历列表、新建、重命名、复制、删除、分页
- `EditorView.vue`：左侧模块编辑区、右侧预览、模板切换、导出入口
- `TemplateSelectView.vue` / `TemplateCenterView.vue`：模板选择与模板中心
- `ExportView.vue`：PDF/Word/Markdown 导出流程 UI
- `AIReviewView.vue` / `AIReviewCenterView.vue`：AI 简历点评、JD 优化入口与结果展示
- `AvatarUploadView.vue`：头像上传、裁剪、一寸照优化 UI
- `DeliveryManagementView.vue`：投递记录管理
- `SettingsView.vue`：用户账号设置
- `DownloadCenterView.vue`：下载中心（PDF/头像任务）
- `NotificationCenterView.vue`：通知中心
- `ResumeDetailView.vue`：简历详情/预览页
- `WorkbenchLayout.vue` / `WorkbenchSidebar.vue`：工作台左侧导航布局

#### 后台管理
- `AdminLayout.vue`、`admin/Dashboard.vue`、`admin/UserManagement.vue`、`admin/ResumeManagement.vue`、`admin/TemplateManagement.vue`、`admin/SystemSettings.vue`（AI 规则管理）、`admin/ContentAudit.vue`、`admin/DeliveryData.vue`、`admin/Login.vue`：管理后台页面与布局

#### 公共
- `LoginView.vue`：登录/注册/游客模式完整页面
- `ShareView.vue`：匿名分享页壳，iframe 加载后端只读 HTML
- `NotFoundView.vue`：404 页面
- `ResumePreview.vue`：iframe 加载后端 `/api/resumes/{id}/preview`
- `frontend/src/utils/download.ts`：PDF/头像任务本地存储与读取工具
- `useAutoSave.ts`：2 秒防抖自动保存 hook
- `components/editor/*.vue`：Profile、Education、Work、Project、Skill、Introduction、Custom 表单及 AI 写作按钮
- 类型、API 封装、状态管理、路由骨架、路由守卫

### 当前实现边界

- 编辑器已实现两栏布局、模块 Tab、上下排序、表单更新、自动保存和实时预览。
- 编辑器已实现撤销/重做（连续输入合并）、`Ctrl/Cmd + Z`、`Ctrl/Cmd + Shift + Z`、`Ctrl/Cmd + S` 快捷键、缩放、页码导航和本地草稿恢复。
- 工作台已实现窄屏侧栏抽屉；编辑器移动端支持“编辑内容 / 预览简历”双 Tab，桌面端支持收起编辑面板进入专注预览。
- 自我介绍、工作描述、项目描述和成就字段支持白名单富文本；富文本保存为 `contentHtml` / `descriptionHtml`，旧纯文本字段继续保留。
- AI 写作支持 SSE 增量预览，流式接口不可用时回退到同步写作接口。
- AI 写作已接入自我介绍、工作/项目描述等文本字段，结果支持预览后应用。
- 编辑器工具栏提供 AI 语法检查抽屉，问题可按模块定位回编辑区；未配置供应商时显示不可用状态。
- 编辑器支持保存一页纸适配、字体、字号、行高、边距、模块间距和主题色，并同步到实时预览、PDF 与 Word 导出。
- E2E 当前仅覆盖官网基础流程，新增页面仍需持续补充端到端覆盖。
- 真实头像 AI、AI 按日配额、简历导入和富文本编辑属于后续迭代。

---

## 9. 编辑器设计约定

### 9.1 布局

- 桌面端：左侧表单区 + 右侧实时预览区。
- 移动端：底部标签切换（编辑 / 预览）。

### 9.2 数据流

1. 进入编辑器时调用 `resumeApi.get(id)` 加载简历。
2. 简历数据写入 `resumeStore.currentResume`。
3. 各表单组件修改 `sections` 后调用 `resumeStore.updateSections(newSections)`。
4. `useAutoSave` 监听 `sections` 变化，2 秒防抖调用 `resumeApi.update(id, sections)`。
5. 右侧预览通过 iframe `srcdoc` 或后端 `/api/resumes/{id}/preview` 刷新。

### 9.3 Section 编辑

- 每个 Section 类型对应一个表单组件。
- 表单组件接收 `section: Section` 和 `onUpdate` 回调。
- `onUpdate` 返回新的 `Section` 对象，不得原地修改。

---

## 10. 开发约束

### 10.1 组件规范

- 使用 Composition API + `script setup`。
- 组件名使用 PascalCase，文件名与组件名一致。
- Props 使用 `defineProps` 并声明类型。
- Emits 使用 `defineEmits`。

### 10.2 类型安全

- 禁止使用 `any` 作为类型偷懒方案。
- API 响应必须声明返回类型。
- Store actions 入参必须声明类型。

### 10.3 状态管理

- 跨组件共享状态放 Pinia。
- 组件内部状态用 `ref` / `reactive`。
- 用户登录态持久化到 `localStorage`。

### 10.4 安全

- Token 从 `localStorage` 读取后附加到请求头。
- XSS：渲染用户输入时使用 Vue 自动转义或 `textContent`。
- 不将敏感信息写入 URL。

### 10.5 样式

- 使用 Element Plus 组件优先。
- 自定义样式使用 SCSS，放 `assets/styles/`。
- 避免行内样式，使用 class。

---

## 11. 环境变量

| 变量名 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` | 后端 API 基础路径 |
| `VITE_APP_TITLE` | `智能简历生成工具` | 页面标题 |

开发代理已配置在 `vite.config.ts`：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

---

## 12. 常用命令

```bash
cd frontend
npm install
npm run dev              # http://localhost:5173
npm run build            # 生产构建
npm run test:unit        # 单元/组件测试
npm run test:e2e         # E2E 测试
npx eslint . --ext .vue,.ts,.tsx
npm run format
```

---

## 13. 测试要求

- 单元测试：`tests/unit/`
- 组件测试：`tests/component/`
- E2E 测试：`tests/e2e/`
- 新增组件/视图必须补充测试。
- 运行：`npm run test:unit -- --run`
- 当前基线：13 个测试文件、36 个单元/组件测试通过；E2E 覆盖仍较少。

---

## 14. 前端开发红线

- 禁止在组件中直接调用 Axios，必须通过 `api/*.ts`。
- 禁止在模板中直接拼接用户输入的 HTML（防 XSS）。
- 禁止将 `accessToken` 写入 URL 或日志。
- 禁止未声明类型使用 `any`。
- 禁止跨组件直接修改 Pinia state（必须通过 actions）。

---

## 15. 相关文档

- `../docs/superpowers/specs/2026-07-03-api-spec.md`
- `../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- `../docs/superpowers/specs/2026-07-03-validation-rules.md`
- `../docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `../docs/superpowers/specs/2026-07-03-tdd-test-plan.md` §3
- `../docs/superpowers/specs/2026-07-03-scope-alignment.md`
- `../docs/adr/ADR-002-json-sections.md`
- `../docs/setup-guide.md`
- `../docs/environment.md`
- `../docs/security-guide.md`
