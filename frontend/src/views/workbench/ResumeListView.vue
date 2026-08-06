<template>
  <main class="max-w-[1440px] mx-auto px-margin-page py-stack-lg flex flex-col lg:flex-row gap-stack-lg">
    <!-- Main Content Area -->
    <div class="flex-1">
      <!-- Header Section -->
      <div class="flex justify-between items-center mb-stack-lg">
        <h1 class="font-headline-md text-headline-md text-on-surface">
          我的简历
        </h1>
        <div class="flex items-center gap-3">
          <button
            class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-6 py-2.5 font-label-md transition-colors"
            @click="openImportDialog"
          >
            <el-icon
              class="mr-1"
              size="16"
            >
              <Upload />
            </el-icon>
            导入简历
          </button>
          <button
            class="bg-primary text-on-primary px-6 py-2.5 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
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
      <el-dialog
        v-model="importDialogVisible"
        title="导入简历"
        width="560px"
        align-center
      >
        <div class="flex flex-col gap-4">
          <div class="flex items-center gap-4">
            <el-select
              v-model="importForm.format"
              class="w-36"
            >
              <el-option
                label="Markdown"
                value="markdown"
              />
              <el-option
                label="JSON"
                value="json"
              />
            </el-select>
            <el-input
              v-model="importForm.title"
              placeholder="简历标题（可选，默认取首个模块标题）"
              maxlength="128"
            />
          </div>
          <el-input
            v-model="importForm.content"
            type="textarea"
            :rows="10"
            :placeholder="importForm.format === 'markdown'
              ? '粘贴 Markdown 内容：# 姓名\n## 工作经历\n公司 · 岗位\n- 工作描述\n## 自我介绍\n...'
              : '粘贴 JSON：模块数组或 {sections: [...]}'"
          />
          <div class="text-xs text-on-surface-variant">
            Markdown 按「## 模块标题」自动识别教育 / 工作 / 项目 / 技能 / 自我介绍等模块，导入后可在编辑器中继续精修。
          </div>
        </div>
        <template #footer>
          <el-button @click="importDialogVisible = false">
            取消
          </el-button>
          <el-button
            type="primary"
            :loading="importing"
            :disabled="!importForm.content.trim()"
            @click="handleImport"
          >
            导入并打开
          </el-button>
        </template>
      </el-dialog>

      <!-- Tabs & Search -->
      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-stack-lg bg-surface-container-lowest p-2 rounded-xl border border-outline-variant/30">
        <div class="flex gap-2">
          <button
            v-for="t in tabs"
            :key="t.value"
            :class="[
              'px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
              currentTab === t.value
                ? 'bg-primary-fixed text-primary'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            ]"
            @click="currentTab = t.value"
          >
            {{ t.label }}
          </button>
        </div>
        <div class="flex items-center gap-2 w-full sm:w-auto">
          <div class="relative flex-1 sm:w-64">
            <input
              v-model="keyword"
              class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
              placeholder="搜索简历名称"
              type="text"
            >
            <el-icon
              class="absolute right-2 top-2 text-outline text-lg"
              @click="handleSearch"
            >
              <Search />
            </el-icon>
          </div>
          <button class="p-2 border border-outline-variant rounded-lg hover:bg-surface-container-low">
            <el-icon class="text-on-surface-variant">
              <Filter />
            </el-icon>
          </button>
        </div>
      </div>

      <!-- Resume Grid -->
      <div
        v-if="loading"
        class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-stack-lg"
      >
        <div
          v-for="i in 3"
          :key="i"
          class="aspect-[3/4] bg-surface-container-lowest rounded-xl border border-outline-variant animate-pulse p-4"
        >
          <div class="w-full h-3/4 bg-surface-container-low rounded-lg mb-4" />
          <div class="h-4 bg-surface-container rounded w-3/4 mb-2" />
          <div class="h-3 bg-surface-container rounded w-1/2" />
        </div>
      </div>

      <div
        v-else-if="filteredResumes.length === 0"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 text-center"
      >
        <div class="w-16 h-16 mx-auto mb-4 bg-surface-container-low rounded-full flex items-center justify-center">
          <el-icon
            size="28"
            class="text-outline"
          >
            <Document />
          </el-icon>
        </div>
        <h3 class="text-title-md font-bold text-on-surface mb-2">
          还没有简历
        </h3>
        <p class="text-body-md text-on-surface-variant mb-6">
          创建第一份简历，开始你的求职之旅
        </p>
        <button
          class="bg-primary text-on-primary px-6 py-2.5 rounded-lg font-label-md flex items-center gap-2 mx-auto"
          @click="goCreate"
        >
          <el-icon><Plus /></el-icon>
          新建简历
        </button>
      </div>

      <div
        v-else
        class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-stack-lg"
      >
        <!-- Resume Cards -->
        <div
          v-for="(resume, idx) in filteredResumes"
          :key="resume.id"
          class="resume-card group bg-surface-container-lowest rounded-xl border border-outline-variant hover:shadow-xl transition-all duration-300 relative overflow-hidden"
        >
          <div class="aspect-[3/4] bg-surface-container-low relative overflow-hidden p-4">
            <span
              v-if="idx === 0"
              class="absolute top-2 left-2 z-10 bg-primary/10 text-primary text-[10px] font-bold px-2 py-0.5 rounded border border-primary/20"
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
                class="w-32 bg-white text-on-surface py-2 rounded-lg font-label-md flex items-center justify-center gap-2 hover:bg-primary hover:text-white transition-all"
                @click.stop="openEditor(resume)"
              >
                <el-icon><Edit /></el-icon> 编辑简历
              </button>
              <div class="flex gap-2">
                <button
                  class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-lg hover:bg-white/40 transition-all flex items-center justify-center"
                  @click.stop="handleCopy(resume)"
                >
                  <el-icon><CopyDocument /></el-icon>
                </button>
                <button
                  class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-lg hover:bg-white/40 transition-all flex items-center justify-center"
                  @click.stop="handleExport(resume)"
                >
                  <el-icon><Download /></el-icon>
                </button>
                <button
                  class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-lg hover:bg-error transition-all flex items-center justify-center"
                  @click.stop="handleDelete(resume)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-outline-variant/30 flex justify-between items-start">
            <div>
              <h3 class="font-title-md text-title-md text-on-surface mb-1">
                {{ resume.title || '未命名简历' }}
              </h3>
              <p class="text-label-md font-label-md text-on-surface-variant flex items-center gap-1">
                <el-icon size="12">
                  <Grid />
                </el-icon> 模板: {{ sceneLabel(resume.scene) }} · {{ templateName(resume.templateId) }}
              </p>
              <p class="text-[11px] text-outline mt-2">
                {{ formatTime(resume.updatedAt || resume.createdAt) }}
              </p>
            </div>
            <el-dropdown
              trigger="click"
              @command="(cmd: string) => handleResumeAction(cmd, resume)"
              @click.stop
            >
              <button
                class="p-1 hover:bg-surface-container rounded transition-colors"
                @click.stop
              >
                <el-icon><More /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="copy">
                    复制
                  </el-dropdown-item>
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

        <!-- Create New Card -->
        <button
          class="aspect-[3/4] bg-background border-2 border-dashed border-outline-variant rounded-xl flex flex-col items-center justify-center group hover:border-primary transition-colors cursor-pointer"
          @click="goCreate"
        >
          <div class="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center group-hover:bg-primary group-hover:text-white transition-all mb-4">
            <el-icon size="24">
              <Plus />
            </el-icon>
          </div>
          <span class="font-title-md text-on-surface">新建简历</span>
          <span class="text-label-md text-on-surface-variant">选择模板，快速创建</span>
        </button>
      </div>
    </div>

    <!-- Sidebar / Inspector Drawer -->
    <aside class="w-full lg:w-[360px] flex flex-col gap-stack-lg">
      <!-- Empty placeholder: real insights will be shown here once available -->
      <div class="bg-surface-container-lowest rounded-2xl border border-outline-variant p-8 text-center">
        <el-icon
          size="40"
          class="text-outline mb-3"
        >
          <InfoFilled />
        </el-icon>
        <h2 class="font-title-md text-title-md text-on-surface">
          求职准备度
        </h2>
        <p class="text-body-md text-on-surface-variant mt-2 leading-relaxed">
          选择一份简历后，可查看 AI 给出的评估与改进建议
        </p>
        <RouterLink
          to="/workbench/ai-review"
          class="mt-4 inline-flex items-center gap-1 text-primary text-label-md font-label-md hover:underline"
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  Upload
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const resumes = ref<Resume[]>([])
const keyword = ref((route.query.q as string) || '')
const currentTab = ref<'recent' | 'all'>('recent')
const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Resume | null>(null)
const templateNameMap = ref<Record<string, string>>({})
const importDialogVisible = ref(false)
const importing = ref(false)
const importForm = ref<{ format: 'json' | 'markdown'; title: string; content: string }>({
  format: 'markdown',
  title: '',
  content: ''
})

function openImportDialog() {
  importForm.value = { format: 'markdown', title: '', content: '' }
  importDialogVisible.value = true
}

async function handleImport() {
  const content = importForm.value.content.trim()
  if (!content) {
    ElMessage.warning('请粘贴导入内容')
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
    ElMessage.success('导入成功')
    router.push(`/workbench/editor/${resume.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    importing.value = false
  }
}

const tabs = [
  { label: '最近编辑', value: 'recent' as const },
  { label: '全部简历', value: 'all' as const }
]

const filteredResumes = computed(() => {
  let list = [...resumes.value]
  if (currentTab.value === 'recent') {
    list.sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
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

function templateName(id?: string) {
  if (!id) return '默认'
  return templateNameMap.value[id] || id
}

function sceneLabel(s: string) {
  const map: Record<string, string> = {
    campus_recruitment: '校招',
    internship: '实习',
    social_recruitment: '社招',
    postgraduate_reexam: '考研',
    project_application: '项目',
    custom: '自定义'
  }
  return map[s] || '默认'
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
  } catch {
    return // 用户取消
  }
  try {
    await resumeApi.remove(resume.id)
    ElMessage.success('已删除')
    fetchResumes()
  } catch {
    ElMessage.error('删除失败')
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
    const data = await resumeApi.list(1, 50)
    resumes.value = data?.records || []
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

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchResumes(), loadTemplateNames()])
  loading.value = false
})
</script>

<style scoped lang="scss">
.resume-card:hover .resume-overlay {
  opacity: 1;
}
</style>
