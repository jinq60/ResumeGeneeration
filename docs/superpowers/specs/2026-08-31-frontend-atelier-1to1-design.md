# Atelier 1:1 前端重建 — 设计稿 + API 契约分离

- **日期**: 2026-08-31
- **Stitch 源**: `projects/2026673675331218421` `Resume Atelier Design System` `Atelier Editorial`
- **稿件**: 13 屏 `b81b988c Marketing Landing / 32d3346 Workbench / 6f1d0e7 简历列表 / 802fc19 Core Editor / 1a64a09 Core Editor Deep Detail / b51102 Mobile Editor / b57332 模板中心 / c70e2 AI Review / 8105b9 诊断详情 / 01d035 投递管理 / df319a Mobile Deep Detail` + `Design System Instance assets_01869d`
- **契约**: `docs/API.md` (`backend/**/controller + dto + R + ResultCode` 直译)
- **决策**: 方案 B — Token 抽取 + 组件重建 1:1，不直出 Stitch HTML

## 1. 目标与非目标

**目标**: 像素级还原 13 屏 Editorial 质感 (paper/ink/vermilion, Newsreader/Inter/JetBrains Mono, 4pt 网格, A4 画布居中, hairline 1pt)，随后以 `docs/API.md` 为唯一契约补齐数据联调，达到可演示 `登录→工作台→编辑器→模板→预览→导出→分享` 全链路。

**非目标**: 不引入独立 UI 框架 (shadcn) 套壳；不重写后端渲染 (`ResumeRenderService`)；首版不做离线草稿冲突合并 (仅本地恢复提示)。

## 2. 架构

```
frontend/
  vite.config.ts (proxy /api → localhost:8080)
  tailwind.config.ts // tokens 直译
  src/
    styles/tokens.ts  // designMd → colors/typography/rounded/spacing
    styles/atelier.css // A4 shadow, hairline, editorial
    api/types.ts      // docs/API.md §1.5 直译
    api/client.ts     // axios + R<T> + refresh + Idempotency-Key + X-Trace-Id
    router/index.ts   // 见 §3
    stores/auth.ts, resume.ts, template.ts // Pinia
    composables/useResumeAutosave.ts // 2s debounce PUT version
    components/atelier/ // Button Pill/AI Pill/Ghost, Input 38px/10px, Card 12px, Tag, Skeleton
    components/editor/SectionEditor.vue, A4Canvas.vue, Marginalia.vue, TemplatePicker.vue
    views/ // 13 屏 1:1
```

- **工程**: Vite 7 + Vue 3.4 SFC + TS 5 strict + Pinia + Vue Router 4 + Tailwind 3.4 + Axios + @vueuse/core, pnpm, Node 20.19。
- **令牌**: `src/styles/tokens.ts` 直译 `designMd`：`surface:#fbf9f5 ink:#0f0f0e vermilion:#ff3b1f outline:#c7c7c0 secondary:#5d5f5d` `display 40px/500 Newsreader -0.03em ... caption 11px JetBrains Mono` `rounded sm0.25 md0.75 lg1 xl1.5 pill9999` `spacing 4/8/12/16/24...` → `tailwind.config.ts theme.extend`。
- **契约分离**: `api/types.ts` 为 `docs/API.md` 唯一真值，禁止手写第二套类型；新增字段后端改 DTO 前端即改 type。
- **渲染一致性**: 前端 `A4Canvas` 仅做排版容器，真实简历 HTML 由 `GET /resumes/{id}/preview` 服务端直出 `iframe srcDoc`，与 `PdfService` 复用 `ResumeRenderService` 保证导出一致。

## 3. 路由与 13 屏映射

| 路径 | Stitch 屏 | 权限 | 核心对接 |
|------|-----------|------|----------|
| `/` | `b81b988c Marketing Landing` | public | 静态 |
| `/login` | Auth 自定义 | guest | `GET /auth/methods`, `POST /auth/login`, `POST /auth/guest`, `GET /auth/oauth/{p}/authorize` |
| `/workbench` | `32d3346 Workbench Dashboard` | auth | `GET /resumes` 聚合统计 |
| `/resumes` | `6f1d0e7 简历列表` | auth | `GET /resumes?page&size` |
| `/editor/:id` | `802fc19 Core Editor` + `1a64a09 Deep Detail` (desktop) + `b51102/df319a` (mobile) | auth | `GET /resumes/{id}`, `PUT /resumes/{id} {version}` 409=>刷新, `POST /resumes/{id}/duplicate` |
| `/templates` | `b57332 模板中心` | auth | `GET /templates` 304 ETag |
| `/ai/review/:id` | `c70e2 AI Review` + `8105b9 诊断详情` | auth | `POST /resumes/{id}/grammar-check` |
| `/deliveries` | `01d035 投递管理` | auth | `GET /deliveries` |
| `/share/:token` | `ShareController /share/{token}` | public | `GET /share/{token}` text/html |
| `/admin/*` | 复用稿件 + 表格 | ADMIN | `GET /admin/**` |
| `*` | 404 | - | - |

- `404/403` 统一映射 `ResultCode`。
- `/editor/:id` 三屏合成一路由，`useMediaQuery <768` 切 `BottomSheet`，桌面 `左 360px 表单 | 中 A4 | 右 280px 属性`。

## 4. 组件

- **Atelier 原子**: `Button` (Primary Ink 36px Pill / AI Pill 带 6px vermilion dot / Ghost) `Input` (38px 10px Line→focus 1.5px Ink) `Card` (12px hairline) `Tag` `Skeleton`。
- **Editor**: `SectionEditor.vue` 按 `SectionDTO.type` 分发 `profile/education/work/project/skill/introduction/custom`；`visibility` 眼睛显隐与删除分离；`descriptionHtml` 用 `RichTextSanitizer` 白名单；`A4Canvas.vue` 固定 210×297mm 居中 `shadow 0 4px 24px rgba(0,0,0,.04)`；`Marginalia.vue` 64px 侧栏编号/状态；`TemplatePicker.vue` 缩略图 `GET /templates`。
- **AI**: `AI Pill` 触发生成 `POST /resumes/{id}/ai/write/stream` SSE `delta/done/error`。

## 5. 数据流与状态

```
Pinia resumeStore { detail: ResumeDetailResponse|null, saving: boolean, error: string|null }
  GET /resumes/{id} → detail → SectionEditor v-model → A4Canvas (iframe preview polling 1s 或 PUT 后刷新)
  useResumeAutosave: watch deep sections debounce 2s → PUT /resumes/{id} {sections, version}
    200 → detail.version = newVersion
    409 {code:2012} → toast "已被其他编辑修改，请刷新" + 按钮
    429 → 灰显 AI 按钮
  fillAvatarUrl 为旁路：POST /avatars/upload → POST /avatars/optimize → 轮询 → PUT avatarUrl 不经 version (后端已 validate safeUrl)
```

- `templateStore` 缓存 `GET /templates` ETag。
- `authStore` 存 `accessToken/refreshToken/isGuest`，`401` 触发 `POST /auth/refresh` 失败跳 `/login`。

## 6. 错误与边界

- **网络**: Axios 拦截统一 `R.code` → `403` 跳无权页，`429` 限流提示，`425` 幂等冲突 `稍后重试`。
- **富文本**: 入库前已 `RichTextSanitizer`，前端 `v-html` 仍仅渲染白名单 `p/br/strong/b/em/i/u/ul/ol/li/a[href^http]`
- **上传**: `validateAvatarFile` 前置 `file.size 10MB` 魔数 `jpg/png/webp`，失败直接 `4001`
- **分享**: `GET /share/{token}` `no-store` + `CSP default-src 'none'`

## 7. 测试与验收

- **单测**: Vitest + Vue Test Utils 覆盖 `SectionEditor` (显隐/删除分流)、`A4Canvas` (A4 比例)、`client.ts` (刷新)
- **联调**: MSW 模拟 `docs/API.md` 全部端点
- **E2E**: Playwright `登录→新建→编辑→切换模板→预览→导出 PDF→分享` 与后端 `PdfService` 像素对比
- **验收**: 13 屏与 Stitch 截图 `pixelmatch <2%`，`lighthouse` 性能 >90

## 8. 实施顺序

1) 令牌 + `api/types+client` 2) `atelier` 原子 3) `Marketing/Workbench/简历列表/模板中心` 静态 4) `Editor` 三屏合一 + `Pinia` + `autosave` 5) `A4Canvas iframe` + `PDF/分享` 6) `AI` SSE 7) `投递/审核` 管理端

## 9. 风险

- Stitch `htmlCode` 为生成稿，非手工组件，直接复制会导致 `data` 硬编码 → 采用 B 方案规避
- `version` 乐观锁需前端必传，否则 `409` 误判 → `UpdateResumeRequest.version` 必带
- 移动端 A4 缩放 `transform scale` 需 `A4Canvas` 容器 `overflow` 处理
