# 开发流程与分支规范

本文档描述 `ResumeGeneeration` 项目的分支模型、CI/CD 流程、代码提交规范以及测试/部署约定。所有贡献者应遵循本流程。

---

## 1. 分支模型

采用 **GitHub Flow + 稳定分支** 的混合模型：

```
stable  ─────────────────────────────  生产稳定快照（用于回滚）
          \                      /
           \   release PR       /
production ─── develop ── feature/xxx    默认分支为 production
              |   \\               /
              |    \\ hotfix/xxx /
              |     \\__________/
              |
              └──  特性开发从 develop 切出
```

### 1.1 长期分支

| 分支 | 用途 | 保护策略 |
|---|---|---|
| `production` | 默认分支，代表可随时部署的生产代码 | 必需 status checks；Code review 建议 |
| `develop` | 日常开发集成分支，特性完成后合并到这里 | 必需 status checks |
| `stable` | 上一个已验证的生产版本快照，用于紧急回滚 | 建议只读或管理员推送 |
| `main` | 旧初始提交，保留仅作历史，不再使用 | 可归档 |

### 1.2 临时分支

- **特性分支**：`feature/{short-desc}`（例：`feature/resume-export-pdf`）
  - 基于 `develop` 切出，合并回 `develop`。
- **修复分支**：`fix/{short-desc}`
  - 基于 `develop` 切出，合并回 `develop`。
- **热修复分支**：`hotfix/{short-desc}`
  - 基于 `production` 切出，合并回 `production`，同时同步回 `develop`。
- **其他 Devin/自动化分支**：`devin/{task}`，合并后删除。

---

## 2. 工作流

### 2.1 特性开发

```bash
# 1. 基于最新 develop 创建特性分支
git checkout develop
git pull origin develop
git checkout -b feature/resume-export-pdf

# 2. 开发并提交（遵循 Conventional Commits）
git commit -m "feat(resume): add PDF export endpoint with Playwright render"

# 3. 推送到远程并创建 PR 到 develop
git push -u origin feature/resume-export-pdf
# 在 GitHub 创建 PR: feature/resume-export-pdf -> develop
```

### 2.2 发布流程

1. 当 `develop` 积累足够变更并通过 CI 后，创建 PR `develop -> production`。
2. 发布前将 `stable` 快进到当前 `production` 作为回滚点。
3. 合并 PR 到 `production`。
4. 部署 `production` 到测试/生产服务器。

### 2.3 紧急热修复

1. 基于 `production` 创建 `hotfix/{desc}`。
2. 修复后创建 PR `hotfix/{desc} -> production`。
3. 合并后，将 `production` 反向合并到 `develop` 以保持同步。

---

## 3. CI/CD

### 3.1 触发条件

以下事件会触发 GitHub Actions：

- `push` 到 `production`、`develop`、`stable`
- `pull_request` 目标分支为 `production` 或 `develop`

### 3.2 工作流

| 工作流文件 | 作用 |
|---|---|
| `.github/workflows/backend-ci.yml` | JDK 17 环境；运行 `mvn -B clean test` |
| `.github/workflows/frontend-ci.yml` | Node 20 环境；运行 `npm ci`、lint、unit tests、production build |
| `.github/workflows/deploy.yml` | `production`/`develop` 推送时构建 dist，SCP 源码/产物到服务器后由后端 Dockerfile 构建 jar，再用 Docker Compose 部署 |

### 3.3 本地验证

提交 PR 前，应在本地通过：

```bash
# 后端
cd backend
mvn clean test

# 前端
cd frontend
npm run lint
npm run test:unit -- --run
npm run build
```

---

## 4. 提交规范

采用 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>(<scope>): <short summary>

<body>
```

常用 type：

- `feat`：新功能
- `fix`：修复 bug
- `refactor`：重构
- `test`：测试
- `docs`：文档
- `ci`：CI/CD
- `chore`：构建/工具

Scope 示例：`backend`、`frontend`、`common`、`user`、`resume`、`template`。

---

## 5. 代码质量

- 后端遵循 `backend/CLAUDE.md` 及子模块 `CLAUDE.md` 中的约束。
- 前端遵循 `frontend/CLAUDE.md` 中的约束。
- 禁止物理删除业务数据、禁止在代码中硬编码密钥。
- 新增功能优先补充测试（后端 TDD）。

---

## 6. 部署约定

- 开发/测试环境服务器：`101.43.117.17`（Ubuntu 24.04）。
- 服务器初始化由 Devin 统一配置，业务服务以 Docker Compose 运行。
- 生产部署通过 `production` 分支触发，测试部署通过 `develop` 分支触发。
- 详细环境变量与运行参数见 `docs/environment.md` 与 `docs/setup-guide.md`。

---

## 7. 相关文档

- `docs/setup-guide.md`：环境搭建与本地运行
- `docs/environment.md`：环境变量说明
- `docs/security-guide.md`：安全与隐私合规
- `backend/CLAUDE.md`、子模块 `CLAUDE.md`：后端开发约束
- `frontend/CLAUDE.md`：前端开发约束
