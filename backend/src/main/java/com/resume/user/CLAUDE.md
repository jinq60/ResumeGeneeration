# user 模块 — Claude 约束

> 作用：用户账号生命周期与认证流程。
> 范围：`backend/src/main/java/com/resume/user/`。
> 必读：`../CLAUDE.md`（后端工程约束） + `../common/CLAUDE.md` + 本文件。

---

## 1. 模块职责

`user` 模块负责：

- 用户注册（手机/邮箱）
- 用户登录
- 游客会话创建
- Token 刷新
- 当前用户信息查询
- JWT Token 生成与解析

---

## 2. 包目录结构

```
com.resume.user/
├── controller/
│   ├── AuthController.java       # /auth/** 认证接口
│   └── UserController.java       # /users/me 当前用户信息
├── dto/
│   ├── AuthResponse.java         # 登录/注册/刷新响应
│   ├── LoginRequest.java         # 登录请求
│   ├── RefreshRequest.java       # 刷新 Token 请求
│   ├── RegisterRequest.java      # 注册请求
│   └── UserInfoResponse.java     # 用户信息响应
├── entity/
│   └── User.java                 # 用户实体
├── mapper/
│   └── UserMapper.java           # 用户数据访问
├── security/
│   └── JwtTokenProvider.java     # JWT 生成/解析/校验
└── service/
    └── UserService.java          # 用户业务逻辑
```

---

## 3. HTTP API 端点

Base URL：`http://localhost:8080/api`

### 3.1 认证接口 `/auth/**`（permitAll）

| 方法 | 路径 | 请求 DTO | 响应 DTO | 说明 |
|---|---|---|---|---|
| POST | `/auth/register` | `RegisterRequest` | `R<AuthResponse>` | 手机或邮箱注册；验证码 P0 为任意 6 位占位 |
| POST | `/auth/login` | `LoginRequest` | `R<AuthResponse>` | 手机/邮箱 + 密码登录 |
| POST | `/auth/guest` | 无 | `R<AuthResponse>` | 创建临时游客会话 |
| POST | `/auth/refresh` | `RefreshRequest` | `R<AuthResponse>` | 用 Refresh Token 换新的 Token 对 |

### 3.2 用户接口 `/users/**`（需认证）

| 方法 | 路径 | 请求参数 | 响应 DTO | 说明 |
|---|---|---|---|---|
| GET | `/users/me` | `@AuthenticationPrincipal String userId` | `R<UserInfoResponse>` | 获取当前登录用户信息 |

---

## 4. DTO 字段与约束

### 4.1 `RegisterRequest`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `phone` | String | 可选，符合中国大陆手机号格式 | 手机/邮箱至少填一个 |
| `email` | String | 可选，符合邮箱格式 | 手机/邮箱至少填一个 |
| `verifyCode` | String | 必填，6 位数字 | P0 占位：任意 6 位数字均通过 |
| `password` | String | 必填，8–32 位 | 需包含字母 + 数字 |

### 4.2 `LoginRequest`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `account` | String | 必填 | 手机号或邮箱 |
| `password` | String | 必填 | 密码 |
| `loginType` | String | 可选 | `phone` / `email`，不传则自动识别 |

### 4.3 `RefreshRequest`

| 字段 | 类型 | 约束 |
|---|---|---|
| `refreshToken` | String | 必填 |

### 4.4 `AuthResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | String | 用户 ID |
| `accessToken` | String | JWT Access Token |
| `refreshToken` | String | JWT Refresh Token |
| `expiresIn` | Long | Access Token 有效期（秒） |
| `isGuest` | Boolean | 是否游客 |

### 4.5 `UserInfoResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | String | 用户 ID |
| `nickname` | String | 昵称 |
| `phone` | String | 脱敏手机号 |
| `email` | String | 脱敏邮箱 |
| `avatarUrl` | String | 用户账号头像 URL |
| `isGuest` | Boolean | 是否游客 |

---

## 5. 关键实体 `User`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | String | Snowflake ID，主键 |
| `phone` | String | 手机号，唯一可空 |
| `email` | String | 邮箱，唯一可空 |
| `passwordHash` | String | BCrypt 哈希，不返回前端 |
| `nickname` | String | 昵称 |
| `avatarUrl` | String | 用户账号头像 URL |
| `isGuest` | Integer | `1` 游客 / `0` 正式用户 |
| `status` | String | `active` / `disabled` |
| `deleted` | Integer | 逻辑删除 |
| `createdAt` / `updatedAt` | LocalDateTime | 时间戳 |

---

## 6. JWT 约定

文件：`security/JwtTokenProvider.java`

### 6.1 Access Token Claims

- `sub`：userId
- `guest`：是否游客
- `iat`、`exp`

### 6.2 Refresh Token Claims

- `sub`：userId
- `iat`、`exp`

### 6.3 配置

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:...}      # Base64 编码，≥ 256 bit
    access-token-expiration: 3600000     # 1 小时
    refresh-token-expiration: 604800000  # 7 天
```

---

## 7. 依赖模块

| 依赖 | 用途 |
|---|---|
| `common` | `R`、`BusinessException`、`ResultCode`、`BizConstant`、SecurityConfig、JWT Filter |
| `common.security.JwtAuthenticationFilter` | 解析 Token 写入 SecurityContext |

---

## 8. 开发约束

### 8.1 注册

- 手机/邮箱至少填一个。
- 手机号、邮箱分别唯一（数据库唯一索引 + 业务层校验）。
- 密码使用 `BCryptPasswordEncoder` 哈希存储。
- 游客账号 `isGuest=1`，无密码。

### 8.2 登录

- 根据 `account` 自动识别手机号或邮箱。
- 密码错误返回 `AUTH_PASSWORD_INCORRECT`。
- 账号被禁用时返回 `AUTH_ACCOUNT_LOCKED`。

### 8.3 Token 刷新

- 校验 Refresh Token 有效性。
- 返回新的 Access Token + Refresh Token 对。

### 8.4 用户信息

- 返回前必须脱敏手机号和邮箱。
- 禁止返回 `passwordHash`。

### 8.5 事务

- 注册、创建游客需标注 `@Transactional(rollbackFor = Exception.class)`。

---

## 9. 错误码

| 错误码 | 常量 | 含义 |
|---|---|---|
| 1000 | `AUTH_PHONE_REGISTERED` | 手机号已注册 |
| 1001 | `AUTH_EMAIL_REGISTERED` | 邮箱已注册 |
| 1002 | `AUTH_VERIFY_CODE_INVALID` | 验证码错误 |
| 1003 | `AUTH_PASSWORD_TOO_WEAK` | 密码强度不足 |
| 1004 | `AUTH_ACCOUNT_NOT_FOUND` | 账号不存在 |
| 1005 | `AUTH_PASSWORD_INCORRECT` | 密码错误 |
| 1006 | `AUTH_ACCOUNT_LOCKED` | 账号被锁定 |
| 1007 | `AUTH_REFRESH_TOKEN_INVALID` | Refresh Token 无效 |

---

## 10. 测试要求

- 测试目录：`backend/src/test/java/com/resume/user/`
- 必须覆盖：
  - `JwtTokenProvider` 生成/解析/过期校验
  - `UserService` 注册、登录、游客创建、Token 刷新
  - 密码编码器行为
- 运行：`mvn test -Dtest=com.resume.user.**`

---

## 11. 相关文档

- `../../../docs/superpowers/specs/2026-07-03-api-spec.md` §5、§6
- `../../../docs/superpowers/specs/2026-07-03-data-model-and-ddl.md` §2.1
- `../../../docs/superpowers/specs/2026-07-03-validation-rules.md` §2、§11、§13
- `../../../docs/superpowers/specs/2026-07-03-tdd-test-plan.md` §2.1、§2.3
- `../../../docs/security-guide.md`
