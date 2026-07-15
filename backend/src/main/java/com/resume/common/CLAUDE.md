# common 模块 — Claude 约束

> 作用：全局基础设施与跨模块共享能力。
> 范围：`backend/src/main/java/com/resume/common/`。
> 必读：`../CLAUDE.md`（后端工程约束） + 本文件。

---

## 1. 模块职责

`common` 模块是后端最底层模块，被所有业务模块依赖。它提供：

- 统一 API 响应 `R<T>`
- 业务异常 `BusinessException` 与全局异常处理
- 业务常量 `BizConstant` 与错误码 `ResultCode`
- JWT 认证过滤器与未认证入口点
- Spring Security 配置
- MyBatis-Plus 全局配置
- MinIO 客户端配置与存储服务
- 简历 HTML 渲染服务（跨模块共享）

---

## 2. 包目录结构

```
com.resume.common/
├── config/
│   ├── MinioConfig.java          # MinIO 客户端配置
│   ├── MybatisPlusConfig.java    # 分页插件、全局配置
│   └── SecurityConfig.java       # Spring Security 无状态 JWT 配置
├── constant/
│   ├── BizConstant.java          # 业务枚举常量
│   └── ResultCode.java           # 错误码常量
├── entity/
│   └── R.java                    # 统一响应体
├── exception/
│   └── BusinessException.java    # 业务异常
├── handler/
│   └── GlobalExceptionHandler.java # 全局异常处理
├── security/
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAuthenticationFilter.java
└── service/
    ├── MinioStorageService.java  # MinIO 上传/下载/删除
    └── ResumeRenderService.java  # 简历 HTML 渲染
```

---

## 3. 核心约定

### 3.1 统一响应 `R<T>`

文件：`common/entity/R.java`

```java
@Data
public class R<T> implements Serializable {
    private Integer code;      // 200 成功；其他为业务错误码
    private String message;    // 提示信息
    private T data;            // 业务数据
}
```

- 成功：`R.success(data)` → `code=200, message="ok"`
- 失败：`R.error(code, message)` 或 `throw new BusinessException(code, message)`

### 3.2 业务异常

```java
throw new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在");
```

- `BusinessException` 必须带 `errorCode`。
- 由 `GlobalExceptionHandler` 统一转换为 `R`。

### 3.3 错误码 `ResultCode`

| 范围 | 模块 |
|---|---|
| 200/400/401/403/404/429/500 | 通用 HTTP 状态码 |
| 1000–1099 | 认证模块 |
| 2000–2099 | 简历模块 |
| 3000–3099 | 模板模块 |
| 4000–4099 | 头像模块 |
| 5000–5099 | PDF 模块 |
| 6000–6099 | AI 点评模块 |

新增错误码时：

1. 在 `ResultCode` 中按模块范围新增常量。
2. 在 `docs/superpowers/specs/2026-07-03-validation-rules.md` §13 补充说明。
3. 在 `docs/api-changelog.md` 记录变更。

### 3.4 JWT 认证

- Access Token：有效期 1 小时， claims 含 `sub=userId`、`guest=boolean`。
- Refresh Token：有效期 7 天， claims 含 `sub=userId`。
- 请求头：`Authorization: Bearer {accessToken}`
- 白名单（permitAll）：`/auth/**`、`/templates`、`/templates/**`、`/actuator/health`

### 3.5 逻辑删除

MyBatis-Plus 全局配置：

```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

- 所有业务表必须有 `deleted` 字段。
- 禁止物理删除，只能逻辑删除。

### 3.6 安全配置

- 无状态会话（`STATELESS`）。
- CORS 仅允许 `http://localhost:5173`。
- 密码编码器：`BCryptPasswordEncoder`。

---

## 4. 公共服务

### 4.1 `MinioStorageService`

| 方法 | 输入 | 输出 | 说明 |
|---|---|---|---|
| `upload(MultipartFile file, String bucket, String objectName)` | 文件、Bucket、对象名 | 可访问 URL | 上传文件，Bucket 不存在则自动创建 |
| `delete(String bucket, String objectName)` | Bucket、对象名 | `void` | 删除对象 |
| `getObjectUrl(String bucket, String objectName)` | Bucket、对象名 | URL | 获取对象访问 URL |

**约束**：

- 文件名使用 UUID 重命名，禁止保留原始文件名。
- 路径格式：`{userId}/{type}/{id}/{fileName}`
- 数据库只存 URL，不存二进制。

### 4.2 `ResumeRenderService`

| 方法 | 输入 | 输出 | 说明 |
|---|---|---|---|
| `render(Resume resume, Template template)` | 简历实体、模板实体 | HTML 字符串 | 将简历 + 模板渲染为完整 HTML |

**约束**：

- 渲染 HTML 用于前端预览和 PDF 导出，必须保持一致。
- 模板配置 `template.config` 决定颜色、字体、布局。
- HTML 模板文件名/内容来自 `template.htmlTemplate`。

---

## 5. 输入输出约定

### 5.1 Controller 层

- 所有 Controller 方法返回 `R<T>`。
- 当前用户 ID 通过 `@AuthenticationPrincipal String userId` 注入。
- 入参 DTO 使用 `@Valid` 触发校验。

### 5.2 Service 层

- 业务异常统一使用 `BusinessException`。
- 涉及多张表更新的操作标注 `@Transactional(rollbackFor = Exception.class)`。
- 敏感字段不得返回给前端。

### 5.3 Mapper 层

- 继承 `BaseMapper<T>`。
- 不手写 XML，复杂查询使用 MyBatis-Plus QueryWrapper/LambdaQueryWrapper。

---

## 6. 开发约束

- `common` 模块不得依赖 `user/resume/template/avatar/pdf` 等上层模块。
- `ResumeRenderService` 目前依赖 `resume` 和 `template` 实体，这是允许的渲染耦合。
- 新增常量优先放入 `BizConstant`；新增错误码必须放入 `ResultCode`。
- 所有配置类使用 `@Configuration` + `@ConfigurationProperties`（如适用）。
- 日志使用 Lombok `@Slf4j`。

---

## 7. 测试要求

- `common` 模块的测试位于 `backend/src/test/java/com/resume/common/`。
- 异常处理、JWT、MinIO、渲染服务都需要单元测试。
- 运行命令：`mvn test -Dtest=com.resume.common.**`

---

## 8. 相关文档

- `../../docs/superpowers/specs/2026-07-03-api-spec.md`
- `../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- `../../docs/superpowers/specs/2026-07-03-validation-rules.md`
- `../../docs/superpowers/specs/2026-07-03-template-system-spec.md`
- `../../docs/adr/ADR-001-minio-over-localfs.md`
- `../../docs/adr/ADR-002-json-sections.md`
- `../../docs/security-guide.md`
