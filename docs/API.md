# 智能简历生成工具 — 前端 API 手册

> **生成依据**: `backend/src/main/java/**/controller/*.java` + `**/dto/*.java` + `common/entity/R.java` + `common/constant/ResultCode.java` | **实测分支** `eb07de3` 后 16 项优化版 `2026-08-31` + 2026-09-01 四期 22 项 + 2026-09-02 富文本/导入/校验对齐 | **BaseURL** `http://localhost:8080/api` (`server.servlet.context-path=/api` `application.yml:34`)

前端重建时 **唯一可信契约** 就是后端的 `Controller` + `DTO`，本文件已将二者翻译为 `TypeScript`，后续新增接口只需照此复刻。

```
前端请求 = Controller 的 @RequestMapping + @GetMapping/@PostMapping
          + DTO 的 @NotBlank/@Size/@Valid 字段
          + R<T> 统一包裹
```

| 后端真值 | 前端映射 |
|----------|----------|
| `AuthController.java:32 @RequestMapping("/auth")` | `POST /auth/login` |
| `ResumeController.java:28` `CreateResumeRequest.java:8 title/scene/templateId` | `POST /resumes body: CreateResumeRequest` |
| `R.java:15 {code,message,data}` | `ApiResp<T>` |
| `ResultCode.java:22 1017 AUTH_CREDENTIALS_INVALID` | `if (resp.code===1017) ...` |

---

## 1. 通用约定

### 1.1 统一响应 `R.java:15`
```ts
export interface ApiResp<T> { code: number; message: string; data: T; timestamp: number }
// code===200 成功，其余见 §1.4
export interface Page<T> { records: T[]; total: number; current: number; size: number; pages: number }
// 分页请求统一 ?page=1&size=20 @Min(1) @Max(100)
```

### 1.2 鉴权与请求头
```ts
// AuthController.java:124 + SecurityConfig.java:90
// 登录后 localStorage 存对
localStorage.setItem('accessToken', data.accessToken)
localStorage.setItem('refreshToken', data.refreshToken)

// 每次请求
headers: {
  Authorization: `Bearer ${accessToken}`,
  'Idempotency-Key': crypto.randomUUID(), // POST/PUT/PATCH/DELETE 自动幂等 IdempotencyFilter:52
  'X-Trace-Id': 'abc123' // 可选，RequestLoggingFilter:28 透传
}
```

### 1.3 自动刷新 `AuthController.java:196`
```ts
api.interceptors.response.use(r=>r, async err=>{
  if (err.response?.status===401 && err.config.url!=='/auth/refresh') {
    const {data: resp} = await axios.post('/auth/refresh', { refreshToken: localStorage.refreshToken })
    if (resp.code===200) {
      localStorage.accessToken = resp.data.accessToken
      localStorage.refreshToken = resp.data.refreshToken
      err.config.headers.Authorization = `Bearer ${resp.data.accessToken}`
      return api(err.config)
    }
  }
  return Promise.reject(err)
})
```

### 1.4 错误码 `ResultCode.java` → `GlobalExceptionHandler.java:130`
| code | HTTP | 含义 | 前端处理 |
|------|------|------|----------|
| 200 | 200 | 成功 | - |
| 400 | 400 | PARAM_INVALID 参数错误 | `message` 弹 toast |
| 401 | 401 | UNAUTHORIZED / AUTH_... 未登录或令牌失效 | 跳 /login |
| 403 | 403 | ACCESS_DENIED 无权 | 403 页 |
| 404 | 404 | RESOURCE_NOT_FOUND | 空状态 |
| 409 | 409 | 已注册/编码已存在/版本冲突 `2012 RESUME_VERSION_CONFLICT` | 冲突弹框刷新 |
| 425 | 425 | IDEMPOTENCY_CONFLICT 幂等键正处理中 | 轻提示稍后重试 |
| 429 | 429 | RATE_LIMITED / AI_DAILY_QUOTA_EXCEEDED | 429 提示 |
| 500 | 500 | INTERNAL_ERROR | 重试 |
| 503 | 503 | AUTH_EMAIL_CODE_SEND_FAILED SMTP 未配置 | 提示联系管理员 |

### 1.5 核心 TS 类型（由 DTO 直译）
```ts
// User
export interface AuthResponse { userId: string; accessToken: string; refreshToken: string; expiresIn: number; isGuest: boolean }
export interface UserInfoResponse { userId: string; nickname?: string; phone?: string; email?: string; avatarUrl?: string; isGuest: boolean }

// Resume — ResumeDetailResponse.java:10
export interface ResumeDetailResponse {
  id: string; userId: string; title: string; scene: string;
  targetPosition?: string; targetIndustry?: string; templateId: string;
  sections: SectionDTO[]; renderSettings?: RenderSettings;
  exportCount: number; version: number; // 乐观锁 传回 PUT 时必填
  lastEditedAt: string; createdAt: string; updatedAt: string;
}
export interface SectionDTO { id: string; type: 'profile'|'education'|'project'|'work'|'skill'|'introduction'|'custom'; title: string; order: number; visible: boolean; data: any }
export interface RenderSettings { fontFamily?: string; baseFontSize?: number; lineHeight?: number; pagePadding?: number; sectionSpacing?: number; accentColor?: string; autoOnePage?: boolean }
export interface CreateResumeRequest { title?: string; scene: string; targetPosition?: string; targetIndustry?: string; templateId: string }
export interface UpdateResumeRequest extends Partial<CreateResumeRequest> { sections?: SectionDTO[]; renderSettings?: RenderSettings; version?: number }

// Template — TemplateDTO.java:10
export interface TemplateDTO { id: string; code: string; name: string; category: string; thumbnailUrl?: string; description?: string; config: any; htmlTemplate: string; renderEngine: string; sortOrder: number; isRecommended: boolean; status: string; createdAt: string; updatedAt: string }

// AI
export interface ResumeAiWriteRequest { sectionType: string; field: string; action: 'generate'|'polish'|'shorten'|'expand'|'translate'; originalText: string; targetLang?: string }
export interface ResumeAiWriteResponse { content: string }
export interface GrammarCheckResponse { status: string; model: string; checkedAt: string; issues: { sectionType: string; field: string; severity: string; suggestion: string; explanation: string }[] }

// 其他
export interface PdfTaskResponse { taskId: string; resumeId: string; templateId: string; status: 'pending'|'processing'|'success'|'failed'; fileName?: string; fileSize?: number; errorMsg?: string; createdAt: string; completedAt?: string }
export interface ShareResponse { token: string; url: string; status: string; hideContact: boolean; expiresAt?: string; createdAt: string }
```

---

## 2. 认证 `AuthController.java:32` `@RequestMapping("/auth")` 全部 `permitAll`

### `POST /auth/login` — 密码登录
**Req** `LoginRequest.java:10 { account, password, loginType?: 'phone'|'email' }` **Resp** `R<AuthResponse>`
```ts
await api.post('/auth/login', { account: '13800000000', password: 'Aa123456' })
```

### `POST /auth/login/{method}` — 统一适配器 `method=password|email_code|sms_code`
```ts
await api.post('/auth/login/email_code', { email: 'a@b.com', code: '123456' })
```

### `GET /auth/methods` — 渲染登录页
**Resp** `R<{ loginMethods:{method,configured}[], oauthProviders:{provider,configured}[] }>`

### 邮箱/短信
| 方法 | 路径 | Req |
|------|------|-----|
| `POST` | `/auth/email-code/send` | `SendEmailCodeRequest{email}` 60s 冷却 |
| `POST` | `/auth/email-code/login` | `EmailCodeLoginRequest{email,code}` 自动建号 |
| `POST` | `/auth/sms-code/send` | `SendSmsCodeRequest{phone}` 日 10 次 `SmsCodeService:118` |
| `POST` | `/auth/sms-code/login` | `SmsCodeLoginRequest{phone,code}` |

### OAuth `AuthController.java:120`
```ts
// 跳转
window.location.href = '/api/auth/oauth/github/authorize' // 302
// 回调 → 前端 ?oauth_code=xxx 再换
await api.post('/auth/oauth/exchange', { code: oauth_code })
```

### `POST /auth/guest` — 游客 `IP 50/日` `GuestAccountGuard:32`
### `POST /auth/refresh` — `RefreshRequest{refreshToken}` 家族 `fid` 重放整族吊销 `UserService:191`
### `POST /auth/logout` — `LogoutRequest{refreshToken}`

## 3. 用户 `UserController.java:25` `authenticated`

| 方法 | 路径 | Req | Resp |
|------|------|-----|------|
| `GET` | `/users/me` | - | `R<UserInfoResponse>` |
| `PUT` | `/users/me` | `UpdateProfileRequest{nickname?,phone?,email?,avatarUrl?}` | `R<UserInfoResponse>` |
| `GET` | `/users/me/preferences` | - | `R<Record<string,any>>` |
| `PUT` | `/users/me/preferences` | `Map` 整体覆盖 | `R<Map>` |
| `PUT` | `/users/me/password` | `ChangePasswordRequest{oldPassword,newPassword}` 8-32 字母+数字 | `R<Void>` 旧会话全失效 |

## 4. 简历 `ResumeController.java:28` `authenticated`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/resumes` | `CreateResumeRequest` → `R<ResumeDetailResponse>` |
| `POST` | `/resumes/import` | `ResumeImportRequest{format:'json'|'markdown',content,title?,scene?,targetPosition?,templateId}` → `R<ResumeDetailResponse>` |
| `GET` | `/resumes?page&size` | `Page<ResumeListItemResponse>` |
| `GET` | `/resumes/{id}` | 归属校验 `403` |
| `PUT` | `/resumes/{id}` | `UpdateResumeRequest` 必须带 `version` 否则 `2012` 冲突 `ResumeService:245` |
| `DELETE` | `/resumes/{id}` | 级联清 PDF/头像/分享 |
| `POST` | `/resumes/{id}/duplicate` | 副本 `DuplicateResumeResponse` |
| `PUT` | `/resumes/{id}/title` | `RenameResumeRequest{title}` |
| `POST` | `/resumes/{id}/reviews` | `ReviewResumeRequest{jobDescription}` AI 点评 |
| `GET` | `/resumes/{id}/reviews/latest` | 最近一次点评 `null` 兜底 |

**乐观锁示例**
```ts
const { data: detail } = await api.get(`/resumes/${id}`)
await api.put(`/resumes/${id}`, { title: '新标题', version: detail.version })
// 409 {code:2012} => toast 刷新
```

### 预览/导出 `PreviewController.java:46` `ResumeExportController.java:20`
```ts
// 服务端统一渲染，确保预览与 PDF 一致 ResumeRenderService:122
await api.get(`/resumes/${id}/preview?templateId=tech`, { responseType: 'text' }) // => iframe srcDoc
await api.get(`/resumes/${id}/export/markdown`, { responseType: 'blob' })
await api.get(`/resumes/${id}/export/word`, { responseType: 'blob' })
```

## 5. 模板 `TemplateController.java:24` `GET permitAll`

| 方法 | 路径 | 备注 |
|------|------|------|
| `GET` | `/templates` | `304 ETag` `60s public` |
| `GET` | `/templates/{id}` | 同上 |
| `GET` | `/admin/templates?page&size&status&category` | `ADMIN` |
| `GET` | `/admin/templates/stats` |  |
| `POST` | `/admin/templates` | `AdminTemplateRequest{code,name,category,config,htmlTemplate,renderEngine}` code 唯一含已删 `uk_template_code` |
| `PUT` | `/admin/templates/{id}` | `code` 不可改 `3003` |
| `PATCH` | `/admin/templates/{id}/status` | `active/inactive` |
| `POST` | `/admin/templates/upload` | `multipart file` 魔数 `jpg/png/webp/svg` |
| `DELETE` | `/admin/templates/{id}` | 引用>0 `409` |

## 6. 头像 `AvatarController.java:24`
```ts
const {data: {sourceImageUrl}} = await api.postForm('/avatars/upload', { file, resumeId })
// 图片 10MB 內，先 validated by ImageMagicUtil:12
await api.post('/avatars/optimize', { sourceImageUrl, resumeId, backgroundType:'white'|'blue'|'red', style:'formal'|'natural' })
// => {taskId, status:'pending'}
await api.get(`/avatars/tasks/${taskId}`) // 轮询
await api.delete(`/avatars/${id}`)
```

## 7. PDF `PdfController.java:27`
```ts
const {data: {taskId}} = await api.post('/pdf/export', { resumeId, templateId })
let t; while((t=(await api.get(`/pdf/tasks/${taskId}`)).data.data).status!=='success') await sleep(1000)
window.location.href = `/api/pdf/download/${taskId}` // 流式 StreamingResponseBody:8KB
```
`AdminPdfController.java:28` `GET /admin/pdf/tasks/{id}` `GET /admin/pdf/download/{id}`

## 8. 分享 `ShareController.java:29`
```ts
await api.post(`/resumes/${resumeId}/share`, { hideContact: true, expiresAt: '2026-12-31T00:00:00' })
// => ShareResponse { token, url:'/share/xxx' }
await api.get(`/resumes/${resumeId}/share`) // null 无分享
await api.delete(`/resumes/${resumeId}/share`)
// 公开页 无需鉴权
window.open(`/api/share/${token}`) // text/html no-store CSP: default-src 'none'
```

## 9. AI `AiResumeController:28` `AiWritingController:27` `AiGrammarController:22`
```ts
// JD 优化
await api.post(`/resumes/${id}/optimize`, { jobDescription: 'JD...' }) // => {taskId, status:pending}
await api.get(`/resumes/${id}/optimize/${taskId}`)
// 行内写作 配额 guest3/日 user30/日 AiDailyQuotaService:66 精准 refund
await api.post(`/resumes/${id}/ai/write`, { sectionType:'introduction', field:'content', action:'polish', originalText:'...' })
// 流式
const es = new EventSource(`/api/resumes/${id}/ai/write/stream`)
es.addEventListener('delta', e=> append(e.data))
es.addEventListener('done', ()=> es.close())
// 语法检查
await api.post(`/resumes/${id}/grammar-check`) // => GrammarCheckResponse
```

## 10. 投递 `DeliveryController.java:24`
`POST /deliveries {resumeId,company,position,channel?,status?,applyDate?,jdContent?}` | `GET /deliveries?page&size&keyword&company&position&status&startDate&endDate` | `GET/PUT/DELETE /deliveries/{id}` | `ADMIN: GET /admin/deliveries?keyword`, `GET /admin/deliveries/stats`, `GET /admin/deliveries/export=>CSV`

## 11. 通知 `NotificationController.java:28`
`GET /notifications?page&size&unreadOnly`, `GET /notifications/unread-count=>{count}`, `PUT /notifications/read-all`, `PUT /notifications/{id}/read`, `DELETE /notifications/{id}`

## 12. 管理 `AdminAuditController:25` `AdminAiRuleController:28` `AdminResumeController:35`
`GET /admin/audits?page&size&keyword&status&riskLevel`, `POST /admin/audits/{id}/approve|reject|mark-warning {note?}` |
`GET /admin/ai-rules...`, `POST /admin/ai-rules`, `PUT /admin/ai-rules/{id}/draft` |
`GET /admin/resumes?keyword`, `GET /admin/resumes/stats`

## 13. 静态 `StaticResourceController.java:27` `permitAll`
`GET /uploads/avatars/{userId}/... => image/*` `no-store` | `GET /uploads/templates/... 1h` | `GET /templates/thumbs/*.svg 1h` 路径穿越校验 `validateObjectName:105`

---

## 附：前端请求应以哪些后端文件为准

> 重建前端时，不要猜接口，直接对着这三类文件抄。

| 后端文件 | 前端作用 |
|----------|----------|
| `backend/src/main/java/**/controller/*.java` | 路径、方法、鉴权 `@RequestMapping` `@GetMapping` |
| `backend/src/main/java/**/dto/*.java` | 请求/响应字段 `@NotBlank @Size` 即前端表单校验 |
| `backend/src/main/java/com/resume/common/entity/R.java` + `common/constant/ResultCode.java` | 统一包裹与错误码 |
| `backend/src/main/resources/application.yml:34 context-path` | 实际 `fetch('/api'+path)` 前缀 |

> `npm` 前端若需重新生成：`npx openapi-typescript http://localhost:8080/api/v3/api-docs -o src/api/schema.d.ts` (`springdoc-openapi 2.5.0` `OpenApiConfig.java` 已开启, prod 自动关闭)。

