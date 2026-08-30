<template>
  <main class="workbench-page resume-list-page flex flex-col gap-stack-lg animate-fade-in-up">
    <!-- Main Content Area -->
    <div class="flex-1">
      <!-- Header Section -->
      <div class="flex justify-between items-center mb-stack-lg">
        <h1 class="font-serif text-3xl font-medium text-foreground">
          我的简历
        </h1>
        <div class="flex items-center gap-3">
          <button
            class="border border-border rounded-xl hover:bg-secondary text-foreground px-6 py-2.5 text-sm font-medium transition-colors flex items-center gap-2"
            @click="openImportDialog"
          >
            <el-icon size="16">
              <Upload />
            </el-icon>
            导入简历
          </button>
          <button
            class="bg-foreground text-primary-foreground px-6 py-2.5 rounded-xl text-sm font-medium flex items-center gap-2 shadow-md hover:opacity-90 active:scale-[0.98] transition-all"
            @click="goCreate"
          >
            <el-icon size="18">
              <Plus />
            </el-icon>
            新建简历
          </button>
        </div>
      </div>

      <!-- 导入对话框 -->
      <a-modal
        :visible="importDialogVisible"
        title="导入简历"
        :width="560"
        @cancel="importDialogVisible = false"
        @before-ok="handleImport"
      >
        <div class="flex flex-col gap-4">
          <div class="flex items-center gap-4">
            <a-select
              v-model="importForm.format"
              class="w-36"
            >
              <a-option value="markdown">
                Markdown
              </a-option>
              <a-option value="json">
                JSON
              </a-option>
            </a-select>
            <a-input
              v-model="importForm.title"
              placeholder="简历标题（可选，默认取首个模块标题）"
              :max-length="128"
            />
          </div>
          <a-textarea
            v-model="importForm.content"
            :auto-size="{ minRows: 10, maxRows: 14 }"
            :placeholder="importForm.format === 'markdown'
              ? '粘贴 Markdown 内容：# 姓名\n## 工作经历\n公司 · 岗位\n- 工作描述\n## 自我介绍\n...'
              : '粘贴 JSON：模块数组或 {sections: [...]}'"
          />
          <div class="text-xs text-muted-foreground">
            Markdown 按「## 模块标题」自动识别教育 / 工作 / 项目 / 技能 / 自我介绍等模块，导入后可在编辑器中继续精修。
          </div>
        </div>
        <template #footer>
          <button
            class="px-4 py-2 border border-border rounded-xl hover:bg-secondary text-foreground text-sm font-medium transition-colors"
            @click="importDialogVisible = false"
          >
            取消
          </button>
          <button
            class="px-4 py-2 bg-foreground text-primary-foreground rounded-xl text-sm font-medium shadow-md hover:opacity-90 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
            :disabled="importing || !importForm.content.trim()"
            @click="handleImport"
          >
            <el-icon
              v-if="importing"
              class="animate-spin"
              size="14"
            >
              <Loading />
            </el-icon>
            {{ importing ? '导入中...' : '导入并打开' }}
          </button>
        </template>
      </a-modal>

      <!-- Tabs & Search -->
      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-stack-lg bg-card p-2 rounded-2xl border border-border">
        <div class="flex gap-2">
          <button
            v-for="t in tabs"
            :key="t.value"
            :class="[
              'px-4 py-2 rounded-xl text-sm font-medium transition-all',
              currentTab === t.value
                ? 'bg-secondary text-foreground'
                : 'text-muted-foreground hover:bg-secondary/60'
            ]"
            @click="currentTab = t.value"
          >
            {{ t.label }}
          </button>
        </div>
        <div class="resume-search-controls">
          <div class="resume-search-input">
            <input
              v-model="keyword"
              class="resume-search-field"
              placeholder="搜索简历名称"
              type="text"
            >
            <el-icon
              class="resume-search-icon text-muted cursor-pointer"
              @click="handleSearch"
            >
              <Search />
            </el-icon>
          </div>
          <el-dropdown
            trigger="click"
            @command="handleSceneFilter"
          >
            <button class="resume-filter-button border border-border rounded-xl hover:bg-secondary text-muted-foreground transition-colors">
              <el-icon>
                <Filter />
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  command=""
                  :class="{ 'font-bold text-foreground': !sceneFilter }"
                >
                  全部场景
                </el-dropdown-item>
                <el-dropdown-item
                  v-for="(label, value) in sceneLabels"
                  :key="value"
                  :command="value"
                  :class="{ 'font-bold text-foreground': sceneFilter === value }"
                >
                  {{ label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- Resume Grid -->
      <div
        v-if="loading"
        class="resume-grid"
      >
        <div
          v-for="i in 3"
          :key="i"
          class="aspect-[3/4] bg-card rounded-2xl border border-border animate-pulse p-4"
        >
          <div class="w-full h-3/4 bg-secondary rounded-xl mb-4" />
          <div class="h-4 bg-border rounded w-3/4 mb-2" />
          <div class="h-3 bg-border rounded w-1/2" />
        </div>
      </div>

      <div
        v-else-if="filteredResumes.length === 0"
        class="bg-card rounded-2xl border border-border p-12 text-center"
      >
        <div class="w-16 h-16 mx-auto mb-4 bg-secondary rounded-full flex items-center justify-center">
          <el-icon
            size="28"
            class="text-muted"
          >
            <Document />
          </el-icon>
        </div>
        <h3 class="text-xl font-serif font-medium text-foreground mb-2">
          还没有简历
        </h3>
        <p class="text-base text-muted-foreground mb-6">
          创建第一份简历，开始你的求职之旅
        </p>
        <button
          class="bg-foreground text-primary-foreground px-6 py-2.5 rounded-xl text-sm font-medium flex items-center gap-2 mx-auto shadow-md hover:opacity-90 active:scale-[0.98] transition-all"
          @click="goCreate"
        >
          <el-icon><Plus /></el-icon>
          新建简历
        </button>
      </div>

      <div
        v-else
        class="resume-grid"
      >
        <!-- Resume Cards -->
        <div
          v-for="(resume, idx) in filteredResumes"
          :key="resume.id"
          class="resume-card group bg-card rounded-2xl border border-border hover:shadow-xl transition-all duration-300 relative overflow-hidden"
        >
          <div class="aspect-[3/4] bg-secondary relative overflow-hidden p-4">
            <span
              v-if="idx === 0"
              class="absolute top-2 left-2 z-10 bg-secondary text-foreground text-[10px] font-bold px-2 py-0.5 rounded border border-border"
            >默认</span>
            <div class="w-full h-full bg-white shadow-md rounded-sm p-4 origin-top transition-transform group-hover:scale-[1.02]">
              <img
                class="w-full h-full object-contain"
                :src="TEMPLATE_PLACEHOLDER"
                :alt="resume.title"
              >
            </div>
            <!-- Hover Actions Overlay -->
            <div class="resume-overlay absolute inset-0 bg-black/40 backdrop-blur-sm flex flex-col items-center justify-center gap-3 opacity-0 transition-opacity duration-300">
              <button
                class="w-32 bg-card text-foreground py-2 rounded-xl text-sm font-medium flex items-center justify-center gap-2 hover:bg-foreground hover:text-primary-foreground transition-all"
                @click.stop="openEditor(resume)"
              >
                <el-icon><Edit /></el-icon> 编辑简历
              </button>
              <div class="flex gap-2">
                <button
                  class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-xl hover:bg-white/40 transition-all flex items-center justify-center"
                  @click.stop="handleCopy(resume)"
                >
                  <el-icon><CopyDocument /></el-icon>
                </button>
                <button
                  class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-xl hover:bg-white/40 transition-all flex items-center justify-center"
                  @click.stop="handleExport(resume)"
                >
                  <el-icon><Download /></el-icon>
                </button>
                <button
                  class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-xl hover:bg-destructive transition-all flex items-center justify-center"
                  @click.stop="handleDelete(resume)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-border flex justify-between items-start">
            <div>
              <h3 class="text-lg font-medium text-foreground mb-1">
                {{ resume.title || '未命名简历' }}
              </h3>
              <p class="text-sm text-muted-foreground flex items-center gap-1">
                <el-icon size="12">
                  <Grid />
                </el-icon> 模板: {{ sceneLabel(resume.scene) }} · {{ templateName(resume.templateId) }}
              </p>
              <p class="text-[11px] text-muted mt-2">
                {{ formatTime(resume.updatedAt || resume.createdAt) }}
              </p>
            </div>
            <a-dropdown
              trigger="click"
              @select="(value: any) => handleResumeAction(String(value), resume)"
              @click.stop
            >
              <button
                class="p-1.5 hover:bg-secondary rounded-lg transition-colors text-muted-foreground"
                @click.stop
              >
                <el-icon><More /></el-icon>
              </button>
              <template #content>
                <a-doption value="edit">
                  编辑
                </a-doption>
                <a-doption value="copy">
                  复制
                </a-doption>
                <a-doption value="rename">
                  重命名
                </a-doption>
                <a-doption
                  value="delete"
                  class="!text-destructive"
                >
                  删除
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </div>

        <!-- Create New Card -->
        <button
          class="resume-create-card bg-card border-2 border-dashed border-border rounded-2xl flex flex-col items-center justify-center group hover:border-foreground transition-colors cursor-pointer"
          @click="goCreate"
        >
          <div class="w-12 h-12 bg-secondary rounded-full flex items-center justify-center group-hover:bg-foreground group-hover:text-primary-foreground transition-all mb-4">
            <el-icon size="24">
              <Plus />
            </el-icon>
          </div>
          <span class="text-lg font-serif font-medium text-foreground">新建简历</span>
          <span class="text-sm text-muted-foreground">选择模板，快速创建</span>
        </button>
      </div>

      <!-- Pagination -->
      <div
        v-if="!loading && displayTotal > pageSize"
        class="flex justify-center mt-6"
      >
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="displayTotal"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </div>

    <!-- Sidebar / Inspector Drawer -->
    <aside class="resume-insight w-full">
      <div class="resume-insight-card bg-card rounded-2xl border border-border p-8 text-center">
        <el-icon
          size="40"
          class="text-muted mb-3"
        >
          <InfoFilled />
        </el-icon>
        <div class="resume-insight-copy">
          <h2 class="text-lg font-serif font-medium text-foreground">
            求职准备度
          </h2>
          <p class="text-base text-muted-foreground mt-2 leading-relaxed">
            {{ insightDescription }}
          </p>
          <template v-if="insightScore !== null">
            <div class="flex items-center gap-3 mt-3">
              <span class="text-3xl font-serif font-bold text-foreground">{{ insightScore }}</span>
              <span class="text-sm text-muted-foreground">/ 100 综合评分</span>
            </div>
            <div class="h-1.5 w-full max-w-[240px] bg-secondary rounded-full overflow-hidden mt-2">
              <div
                class="h-full bg-foreground rounded-full"
                :style="{ width: insightScore + '%' }"
              />
            </div>
          </template>
        </div>
        <RouterLink
          to="/workbench/ai-review"
          class="resume-insight-action mt-4 inline-flex items-center gap-1 text-foreground text-sm font-medium hover:underline"
        >
          去 AI 点评
          <el-icon size="12">
            <ArrowRight />
          </el-icon>
        </RouterLink>
      </div>
    </aside>
  </main>

  <!-- Rename Dialog -->
  <a-modal
    :visible="renameVisible"
    title="重命名简历"
    :width="420"
    @cancel="renameVisible = false"
    @before-ok="handleRenameConfirm"
  >
    <a-input
      v-model="renameTitle"
      placeholder="输入新标题"
    />
    <template #footer>
      <button
        class="px-4 py-2 border border-border rounded-xl hover:bg-secondary text-foreground text-sm font-medium transition-colors"
        @click="renameVisible = false"
      >
        取消
      </button>
      <button
        class="px-4 py-2 bg-foreground text-primary-foreground rounded-xl text-sm font-medium shadow-md hover:opacity-90 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="!renameTitle.trim()"
        @click="handleRenameConfirm"
      >
        确认
      </button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { resumeApi } from '@/api/resume'
import { templateApi } from '@/api/template'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'
import type { Resume } from '@/types/resume'
import {
  Plus,
  Search,
  Filter,
  Document,
  Edit,
  CopyDocument,
  Download,
  Delete,
  Grid,
  More,
  InfoFilled,
  ArrowRight,
  Upload,
  Loading
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const resumes = ref<Resume[]>([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const keyword = ref((route.query.q as string) || '')
const sceneFilter = ref('')
const currentTab = ref<'recent' | 'all'>('recent')
const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Resume | null>(null)
const templateNameMap = ref<Record<string, string>>({})
const importDialogVisible = ref(false)
const importing = ref(false)
const latestReview = ref<{ overallScore?: number; suggestions?: unknown[] } | null>(null)
const importForm = ref<{ format: 'json' | 'markdown'; title: string; content: string }>({
  format: 'markdown',
  title: '',
  content: ''
})

const sceneLabels: Record<string, string> = {
  campus_recruitment: '校招',
  internship: '实习',
  social_recruitment: '社招',
  postgraduate_reexam: '考研',
  project_application: '项目',
  custom: '自定义'
}

const insightDescription = computed(() => {
  const review = latestReview.value
  if (review?.overallScore != null) {
    const count = review.suggestions?.length || 0
    return count > 0
      ? `AI 已评估你的最新简历，共给出 ${count} 条改进建议，点击下方进入完整点评。`
      : 'AI 已评估你的最新简历，继续优化可显著提升竞争力。'
  }
  return '选择一份简历后，可查看 AI 给出的评估与改进建议'
})

const insightScore = computed(() => latestReview.value?.overallScore ?? null)

function openImportDialog() {
  importForm.value = { format: 'markdown', title: '', content: '' }
  importDialogVisible.value = true
}

async function handleImport() {
  const content = importForm.value.content.trim()
  if (!content) {
    Message.warning('请粘贴导入内容')
    return
  }
  importing.value = true
  try {
    const resume = await resumeApi.importResume({
      title: importForm.value.title.trim() || undefined,
      format: importForm.value.format,
      content
    })
    importDialogVisible.value = false
    Message.success('导入成功')
    router.push(`/workbench/editor/${resume.id}`)
  } catch (e: any) {
    Message.error(e.message || '导入失败')
  } finally {
    importing.value = false
  }
}

const tabs = [
  { label: '最近编辑', value: 'recent' as const },
  { label: '全部简历', value: 'all' as const }
]

const filteredAll = computed(() => {
  let list = [...resumes.value]
  if (currentTab.value === 'recent') {
    list.sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
  }
  if (sceneFilter.value) {
    list = list.filter(r => r.scene === sceneFilter.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(r =>
      (r.title || '').toLowerCase().includes(kw) ||
      (r.targetPosition || '').toLowerCase().includes(kw)
    )
  }
  return list
})

const filteredResumes = computed(() => {
  const all = filteredAll.value
  const start = (page.value - 1) * pageSize.value
  return all.slice(start, start + pageSize.value)
})

const displayTotal = computed(() => {
  // 当存在客户端过滤时，总数应为过滤后总数而非服务端总数
  if (sceneFilter.value || keyword.value.trim() || currentTab.value === 'recent') {
    return filteredAll.value.length
  }
  return total.value
})

function handleSceneFilter(value: string) {
  sceneFilter.value = value
  page.value = 1
}

watch([keyword, sceneFilter, currentTab], () => {
  page.value = 1
})

function templateName(id?: string) {
  if (!id) return '默认'
  return templateNameMap.value[id] || id
}

function sceneLabel(s: string) {
  return sceneLabels[s] || '默认'
}

function formatTime(d: string) {
  if (!d) return ''
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 86400000)
  if (diff === 0) return '今天 ' + new Date(d).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff === 1) return '昨天 ' + new Date(d).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff < 7) return `${diff}天前`
  return new Date(d).toLocaleDateString('zh-CN')
}

function handleSearch() {
  if (keyword.value.trim()) {
    router.replace({ query: { q: keyword.value.trim() } })
  } else {
    router.replace({ query: {} })
  }
}

function goCreate() {
  router.push('/workbench/templates')
}

function openEditor(resume: Resume) {
  router.push(`/workbench/editor/${resume.id}`)
}

function handleExport(resume: Resume) {
  router.push(`/workbench/resumes/${resume.id}/export`)
}

function handleResumeAction(cmd: string, resume: Resume) {
  if (cmd === 'edit') openEditor(resume)
  else if (cmd === 'copy') handleCopy(resume)
  else if (cmd === 'rename') handleRename(resume)
  else if (cmd === 'delete') handleDelete(resume)
}

async function handleCopy(resume: Resume) {
  try {
    await resumeApi.duplicate(resume.id)
    Message.success('已复制')
    fetchResumes()
  } catch {
    Message.error('复制失败')
  }
}

async function handleDelete(resume: Resume) {
  try {
    await new Promise<void>((resolve, reject) => {
      Modal.confirm({
        title: '确认',
        content: '确定删除该简历？删除后不可恢复。',
        okText: '删除',
        cancelText: '取消',
        onOk: () => resolve(),
        onCancel: () => reject(new Error('cancelled'))
      })
    })
  } catch {
    return // 用户取消
  }
  try {
    await resumeApi.remove(resume.id)
    Message.success('已删除')
    fetchResumes()
  } catch {
    Message.error('删除失败')
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
      Message.success('重命名成功')
      renameVisible.value = false
      fetchResumes()
    } catch {
      Message.error('重命名失败')
    }
  }
}

async function fetchResumes() {
  try {
    const data = await resumeApi.list(page.value, pageSize.value)
    resumes.value = data?.records || []
    total.value = data?.total || resumes.value.length
  } catch {
    // ignore
  }
}

async function loadTemplateNames() {
  try {
    const templates = await templateApi.list()
    templateNameMap.value = Object.fromEntries(templates.map(t => [t.id, t.name]))
  } catch {
    // 模板名加载失败时退化为显示模板 ID
  }
}

async function loadLatestReview() {
  const sorted = [...resumes.value].sort(
    (a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime()
  )
  const target = sorted[0]
  if (!target) return
  try {
    const review = await resumeApi.getLatestReview(target.id)
    if (review?.overallScore != null) {
      latestReview.value = review
    }
  } catch {
    // 点评数据为非关键信息，失败时保持默认文案
  }
}

watch([page, pageSize], () => {
  fetchResumes()
})

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchResumes(), loadTemplateNames()])
  loading.value = false
  if (route.query.import === '1') {
    openImportDialog()
  }
  loadLatestReview()
})
</script>

<style scoped lang="scss">
.resume-grid {
  display: grid;
  /* PC 工作区固定三列：两份已有简历 + 新建简历卡片。 */
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--st-stack-lg);
  align-items: stretch;
}

.resume-search-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  min-width: 280px;
}

.resume-search-input {
  position: relative;
  width: 256px;
  height: 36px;
  flex: 0 1 256px;
}

.resume-search-field {
  display: block;
  width: 100%;
  height: 36px;
  box-sizing: border-box;
  padding: 0 38px 0 14px;
  border: 1px solid transparent;
  border-radius: var(--st-radius-xl);
  outline: none;
  color: var(--st-on-surface);
  background: var(--st-surface-container-low);
  font-size: 13px;
  line-height: 36px;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.resume-search-field::placeholder {
  color: var(--st-on-surface-variant);
}

.resume-search-field:focus {
  border-color: var(--st-outline-variant);
  background: var(--st-surface-container-lowest);
}

.resume-search-icon {
  position: absolute;
  top: 50%;
  right: 12px;
  z-index: 1;
  width: 16px;
  height: 16px;
  margin: 0;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.resume-filter-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.resume-list-page > .flex-1 {
  min-width: 0;
  width: 100%;
}

.resume-insight-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 88px;
  padding: 16px 20px !important;
  text-align: left;
}

.resume-insight-card > .el-icon {
  flex: 0 0 auto;
  margin: 0 !important;
}

.resume-insight-copy {
  flex: 1;
  min-width: 0;
}

.resume-insight-copy p {
  margin-top: 4px !important;
}

.resume-insight-action {
  flex: 0 0 auto;
  margin-top: 0 !important;
}

.resume-card:hover .resume-overlay {
  opacity: 1;
}

.resume-create-card {
  min-height: 100%;
  aspect-ratio: auto !important;
}

@media (max-width: 900px) {
  .resume-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .resume-insight-card {
    align-items: flex-start;
  }
}

@media (max-width: 700px) {
  .resume-search-controls {
    width: 100%;
    min-width: 0;
  }

  .resume-search-input {
    width: auto;
    flex: 1 1 auto;
  }
}

@media (max-width: 640px) {
  .resume-grid {
    grid-template-columns: 1fr;
  }

  .resume-insight-card {
    flex-wrap: wrap;
  }

  .resume-insight-action {
    margin-left: 40px !important;
  }
}
</style>
