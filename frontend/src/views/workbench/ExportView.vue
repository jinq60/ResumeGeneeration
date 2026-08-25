<template>
  <div class="flex flex-col h-screen bg-background animate-fade-in-up">
    <header class="h-14 shrink-0 px-6 bg-card border-b border-border flex items-center justify-between">
      <div class="flex items-center gap-3">
        <button
          class="w-8 h-8 bg-transparent border border-border rounded-xl flex items-center justify-center text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors"
          title="返回"
          @click="router.back()"
        >
          <el-icon size="16">
            <ArrowLeft />
          </el-icon>
        </button>
        <h1 class="text-xl font-medium text-foreground">
          导出 PDF
        </h1>
      </div>
      <button
          class="border border-border rounded-xl hover:bg-secondary text-muted-foreground px-4 py-2 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          :disabled="!resume"
          @click="goEditor"
        >
          <el-icon size="14">
            <Edit />
          </el-icon>
          <span>继续编辑</span>
        </button>
    </header>

    <div class="flex-1 grid grid-cols-1 lg:grid-cols-[1fr_460px] min-h-0">
      <!-- 预览 -->
      <section class="bg-secondary/30 flex flex-col items-center p-8 overflow-auto">
        <div class="w-full max-w-[210mm] bg-card rounded-2xl shadow-xl min-h-[600px]">
          <ResumePreview
            v-if="resume"
            :resume="resume"
            :template-id="selectedTemplateId || undefined"
          />
          <div
            v-else
            class="h-[600px] flex flex-col items-center justify-center gap-3 text-muted-foreground text-sm"
          >
            <el-icon
              size="28"
              class="text-muted-foreground"
            >
              <Document />
            </el-icon>
            <p>加载简历中…</p>
          </div>
        </div>
        <div class="mt-3 text-[11px] text-muted-foreground font-mono tabular-nums">
          A4 · 210 × 297mm · 模板 {{ selectedTemplateId || 'default' }}
        </div>
      </section>

      <!-- 设置面板 -->
      <section class="bg-card border-l border-border p-6 overflow-y-auto flex flex-col">
        <h2 class="text-xl font-medium text-foreground">
          导出设置
        </h2>
        <p class="mt-1 text-sm text-muted-foreground">
          导出后可在下载中心获取。
        </p>
        <button
          class="mt-2 self-start text-sm font-medium text-foreground hover:text-muted-foreground transition-colors"
          @click="router.push('/workbench/downloads')"
        >
          前往下载中心 →
        </button>

        <div class="mt-5 flex flex-col gap-1.5">
          <label class="text-sm font-medium text-foreground">选择模板</label>
          <div class="relative">
            <select
              v-model="selectedTemplateId"
              class="w-full px-3 py-2.5 bg-secondary border border-transparent rounded-xl text-sm text-foreground focus:outline-none focus:bg-card focus:border-border transition-all appearance-none"
              @change="handleTemplateChange"
            >
              <option
                v-for="t in templates"
                :key="t.id"
                :value="t.id"
              >
                {{ t.name }}{{ t.isRecommended ? ' · 推荐' : '' }}
              </option>
            </select>
            <span class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none">
              <el-icon size="14">
                <ArrowDown />
              </el-icon>
            </span>
          </div>
        </div>

        <div class="mt-5 flex flex-col gap-1.5">
          <label class="text-sm font-medium text-foreground">导出格式</label>
          <el-radio-group
            v-model="exportFormat"
            class="w-full"
          >
            <el-radio-button
              value="pdf"
              label="PDF（推荐）"
            />
            <el-radio-button
              value="word"
              label="Word"
            />
            <el-radio-button
              value="markdown"
              label="Markdown"
            />
          </el-radio-group>
        </div>

        <div class="mt-5 flex flex-col gap-1.5">
          <label class="text-sm font-medium text-foreground">文件名</label>
          <div class="relative">
            <input
              v-model="exportForm.fileName"
              type="text"
              class="w-full px-3 py-2.5 pr-12 bg-secondary border border-transparent rounded-xl text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:bg-card focus:border-border transition-all"
              placeholder="文件名"
            >
            <span class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm pointer-events-none font-mono tabular-nums">.{{ exportFormat === 'pdf' ? 'pdf' : exportFormat === 'word' ? 'docx' : 'md' }}</span>
          </div>
        </div>

        <button
          class="mt-6 w-full bg-foreground text-primary-foreground px-4 py-3 rounded-xl font-medium text-sm flex items-center justify-center gap-2 shadow-lg hover:opacity-90 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!selectedTemplateId || exporting || (exportFormat !== 'pdf' && directExporting)"
          @click="handleExport"
        >
          <el-icon
            v-if="exporting || directExporting"
            class="animate-spin"
            size="16"
          >
            <Loading />
          </el-icon>
          <el-icon
            v-else
            size="14"
          >
            <Download />
          </el-icon>
          <span>{{ exporting || directExporting ? '导出中…' : '导出 ' + formatLabel }}</span>
        </button>

        <hr class="my-6 border-t border-border">

        <!-- 任务状态 -->
        <div
          v-if="exportTask"
          class="p-5 bg-secondary rounded-2xl"
        >
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-lg font-medium text-foreground">
              导出任务
            </h3>
            <span :class="['text-[11px] px-2.5 py-0.5 rounded-full font-medium', taskTagClass]">
              {{ getTaskStatusText() }}
            </span>
          </div>

          <ul class="list-none p-0 mb-4 flex flex-col gap-1.5">
            <li
              v-if="exportTask.completedAt"
              class="flex justify-between text-xs text-muted-foreground font-mono tabular-nums"
            >
              <span>完成时间</span><span>{{ formatDate(exportTask.completedAt) }}</span>
            </li>
            <li
              v-if="exportTask.fileSize"
              class="flex justify-between text-xs text-muted-foreground font-mono tabular-nums"
            >
              <span>文件大小</span><span>{{ formatFileSize(exportTask.fileSize) }}</span>
            </li>
            <li
              v-if="exportTask.errorMsg"
              class="flex justify-between text-xs"
            >
              <span class="text-muted-foreground">错误</span><span class="text-destructive max-w-[70%] text-right">{{ exportTask.errorMsg }}</span>
            </li>
          </ul>

          <div
            v-if="exportTask.status === 'success'"
            class="flex gap-2 flex-wrap"
          >
            <button
              class="bg-foreground text-primary-foreground px-4 py-2 rounded-xl font-medium text-sm flex items-center gap-2 shadow-lg hover:opacity-90 active:scale-[0.98] transition-all"
              @click="handleDownload"
            >
              <el-icon size="14">
                <Download />
              </el-icon>
              <span>下载 PDF</span>
            </button>
            <button
              class="border border-border rounded-xl hover:bg-card text-muted-foreground px-4 py-2 text-sm font-medium transition-colors"
              @click="handleExport"
            >
              重新导出
            </button>
          </div>
          <div
            v-else-if="exportTask.status === 'failed'"
            class="flex gap-2 flex-wrap"
          >
            <button
              class="bg-foreground text-primary-foreground px-4 py-2 rounded-xl font-medium text-sm flex items-center gap-2 shadow-lg hover:opacity-90 active:scale-[0.98] transition-all"
              @click="handleExport"
            >
              重试
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowDown, Download, Edit, Document, Loading } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import { templateApi } from '@/api/template'
import { pdfApi } from '@/api/pdf'
import { exportApi } from '@/api/export'
import type { Template } from '@/api/template'
import type { PdfTask } from '@/api/pdf'
import type { Resume } from '@/types/resume'
import ResumePreview from '@/components/preview/ResumePreview.vue'
import { addPdfTask, updatePdfTask } from '@/utils/download'

const router = useRouter()
const route = useRoute()
const resumeId = (route.params.id || route.query.resumeId) as string

const resume = ref<Resume | null>(null)
const templates = ref<Template[]>([])
const selectedTemplateId = ref('')
const exporting = ref(false)
const directExporting = ref(false)
const exportFormat = ref<'pdf' | 'word' | 'markdown'>('pdf')
const exportTask = ref<PdfTask | null>(null)
const exportForm = ref({ fileName: '' })
let taskPollingTimer: number | null = null

const formatLabel = computed(() => {
  const map: Record<string, string> = { pdf: 'PDF', word: 'Word', markdown: 'Markdown' }
  return map[exportFormat.value]
})

const taskTagClass = computed(() => {
  const status = exportTask.value?.status
  if (status === 'pending') return 'bg-secondary text-muted-foreground'
  if (status === 'processing') return 'bg-secondary text-foreground'
  if (status === 'success') return 'bg-foreground text-primary-foreground'
  if (status === 'failed') return 'bg-destructive/10 text-destructive'
  return ''
})

async function loadResume() {
  try {
    resume.value = await resumeApi.get(resumeId)
    exportForm.value.fileName = resume.value.title || '简历'
    selectedTemplateId.value = resume.value.templateId
  } catch (e: any) {
    ElMessage.error(e.message || '加载简历失败')
  }
}

async function loadTemplates() {
  try {
    templates.value = await templateApi.list()
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板失败')
  }
}

function handleTemplateChange() {
  exportTask.value = null
  if (taskPollingTimer) {
    clearInterval(taskPollingTimer)
    taskPollingTimer = null
  }
}

function normalizeFileName(raw: string): string {
  let name = (raw || '简历').trim()
  if (name.toLowerCase().endsWith('.pdf')) {
    name = name.slice(0, -4)
  }
  return name || '简历'
}

async function handleExport() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择模板')
    return
  }
  if (exportFormat.value === 'pdf') {
    await exportPdf()
  } else {
    await exportDirect()
  }
}

async function exportDirect() {
  directExporting.value = true
  try {
    if (exportFormat.value === 'word') {
      const fileName = await exportApi.downloadWord(resumeId)
      ElMessage.success(`Word 已下载：${fileName}`)
    } else {
      const fileName = await exportApi.downloadMarkdown(resumeId)
      ElMessage.success(`Markdown 已下载：${fileName}`)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    directExporting.value = false
  }
}

async function exportPdf() {
  exporting.value = true
  exportTask.value = null
  try {
    const response = await pdfApi.export(resumeId, selectedTemplateId.value)
    const task: PdfTask = {
      taskId: response.taskId,
      status: response.status as PdfTask['status'],
      fileName: `${normalizeFileName(exportForm.value.fileName)}.pdf`,
      createdAt: new Date().toISOString()
    }
    exportTask.value = task
    addPdfTask(task)
    startTaskPolling(response.taskId)
    ElMessage.success('导出任务已创建')
  } catch (e: any) {
    ElMessage.error(e.message || '创建导出任务失败')
  } finally {
    exporting.value = false
  }
}

function startTaskPolling(taskId: string) {
  if (taskPollingTimer) clearInterval(taskPollingTimer)
  let consecutiveFailures = 0
  taskPollingTimer = window.setInterval(async () => {
    try {
      const task = await pdfApi.getTask(taskId)
      consecutiveFailures = 0
      exportTask.value = task
      updatePdfTask(taskId, task)
      if (task.status === 'success' || task.status === 'failed') {
        if (taskPollingTimer) { clearInterval(taskPollingTimer); taskPollingTimer = null }
        if (task.status === 'success') ElMessage.success('PDF 已生成，可以下载了。')
        else ElMessage.error('导出失败')
      }
    } catch {
      // 连续失败超过 5 次停止轮询，避免网络异常时无限空转
      consecutiveFailures++
      if (consecutiveFailures >= 5) {
        if (taskPollingTimer) { clearInterval(taskPollingTimer); taskPollingTimer = null }
        ElMessage.error('网络异常，请稍后刷新查看结果')
      }
    }
  }, 2000)
}

async function handleDownload() {
  if (!exportTask.value) return
  try {
    const response = await pdfApi.download(exportTask.value.taskId)
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${normalizeFileName(exportForm.value.fileName)}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e: any) {
    ElMessage.error(e.message || '下载失败')
  }
}

function getTaskStatusText(): string {
  const map: Record<string, string> = {
    pending: '等待处理',
    processing: '处理中',
    success: '完成',
    failed: '失败'
  }
  return exportTask.value ? (map[exportTask.value.status] || exportTask.value.status) : ''
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString()
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

function goEditor() {
  if (resume.value) router.push(`/workbench/editor/${resume.value.id}`)
}

onMounted(() => {
  loadResume()
  loadTemplates()
})

onUnmounted(() => {
  if (taskPollingTimer) clearInterval(taskPollingTimer)
})
</script>
