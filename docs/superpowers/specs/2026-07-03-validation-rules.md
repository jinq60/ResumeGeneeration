# 简历生成工具字段校验规则

> 版本：v1.1  
> 日期：2026-07-07  
> 基于：`docs/superpowers/specs/2026-07-03-scope-alignment.md`、`docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`

---

## 1. 校验规则总览

### 1.1 校验分层

| 层级 | 触发时机 | 作用 |
|---|---|---|
| 前端即时校验 | 用户输入时 / 失焦时 | 提升体验，减少无效请求 |
| 前端提交校验 | 点击保存 / 导出前 | 兜底校验，确保数据完整 |
| 后端接口校验 | Controller / Service 层 | 安全兜底，防止绕过前端 |
| 数据库约束 | DDL 层 | 最终一致性保障 |

### 1.2 校验原则

1. **前后端规则必须一致**：同一字段的格式、长度、必填规则前后端统一。
2. **错误提示文案统一**：同一异常使用同一文案，便于用户理解和测试断言。
3. **必填项明确标注**：前端表单用 `*` 标识，后端校验失败返回明确字段名。
4. **跨字段校验**：如“手机与邮箱至少填一个”，前后端都要实现。

### 1.3 草稿保存 vs 导出/PDF 预检查

| 校验层级 | 触发时机 | 校验强度 | 说明 |
|---|---|---|---|
| **草稿自动保存** | 编辑器输入停止 2 秒后 | 结构校验 + 字段格式校验 | 允许 `data` 内字段不完整，但 `type`、`order`、`visible`、Section ID 必须合法；`Profile.name` 等必填项在草稿阶段仅做格式校验，不强制存在。 |
| **手动保存/提交** | 用户点击保存 | 结构校验 + 字段格式校验 + 跨字段规则 | 校验手机/邮箱至少填一个、URL 格式、数组长度等。 |
| **导出/PDF 预检查** | 导出前 | 最严格 | 必须存在 `Profile.name`、手机或邮箱至少一个；隐藏所有空 visible 模块；检查头像 URL 有效性。 |

**原则**：草稿保存不能因为字段不完整而失败；导出/PDF 预检查必须保证简历可生成正式 PDF。

---

## 2. 通用正则表达式

| 规则 | 正则表达式 | 说明 |
|---|---|---|
| 手机号（中国大陆） | `^1[3-9]\d{9}$` | 11 位数字，以 1 开头，第二位 3-9 |
| 邮箱 | `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | 标准邮箱格式 |
| URL | `^https?://([\w-]+\.)+[\w-]+(/[\w-./?%&=]*)?$` | 仅允许 http/https |
| 年月 `YYYY-MM` | `^(19|20)\d{2}-(0[1-9]|1[0-2])$` | 1900–2099 年，01–12 月 |
| 中文姓名 | `^[一-龥·]{1,50}$` | 支持中文和间隔号 |
| 通用名称 | `^.{1,100}$` | 非空，最长 100 字符 |
| 非空文本 | `^\s*\S+[\s\S]*$` | 去除纯空格 |

---

## 3. 个人信息 Profile 校验

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `name` | 是 | 1–50 字符，支持中文/英文/间隔号 | `@NotBlank @Size(max=50)` | “姓名为必填项” |
| `gender` | 否 | `male` / `female` / `other` | `@Pattern` | “性别格式不正确” |
| `birthDate` | 否 | `YYYY-MM` | `@Pattern` | “出生年月格式不正确” |
| `phone` | 条件 | 11 位手机号 | `@Pattern` | “手机号格式不正确” |
| `email` | 条件 | 标准邮箱 | `@Email` | “邮箱格式不正确” |
| `city` | 否 | ≤50 字符 | `@Size(max=50)` | “所在城市过长” |
| `targetPosition` | 否 | ≤128 字符 | `@Size(max=128)` | “目标岗位过长” |
| `expectedSalary` | 否 | ≤64 字符 | `@Size(max=64)` | “期望薪资过长” |
| `availability` | 否 | ≤64 字符 | `@Size(max=64)` | “到岗时间描述过长” |
| `personalWebsite` | 否 | URL，≤512 字符 | `@Pattern @Size` | “个人网站格式不正确” |
| `github` | 否 | URL，≤512 字符 | `@Pattern @Size` | “GitHub 链接格式不正确” |
| `portfolio` | 否 | URL，≤512 字符 | `@Pattern @Size` | “作品集链接格式不正确” |
| `avatarUrl` | 否 | URL，≤512 字符 | `@Pattern @Size` | “头像地址格式不正确” |
| `showGender` | 否 | boolean | 类型校验 | — |
| `showAge` | 否 | boolean | 类型校验 | — |
| `showSalary` | 否 | boolean | 类型校验 | — |
| `showAvatar` | 否 | boolean | 类型校验 | — |

### 3.1 跨字段规则：手机与邮箱至少填一个

```java
// 后端校验逻辑示例
if (StringUtils.isBlank(profile.getPhone()) && StringUtils.isBlank(profile.getEmail())) {
    throw new BusinessException("RESUME_PROFILE_CONTACT_REQUIRED", "简历中至少需要填写手机号或邮箱。");
}
```

---

## 4. 教育经历 Education 校验

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `school` | 是 | 1–100 字符 | `@NotBlank @Size(max=100)` | “学校名称为必填项” |
| `degree` | 是 | ≤32 字符 | `@NotBlank @Size(max=32)` | “学历为必填项” |
| `major` | 是 | 1–100 字符 | `@NotBlank @Size(max=100)` | “专业为必填项” |
| `college` | 否 | ≤100 字符 | `@Size(max=100)` | “学院名称过长” |
| `startDate` | 是 | `YYYY-MM` | `@NotBlank @Pattern` | “入学时间格式不正确” |
| `endDate` | 否 | `YYYY-MM` 或 `present` | `@Pattern` | “毕业时间格式不正确” |
| `gpa` | 否 | ≤32 字符 | `@Size(max=32)` | “GPA 描述过长” |
| `rank` | 否 | ≤64 字符 | `@Size(max=64)` | “排名描述过长” |
| `honors` | 否 | 数组，每项 ≤128 字符 | `@Size` | “荣誉描述过长” |
| `courses` | 否 | 数组，每项 ≤64 字符 | `@Size` | “课程名称过长” |

### 4.1 时间范围校验

```java
// 后端校验逻辑示例
if (!"present".equals(education.getEndDate()) 
    && education.getEndDate().compareTo(education.getStartDate()) < 0) {
    throw new BusinessException("EDUCATION_TIME_INVALID", "毕业时间不能早于入学时间。");
}
```

---

## 5. 项目经历 Project 校验

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `name` | 是 | 1–128 字符 | `@NotBlank @Size(max=128)` | “项目名称为必填项” |
| `role` | 否 | ≤64 字符 | `@Size(max=64)` | “项目角色描述过长” |
| `type` | 否 | projectType 枚举 | `@Pattern` | “项目类型不正确” |
| `startDate` | 否 | `YYYY-MM` | `@Pattern` | “项目开始时间格式不正确” |
| `endDate` | 否 | `YYYY-MM` 或 `present` | `@Pattern` | “项目结束时间格式不正确” |
| `techStack` | 否 | 数组，每项 ≤32 字符 | `@Size` | “技术栈标签过长” |
| `background` | 否 | ≤500 字符 | `@Size(max=500)` | “项目背景描述过长” |
| `responsibility` | 否 | ≤500 字符 | `@Size(max=500)` | “个人职责描述过长” |
| `achievements` | 否 | 数组，每项 ≤200 字符 | `@Size` | “项目成果描述过长” |
| `description` | 是 | 数组，1–8 条，每条 ≤200 字符 | `@Size(min=1, max=8)` | “项目描述至少需要 1 条” |
| `link` | 否 | URL，≤512 字符 | `@Pattern @Size` | “项目链接格式不正确” |
| `github` | 否 | URL，≤512 字符 | `@Pattern @Size` | “GitHub 链接格式不正确” |

### 5.1 时间范围校验

```java
if (!"present".equals(project.getEndDate()) 
    && project.getEndDate().compareTo(project.getStartDate()) < 0) {
    throw new BusinessException("PROJECT_TIME_INVALID", "项目结束时间不能早于开始时间。");
}
```

---

## 6. 工作经历 WorkExperience 校验

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `company` | 是 | 1–128 字符 | `@NotBlank @Size(max=128)` | “公司名称为必填项” |
| `department` | 否 | ≤64 字符 | `@Size(max=64)` | “部门名称过长” |
| `position` | 是 | 1–64 字符 | `@NotBlank @Size(max=64)` | “职位为必填项” |
| `type` | 否 | workType 枚举 | `@Pattern` | “工作类型不正确” |
| `city` | 否 | ≤50 字符 | `@Size(max=50)` | “工作城市过长” |
| `startDate` | 是 | `YYYY-MM` | `@NotBlank @Pattern` | “入职时间格式不正确” |
| `endDate` | 否 | `YYYY-MM` 或 `present` | `@Pattern` | “离职时间格式不正确” |
| `description` | 是 | 数组，1–8 条，每条 ≤200 字符 | `@Size(min=1, max=8)` | “工作内容至少需要 1 条” |
| `achievements` | 否 | 数组，每项 ≤200 字符 | `@Size` | “工作成果描述过长” |
| `techStack` | 否 | 数组，每项 ≤32 字符 | `@Size` | “技术栈标签过长” |
| `leaveReason` | 否 | ≤200 字符 | `@Size(max=200)` | “离职原因描述过长” |
| `showLeaveReason` | 否 | boolean | 类型校验 | — |

---

## 7. 技能 Skill 校验

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `category` | 是 | skillCategory 枚举 | `@NotBlank` | “技能分类为必填项” |
| `items` | 是 | 数组，1–20 项 | `@NotEmpty @Size(max=20)` | “技能项不能为空” |
| `items[].name` | 是 | 1–64 字符 | `@NotBlank @Size(max=64)` | “技能名称为必填项” |
| `items[].level` | 否 | skillLevel 枚举 | `@Pattern` | “熟练程度不正确” |
| `items[].highlight` | 否 | boolean | 类型校验 | — |

### 7.1 技能分类枚举

| 枚举值 | 中文 |
|---|---|
| `programming_language` | 编程语言 |
| `frontend` | 前端技术 |
| `backend` | 后端技术 |
| `database` | 数据库 |
| `ai_data` | AI / 数据分析 |
| `design` | 设计软件 |
| `office` | 办公软件 |
| `language` | 语言能力 |
| `professional_tool` | 专业工具 |
| `other` | 其他 |

---

## 8. 自我介绍 Introduction 校验

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `content` | 是 | 1–500 字符 | `@NotBlank @Size(max=500)` | “自我介绍为必填项” |
| `keywords` | 否 | 数组，每项 ≤20 字符 | `@Size` | “关键词过长” |
| `style` | 否 | introductionStyle 枚举 | `@Pattern` | “风格选择不正确” |
| `maxWords` | 否 | 整数，默认 200 | `@Max(500)` | “字数限制过大” |

### 8.1 字数提示规则（前端）

- 默认建议 80–150 字。
- 超过 200 字时提示“自我介绍建议控制在 200 字以内”。
- 超过 500 字时禁止继续输入。

---

## 9. 头像 Avatar 校验

### 9.1 文件上传校验

| 校验项 | 规则 | 错误提示 |
|---|---|---|
| 文件格式 | JPG、PNG、WEBP | “请上传 JPG、PNG 或 WEBP 格式图片。” |
| 文件大小 | ≤10MB | “图片大小不能超过 10MB。” |
| MIME 类型 | `image/jpeg`、`image/png`、`image/webp` | “图片格式不正确。” |
| 扩展名 | `.jpg`、`.jpeg`、`.png`、`.webp`（不区分大小写） | “图片扩展名不正确。” |
| 最小分辨率 | 建议 ≥300×300（非强制，仅提示） | “图片分辨率较低，建议使用 300×300 以上图片。” |

### 9.2 后端文件校验示例

```java
private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp"
);
private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

public void validateAvatarFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
        throw new BusinessException("AVATAR_FILE_EMPTY", "请上传头像图片。");
    }
    if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
        throw new BusinessException("AVATAR_FORMAT_UNSUPPORTED", "请上传 JPG、PNG 或 WEBP 格式图片。");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new BusinessException("AVATAR_FILE_TOO_LARGE", "图片大小不能超过 10MB。");
    }
}
```

### 9.3 头像优化请求校验

| 字段 | 必填 | 规则 | 错误提示 |
|---|---|---|---|
| `sourceImageUrl` | 是 | 非空，≤512 字符 | “原图地址为必填项” |
| `backgroundType` | 是 | `white` / `blue` / `red` | “背景类型不正确” |
| `style` | 是 | `formal` / `natural` / `professional` | “头像风格不正确” |
| `keepIdentity` | 否 | boolean | — |
| `enhanceQuality` | 否 | boolean | — |
| `removeBackground` | 否 | boolean | — |
| `brightenSkin` | 否 | boolean | — |

---

## 10. PDF 导出预检查

导出前前端与后端均需检查：

| 检查项 | 规则 | 错误提示 |
|---|---|---|
| 姓名 | 已填写 | “建议填写姓名，便于生成正式简历。” |
| 联系方式 | 手机或邮箱至少填一个 | “简历中至少需要填写手机号或邮箱。” |
| 空模块 | 隐藏所有无数据的 visible 模块 | — |
| 头像加载 | 头像 URL 有效 | “头像加载失败，请重新上传。” |
| 未保存内容 | 无 | “请先保存当前简历。” |
| 内容超出 | 仅提示，不阻止 | “当前简历内容较长，可能导出为多页。” |

---

## 11. 简历 Resume 校验

### 11.1 创建简历（`POST /resumes`）

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `title` | 是 | 1–128 字符 | `@NotBlank @Size(max=128)` | “简历名称为必填项” |
| `scene` | 是 | scene 枚举 | `@NotBlank @Pattern` | “使用场景不正确” |
| `targetPosition` | 否 | ≤128 字符 | `@Size(max=128)` | “目标岗位过长” |
| `templateId` | 是 | 非空，≤64 字符 | `@NotBlank @Size(max=64)` | “模板 ID 为必填项” |
| `sections` | 是 | 数组 | `@NotNull` | “简历内容不能为空” |

### 11.2 更新简历（`PUT /resumes/{id}`）

| 字段 | 必填 | 长度/格式 | 后端校验注解 | 错误提示 |
|---|---|---|---|---|
| `title` | 否 | 1–128 字符 | `@Size(max=128)` | “简历名称过长” |
| `targetPosition` | 否 | ≤128 字符 | `@Size(max=128)` | “目标岗位过长” |
| `templateId` | 否 | 非空，≤64 字符 | `@Size(max=64)` | “模板 ID 过长” |
| `sections` | 否 | 数组 | — | “简历内容不能为空”仅在传入时校验 |
| `renderSettings` | 否 | 排版设置对象 | `@Valid`；数值范围与主题色格式见下表 | “排版设置不正确” |

**注意**：更新接口所有字段均为可选；仅更新传入的字段，未传入的字段保持原值不变。

### 11.3 渲染设置（`renderSettings`）

| 字段 | 规则 |
|---|---|
| `autoOnePage` | boolean；默认关闭 |
| `fontFamily` | 仅允许前端提供的字体选项，后端渲染时再次执行白名单清洗 |
| `baseFontSize` | 8–16pt |
| `lineHeight` | 1.0–2.2 |
| `pagePadding` | 8–30mm |
| `sectionSpacing` | 4–32px |
| `accentColor` | `#RRGGBB` 格式 |

---

## 12. Section 统一结构校验

| 字段 | 必填 | 规则 | 错误提示 |
|---|---|---|---|
| `id` | 是 | 非空，≤64 字符 | “模块 ID 为必填项” |
| `type` | 是 | sectionType 枚举 | “模块类型不正确” |
| `title` | 是 | 1–64 字符 | “模块标题为必填项” |
| `order` | 是 | 整数 ≥0 | “模块排序不正确” |
| `visible` | 是 | boolean | “模块展示状态不正确” |
| `data` | 是 | object | “模块数据不能为空” |

---

## 13. 错误提示文案映射表

| 场景 | 错误码（字符串） | 数字错误码 | 提示文案 |
|---|---|---|---|
| 未填写姓名 | `RESUME_PROFILE_NAME_REQUIRED` | `2004` | “姓名为必填项。” |
| 未填写联系方式 | `RESUME_PROFILE_CONTACT_REQUIRED` | `2005` | “简历中至少需要填写手机号或邮箱。” |
| 手机号格式错误 | `RESUME_PROFILE_PHONE_INVALID` | `2007` | “手机号格式不正确。” |
| 邮箱格式错误 | `RESUME_PROFILE_EMAIL_INVALID` | `2008` | “邮箱格式不正确。” |
| URL 格式错误 | `RESUME_PROFILE_URL_INVALID` | `2009` | “请输入正确的网址格式。” |
| 内容过短 | `RESUME_CONTENT_TOO_SHORT` | `2006` | “简历内容较少，建议补充后再导出。” |
| 内容过长 | `RESUME_CONTENT_TOO_LONG` | `2010` | “当前内容较长，可能导致简历超出一页。” |
| 图片格式不支持 | `AVATAR_FORMAT_UNSUPPORTED` | `4001` | “请上传 JPG、PNG 或 WEBP 格式图片。” |
| 图片过大 | `AVATAR_FILE_TOO_LARGE` | `4002` | “图片大小不能超过 10MB。” |
| 图片加载失败 | `AVATAR_LOAD_FAILED` | `4011` | “图片加载失败，请重新上传。” |
| 头像优化失败 | `AVATAR_OPTIMIZE_FAILED` | `4007` | “头像优化失败，请稍后重试或使用原图。” |
| PDF 生成失败 | `PDF_EXPORT_FAILED` | `5004` | “PDF 生成失败，请稍后重试或检查网络连接。” |
| 网络异常 | `NETWORK_ERROR` | 前端自定义 | “网络异常，PDF 生成失败。” |
| 越权访问 | `ACCESS_DENIED` | `403` | “无权访问该资源。” |
| 资源不存在 | `RESOURCE_NOT_FOUND` | `404` | “请求的资源不存在。” |
| 参数校验失败 | `PARAM_INVALID` | `400` | “请求参数不正确，请检查。” |

> **说明**：数字错误码与 `backend/src/main/java/com/resume/common/constant/ResultCode.java` 保持一致。新增错误码（如 `2007`/`2008`/`2009`/`2010`）需在 ResultCode 中同步补充。

---

## 14. 前端 Element Plus Form Rules 示例

```typescript
const profileRules = {
  name: [
    { required: true, message: '姓名为必填项。', trigger: 'blur' },
    { max: 50, message: '姓名不能超过 50 个字符。', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确。', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确。', trigger: 'blur' }
  ],
  personalWebsite: [
    { pattern: /^https?:\/\/([\w-]+\.)+[\w-]+(\/[\w-./?%&=]*)?$/, message: '个人网站格式不正确。', trigger: 'blur' }
  ],
  github: [
    { pattern: /^https?:\/\/([\w-]+\.)+[\w-]+(\/[\w-./?%&=]*)?$/, message: 'GitHub 链接格式不正确。', trigger: 'blur' }
  ],
  portfolio: [
    { pattern: /^https?:\/\/([\w-]+\.)+[\w-]+(\/[\w-./?%&=]*)?$/, message: '作品集链接格式不正确。', trigger: 'blur' }
  ]
}
```

---

## 15. 后端 Java Bean Validation 示例

```java
@Data
public class ProfileDTO {
    @NotBlank(message = "姓名为必填项。")
    @Size(max = 50, message = "姓名不能超过 50 个字符。")
    private String name;

    @Pattern(regexp = "^(male|female|other)$", message = "性别格式不正确。")
    private String gender;

    @Pattern(regexp = "^(19|20)\\d{2}-(0[1-9]|1[0-2])$", message = "出生年月格式不正确。")
    private String birthDate;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确。")
    private String phone;

    @Email(message = "邮箱格式不正确。")
    @Size(max = 128, message = "邮箱过长。")
    private String email;

    @Size(max = 50, message = "所在城市过长。")
    private String city;

    @Size(max = 128, message = "目标岗位过长。")
    private String targetPosition;

    @Pattern(regexp = "^https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$", message = "请输入正确的网址格式。")
    @Size(max = 512, message = "链接过长。")
    private String personalWebsite;

    @Pattern(regexp = "^https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$", message = "请输入正确的网址格式。")
    @Size(max = 512, message = "链接过长。")
    private String github;

    @Pattern(regexp = "^https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$", message = "请输入正确的网址格式。")
    @Size(max = 512, message = "链接过长。")
    private String portfolio;
}
```

---

## 16. 参考文档

- `docs/superpowers/specs/2026-07-03-scope-alignment.md`
- `docs/superpowers/specs/2026-07-03-data-model-and-ddl.md`
- `docs/superpowers/specs/2026-07-03-api-spec.md`（下游文档）
