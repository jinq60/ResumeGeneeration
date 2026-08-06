# 项目维护指南（新人快速上手）

> 版本：v1.0
> 日期：2026-07-31
> 作用：面向新接手本项目的开发者的代码阅读与维护指南。已有文档（PRD、API 规范、数据模型等）回答"系统应该是什么样"，本文档回答"代码怎么读、怎么改"。

---

## 1. 总体架构

### 1.1 一个比喻：后端是一家餐厅

| 角色 | 代码层 | 职责 |
|---|---|---|
| 顾客 | 前端（浏览器） | 发起请求、展示结果 |
| 门口安检 | `common/security/` 过滤器 | 登记、限流、查票（JWT） |
| 服务员 | `Controller` | 收请求、调业务、返回结果（不干活） |
| 厨师 | `Service` | 真正的业务逻辑 |
| 采购员 | `Mapper` | 读写数据库 |
| 仓库 | MySQL | 数据存储 |
| 侧库 | MinIO | 文件存储（头像/PDF/模板） |
| 上菜的盘子 | `R<T>` | 统一响应格式 |

请求永远按一个方向流动：

```
浏览器 → 过滤器链 → Controller → Service → Mapper → 数据库
                               ↓ (文件类)
                             MinIO
```

返回时原路返回，所有结果装进统一的盘子 `R<T>`：

```json
成功：{ "code": 200, "message": "ok", "data": { ...业务数据... } }
失败：{ "code": 2002, "message": "简历不存在", "data": null }
```

### 1.2 请求全链路（以登录为例）

```
POST /api/auth/login  {"account":"13812345678","password":"123456"}

① RequestLoggingFilter  登记 traceId，记录日志
② IdempotencyFilter     无 Idempotency-Key 头 → 直接放行
③ RateLimitFilter       计数，60 次/60秒/IP，超限返回 429
④ JwtAuthenticationFilter 无 token → 放行（登录接口是白名单）
⑤ AuthController        收请求 → 调 userService.login()
⑥ UserService           查用户 → 验密码 → 验状态 → 生成 JWT
⑦ UserMapper            selectOne(phone=...) 查数据库
⑧ 原路返回 → R<AuthResponse>（含 accessToken/refreshToken）
```

### 1.3 技术栈速查

| 层 | 技术 | 一句话说明 |
|---|---|---|
| 后端 | Spring Boot 3.2.5 + Java 17 | 单体应用，按包分模块 |
| 数据访问 | MyBatis-Plus 3.5.5 | 不用写 SQL，继承 `BaseMapper` 自带 CRUD |
| 数据库 | MySQL 8.0 + Flyway | 表结构由 `V1__init.sql` 迁移创建 |
| 认证 | Spring Security + JJWT | 无状态 JWT，access 1 小时 + refresh 7 天 |
| 文件 | MinIO | 数据库只存 URL |
| AI | 多厂商（OpenAI/通义千问） | 异步任务 + 占位降级 |
| PDF | Playwright | 用 HTML 渲染 PDF |

---

## 2. 模块地图

### 2.1 后端模块（`backend/src/main/java/com/resume/`）

```
ResumeApplication.java    ← 启动入口（就一个注解，没有配置）

com.resume/
├── common/     基础设施层：R、异常、错误码、安全链、限流、幂等、MinIO、HTML渲染
├── user/       用户与认证：注册/登录/游客/刷新令牌/管理端
├── resume/     简历核心：CRUD、复制、重命名、Section 校验、AI 点评
├── template/   模板：查询、后台模板管理
├── avatar/     头像：上传、一寸照优化任务
├── pdf/        PDF 导出任务、下载
└── ai/         AI：多厂商 LLM 抽象、异步优化/点评/头像任务
```

### 2.2 依赖关系（谁依赖谁）

```
common（最底层，不依赖任何业务模块）
 ├── user（依赖 common）
 ├── template（依赖 common）
 ├── resume ──→ template（校验模板是否存在）
 ├── avatar ──→ resume（把一寸照 URL 回填简历）
 ├── pdf ────→ resume、template（读简历和模板渲染）
 └── ai ────→ resume、avatar
```

**红线**：禁止循环依赖；`common` 不得依赖任何业务模块；跨模块只能调 Service，禁止直接调别人的 Mapper。

### 2.3 各模块核心文件速查

| 模块 | 核心类 | 作用 |
|---|---|---|
| common | `R`、`BusinessException`、`GlobalExceptionHandler`、`SecurityConfig`、`JwtAuthenticationFilter`、`RateLimitFilter`、`MinioStorageService`、`ResumeRenderService` | 见第 3 章 |
| user | `UserService`（注册/登录/游客/刷新）、`JwtTokenProvider`（造令牌）、`AuthController` | 认证全流程 |
| resume | `ResumeService`（CRUD 核心）、`ResumeSectionValidator`（Section 校验）、`ResumeController` | 简历生命周期 |
| template | `TemplateService`、`TemplateController` | 模板查询/管理 |
| avatar | `AvatarService`、`AvatarController` | 头像上传/优化 |
| pdf | `PdfService`、`PdfController` | PDF 导出/下载 |
| ai | `ProviderRouter`（厂商路由）、`AiResumeReviewService`、`AiResumeOptimizeService`、`AiAvatarService` | AI 异步任务 |

---

## 3. 包内约定（看到包名就知道里面是什么）

每个业务模块（user/resume/template/avatar/pdf/ai）都是同一种结构：

| 包 | 放什么 | 规矩 |
|---|---|---|
| `controller/` | REST 接口 | 只收参数、调 Service、返回 `R<T>`；**禁止直接调 Mapper** |
| `service/` | 业务逻辑 | 可以调本模块 Mapper；跨模块只能调别的 Service；多表更新加 `@Transactional` |
| `mapper/` | 数据库访问 | 全部继承 `BaseMapper<T>`，**不写 SQL** |
| `entity/` | 数据库实体 | 和表一一对应，用 `@TableName` 标注表名 |
| `dto/` | 请求/响应对象 | 和 Entity 分开；**敏感字段（如密码）永远不出现** |
| `config/` | 配置类 | `@Configuration` + `@ConfigurationProperties` |
| `security/` | 安全组件 | 过滤器、token 工具（只有 common/user 有） |

`common` 模块的包结构：

| 包 | 放什么 |
|---|---|
| `common/config/` | 全局配置（安全链、MinIO、MyBatis-Plus、Swagger、调度） |
| `common/constant/` | `BizConstant`（业务枚举值）+ `ResultCode`（错误码） |
| `common/entity/` | `R`（统一响应）+ `IdempotencyRecord` |
| `common/enums/` | 7 个枚举（Section 类型、技能分类等，带 code + 中文名） |
| `common/exception/` | `BusinessException` |
| `common/handler/` | `GlobalExceptionHandler` + JSON TypeHandler |
| `common/security/` | 过滤器链（限流、幂等、JWT、Trace） |
| `common/service/` | `MinioStorageService`、`ResumeRenderService`、限流器 |
| `common/controller/` | `StaticResourceController`（/uploads/** 代理到 MinIO） |

---

## 4. 代码阅读词典

遇到看不懂的关键词，先查这张表。

### 4.1 Spring 注解

| 关键词 | 含义 |
|---|---|
| `@SpringBootApplication` | 应用启动开关 |
| `@RestController` | 本类是接口层（服务员） |
| `@RequestMapping("/auth")` | 本类接口统一挂在 `/auth` 前缀下 |
| `@PostMapping("/login")` | 本方法处理 `POST /auth/login` |
| `@RequestBody` | 请求体 JSON 自动转成 Java 对象 |
| `@PathVariable` | 从 URL 路径取值（`/resumes/{id}` 里的 `id`） |
| `@RequestParam` | 从 URL 问号后取值（`?page=1`） |
| `@Valid` | 按 DTO 字段上的校验注解检查参数 |
| `@AuthenticationPrincipal String userId` | 取出当前登录用户 ID（过滤器写入的） |
| `@Service` | 本类是业务层（厨师） |
| `@Mapper` | 本接口是数据访问层（采购员） |
| `@Component` | 通用零件：交给 Spring 管理，供别人注入 |
| `@Configuration` | 本类生产配置/零件 |
| `@Bean` | 方法返回一个零件对象，交给 Spring 仓库 |
| `@Value("${app.jwt.secret:}")` | 从配置读值，`${}` 是配置项，冒号后是默认值 |
| `@ConfigurationProperties(prefix="app.minio")` | 批量绑定 `app.minio.*` 配置到类字段 |
| `@PostConstruct` | 启动完成后自动执行（初始化/校验） |
| `@Autowired` | 自动注入依赖 |
| `@RequiredArgsConstructor` | 自动生成构造函数（final 字段变构造参数）——**最常见，到处都有** |
| `@Transactional(rollbackFor=Exception.class)` | 事务：任一失败全部回滚 |
| `@Lazy` | 延迟创建依赖，用于解开循环依赖 |
| `@Scheduled` | 定时任务 |
| `@ExceptionHandler` / `@RestControllerAdvice` | 全局异常处理 |
| `@Profile("!test")` | 只在特定环境生效 |
| `@ConditionalOnProperty` | 满足配置条件才创建（如切换限流实现） |
| `@Order` | 过滤器执行顺序（越小越靠前） |

### 4.2 MyBatis-Plus

| 关键词 | 含义 |
|---|---|
| `@TableName("resume")` | 类对应数据库表 |
| `@TableId(type=IdType.ASSIGN_ID)` | 主键，雪花 ID 自动生成 |
| `@TableField(typeHandler=...)` | JSON 列，存取自动序列化/反序列化 |
| `@TableLogic` | 逻辑删除字段（删 = 置 1，不是真删） |
| `extends BaseMapper<User>` | 自带 selectById/insert/updateById/deleteById/selectPage/selectCount 等，**不用写 SQL** |
| `LambdaQueryWrapper` | 拼查询条件：`eq`=WHERE，`like`=LIKE，`orderByDesc`=排序，`.last("LIMIT 1")`=追加 SQL 片段 |
| `Page<T>` | 分页结果：records/total/current/size |
| `User::getPhone` | 方法引用，表示"User 的 getPhone 方法"，MyBatis-Plus 用它做类型安全的列名 |

### 4.3 Lombok

| 关键词 | 含义 |
|---|---|
| `@Data` | 自动生成 getter/setter/toString |
| `@Getter` / `@Setter` | 只生成读/写方法 |
| `@Slf4j` | 直接用 `log.info(...)` 打日志 |
| `@RequiredArgsConstructor` | 见 4.1 |

### 4.4 参数校验注解（配合 `@Valid`）

| 关键词 | 含义 |
|---|---|
| `@NotBlank` | 非空（空格也不行） |
| `@Size(min, max)` | 长度范围 |
| `@Pattern(regexp)` | 正则匹配 |
| `@Min` / `@Max` | 数值范围 |
| `@Validated` | 类级校验开关（让路径参数上的 `@Min` 生效） |

### 4.5 JWT / 安全

| 关键词 | 含义 |
|---|---|
| `Jwts.builder()...compact()` | 生成 JWT 令牌 |
| `.subject(userId)` | 令牌里装用户 ID |
| `.claim("type", "access")` | 塞自定义信息 |
| `.signWith(key)` | 密钥签名（防伪造） |
| `Jwts.parser()...parseSignedClaims(token)` | 验签并读取令牌内容 |
| `SecurityContextHolder` | Spring Security 的"当前登录人档案袋" |

### 4.6 Jackson（JSON）

| 关键词 | 含义 |
|---|---|
| `ObjectMapper` | JSON 与 Java 对象互转 |
| `TypeReference<...>` | 声明反序列化的复杂目标类型 |

### 4.7 项目自己的约定

| 关键词 | 含义 |
|---|---|
| `R.success(x)` / `R.error(code, msg)` | 装盘 / 报错 |
| `throw new BusinessException(code, msg)` | 抛业务错误（由全局异常处理器统一翻译） |
| `ResultCode.XXX` | 错误码（1000 认证 / 2000 简历 / 3000 模板 / 4000 头像 / 5000 PDF / 6000 AI） |
| `BizConstant.XXX` | 业务枚举值（状态、场景、类型……） |
| `maskPhone` / `maskEmail` | 脱敏工具（`138****1234`） |
| `@AuthenticationPrincipal String userId` | 当前用户 ID（见 4.1） |

### 4.8 遇到不认识的关键词时

1. 按住 **Ctrl 点击** 它 → 看定义和注释。
2. **只看签名**：`userMapper.selectById(userId)` 不需要知道内部实现，知道"传 id 返回用户，查不到返回 null"就够了。
3. **盯住返回类型**：`R<AuthResponse>` 返回的一定是装好的盘子。

---

## 5. 端到端代码走读（两个示例）

### 5.1 示例一：登录（user 模块，最典型的请求）

**① 过滤器**（`common/security/`）不重复展开，见 1.2。

**② `AuthController`（user/controller/）**——服务员：

```java
@PostMapping("/login")
public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return R.success(userService.login(request));   // 递菜单给厨师，等结果
}
```

**③ `UserService.login()`（user/service/）**——厨师，5 步：

```java
1. detectLoginType()：手机号正则 ^1[3-9]\d{9}$ 判断是手机还是邮箱
2. findByPhone/findByEmail()：调 userMapper 查用户          → 找不到抛 1004
3. passwordEncoder.matches()：BCrypt 比对密码              → 不对抛 1005
4. status == disabled？                                      → 锁定抛 1006
5. buildAuthResponse()：造 access+refresh 双令牌，refresh 哈希落库
```

**④ 返回**：`R<AuthResponse>` 装好令牌返回前端。

**失败时发生了什么**：厨师抛 `BusinessException(1005, "密码不正确")` → 异常传到 `GlobalExceptionHandler` → `resolveHttpStatus(1005)` → 1000-1999 区间默认 401 → 响应 `{code:1005, message:"密码不正确"}`。

### 5.2 示例二：创建简历（resume 模块）

**① `ResumeController.create()`**：

```java
@PostMapping
public R<ResumeDetailResponse> create(@AuthenticationPrincipal String userId,
                                      @Valid @RequestBody CreateResumeRequest request) {
    return R.success(resumeService.createResume(userId, request));
}
```

**② `ResumeService.createResume()`**：

```java
1. validateScene()：场景必须是 BizConstant.SCENES 之一          → 否则抛 2000
2. templateService.getTemplateEntity()：模板必须存在            → 否则抛 2001
3. 标题为空 → 自动生成"我的简历 N"（数一下该用户已有几份）
4. buildDefaultSections()：生成 6 个默认 Section（个人信息/教育/项目/工作/技能/自我介绍）
5. resumeMapper.insert(resume) 入库
```

**③ 数据落库时的关键点**：`Resume.sections` 是 `List<SectionDTO>`（Java 对象），入库时 `SectionListTypeHandler` 自动把它序列化成 JSON 字符串存进 `sections` 列；读出来时再反序列化回强类型。**约定：禁止手动拼接/解析这个 JSON。**

---

## 6. 如何新增一个功能（标准配方）

以"给简历加一个收藏功能（POST /resumes/{id}/favorite）"为例：

### 第 1 步：确定错误码（如需）
在 `common/constant/ResultCode.java` 简历段（2000-2099）加常量；同步更新 `docs/superpowers/specs/2026-07-03-validation-rules.md` §13 和 `docs/api-changelog.md`。

### 第 2 步：DTO
在 `resume/dto/` 新建请求/响应类（如 `FavoriteResumeRequest`、`FavoriteResumeResponse`），字段加校验注解。

### 第 3 步：Service 方法
在 `ResumeService` 加方法，套路是：
```java
1. getResumeEntity(userId, resumeId)   // 自带归属校验，别自己写查询
2. 业务逻辑（改字段）
3. resumeMapper.updateById(...)
4. 返回 DTO
```

### 第 4 步：Controller 端点
```java
@PostMapping("/{id}/favorite")
public R<FavoriteResumeResponse> favorite(@AuthenticationPrincipal String userId,
                                          @PathVariable String id,
                                          @Valid @RequestBody FavoriteResumeRequest request) {
    return R.success(resumeService.favorite(userId, id, request));
}
```

### 第 5 步：数据库（如需新表）
在 `backend/src/main/resources/db/migration/` 新建 `V2__xxx.sql`（Flyway 自动执行，**不要改已执行的 V1**）；实体必须有 `deleted` 字段（逻辑删除）。

### 第 6 步：测试（项目要求 TDD）
在 `backend/src/test/java/com/resume/...` 写测试，覆盖成功 + 失败（越权 403、不存在 404）路径。

### 第 7 步：验证
```bash
cd backend
mvn clean test          # 后端测试
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # 本地启动
```

---

## 7. 维护红线与常见坑

### 7.1 红线（违反即返工）

- 禁止物理删除业务数据（只能 `deleted=1` 逻辑删除）。
- 禁止绕过 `R<T>` 自定义返回结构。
- 禁止 Controller 直接调 Mapper；禁止跨模块直接调别人的 Mapper。
- 禁止手动拼接/解析 `resume.sections` JSON。
- 禁止在代码里硬编码密钥、密码、Bucket 名（用配置 `app.*`）。
- 禁止把文件以 BLOB 存数据库。
- 禁止返回 `passwordHash` 等敏感字段。

### 7.2 常见坑

| 坑 | 说明 |
|---|---|
| 循环依赖 | 新注入如果产生循环，用 `@Lazy`（参考 `ResumeService` 注入 `PdfService`/`AvatarService` 的写法） |
| 逻辑删除漏条件 | 手写 `selectById` 会绕过逻辑删除吗？——**不会**，`@TableLogic` 全局生效；但自建 SQL 时要手动加 `deleted=0` |
| JSON 字段乱改 | `sections`、`dimensionScores`、`suggestions` 都是 JSON 列，必须走 TypeHandler，改类型时同步改 handler |
| 越权 | 查询类接口必须走 `getResumeEntity(userId, id)` 这类归属校验，否则 403 |
| 修改已执行的 Flyway 脚本 | 会破坏已有环境，只能新增 V2/V3... |
| 过滤器顺序 | 新过滤器注意 `@Order`，放错位置会导致未认证就限流或日志缺失 |
| dev 环境无 MinIO | 能启动但上传会失败（设计如此：软失败），别误以为配置丢了 |

---

## 8. 常见问题排查

| 现象 | 可能原因 | 排查路径 |
|---|---|---|
| 401 `请先登录后再访问` | 没带 token / token 过期 / 用了 refresh token | 检查 `Authorization: Bearer xxx`；access 过期换新 |
| 403 无权访问 | 访问他人资源 / 非 ADMIN 调 /admin/** | 查接口归属校验、用户角色 |
| 404 简历不存在 | 简历被删（逻辑删除）或 id 错 | 查 `resume.deleted` 字段 |
| 429 请求过于频繁 | 限流触发（60 次/60s/IP） | 稍后再试；检查是否有循环请求 |
| 密码不正确但账号确实存在 | 注册时密码策略（8-32 位含字母数字）不符？ | 用 `bcrypt` 校验工具核对哈希 |
| 上传文件失败 | MinIO 未启动 / bucket 未建 | 查 MinIO 日志；`BucketInitializer` 启动时会自动建 bucket |
| AI 点评返回占位数据 | API Key 未配置（`OPENAI_API_KEY`/`DASHSCOPE_API_KEY`） | 查 `app.ai.*` 配置；占位是设计好的降级 |
| 启动失败：JWT secret | 非 dev/test 环境要求配置 `app.jwt.secret`（Base64 ≥256bit） | 参考 `docs/environment.md` |
| 页面样式变了但代码没改 | 模板 HTML 骨架（`templates/resume/*.html`）或 `ResumeRenderService` 是共享渲染 | 预览和 PDF 用同一渲染器，改一处影响两者 |

### 8.1 排查工具

- 日志：每个请求带 `X-Trace-Id`，用它在日志里串联全链路。
- Swagger：`http://localhost:8080/api/swagger-ui.html`（带 token 调试）。
- 数据库：直连 MySQL 查 `resume_generation` 库。
- 测试：`mvn test -Dtest=com.resume.xxx.**` 只跑某个模块的测试。

### 8.2 备份与恢复

备份脚本：`ops/backup.sh`（MySQL mysqldump + MinIO 对象镜像，保留最近 7 份）。

```bash
# 手动备份
/opt/resume-generation/app/ops/backup.sh /opt/resume-generation/backups

# 每日自动备份（crontab -e）
0 2 * * * /opt/resume-generation/app/ops/backup.sh >> /var/log/resume-backup.log 2>&1
```

恢复步骤（以灾难恢复为例）：

```bash
# 1) 恢复 MySQL
docker exec -i resume-mysql mysql -u resume -p'<密码>' resume_generation < backups/mysql/resume_generation_20260805_020000.sql

# 2) 恢复 MinIO（mc 镜像回滚）
mc mirror --overwrite backups/minio/20260805_020000/ resume-local

# 3) 重启后端让 Flyway 状态与数据一致（如备份早于当前迁移版本，需评估迁移回退）
cd /opt/resume-generation/app/ops && docker compose -f docker-compose.server.yml up -d backend
```

> 注意：恢复前先确认 Flyway `flyway_schema_history` 与备份时点一致，避免版本错位。

### 8.3 发布与回滚

- 每次部署的镜像都带 Git 提交短 SHA 标签（`BACKEND_TAG` / `FRONTEND_TAG`），例如 `resume-backend:a1b2c3d4`。
- 回滚到上一版本：

```bash
cd /opt/resume-generation/app/ops
# 找到上一个部署使用的 SHA（GitHub Actions 部署日志可见，或 docker images 列出历史标签）
export BACKEND_TAG=<上一版本SHA>
export FRONTEND_TAG=<上一版本SHA>
docker compose -f docker-compose.server.yml up -d --no-build
```

- 紧急回滚到已验证的 stable 快照：`git checkout stable && git push origin production`（触发 CI 重新部署），或将 stable 构建产物打成对应标签再启动。
- 数据库迁移（Flyway）不可自动回退：若新版本包含迁移且需要回滚，按 `8.2` 从备份恢复，或人工评审迁移的向下兼容性。

---

## 9. 常用命令

```bash
# 后端
cd backend
mvn clean test                                   # 全部测试
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # 本地启动（默认端口 8080，前缀 /api）
mvn test -Dtest=com.resume.user.**               # 只测 user 模块

# 前端
cd frontend
npm install
npm run dev                                      # 开发服务器（5173）
npm run test:unit                                # 单元测试
npm run build                                    # 生产构建

# 数据库
# Flyway 自动迁移，无需手动建表；本地先建库：
CREATE DATABASE resume_generation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 10. 相关文档索引

| 需要了解 | 看哪里 |
|---|---|
| 产品需求 | `docs/需求PRD-v1.md` |
| 系统设计 | `docs/superpowers/specs/2026-07-03-resume-generation-design.md` |
| API 规范 | `docs/superpowers/specs/2026-07-03-api-spec.md` |
| 数据模型/DDL | `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` |
| 校验规则/错误码 | `docs/superpowers/specs/2026-07-03-validation-rules.md` |
| 模板系统 | `docs/superpowers/specs/2026-07-03-template-system-spec.md` |
| 环境搭建 | `docs/setup-guide.md` |
| 环境变量 | `docs/environment.md` |
| 安全合规 | `docs/security-guide.md` |
| 开发流程 | `docs/development-workflow.md` |
| AI 辅助约束 | 根目录 `AGENTS.md` + `backend/CLAUDE.md` + 各模块 `CLAUDE.md` |
