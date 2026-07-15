# ai 模块 — Claude 约束

> 作用：多厂商 LLM 集成、异步任务调度、AI 调用审计。
> 范围：`backend/src/main/java/com/resume/ai/`。
> 必读：`../CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

`ai` 模块负责：

- 多 LLM 厂商统一抽象与路由
- Prompt 模板管理
- 异步 AI 任务执行（简历点评、JD 优化、头像优化）
- AI 调用日志与成本审计

---

## 2. 包目录结构

```
com.resume.ai/
├── config/
│   ├── AiProperties.java          # 多厂商配置绑定 (app.ai.*)
│   ├── AsyncAiConfig.java         # AI 专用线程池
│   └── AiPromptTemplates.java     # Prompt 模板管理
├── controller/
│   └── AiResumeController.java    # POST /resumes/{id}/optimize 等
├── dto/
│   ├── AiChatRequest.java         # LLM 统一请求
│   ├── AiChatResponse.java        # LLM 统一响应
│   ├── ResumeOptimizeRequest.java # JD 优化请求
│   └── ResumeOptimizeResponse.java # JD 优化响应
├── entity/
│   ├── AiCallLog.java             # AI 调用日志
│   └── ResumeOptimizeTask.java    # JD 优化任务
├── mapper/
│   ├── AiCallLogMapper.java
│   └── ResumeOptimizeTaskMapper.java
├── provider/
│   ├── LlmProvider.java           # 文本 LLM 统一接口
│   ├── ProviderRouter.java        # 功能→厂商→模型路由
│   ├── openai/OpenAiLlmProvider.java
│   └── qwen/QwenLlmProvider.java
└── service/
    ├── AiResumeReviewService.java    # 异步简历点评
    ├── AiResumeOptimizeService.java  # 异步 JD 优化
    └── AiAvatarService.java          # 异步头像优化（P1）
```

---

## 3. 多厂商路由

通过 `application.yml` 配置实现功能 → 厂商 → 模型映射：

```yaml
app.ai.providers:
  resume-review:    { provider: qwen,   model: qwen-turbo }
  resume-optimize:  { provider: openai, model: gpt-4o-mini }
  avatar-optimize:  { provider: openai, model: gpt-4o }
```

- `ProviderRouter.resolve("resume-review")` 返回对应厂商的 `LlmProvider`。
- `ProviderRouter.resolveModel("resume-review")` 返回 `qwen-turbo`。
- 厂商 API Key 通过环境变量注入：`OPENAI_API_KEY`、`DASHSCOPE_API_KEY`、`ERNIE_API_KEY`。

### 3.1 LlmProvider 接口

```java
public interface LlmProvider {
    AiChatResponse chat(AiChatRequest request);
    <T> T chatStructured(AiChatRequest request, Class<T> responseType);
    boolean supportsModel(String modelName);
    String getProviderName();
}
```

- `chat()`: 发送对话请求，返回文本内容 + token 统计。
- `chatStructured()`: 发送对话请求并直接将响应解析为指定类型。
- 所有实现通过 `@Component` 注册，由 `ProviderRouter` 自动发现。

---

## 4. 异步任务架构

### 4.1 线程池

```yaml
app.ai.thread-pool:
  core-size: 4
  max-size: 8
  queue-capacity: 100
```

- 拒绝策略：`CallerRunsPolicy`（队列满时由调用线程同步执行）
- 关闭等待：30s

### 4.2 异步方法

所有 `@Async("aiTaskExecutor")` 方法遵循统一模式：

1. 从 DB 读取任务记录
2. 更新状态为 `processing`
3. 调用 `ProviderRouter` → `LlmProvider`
4. 成功时解析 JSON 结果写入任务记录，状态 → `success`
5. 失败时回退到占位数据（review/optimize）或标记 `failed`
6. 写入 `ai_call_log` 审计记录

### 4.3 用户级并发限制

- 每用户最多 3 个同时进行的 AI 任务
- `AiResumeController.createOptimize()` 在创建任务前通过 DB 查询进行中任务数
- 超出限制返回 `AI_CONCURRENT_LIMIT_EXCEEDED` (6004)

---

## 5. 降级策略

| 场景 | 策略 |
|---|---|
| 未配置 AI API Key | 回退到 P0 占位数据（点评返回固定评分） |
| LLM 调用超时 | WebClient 120s 超时，超时后标记 `failed` |
| LLM 返回非 JSON | 解析失败 → `AI_RESPONSE_PARSE_FAILED` |
| 网络错误 | 重试 2 次（间隔 1s），仍失败则回退占位 |
| 线程池满 | CallerRunsPolicy 降级同步执行 |

---

## 6. 审计日志

`ai_call_log` 表记录每次 LLM 调用：

| 字段 | 说明 |
|---|---|
| user_id | 用户 ID |
| feature_key | 功能标识 |
| provider_name | 厂商名 |
| model_name | 模型名 |
| request_hash | 请求去重哈希 |
| prompt_tokens / completion_tokens / total_tokens | Token 消耗 |
| latency_ms | 调用耗时 |
| success / error_msg | 结果 |

---

## 7. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant`、`MinioStorageService` |
| `resume` | `Resume`、`ResumeReview`、`ResumeReviewSuggestion` 实体与 Mapper |
| `avatar` | `AvatarTask` 实体与 Mapper |

---

## 8. 错误码

| 错误码 | 常量 | 含义 |
|---|---|---|
| 6000 | AI_TASK_NOT_FOUND | AI 任务不存在 |
| 6001 | AI_TASK_FAILED | AI 任务执行失败 |
| 6002 | AI_PROVIDER_NOT_CONFIGURED | 未配置 AI 厂商 |
| 6003 | AI_MODEL_CALL_FAILED | LLM 调用失败 |
| 6004 | AI_CONCURRENT_LIMIT_EXCEEDED | 超出用户并发限制 |
| 6005 | AI_RESPONSE_PARSE_FAILED | AI 响应解析失败 |
| 6006 | AI_CONTENT_TOO_LONG | 内容过长超出 token 限制 |

---

## 9. 新增 Provider 指南

1. 实现 `LlmProvider` 接口。
2. 添加 `@Component` 注解，指定唯一 bean 名。
3. `getProviderName()` 返回与 YAML 配置 `provider` 字段一致的值。
4. `supportsModel()` 根据模型名前缀判断。
5. 在 `application.yml` 添加对应厂商配置块（API Key、Base URL）。

---

## 10. 相关文档

- `../../../../docs/superpowers/specs/2026-07-03-api-spec.md`
- `../../../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- `../../../../docs/superpowers/specs/2026-07-03-validation-rules.md`
- `../../../../docs/superpowers/specs/2026-07-03-tdd-test-plan.md`
