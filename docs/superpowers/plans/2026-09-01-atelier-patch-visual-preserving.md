# Atelier Patch Visual-Preserving Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变 Editorial 视觉（paper/ink/vermilion、Newsreader/Inter/JetBrains Mono、A4 210x297 hairline 0.5px、shadow-paper）的约束下，修补前端契约/代理/状态逻辑裂缝，使 `登录→工作台→编辑器→模板→预览→导出→分享` 全链路可演示且 `npm run build` 与后端 `8080` 对齐。

**Architecture:** 仅逻辑层修补（config/client/types/store/views），零样式改动。Vite proxy 与 `application.yml:32 port8080 + context-path /api` 对齐；`client.ts` 刷新复用 `Idempotency-Key` 与 `authStore` 同步；`types.ts` 以 `backend/**/dto/*.java + R.java` 为唯一真值补全 `Notification/Delivery/Grammar/Template`；各 View 仅改 hardcode 数据源/生命周期，不动 `class` 视觉串。

**Tech Stack:** Vue 3.4 SFC + TS 5 strict + Pinia + Vue Router 4 + Tailwind 3.4 + Axios 1.20 + Vite 7, JDKEndpoints `http://localhost:8080/api`

**Spec:** `docs/API.md` (R+ResultCode 唯一契约, BaseURL `http://localhost:8080/api` `application.yml:34`) + `docs/superpowers/specs/2026-08-31-frontend-atelier-1to1-design.md` (Atelier Editorial 13屏, 令牌 `surface#fbf9f5 ink#0f0f0e vermilion#ff3b1f` `rounded sm0.25 md0.75 lg1 xl1.5` `A4 shadow-paper`, 方案B Token抽取)

## Global Constraints
- Design 视觉冻结：禁止修改 `frontend/src/style.css` `:root` 令牌、`tailwind.config.js` `theme.extend.colors/fontFamily/spacing/boxShadow`、`frontend/src/views/*` 与 `components/**/*` 内的 `class="... border-[#...] bg-[#...] font-[...]"` 字符串、`A4Canvas 210mm/297mm` 内联尺寸；仅允许改 `script setup` 逻辑与 `hardcode 文本 fallback` 的 `??` 分支
- 后端契约：`server.servlet.context-path=/api` `application.yml:34` 不改；前端 `baseURL='/api'` 通过 Vite proxy 转 `http://localhost:8080`
- 幂等：`Idempotency-Key` 需在 401 重试时复用原值（`IdempotencyFilter:52` 按 `user:method:path:key` 计）
- 乐观锁：`PUT /resumes/{id}` 必须回传 `version`，`2012=>409` 前端刷新
- Node 20.19 / JDK 17 / 不引入新 UI 框架 (shadcn) / 不改 `ResumeRenderService` 服务端渲染

---

## File Structure

- Modify: `frontend/vite.config.ts:15-19` — proxy target `8088->8080`
- Modify: `frontend/src/api/client.ts:10-62` — Idempotency-Key 复用、refresh 复用 client、queue reject、withCredentials 清理、TraceId 保留
- Modify: `frontend/src/api/types.ts:100-185` — 补全 Notification/Delivery/Grammar/Template 及 Page 语义
- Modify: `frontend/src/stores/auth.ts:7-50` — refresh 后同步 `accessToken` ref
- Modify: `frontend/src/views/EditorView.vue:38,177,41-83` — shareUrl 去 hardcode 5173、pdf 轮询清理、share expiresAt
- Modify: `frontend/src/views/ShareView.vue:9` — fetch 前缀改为 `VITE_API_BASE` 或相对 `/api`
- Modify: `frontend/src/views/AiReviewView.vue:132-145` — preview 对 `R` 包裹兼容
- Modify: `frontend/src/views/WorkbenchView.vue:59-72` — 假数据仅当空列表时 fallback，不覆盖真实
- Modify: `frontend/src/components/editor/SectionEditor.vue:85-230` — 429 文案去 hardcode `guest 3/日` 改读 `ResultCode 6009` 提示
- Verify: `frontend/src/style.css` `tailwind.config.js` `src/views/*` class diff == 0 (visual regression guard)

---

### Task 1: 代理与刷新链路对齐 (无视觉影响)

**Files:**
- Modify: `frontend/vite.config.ts:15-19`
- Modify: `frontend/src/api/client.ts:4-62`

**Interfaces:**
- Consumes: `application.yml:32 port 8080` `client.baseURL '/api'`
- Produces: `client.post('/auth/refresh')` 复用 `client` 实例 + 原 `Idempotency-Key` 透传供 Task2/3 使用

- [ ] **Step 1: 复现失败 — 验证当前代理错位**

```bash
# 预期：vite proxy 8088 导致 ECONNREFUSED（后端仅监听 8080）
# 检查当前值
grep -n "target" frontend/vite.config.ts
# 预期输出：target: 'http://localhost:8088'
grep -n "/api/auth/refresh" frontend/src/api/client.ts
# 预期：axios.post('/api/auth/refresh'  # 硬编码绕过 baseURL
```

- [ ] **Step 2: 修复 vite.config.ts — 8088 -> 8080**

```ts
// frontend/vite.config.ts:15-19 修改前
'/api': { target: 'http://localhost:8088', changeOrigin: true }
// 修改后
'/api': { target: 'http://localhost:8080', changeOrigin: true }
```

- [ ] **Step 3: 修复 client.ts — 复用实例 + 复用幂等键 + 同步 auth ref**

```ts
// frontend/src/api/client.ts:10-19 修改点
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  if (['post','put','patch','delete'].includes((config.method||'').toLowerCase())) {
    // 401 重试时复用原 key，不再生
    if (!config.headers['Idempotency-Key']) config.headers['Idempotency-Key'] = crypto.randomUUID()
  }
  if (!config.headers['X-Trace-Id']) config.headers['X-Trace-Id'] = Math.random().toString(36).slice(2,10)
  return config
})

// frontend/src/api/client.ts:42-52 刷新段改为复用 client 而非 axios
const { data } = await client.post<ApiResp<AuthResponse>>('/auth/refresh', { refreshToken })
// 并同步内存 ref (需 import useAuthStore 或直接写 localStorage + 事件；最简：同时写 localStorage 并在成功后若 Pinia 已初始化则同步)
// 方案：仅 localStorage，Pinia 侧 Task3 会监听 storage 事件；此处补：
localStorage.setItem('accessToken', data.data.accessToken)
localStorage.setItem('refreshToken', data.data.refreshToken)
// 可选：若 window.__piniaAuth 则 同步

// 队列增加 reject 传播
let queue: Array<{resolve:()=>void, reject:(e:any)=>void}> = []
// refreshing 分支：
await new Promise<void>((resolve, reject) => queue.push({resolve, reject}))
// 成功时 queue.forEach(q=>q.resolve()); 失败时 queue.forEach(q=>q.reject(e))
```

完整修改后文件参考 `frontend/src/api/client.ts:1-76` 保留 `timeout 30000`、`withCredentials: false`（改为 false，因 Bearer 无 Cookie，保留 true 冗余可置 false 或保持不变但注释说明）

- [ ] **Step 4: 验证**

```bash
grep -n "8080" frontend/vite.config.ts
# 预期：target: 'http://localhost:8080'
grep -n "client.post.*auth/refresh" frontend/src/api/client.ts
# 预期：client.post('/auth/refresh' 且 无 axios.post
grep -n "Idempotency-Key" frontend/src/api/client.ts
# 预期：if (!config.headers['Idempotency-Key'])
npm run build --prefix frontend
# 预期：PASS (vue-tsc -b && vite build)
```

- [ ] **Step 5: Commit**

```bash
git add frontend/vite.config.ts frontend/src/api/client.ts
git commit -m "fix(frontend): align vite proxy 8080 and reuse Idempotency-Key on refresh"
```

---

### Task 2: 契约类型补全（仅 types.ts，不碰样式）

**Files:**
- Modify: `frontend/src/api/types.ts:100-185`

**Interfaces:**
- Consumes: `backend/**/dto/*.java` `NotificationResponse.java:13` `DeliveryRecordResponse.java:15` `GrammarCheckResponse.java:12` `TemplateDTO.java:11`
- Produces: `NotificationResponse/DeliveryRecordResponse/GrammarCheckResponse` 供 `AtelierLayout.vue:100` `DeliveriesView:136` `AiReviewView:164` 消费

- [ ] **Step 1: 复现 — 类型不匹配导致前端 undefined**

```ts
// 检查当前
// NotificationResponse.readFlag:number vs backend boolean read
// Delivery 缺 jdContent/note/interviewTime/Location
// Grammar 缺 message/itemIndex
grep -n "readFlag" frontend/src/api/types.ts
grep -n "jdContent" backend/src/main/java/com/resume/delivery/dto/DeliveryRecordResponse.java
```

- [ ] **Step 2: 修复 types.ts — 精确直译，增加兼容字段不删旧字段**

```ts
// frontend/src/api/types.ts 修改

// 修正 Notification — 保留 readFlag 兼容旧后端，同时新增 read boolean
export interface NotificationResponse {
  id: string
  type: string
  title: string
  content: string
  read: boolean            // 新增：后端 isRead() -> JSON "read"
  readFlag?: number        // 保留兼容：0/1 旧前端写入
  createdAt: string
}

// 补全 Delivery — 新增可选字段
export interface DeliveryRecordResponse {
  id: string
  resumeId: string
  userId?: string
  company: string
  position: string
  channel?: string
  status: string
  applyDate?: string
  jdContent?: string       // 新增
  note?: string            // 新增
  interviewTime?: string   // 新增
  interviewLocation?: string // 新增
  createdAt: string
  updatedAt?: string       // 新增
}

// 补全 Grammar — 新增 message/itemIndex
export interface GrammarCheckResponse {
  status: string
  model: string
  message?: string          // 新增 GrammarCheckResponse.java:16
  checkedAt: string
  issues: Array<{
    sectionType: string
    field: string
    itemIndex?: number      // 新增
    severity: string
    originalText: string
    suggestion: string
    explanation: string
  }>
}

// 补全 Template — 新增 isBuiltin 版本等可选
export interface TemplateDTO {
  id: string; code: string; name: string; category: string
  thumbnailUrl?: string; description?: string
  config: any; htmlTemplate: string; renderEngine: string
  sortOrder: number; isRecommended: boolean; isBuiltin?: boolean
  status: string; createdAt: string; updatedAt: string; version?: number
}

// 新增 Page 解包 helper 类型（不改运行时）
export type UnwrapPage<T> = Page<T> | ApiResp<Page<T>>
```

- [ ] **Step 3: 同步消费侧兼容（不改样式，仅改取值）**

```ts
// AtelierLayout.vue:48 取已读时 兼容 read/readFlag
const isRead = (n: NotificationResponse) => typeof n.read === 'boolean' ? n.read : (n.readFlag === 1)
// DeliveriesView 空字段仅展示时 ?. 可选链，已有 hardcode 表格无需新列
```

- [ ] **Step 4: 验证**

```bash
npx vue-tsc -b --noEmit
# 预期：PASS，无类型错误
grep -n "read: boolean" frontend/src/api/types.ts
# 预期：命中
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/types.ts frontend/src/layouts/AtelierLayout.vue
git commit -m "fix(frontend): complete Api types for notification/delivery/grammar alignment"
```

---

### Task 3: 分享与认证状态硬编码清理

**Files:**
- Modify: `frontend/src/stores/auth.ts:42-50`
- Modify: `frontend/src/views/EditorView.vue:38,41-83`
- Modify: `frontend/src/views/ShareView.vue:9`

**Interfaces:**
- Consumes: `client.ts` 刷新后 token, `ShareController:69 GET /share/{token} text/html`
- Produces: `shareUrl` 动态 origin 供复制，`auth.isLoggedIn` 内存与 LS 一致

- [ ] **Step 1: 复现 hardcode**

```bash
grep -n "5173" frontend/src/views/EditorView.vue
# 预期：http://localhost:5173/share/
grep -n "fetch('/api/share" frontend/src/views/ShareView.vue
# 预期：硬编码 /api
grep -n "localStorage.setItem" frontend/src/stores/auth.ts
```

- [ ] **Step 2: 修复 auth.ts — 同步 ref**

```ts
// frontend/src/stores/auth.ts:42 setAuth 补 ref 同步
function setAuth(auth: AuthResponse) {
  accessToken.value = auth.accessToken
  refreshToken.value = auth.refreshToken
  localStorage.setItem('accessToken', auth.accessToken)
  localStorage.setItem('refreshToken', auth.refreshToken)
}
// client 刷新成功后同样需同步：最简在 client 拦截器成功后写入 localStorage 后，尝试同步 pinia
// 若不想引入循环依赖，client 内额外：
try {
  const { useAuthStore } = await import('@/stores/auth')
  const s = useAuthStore()
  s.accessToken = data.data.accessToken
  s.refreshToken = data.data.refreshToken
} catch {}
// 或者在 auth.ts 暴露 syncFromStorage() 供 client 回调
```

- [ ] **Step 3: 修复 EditorView shareUrl 去 hardcode**

```ts
// frontend/src/views/EditorView.vue:38 前
const shareUrl = computed(() => shareData.value ? `http://localhost:5173/share/${shareData.value.token}` : '')
// 后
const shareUrl = computed(() => shareData.value ? `${window.location.origin}/share/${shareData.value.token}` : '')
// 若需与后端 ShareResponse.url 一致，优先用 shareData.value.url 否则 origin
const shareUrl = computed(() => {
  if (!shareData.value) return ''
  return shareData.value.url?.startsWith('http') ? shareData.value.url : `${window.location.origin}/share/${shareData.value.token}`
})
```

- [ ] **Step 4: 修复 ShareView fetch 前缀**

```ts
// frontend/src/views/ShareView.vue:9 前
fetch('/api/share/${token}')
// 后
fetch(`${import.meta.env.VITE_API_BASE || '/api'}/share/${token}`)
// 或直接用 client.get(`/share/${token}`, {responseType:'text'}) 避免硬编码
const res = await fetch(`${import.meta.env.VITE_API_BASE || '/api'}/share/${token}`)
```

- [ ] **Step 5: 验证**

```bash
grep -n "window.location.origin" frontend/src/views/EditorView.vue
grep -n "VITE_API_BASE" frontend/src/views/ShareView.vue
npm run build --prefix frontend
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/stores/auth.ts frontend/src/views/EditorView.vue frontend/src/views/ShareView.vue
git commit -m "fix(frontend): remove share hardcode 5173 and sync auth ref on refresh"
```

---

### Task 4: 生命周期与假数据边界（无视觉改动）

**Files:**
- Modify: `frontend/src/views/EditorView.vue:177-191`
- Modify: `frontend/src/views/WorkbenchView.vue:59-72`
- Modify: `frontend/src/views/AiReviewView.vue:132-145`
- Modify: `frontend/src/components/editor/SectionEditor.vue:85-230` (文案段)

**Interfaces:**
- Consumes: `PdfController:30 POST /pdf/export + GET /pdf/tasks/{id}` `PreviewController:63 GET /resumes/{id}/preview`
- Produces: `onUnmounted 清理` 供浏览器不泄漏，Workbench 真实数据优先

- [ ] **Step 1: 修复 Editor pdf 轮询泄漏**

```ts
// frontend/src/views/EditorView.vue:177 前
const iv = setInterval(...)
// 后
import { onUnmounted } from 'vue'
let pdfTimer: ReturnType<typeof setInterval> | null = null
async function exportPdf() {
  const { data } = await client.post('/pdf/export', { resumeId: id })
  const taskId = data.data.taskId
  if (pdfTimer) clearInterval(pdfTimer)
  pdfTimer = setInterval(async () => {
    const t = (await client.get(`/pdf/tasks/${taskId}`)).data.data as PdfTaskResponse
    if (t.status === 'success') { if (pdfTimer) clearInterval(pdfTimer); window.open(`/api/pdf/download/${taskId}`) }
    if (t.status === 'failed') { if (pdfTimer) clearInterval(pdfTimer); pushToast('导出失败: '+(t.errorMsg||'unknown')) }
  }, 1000)
}
onUnmounted(() => { if (pdfTimer) clearInterval(pdfTimer) })
// 同时将 alert 改为 pushToast 保持 Editorial toast 风格
```

- [ ] **Step 2: 修复 Workbench 假数据覆盖真实**

```ts
// frontend/src/views/WorkbenchView.vue:59-72 前 total||12 2h ago 94% 恒覆盖
// 后 仅当 list 为空时 fallback，真实数据优先
const stats = computed(() => ({
  total: store.total || list.value.length || 0,
  updated: list.value[0]?.updatedAt ? formatRelative(list.value[0].updatedAt) : '—',
  score: list.value.length ? '—' : '94%' // 无数据时才示 假分
}))
// 模板中 {{ stats.total || 12 }} 改为 {{ stats.total }}
```

- [ ] **Step 3: 修复 AiReview preview 包裹兼容**

```ts
// frontend/src/views/AiReviewView.vue:132 前 responseType:'text' 可能拿到 R JSON
// 后
try {
  const { data } = await client.get(`/resumes/${id}/preview`, { responseType: 'text' as any })
  // 后端 PreviewController:63 直接写 text/html 非 R 包裹；但若被拦截则 data.data 为 string
  const html = typeof data === 'string' ? data : (data?.data ?? '')
  previewHtml.value = html
} catch { previewHtml.value = '' }
```

- [ ] **Step 4: 去 hardcode 429 文案**

```ts
// frontend/src/components/editor/SectionEditor.vue 搜索 "guest 3/日"
// 前 pushToast('guest 3/日...')
// 后 读后端 ResultCode 6009 message
pushToast(e?.response?.data?.message || '已达今日 AI 配额上限')
```

- [ ] **Step 5: 验证 — class diff 为 0**

```bash
# 视觉回退守卫：确保未改 class 字符串
git diff -- frontend/src/style.css frontend/tailwind.config.js
# 预期：无输出
git diff -- frontend/src/views/EditorView.vue | grep -E "^\+.*class="
# 预期：无新增 class 行
npm run build --prefix frontend
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/EditorView.vue frontend/src/views/WorkbenchView.vue frontend/src/views/AiReviewView.vue frontend/src/components/editor/SectionEditor.vue
git commit -m "fix(frontend): cleanup pdf timer leak and fallback data boundary without visual change"
```

---

### Task 5: 视觉回退验证与最终构建

**Files:**
- Verify: `frontend/src/style.css` `tailwind.config.js` `frontend/src/views/*` `frontend/src/components/**`

- [ ] **Step 1: 视觉 diff 检查**

```bash
git diff --stat HEAD
# 预期：仅 8-10 frontend 逻辑文件，无 style.css/tailwind.config.js 变更
git diff HEAD -- frontend/tailwind.config.js frontend/src/style.css
# 预期：空
```

- [ ] **Step 2: 类型与构建**

```bash
cd frontend
npx vue-tsc -b --noEmit
npm run build
# 预期：PASS，dist/ 生成
```

- [ ] **Step 3: 契约冒烟（可选，需后端 8080 运行）**

```bash
# 后端需先：mvn spring-boot:run -Dspring-boot.run.profiles=dev (8080)
curl -s http://localhost:8080/api/templates | head -c 200
# 预期：200 JSON，含 code/name/category
```

- [ ] **Step 4: 最终 commit（如有遗留）**

```bash
git status --porcelain
git log --oneline -5
```

