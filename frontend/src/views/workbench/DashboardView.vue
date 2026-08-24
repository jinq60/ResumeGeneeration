<template>
  <div class="dashboard">
    <!-- 页头 -->
    <header class="dashboard-header">
      <div>
        <h1 class="dashboard-title">
          工作台
        </h1>
        <p class="dashboard-subtitle">
          {{ userStore.nickname ? `${userStore.nickname}，欢迎回来` : '欢迎回来' }} · 从这里继续完善你的简历
        </p>
      </div>
      <div class="dashboard-header-actions">
        <button
          class="btn btn-ghost"
          type="button"
          @click="router.push('/workbench/resumes?import=1')"
        >
          <el-icon size="16">
            <Upload />
          </el-icon>
          导入简历
        </button>
        <button
          class="btn btn-primary"
          type="button"
          @click="createResume"
        >
          <el-icon size="16">
            <Plus />
          </el-icon>
          新建简历
        </button>
      </div>
    </header>

    <!-- 统计条 -->
    <section class="stats-row">
      <div class="stat-card">
        <div class="stat-value">
          {{ resumes.length }}
        </div>
        <div class="stat-label">
          我的简历
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-value">
          {{ lastEditedLabel }}
        </div>
        <div class="stat-label">
          最近编辑
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-value">
          {{ templateCount || '—' }}
        </div>
        <div class="stat-label">
          可用模板
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-value">
          {{ guestLabel }}
        </div>
        <div class="stat-label">
          账号类型
        </div>
      </div>
    </section>

    <div class="dashboard-grid">
      <!-- 简历列表 -->
      <section class="panel resume-panel">
        <div class="panel-header">
          <h2 class="panel-title">
            我的简历
          </h2>
          <RouterLink
            to="/workbench/resumes"
            class="panel-link"
          >
            全部简历
            <el-icon size="12">
              <ArrowRight />
            </el-icon>
          </RouterLink>
        </div>

        <div
          v-if="loading"
          class="panel-loading"
        >
          <div
            v-for="i in 3"
            :key="i"
            class="resume-row-skeleton"
          />
        </div>

        <div
          v-else-if="resumes.length === 0"
          class="panel-empty"
        >
          <el-icon
            size="32"
            class="empty-icon"
          >
            <Files />
          </el-icon>
          <p class="empty-title">
            还没有简历
          </p>
          <p class="empty-desc">
            选择模板创建第一份简历，几分钟即可完成
          </p>
          <button
            class="btn btn-primary mt-3"
            type="button"
            @click="createResume"
          >
            <el-icon size="16">
              <Plus />
            </el-icon>
            新建简历
          </button>
        </div>

        <div
          v-else
          class="resume-list"
        >
          <div
            v-for="resume in resumes"
            :key="resume.id"
            class="resume-row"
            @click="openEditor(resume)"
          >
            <div class="resume-row-main">
              <div class="resume-row-title">
                {{ resume.title || '未命名简历' }}
              </div>
              <div class="resume-row-meta">
                {{ resume.targetPosition || sceneLabel(resume.scene) }} · {{ formatTime(resume.updatedAt || resume.createdAt) }}
              </div>
            </div>
            <div
              class="resume-row-actions"
              @click.stop
            >
              <button
                class="row-action"
                title="编辑"
                @click="openEditor(resume)"
              >
                <el-icon size="15">
                  <Edit />
                </el-icon>
              </button>
              <button
                class="row-action"
                title="复制"
                @click="handleCopy(resume)"
              >
                <el-icon size="15">
                  <CopyDocument />
                </el-icon>
              </button>
              <el-dropdown
                trigger="click"
                @command="(cmd: string) => handleResumeAction(cmd, resume)"
              >
                <button
                  class="row-action"
                  title="更多"
                >
                  <el-icon size="15">
                    <MoreFilled />
                  </el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      divided
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧工具 -->
      <aside class="dashboard-aside">
        <section class="panel">
          <div class="panel-header">
            <h2 class="panel-title">
              AI 能力
            </h2>
            <RouterLink
              to="/workbench/ai-review"
              class="panel-link"
            >
              查看
              <el-icon size="12">
                <ArrowRight />
              </el-icon>
            </RouterLink>
          </div>
          <div class="tool-list">
            <button
              class="tool-item"
              type="button"
              @click="router.push('/workbench/ai-review')"
            >
              <span class="tool-dot tool-dot-orange" />
              <span class="tool-text">
                <span class="tool-title">AI 简历点评</span>
                <span class="tool-desc">竞争力评估与改进建议</span>
              </span>
              <el-icon
                size="14"
                class="tool-arrow"
              >
                <ArrowRight />
              </el-icon>
            </button>
            <button
              class="tool-item"
              type="button"
              @click="router.push('/workbench/ai-review')"
            >
              <span class="tool-dot tool-dot-green" />
              <span class="tool-text">
                <span class="tool-title">JD 匹配优化</span>
                <span class="tool-desc">对照岗位描述优化内容</span>
              </span>
              <el-icon
                size="14"
                class="tool-arrow"
              >
                <ArrowRight />
              </el-icon>
            </button>
            <button
              class="tool-item"
              type="button"
              @click="goGrammarCheck"
            >
              <span class="tool-dot tool-dot-blue" />
              <span class="tool-text">
                <span class="tool-title">语法检查</span>
                <span class="tool-desc">错别字与表达问题检查</span>
              </span>
              <el-icon
                size="14"
                class="tool-arrow"
              >
                <ArrowRight />
              </el-icon>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <h2 class="panel-title">
              快捷操作
            </h2>
          </div>
          <div class="quick-grid">
            <button
              class="quick-item"
              type="button"
              @click="router.push('/workbench/avatar/upload')"
            >
              <el-icon
                size="18"
                class="quick-icon"
              >
                <Picture />
              </el-icon>
              <span>头像管理</span>
            </button>
            <button
              class="quick-item"
              type="button"
              @click="router.push('/workbench/downloads')"
            >
              <el-icon
                size="18"
                class="quick-icon"
              >
                <Download />
              </el-icon>
              <span>下载中心</span>
            </button>
            <button
              class="quick-item"
              type="button"
              @click="router.push('/workbench/templates')"
            >
              <el-icon
                size="18"
                class="quick-icon"
              >
                <Postcard />
              </el-icon>
              <span>模板中心</span>
            </button>
            <button
              class="quick-item"
              type="button"
              @click="router.push('/workbench/settings')"
            >
              <el-icon
                size="18"
                class="quick-icon"
              >
                <Setting />
              </el-icon>
              <span>账号设置</span>
            </button>
          </div>
        </section>
      </aside>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog
      v-model="renameVisible"
      title="重命名简历"
      width="420px"
      align-center
    >
      <el-input
        v-model="renameTitle"
        placeholder="输入新标题"
      />
      <template #footer>
        <el-button @click="renameVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleRenameConfirm"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { resumeApi } from '@/api/resume'
import { templateApi } from '@/api/template'
import type { Resume } from '@/types/resume'
import {
  Plus,
  ArrowRight,
  Upload,
  Files,
  Edit,
  CopyDocument,
  MoreFilled,
  Picture,
  Download,
  Postcard,
  Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const resumes = ref<Resume[]>([])
const templateCount = ref(0)
const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Resume | null>(null)

const lastEditedLabel = computed(() => {
  if (resumes.value.length === 0) return '—'
  const latest = [...resumes.value].sort(
    (a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime()
  )[0]
  return formatTime(latest.updatedAt || latest.createdAt)
})

const guestLabel = computed(() => (userStore.isGuest ? '游客' : '会员'))

function sceneLabel(scene: string) {
  const map: Record<string, string> = {
    campus_recruitment: '校招',
    internship: '实习',
    social_recruitment: '社招',
    postgraduate_reexam: '考研',
    project_application: '项目',
    custom: '自定义'
  }
  return map[scene] || '简历'
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const diffDays = Math.floor((Date.now() - d.getTime()) / 86400000)
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return d.toLocaleDateString('zh-CN')
}

function createResume() {
  router.push('/workbench/templates')
}

function goGrammarCheck() {
  if (resumes.value.length === 0) {
    ElMessage.info('请先创建一份简历，再进行语法检查')
    router.push('/workbench/templates')
    return
  }
  const latest = [...resumes.value].sort(
    (a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime()
  )[0]
  router.push(`/workbench/editor/${latest.id}?grammar=1`)
}

function openEditor(resume: Resume) {
  router.push(`/workbench/editor/${resume.id}`)
}

function handleResumeAction(cmd: string, resume: Resume) {
  if (cmd === 'rename') handleRename(resume)
  else if (cmd === 'delete') handleDelete(resume)
}

async function handleCopy(resume: Resume) {
  try {
    await resumeApi.duplicate(resume.id)
    ElMessage.success('已复制')
    fetchResumes()
  } catch {
    ElMessage.error('复制失败')
  }
}

async function handleDelete(resume: Resume) {
  try {
    await ElMessageBox.confirm('确定删除该简历？删除后不可恢复。', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resumeApi.remove(resume.id)
    ElMessage.success('已删除')
    fetchResumes()
  } catch {
    // cancelled
  }
}

function handleRename(resume: Resume) {
  renameTarget.value = resume
  renameTitle.value = resume.title
  renameVisible.value = true
}

async function handleRenameConfirm() {
  if (renameTarget.value && renameTitle.value.trim()) {
    try {
      await resumeApi.rename(renameTarget.value.id, renameTitle.value.trim())
      ElMessage.success('重命名成功')
      renameVisible.value = false
      fetchResumes()
    } catch {
      ElMessage.error('重命名失败')
    }
  }
}

async function fetchResumes() {
  try {
    const data = await resumeApi.list(1, 5)
    resumes.value = data?.records || []
  } catch (e: any) {
    ElMessage.error(e.message || '简历加载失败')
  }
}

async function fetchTemplates() {
  try {
    const data = await templateApi.list()
    templateCount.value = data?.length || 0
  } catch {
    // 模板数量为非关键信息，失败时展示占位
  }
}

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchResumes(), fetchTemplates()])
  loading.value = false
})
</script>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  min-width: 0;
}

.dashboard-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.dashboard-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--st-on-surface);
}

.dashboard-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--st-on-surface-variant);
}

.dashboard-header-actions {
  display: flex;
  gap: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border-radius: var(--st-radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.btn-primary {
  background: var(--st-primary);
  color: var(--st-on-primary);

  &:hover {
    background: var(--st-primary-container);
    color: var(--st-on-primary-container);
  }
}

.btn-ghost {
  background: var(--st-surface-container-lowest);
  border-color: var(--st-outline-variant);
  color: var(--st-on-surface-variant);

  &:hover {
    color: var(--st-on-surface);
    background: var(--st-surface-container-low);
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: var(--st-surface-container-lowest);
  border: 1px solid var(--st-outline-variant);
  border-radius: var(--st-radius-md);
  padding: 14px 16px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--st-on-surface);
}

.stat-label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--st-on-surface-variant);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(320px, 25vw, 380px);
  gap: 16px;
  align-items: start;

  @media (max-width: 1100px) {
    grid-template-columns: 1fr;
  }
}

.panel {
  background: var(--st-surface-container-lowest);
  border: 1px solid var(--st-outline-variant);
  border-radius: var(--st-radius-md);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--st-outline-variant);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--st-on-surface);
}

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--st-primary);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.resume-list {
  display: flex;
  flex-direction: column;
}

.resume-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--st-outline-variant);
  cursor: pointer;
  transition: background 0.15s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--st-surface-container-low);
  }
}

.resume-row-main {
  flex: 1;
  min-width: 0;
}

.resume-row-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--st-on-surface);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.resume-row-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--st-on-surface-variant);
}

.resume-row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.resume-row:hover .resume-row-actions {
  opacity: 1;
}

.row-action {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--st-radius-sm);
  background: transparent;
  color: var(--st-on-surface-variant);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    color: var(--st-primary);
    background: var(--st-primary-container);
  }
}

.panel-loading {
  padding: 8px 16px;
}

.resume-row-skeleton {
  height: 40px;
  margin: 8px 0;
  border-radius: var(--st-radius-sm);
  background: linear-gradient(90deg, var(--st-surface-container-low) 25%, var(--st-surface-container) 50%, var(--st-surface-container-low) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

.panel-empty {
  padding: 40px 16px;
  text-align: center;
}

.empty-icon {
  color: var(--st-outline);
}

.empty-title {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--st-on-surface);
}

.empty-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--st-on-surface-variant);
}

.dashboard-aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tool-list {
  display: flex;
  flex-direction: column;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 16px;
  border: none;
  border-bottom: 1px solid var(--st-outline-variant);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--st-surface-container-low);
  }
}

.tool-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tool-dot-orange {
  background: #f59e0b;
}

.tool-dot-green {
  background: #22c55e;
}

.tool-dot-blue {
  background: #3b82f6;
}

.tool-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.tool-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--st-on-surface);
}

.tool-desc {
  margin-top: 1px;
  font-size: 11px;
  color: var(--st-on-surface-variant);
}

.tool-arrow {
  color: var(--st-outline);
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  border: 1px solid var(--st-outline-variant);
  border-radius: var(--st-radius-md);
  background: var(--st-surface-container-lowest);
  font-size: 12px;
  color: var(--st-on-surface-variant);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    color: var(--st-primary);
    border-color: var(--st-primary);
    background: var(--st-primary-container);
  }
}

.quick-icon {
  color: var(--st-on-surface-variant);
}

.quick-item:hover .quick-icon {
  color: var(--st-primary);
}
</style>
