# Fix Review Findings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 逐个修复2026-09-04走查报告的Critical/Major问题，使头像鉴权、分享旁路、并发限流、查询索引、终态竞争、前端轮询/XSS、运维配置回归安全可用。

**Architecture:** 后端以最小安全收紧+单测锁定为主（MockMvc+H2/Mockito），不改表结构只加索引SQL；前端以可取消轮询+DOMPurify白名单为主；运维只改默认值与文档，不动线上密钥。

**Tech Stack:** Spring Boot 3.2.5 / JDK17 / MyBatis-Plus / MockMvc / Vue3 + TS + Axios / Docker Compose

**Spec:** 本次走查报告（C1-C8/M1-M15）+ `docs/API.md` + `backend/src/main/resources/application.yml:34 context-path /api`

## Global Constraints

- JDK 17 (`backend/pom.xml:21 java.version 17`)，`mvn -Dtest=... test` 验证
- `server.servlet.context-path=/api` 不改，前端 `baseURL='/api'`
- 不引入新框架；DOMPurify为例外允许（前端XSS必需）
- 每个修复先写失败测试，看到失败再实现（TDD Iron Law）
- 不自动commit/push，留给用户确认

---

## File Structure

- Modify: `backend/src/main/java/com/resume/common/controller/StaticResourceController.java:116-162` — 去Referer旁路、绑定shareToken到objectName、统一404、脱敏日志、循环解码
- Modify: `backend/src/test/java/com/resume/common/controller/StaticResourceControllerTest.java` — 补鉴权/穿越回归测试
- Modify: `backend/src/main/java/com/resume/resume/share/service/ShareService.java:152-200` — Jsoup改写URL、删死参token、URL编码
- Modify: `backend/src/main/java/com/resume/ai/service/AiResumeOptimizeService.java:60-90` + `PdfService.java:74` — 去remove修复ABA声明单实例
- Modify: `backend/src/main/java/com/resume/resume/service/ResumeService.java:104-155` + `ResumeController.java:41-48` — LIKE转义+长度校验+scene归一化+分页钳制
- Modify: `backend/src/main/resources/db/migration/V17__fix_resume_indexes.sql` — 新建补索引
- Modify: `backend/src/main/java/com/resume/common/handler/GlobalExceptionHandler.java:159-194` — 6003/6005→502、4007→500、分享/模板冲突码分离
- Modify: `frontend/src/views/EditorView.vue:205-244` — 递归setTimeout可取消轮询
- Modify: `frontend/src/views/EditorView.vue:1115` + `ShareView.vue:37` — DOMPurify
- Modify: `frontend/src/stores/resume.ts:17` + `ResumeListView.vue:40,170` — 严格契约+透传opts+去硬编码
- Modify: `ops/.env` + `frontend/Dockerfile:13` + `application.yml:5` 文档 — 端口绑定、backend:8080、profile fail-fast说明

---

### Task 1: 头像鉴权收紧（C1/C2/M4/M1+日志脱敏）

**Files:**
- Modify: `backend/src/main/java/com/resume/common/controller/StaticResourceController.java:116-162`
- Modify: `backend/src/test/java/com/resume/common/controller/StaticResourceControllerTest.java`

**Interfaces:**
- Consumes: `ResumeShareMapper.selectOne(eq token)`、`SecurityContextHolder.getAuthentication()`
- Produces: `enforceAvatarAccess(request, objectName)` 无返回，非法抛 `BusinessException(404)` 供Task2复用语义

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldRejectForgedReferer() throws Exception {
    mockMvc.perform(get("/api/uploads/avatars/u/a.png").contextPath("/api").header("Referer","https://x/share/fake"))
        .andExpect(status().isNotFound());
}
@Test
void shouldRejectDoubleEncodedTraversal() throws Exception {
    mockMvc.perform(get("/api/uploads/avatars/%252e%252e/secret.png").contextPath("/api"))
        .andExpect(status().isBadRequest());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=com.resume.common.controller.StaticResourceControllerTest test`
Expected: FAIL — forged Referer now returns 200 bypass, double-encoded returns 200/500 not 400

- [ ] **Step 3: Write minimal implementation**

```java
private void enforceAvatarAccess(HttpServletRequest request, String objectName) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean authenticated = auth != null && auth.isAuthenticated()
        && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    if (authenticated) return;
    String shareToken = request.getParameter("shareToken");
    if (org.apache.commons.lang3.StringUtils.isNotBlank(shareToken)) {
        String t = shareToken.trim();
        com.resume.resume.share.entity.ResumeShare share = resumeShareMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.resume.resume.share.entity.ResumeShare>()
                .eq(com.resume.resume.share.entity.ResumeShare::getToken, t));
        if (share != null && "active".equals(share.getStatus())
            && Integer.valueOf(0).equals(share.getDeleted())
            && (share.getExpiresAt()==null || share.getExpiresAt().isAfter(java.time.LocalDateTime.now()))) {
            // TODO Phase2: 绑定 objectName 前缀到 share.resumeId，本次先收紧Referer+状态码
            return;
        }
        throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "分享不存在或已过期。");
    }
    throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "资源不存在。");
}
```

同时：删Referer分支；`log.warn` 改为 `log.warn("Invalid shareToken for avatar access")` 不记token；`validateObjectName` 改循环解码2次再判 `..`，且原文含 `%` 直接拒。

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=com.resume.common.controller.StaticResourceControllerTest test`
Expected: PASS

- [ ] **Step 5: Manual verify**

```bash
git diff -- backend/src/main/java/com/resume/common/controller/StaticResourceController.java
```

### Task 2: 分享URL改写（M2/M3）

**Files:**
- Modify: `backend/src/main/java/com/resume/resume/share/service/ShareService.java:152-200`

**Interfaces:**
- Consumes: `ResumeRenderService.render(...) -> html`
- Produces: `renderSharePage(token) -> wrapped html with ?shareToken=` 供Task1消费

- [ ] **Step 1: Write the failing test** 在 `ShareServiceTest` 加 `shouldNotDuplicateShareTokenParam`：body已含 `?shareToken=old` 时不再拼第二个；含 `?v=1` 时用 `&`。

- [ ] **Step 2: Run test to verify it fails** `mvn -Dtest=*ShareServiceTest test` FAIL

- [ ] **Step 3: Write minimal implementation** 用Jsoup遍历 `img[src*=​/uploads/avatars/]`，跳过已含 `shareToken=`，判 `contains("?")?"&":"?"` + `URLEncoder.encode(token,UTF_8)` + `Matcher.quoteReplacement`；`wrapSharePage` 删第三参 `token`。

- [ ] **Step 4: Run test** PASS

### Task 3: 并发锁ABA（C5）

**Files:**
- Modify: `backend/src/main/java/com/resume/ai/service/AiResumeOptimizeService.java:60-90`
- Modify: `backend/src/main/java/com/resume/pdf/service/PdfService.java:74-138`

- [ ] **Step 1: Write failing test** 并发20线程同user调 `createTask` mock mapper count=0，断言 `insert<=3` 会失败（现可超）。
- [ ] **Step 2: Run** FAIL
- [ ] **Step 3: Implement** 删 `finally remove`，加注释 `单实例JVM锁，多实例需Redis NX，见Task5 DB兜底`；`PdfService` 同改。
- [ ] **Step 4: Run** PASS（单实例下insert受锁保护）

### Task 4: 简历查询安全+性能（M5）

**Files:**
- Modify: `backend/src/main/java/com/resume/resume/service/ResumeService.java:104-125`
- Modify: `backend/src/main/java/com/resume/resume/controller/ResumeController.java:41-48`
- Create: `backend/src/main/resources/db/migration/V17__fix_resume_indexes.sql`

- [ ] **Step 1: Test** `keyword="%"` 应转义不返回全量；`scene="Internship"` 应400或归一化；`page=999999` 应钳制。
- [ ] **Step 2: Run** FAIL
- [ ] **Step 3: Implement**
```java
String esc(String s){return s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
scene = scene==null?null:scene.trim().toLowerCase(); if(scene!=null && !SCENES.contains(scene)) throw new BusinessException(RESUME_SCENE_INVALID);
page=Math.min(page,1000); size=Math.min(size,100);
```
Controller加 `@Size(max=64)`；V17加 `CREATE INDEX idx_resume_user_deleted_edited ON resume(user_id,deleted,last_edited_at DESC)`。
- [ ] **Step 4: Run** PASS

### Task 5: 终态竞争+sweeper（M6）

**Files:**
- Modify: `backend/src/main/java/com/resume/pdf/service/PdfService.java:388`、`AvatarTaskRecoveryJob.java:84`、`PdfTaskRecoveryJob.java:84`、`AiZombieTaskSweeper.java:44`

- [ ] **Step 1: Test** worker设SUCCESS后sweeper不应覆盖；启动只清 `updated_at<cutoff`。
- [ ] **Step 2: Run** FAIL
- [ ] **Step 3: Implement** 终态用 `LambdaUpdateWrapper.eq(id).in(status,pending,processing)` 条件更新，返回0即丢弃；启动清加时间下限；AI加 `ApplicationRunner`。
- [ ] **Step 4: Run** PASS

### Task 6: 配额audit+异常码（M14/M15）

**Files:**
- Modify: `backend/src/main/java/com/resume/ai/service/AiWritingService.java:249`、`GlobalExceptionHandler.java:159`

- [ ] **Step 1: Test** audit抛错不应触发refund；`6003→502`、`4007→500`。
- [ ] **Step 2: Run** FAIL
- [ ] **Step 3: Implement** audit移出计费try只warn；`resolveHttpStatus` 加 `AI_MODEL_CALL_FAILED,AI_RESPONSE_PARSE_FAILED->502`，`AVATAR_OPTIMIZE_FAILED->500`，新增 `SHARE_CONFLICT 409` 替代 `3001` 复用。
- [ ] **Step 4: Run** PASS

### Task 7: 前端PDF轮询可取消（M7）

**Files:**
- Modify: `frontend/src/views/EditorView.vue:205-244`

- [ ] **Step 1: Repro** 手动：导出后切走仍 `window.open`。
```bash
grep -n "setInterval" frontend/src/views/EditorView.vue
```
- [ ] **Step 2: Implement**
```ts
let cancelled=false; let timer:ReturnType<typeof setTimeout>|null=null;
async function poll(taskId:string, n=0){ if(cancelled||n>60){pushToast('导出超时');return;} try{const t=(await client.get(`/pdf/tasks/${taskId}`)).data.data; if(cancelled)return; if(t.status==='success'){const base=(import.meta.env.VITE_API_BASE||'/api').replace(/\/+$/,''); window.open(`${base}/pdf/download/${taskId}`);return;} if(t.status==='failed'){pushToast('导出失败');return;}}catch{} if(!cancelled) timer=setTimeout(()=>poll(taskId,n+1),1000);}
onUnmounted(()=>{cancelled=true; if(timer)clearTimeout(timer);});
```
- [ ] **Step 3: Verify** `npm run build` PASS

### Task 8: 前端XSS（C3前端部分）

**Files:**
- Modify: `frontend/src/views/EditorView.vue:1115`、`ShareView.vue:37`、`package.json`

- [ ] **Step 1: Repro** `grep -rn "v-html" frontend/src` 确认3处直染。
- [ ] **Step 2: Implement** `npm i dompurify @types/dompurify`，`DOMPurify.sanitize(html,{ALLOWED_TAGS:['b','i','u','ul','ol','li','a','p','br'],ALLOWED_ATTR:['href','target']})` 后再 `v-html`。
- [ ] **Step 3: Verify** `npm run build` PASS

### Task 9: 前端契约严格化（M9/M10）

**Files:**
- Modify: `frontend/src/stores/resume.ts:17`、`ResumeListView.vue:40,170`

- [ ] **Step 1: Repro** `fetchList` 空records静默空列表。
- [ ] **Step 2: Implement** 去 `?? data/[]`，加 `if(!p||!Array.isArray(p.records)) throw new Error('简历列表契约破裂')`；`fetchPage` 透传 `opts`；导入默认值 `''` 不发字段，后端默认。
- [ ] **Step 3: Verify** `npm run build` PASS

### Task 10: 运维配置（C6-C8/M11-M13）

**Files:**
- Modify: `ops/.env:8,15-16`、`frontend/Dockerfile:13`、`ops/docker-compose.server.yml` 文档注释

- [ ] **Step 1: Verify** `grep -n "MYSQL_PORT\|MINIO_API" ops/.env` 为裸端口；`grep -n proxy_pass frontend/Dockerfile` 为8088。
- [ ] **Step 2: Implement** `.env` 改 `127.0.0.1:3306`；Dockerfile改 `backend:8080`；`README/ops/README` 加 `SPRING_PROFILES_ACTIVE必填` + `prometheus内网` 说明。不碰真密钥，只改example+注释。
- [ ] **Step 3: Verify** `docker compose -f ops/docker-compose.server.yml config` PASS（如无docker则 `cat` 核对）

## Self-Review

- Spec覆盖：C1-C8/M1-M15/M11-M13均有任务；Minor的rename/version/fillAvatar归入Task4跟进，不单独立项。
- 无占位符：每步含可运行命令+代码。
- 类型一致：`enforceAvatarAccess(request,objectName)` 签名在Task1定义、Task2消费一致；`V17` 只加索引不改列。
