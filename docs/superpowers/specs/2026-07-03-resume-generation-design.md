# 智能简历生成工具 - 系统设计文档

> 版本：v1.0  
> 日期：2026-07-03  
> 基于需求文档：[需求PRD-v1.md](../../需求PRD-v1.md)

---

## 1. 设计目标与范围

### 1.1 目标

将简历制作从“手动排版”转变为“结构化填写 + 实时预览 + 一键导出”。本轮实现聚焦 MVP P0，确保编辑器、预览、模板、PDF 导出、头像上传、自动保存、用户登录/游客模式稳定可用。

### 1.2 范围边界

**包含（P0）：**
- 用户登录 / 游客模式 / JWT 鉴权
- 简历新建、列表、重命名、复制、删除
- 简历编辑器（左侧表单、右侧实时预览）
- 个人信息、教育经历、项目经历、工作经历、技能、自我介绍等模块
- 模块增删改与排序
- 模板切换（至少 4 套基础模板）
- 头像上传、裁剪、原图应用
- PDF 导出（A4、中文字体、高清头像）
- 自动保存与导出前检查

**包含（P1）：**
- AI 简历点评（`resume_review`），保存最近一次点评结果与分项分数

**不包含（P2/二期）：**
- AI 文本润色、项目经历优化、岗位匹配、简历评分
- 高级头像 AI 优化真实模型接入（接口预留，MVP 用占位实现）
- Word 导出、多语言、在线分享
- 批量生成、机构版本

---

## 2. 总体架构

采用前后端分离架构：

- **前端**：Vue 3 SPA，负责表单编辑、实时预览、路由、状态管理、文件上传。
- **后端**：Spring Boot 单体应用，负责业务逻辑、数据持久化、PDF 生成、头像处理、用户认证。
- **存储**：MySQL 持久化业务数据；MinIO（兼容 S3 协议的对象存储）存放头像原图、优化图、PDF 文件与模板缩略图，数据库只存可访问 URL。

```
┌─────────────┐      HTTP/REST       ┌─────────────────────────────┐
│  Vue 3 SPA  │ ◄──────────────────► │     Spring Boot Backend     │
│             │                      │  user / resume / template   │
│  - Editor   │                      │  / avatar / pdf / common    │
│  - Preview  │                      │                             │
│  - Cropper  │                      │  - MySQL (business data)    │
│  - PDF view │                      │  - MinIO (avatars / PDFs)   │
└─────────────┘                      └─────────────────────────────┘
```

---

## 3. 技术栈

### 3.1 后端

| 层级 | 选型 | 说明 |
|---|---|---|
| 语言 | Java 17 | LTS 版本 |
| 框架 | Spring Boot 3.x | 主框架 |
| 数据库 | MySQL 8 | 关系型数据 |
| ORM | MyBatis-Plus | 快速 CRUD |
| 安全 | Spring Security + JWT | 认证鉴权 |
| PDF 生成 | Playwright（无头 Chromium） | 渲染 HTML 后导出 PDF，保证与预览一致 |
| 文件存储 | MinIO（兼容 S3） | 头像、PDF、模板缩略图统一对象存储，数据库只存 URL |
| 测试 | JUnit 5 + Mockito + Spring Boot Test + Testcontainers | 单元、集成测试 |

### 3.2 前端

| 层级 | 选型 | 说明 |
|---|---|---|
| 框架 | Vue 3 | Composition API |
| 构建 | Vite | 快速构建与 HMR |
| 组件库 | Element Plus | 企业级 UI |
| 状态管理 | Pinia | 全局状态 |
| 路由 | Vue Router | 单页路由 |
| HTTP | Axios | REST 请求 |
| 头像裁剪 | vue-cropper | 前端裁剪交互 |
| 测试 | Vitest + Vue Test Utils + Playwright | 单元、组件、E2E |

---

## 4. 后端模块划分

```
com.resume
├── ResumeApplication.java
├── common              # 统一响应、全局异常、工具类、常量、枚举
├── user                # 用户与认证
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   └── dto
├── resume              # 简历核心
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   └── dto
├── template            # 模板管理
│   ├── controller
│   ├── service
│   ├── mapper
│   └── entity
├── avatar              # 头像上传与优化任务
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── entity
│   └── dto
└── pdf                 # PDF 导出任务
    ├── controller
    ├── service
    ├── mapper
    ├── entity
    └── dto
```

### 模块职责

- **common**：统一返回结果 `R<T>`、全局异常处理 `GlobalExceptionHandler`、业务异常 `BusinessException`、常量枚举、校验工具。
- **user**：用户注册、登录（手机/邮箱/验证码）、游客模式、JWT 签发与刷新、用户信息查询。
- **resume**：简历 CRUD、模块数据管理、自动保存草稿、复制/重命名/删除。
- **template**：模板元数据、模板配置、模板列表查询。
- **avatar**：头像上传、裁剪元数据、AI 优化任务创建与查询、删除。
- **pdf**：PDF 导出任务、调用 Playwright 渲染、文件下载。

---

## 5. 前端目录结构

```
frontend/
├── src/
│   ├── api/                 # 按模块封装的 HTTP 请求
│   │   ├── auth.ts
│   │   ├── resume.ts
│   │   ├── template.ts
│   │   ├── avatar.ts
│   │   └── pdf.ts
│   ├── assets/              # 样式、字体、图片
│   ├── components/
│   │   ├── editor/          # 表单编辑组件
│   │   ├── preview/         # 简历预览与模板组件
│   │   └── common/          # 通用组件
│   ├── composables/         # 可复用逻辑
│   ├── router/              # Vue Router 配置
│   ├── stores/              # Pinia 状态
│   │   ├── user.ts
│   │   ├── resume.ts
│   │   └── ui.ts
│   ├── utils/               # 工具函数
│   ├── views/               # 页面级组件
│   │   ├── LoginView.vue
│   │   ├── ResumeListView.vue
│   │   └── EditorView.vue
│   ├── App.vue
│   └── main.ts
├── tests/
│   ├── unit/
│   ├── component/
│   └── e2e/
└── package.json
```

---

## 6. 数据模型设计

### 6.1 简历主表 `resume`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(64) PK | 简历 ID |
| user_id | VARCHAR(64) | 用户 ID，游客为 guest session id |
| title | VARCHAR(128) | 简历名称 |
| scene | VARCHAR(32) | 使用场景枚举 |
| target_position | VARCHAR(128) | 目标岗位 |
| target_industry | VARCHAR(128) | 目标行业（P1） |
| template_id | VARCHAR(64) | 当前模板 ID |
| sections | JSON | 模块数组 |
| status | VARCHAR(16) | active / deleted（业务状态） |
| deleted | TINYINT(1) | 逻辑删除：0 未删除，1 已删除 |
| export_count | INT | 导出次数（P1 统计） |
| last_edited_at | DATETIME | 最近编辑时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 6.2 Section 统一模块模型

所有简历内容模块使用统一结构，便于排序、显隐和扩展。

```json
{
  "id": "sec_001",
  "type": "education",
  "title": "教育经历",
  "order": 1,
  "visible": true,
  "data": { }
}
```

`type` 枚举（与 `BizConstant.SECTION_TYPES` 保持一致）：
- `profile`：个人信息
- `education`：教育经历
- `project`：项目经历
- `work`：工作经历
- `skill`：技能技术栈
- `introduction`：自我介绍
- `custom`：自定义模块

### 6.3 个人信息 `Profile`

```json
{
  "name": "张三",
  "gender": "male",
  "birthDate": "2000-01",
  "phone": "13800000000",
  "email": "zhangsan@example.com",
  "city": "西安",
  "targetPosition": "Java后端开发工程师",
  "expectedSalary": "15k-20k",
  "availability": "一周内到岗",
  "personalWebsite": "https://...",
  "github": "https://github.com/...",
  "portfolio": "https://...",
  "avatarUrl": "https://...",
  "showGender": true,
  "showAge": false,
  "showSalary": false,
  "showAvatar": true
}
```

### 6.4 教育经历 `Education`

```json
{
  "school": "某某大学",
  "degree": "本科",
  "major": "计算机科学与技术",
  "college": "信息工程学院",
  "startDate": "2022-09",
  "endDate": "2026-06",
  "gpa": "3.7/4.0",
  "rank": "前10%",
  "honors": ["国家奖学金"],
  "courses": ["数据结构", "操作系统"]
}
```

### 6.5 项目经历 `Project`

```json
{
  "name": "智能简历生成系统",
  "role": "前端开发",
  "type": "课程项目",
  "startDate": "2026-03",
  "endDate": "2026-06",
  "techStack": ["Vue", "Spring Boot", "MySQL"],
  "background": "解决学生简历排版难问题",
  "responsibility": "负责编辑器页面开发",
  "achievements": ["实现实时预览", "导出 PDF"],
  "link": "https://...",
  "github": "https://github.com/..."
}
```

### 6.6 工作经历 `WorkExperience`

```json
{
  "company": "某某科技有限公司",
  "department": "研发部",
  "position": "前端实习生",
  "type": "实习",
  "city": "上海",
  "startDate": "2025-07",
  "endDate": "2025-10",
  "description": ["参与后台管理系统页面开发", "配合后端完成接口联调"],
  "achievements": ["优化页面加载速度 30%"],
  "techStack": ["Vue", "Element Plus"]
}
```

### 6.7 技能 `Skill`

```json
{
  "category": "frontend",
  "items": [
    { "name": "Vue", "level": "proficient", "highlight": true },
    { "name": "JavaScript", "level": "familiar", "highlight": false }
  ]
}
```

`level` 统一使用英文枚举：`beginner`（入门）、`familiar`（熟悉）、`proficient`（熟练）、`expert`（精通）。

### 6.8 头像优化任务 `avatar_task`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(64) PK | 任务 ID |
| user_id | VARCHAR(64) | 用户 ID |
| resume_id | VARCHAR(64) | 简历 ID |
| source_image_url | VARCHAR(512) | 原图地址 |
| result_image_url | VARCHAR(512) | 优化图地址 |
| background_type | VARCHAR(16) | white / blue / red（transparent 为 P2 预留） |
| style | VARCHAR(32) | formal / natural / professional |
| options | JSON | 高清修复、保持特征等选项 |
| status | VARCHAR(16) | pending / processing / success / failed |
| deleted | TINYINT(1) | 逻辑删除：0 未删除，1 已删除 |
| error_msg | VARCHAR(512) | 失败原因 |
| completed_at | DATETIME | 完成时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 6.9 PDF 导出任务 `pdf_task`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(64) PK | 任务 ID |
| user_id | VARCHAR(64) | 用户 ID |
| resume_id | VARCHAR(64) | 简历 ID |
| template_id | VARCHAR(64) | 模板 ID |
| file_path | VARCHAR(512) | 生成文件路径（MinIO 对象名或相对路径） |
| file_name | VARCHAR(128) | 下载文件名 |
| file_size | BIGINT | 文件大小（字节） |
| status | VARCHAR(16) | pending / processing / success / failed |
| deleted | TINYINT(1) | 逻辑删除：0 未删除，1 已删除 |
| error_msg | VARCHAR(512) | 失败原因 |
| created_at | DATETIME | 创建时间 |
| completed_at | DATETIME | 完成时间 |
| updated_at | DATETIME | 更新时间 |

### 6.10 AI 点评记录 `resume_review`

P1 功能：对用户简历进行 AI 点评并保存最近一次结果。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(64) PK | 点评记录 ID |
| resume_id | VARCHAR(64) | 关联简历 ID |
| user_id | VARCHAR(64) | 用户 ID |
| overall_score | INT | 综合评分 0–100 |
| dimension_scores | JSON | 分项评分 |
| suggestions | JSON | 修改建议列表 |
| highlights | JSON | 亮点总结列表 |
| job_description | TEXT | 目标岗位 JD（可选） |
| model_name | VARCHAR(64) | AI 模型名称 |
| model_version | VARCHAR(32) | 模型版本 |
| status | VARCHAR(16) | pending / success / failed |
| deleted | TINYINT(1) | 逻辑删除：0 未删除，1 已删除 |
| error_msg | VARCHAR(512) | 失败原因 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

## 7. 接口设计

### 7.1 认证相关

#### POST /api/auth/register

注册账号。

**请求体：**
```json
{
  "phone": "13800000000",
  "email": "zhangsan@example.com",
  "verifyCode": "123456",
  "password": "******"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "userId": "user_001",
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 3600,
    "isGuest": false
  }
}
```

#### POST /api/auth/login

登录。

**请求体：**
```json
{
  "account": "13800000000",
  "password": "******",
  "loginType": "phone"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "userId": "user_001",
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 3600,
    "isGuest": false
  }
}
```

#### POST /api/auth/guest

创建游客会话。

**响应：**
```json
{
  "code": 200,
  "data": {
    "userId": "guest_001",
    "accessToken": "...",
    "isGuest": true
  }
}
```

#### POST /api/auth/refresh

刷新 Token。

**请求体：**
```json
{
  "refreshToken": "..."
}
```

#### GET /api/users/me

获取当前用户信息。

**响应：**
```json
{
  "code": 200,
  "data": {
    "userId": "user_001",
    "nickname": "张三",
    "phone": "138****0000",
    "isGuest": false
  }
}
```

### 7.2 简历相关

#### POST /api/resumes

创建简历。

**请求体：**
```json
{
  "title": "我的简历",
  "scene": "campus_recruitment",
  "targetPosition": "Java后端开发工程师",
  "templateId": "template_001"
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": "resume_001",
    "title": "我的简历",
    "scene": "campus_recruitment",
    "templateId": "template_001",
    "createdAt": "2026-07-03T10:00:00",
    "updatedAt": "2026-07-03T10:00:00",
    "sections": []
  }
}
```

#### GET /api/resumes

获取当前用户简历列表。

**查询参数：**
- `page`：页码，默认 1
- `size`：每页大小，默认 20

**响应：**
```json
{
  "code": 200,
  "data": {
    "list": [ ... ],
    "total": 10
  }
}
```

#### GET /api/resumes/{id}

获取简历详情。

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": "resume_001",
    "title": "我的简历",
    "scene": "campus_recruitment",
    "targetPosition": "Java后端开发工程师",
    "templateId": "template_001",
    "sections": [ ... ],
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

#### PUT /api/resumes/{id}

更新简历，主要用于自动保存。

**请求体：**
```json
{
  "title": "我的Java后端开发简历",
  "templateId": "template_001",
  "sections": [ ... ]
}
```

#### DELETE /api/resumes/{id}

删除简历（逻辑删除）。

#### POST /api/resumes/{id}/duplicate

复制简历。

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": "resume_002",
    "title": "我的Java后端开发简历 副本"
  }
}
```

#### PUT /api/resumes/{id}/title

重命名简历。

**请求体：**
```json
{
  "title": "新名称"
}
```

### 7.3 模板相关

#### GET /api/templates

获取模板列表。

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "id": "template_001",
      "name": "经典单栏",
      "category": "classic",
      "thumbnail": "https://...",
      "config": { ... }
    }
  ]
}
```

#### GET /api/templates/{id}

获取模板详情。

### 7.4 头像相关

#### POST /api/avatars/upload

上传头像（multipart/form-data）。

**请求参数：**
- `file`：图片文件（JPG/PNG/WEBP，≤10MB）
- `resumeId`：可选，关联简历

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": "avatar_001",
    "sourceImageUrl": "/uploads/avatars/avatar_001_source.png",
    "fileName": "avatar.png"
  }
}
```

#### POST /api/avatars/optimize

创建头像优化任务。

**请求体：**
```json
{
  "sourceImageUrl": "/uploads/avatars/avatar_001_source.png",
  "backgroundType": "white",
  "style": "formal",
  "keepIdentity": true,
  "enhanceQuality": true,
  "removeBackground": true
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "taskId": "avatar_task_001",
    "status": "pending"
  }
}
```

#### GET /api/avatars/tasks/{taskId}

查询优化任务结果。

**响应：**
```json
{
  "code": 200,
  "data": {
    "taskId": "avatar_task_001",
    "status": "success",
    "resultImageUrl": "/uploads/avatars/avatar_task_001_result.png",
    "errorMsg": null
  }
}
```

#### DELETE /api/avatars/{id}

删除头像及相关任务。

### 7.5 PDF 相关

#### POST /api/pdf/export

创建 PDF 导出任务。

**请求体：**
```json
{
  "resumeId": "resume_001",
  "templateId": "template_001"
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "taskId": "pdf_task_001",
    "status": "pending"
  }
}
```

#### GET /api/pdf/tasks/{taskId}

查询 PDF 任务状态。

**响应：**
```json
{
  "code": 200,
  "data": {
    "taskId": "pdf_task_001",
    "status": "success",
    "fileName": "张三_Java后端开发_简历.pdf"
  }
}
```

#### GET /api/pdf/download/{taskId}

下载 PDF 文件。

---

## 8. 核心流程

### 8.1 编辑与实时预览

1. 用户进入编辑器，前端请求 `GET /api/resumes/{id}` 加载简历。
2. 表单变更通过 Pinia store 同步到右侧预览组件，预览即时更新。
3. 输入停止 2 秒后，前端调用 `PUT /api/resumes/{id}` 自动保存；游客模式优先写 `localStorage`。
4. 切换模板仅更新 `templateId`，不改动已填内容。
5. 用户点击预览区模块标题，左侧滚动到对应编辑区。

### 8.2 PDF 导出

1. 导出前前端检查：姓名、联系方式、空模块、头像加载状态。
2. 调用 `POST /api/pdf/export`。
3. 后端读取简历和模板配置，渲染完整 HTML。
4. 调用 Playwright 将 HTML 转为 A4 PDF。
5. 前端轮询 `GET /api/pdf/tasks/{taskId}`，成功后通过 `GET /api/pdf/download/{taskId}` 下载。

### 8.3 头像优化

1. 前端校验格式（JPG/PNG/WEBP）和大小（≤10MB）。
2. 上传到后端，返回原图地址。
3. 用户选择优化类型，前端调用 `POST /api/avatars/optimize`。
4. 后端创建任务，拼接提示词，调用占位/真实图片生成服务。
5. 前端轮询结果，确认后应用到简历。

---

## 9. 错误处理

### 9.1 后端统一响应

```json
{
  "code": 400,
  "message": "姓名为必填项",
  "data": null
}
```

### 9.2 全局异常处理

- `BusinessException`：业务异常，返回 200 + 业务错误码。
- `ValidationException`：参数校验失败，返回 400。
- `AccessDeniedException`：越权访问，返回 403。
- `Exception`：未知异常，返回 500。

### 9.3 关键异常场景

| 场景 | 提示文案 |
|---|---|
| 未填姓名 | “建议填写姓名，便于生成正式简历。” |
| 未填联系方式 | “简历中至少需要填写手机号或邮箱。” |
| URL 格式错误 | “请输入正确的网址格式。” |
| 内容过长 | “当前内容较长，可能导致简历超出一页。” |
| 图片格式不支持 | “请上传 JPG、PNG 或 WEBP 格式图片。” |
| 图片过大 | “图片大小不能超过 10MB。” |
| PDF 生成失败 | “PDF 生成失败，请稍后重试或检查网络连接。” |
| 头像优化失败 | “头像优化失败，请稍后重试或使用原图。” |

---

## 10. 安全与隐私

1. **鉴权**：JWT Access Token + Refresh Token，游客使用临时 Token。
2. **数据隔离**：所有简历/头像/PDF 查询必须带上 `user_id` 校验，防止越权。
3. **文件安全**：上传文件校验 MIME 类型和扩展名，重命名存储，禁止可执行文件。
4. **XSS 防护**：简历内容输出时进行 HTML 转义，PDF 渲染使用安全模板。
5. **隐私删除**：用户删除简历时同步清理关联头像和 PDF 文件。
6. **头像合规**：不将头像用于训练；优化强调“保持本人真实特征”。

---

## 11. 测试策略

采用 **SDD + TDD**：先确定设计文档与接口契约，再编写接口测试，最后实现代码。

### 11.1 后端测试

- **单元测试**：Service 层纯业务逻辑，使用 JUnit 5 + Mockito。
- **集成测试**：Controller + Service + Mapper + Testcontainers MySQL，覆盖接口端到端。
- **接口测试**：使用 JUnit 测试所有 REST API 的字段校验、鉴权、越权、状态流转。

### 11.2 前端测试

- **单元测试**：utils、composables、纯函数使用 Vitest。
- **组件测试**：表单模块组件、预览组件使用 Vue Test Utils。
- **E2E 测试**：核心链路使用 Playwright。

### 11.3 核心测试用例

1. 用户注册/登录/游客模式 Token 获取。
2. 创建简历默认标题为“我的简历 1”。
3. 个人信息校验：姓名为空时报错；手机和邮箱都为空时报错；邮箱格式错误时报错。
4. 教育经历按时间倒序排列。
5. 模块增删改排序后 section order 正确。
6. 切换模板不影响已填内容。
7. PDF 导出文件名：姓名_目标岗位_简历.pdf；未填姓名时使用“我的简历_导出日期.pdf”。
8. 头像上传超过 10MB 失败。
9. 头像上传非 JPG/PNG/WEBP 格式失败。
10. 越权访问他人简历返回 403。
11. 游客模式本地保存与登录后同步。
12. 删除简历后关联头像和 PDF 被清理。

---

## 12. 非功能需求

1. **性能**：表单输入后预览更新延迟 ≤ 300ms；自动保存响应 ≤ 2s；PDF 生成 ≤ 5s。
2. **兼容性**：桌面端支持 Chrome、Edge、Safari、Firefox；移动端基础适配。
3. **可用性**：必填项明确标注；关键操作二次确认；导出失败不丢失数据。
4. **可扩展性**：模板、模块、头像优化服务均通过接口和配置可替换。

---

## 13. 后续演进

- **P1**：模板市场、模块拖拽排序、AI 简历点评（`resume_review`）、头像白底/蓝底/红底真实优化、导出前检查增强。
- **P2**：AI 文本润色、项目经历优化、简历评分、岗位匹配、多语言、Word 导出、在线分享。
