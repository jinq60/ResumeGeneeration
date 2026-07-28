# 前端工程 — Claude 约束

> 作用：为前端开发 Agent 提供全局前端上下文、目录约定、状态管理、API 映射与开发红线。
> 范围：`frontend/` 目录下所有代码。
> 必读：根目录 `../CLAUDE.md` + 本文件。

---

## 1. 前端定位

前端是 Vue 3 单页应用，负责：

- 用户登录、注册、游客模式
- 简历列表管理
- 简历编辑器（左侧表单 + 右侧实时预览）
- 模板选择与切换
- 头像上传与一寸照优化
- PDF 导出
- AI 简历点评展示（P1）

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
| Vitest | 1.4.0 | 单元/组件测试 |
| Playwright | 1.43.0 | E2E 测试 |
| ESLint / Prettier | 8 / 3 | 代码规范 |

---

## 3. 目录结构约定

```
frontend/src/
├── api/                # 按模块封装的 Axios 接口
├── assets/             # 静态资源（图片、字体、样式）
├── components/         # Vue 组件
│   ├── common/         # 通用组件
│   ├── editor/         # 编辑器表单组件
│   └── preview/        # 预览相关组件
├── composables/        # 组合式函数
├── router/             # Vue Router 配置
├── stores/             # Pinia stores
├── types/              # TypeScript 类型
├── utils/              # 工具函数
├── views/              # 页面视图
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

文件：`src/router/index.ts`

```typescript
const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/landing', name: 'Landing', component: () => import('@/views/LandingView.vue') },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
  { path: '/resumes', name: 'ResumeList', component: () => import('@/views/ResumeListView.vue') },
  { path: '/resumes/create', name: 'TemplateSelect', component: () => import('@/views/TemplateSelectView.vue') },
  { path: '/templates', name: 'TemplateCenter', component: () => import('@/views/TemplateCenterView.vue') },
  { path: '/templates/:id', name: 'TemplateDetail', component: () => import('@/views/TemplateDetailView.vue') },
  { path: '/editor/:id', name: 'Editor', component: () => import('@/views/EditorView.vue') },
  { path: '/resumes/:id', name: 'ResumeDetail', component: () => import('@/views/ResumeDetailView.vue') },
  { path: '/resumes/:id/edit', name: 'ResumeEdit', component: () => import('@/views/EditorView.vue') },
  { path: '/resumes/:id/export', name: 'Export', component: () => import('@/views/ExportView.vue') },
  { path: '/resumes/:id/preview', name: 'Preview', component: () => import('@/views/ExportView.vue') },
  { path: '/resumes/:id/review', name: 'AIReview', component: () => import('@/views/AIReviewView.vue') },
  { path: '/ai-review', name: 'AIReviewCenter', component: () => import('@/views/AIReviewCenterView.vue') },
  { path: '/avatar/upload', name: 'AvatarUpload', component: () => import('@/views/AvatarUploadView.vue') },
  { path: '/delivery', name: 'DeliveryManagement', component: () => import('@/views/DeliveryManagementView.vue') },
  { path: '/settings', name: 'Settings', component: () => import('@/views/SettingsView.vue') },
  { path: '/downloads', name: 'DownloadCenter', component: () => import('@/views/DownloadCenterView.vue') },
  { path: '/notifications', name: 'NotificationCenter', component: () => import('@/views/NotificationCenterView.vue') },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue') },
  // admin routes ...
]
```

**路由守卫**（已实现在 `src/router/index.ts`）：

- 用户侧：未登录用户访问受保护页面（除 `Login`、`Landing` 外）时重定向到 `/login`；已登录用户访问 `/login` 时重定向到 `/`。
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

- `LoginView.vue`：登录/注册/游客模式完整页面
- `ResumeListView.vue`：简历列表、新建、重命名、复制、删除、分页
- `DashboardView.vue`：用户工作台
- `EditorView.vue`：左侧模块编辑区、右侧预览、模板切换、导出入口
- `TemplateSelectView.vue` / `TemplateCenterView.vue`：模板选择与模板中心
- `ExportView.vue`：PDF 导出流程 UI
- `AIReviewView.vue` / `AIReviewCenterView.vue`：AI 简历点评入口与结果展示
- `AvatarUploadView.vue`：头像上传、裁剪、一寸照优化 UI
- `DeliveryManagementView.vue`：投递记录管理
- `LandingView.vue`：官网首页
- `SettingsView.vue`：用户账号设置
- `NotFoundView.vue`：404 页面
- `TemplateDetailView.vue`：模板详情页
- `DownloadCenterView.vue`：下载中心（PDF/头像任务）
- `NotificationCenterView.vue`：通知中心
- `ResumeDetailView.vue`：简历详情/预览页
- `AdminLayout.vue`、`admin/Dashboard.vue`、`admin/UserManagement.vue`、`admin/ResumeManagement.vue`、`admin/TemplateManagement.vue`、`admin/SystemSettings.vue`（AI 规则管理）、`admin/ContentAudit.vue`、`admin/DeliveryData.vue`、`admin/Login.vue`：管理后台页面与布局
- `ResumePreview.vue`：iframe 加载后端 `/api/resumes/{id}/preview`
- `frontend/src/utils/download.ts`：PDF/头像任务本地存储与读取工具
- `useAutoSave.ts`：2 秒防抖自动保存 hook
- 类型、API 封装、状态管理、路由骨架、路由守卫

### 待完善（占位）

- `ProfileForm.vue`：个人信息表单
- 新增：`EducationForm.vue`、`WorkForm.vue`、`ProjectForm.vue`、`SkillForm.vue`、`IntroductionForm.vue`、`CustomForm.vue`
- 以上表单组件与编辑器内部数据流的深度集成

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
npm run lint
npm run format
```

---

## 13. 测试要求

- 单元测试：`tests/unit/`
- 组件测试：`tests/component/`
- E2E 测试：`tests/e2e/`
- 新增组件/视图必须补充测试。
- 运行：`npm run test:unit -- --run`

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
