<template>
  <div class="flex flex-col h-screen bg-surface-container-low">
    <header class="h-14 shrink-0 px-6 bg-surface-container-lowest border-b border-outline-variant flex items-center justify-between">
      <div class="flex items-center gap-3">
        <button
          class="w-8 h-8 bg-transparent border border-outline-variant rounded-lg flex items-center justify-center text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface transition-colors"
          title="返回"
          @click="router.back()"
        >
          <el-icon size="16">
            <ArrowLeft />
          </el-icon>
        </button>
        <h1 class="text-title-lg font-title-lg text-on-surface">
          导出 PDF
        </h1>
      </div>
      <button
        class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
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
      <section class="bg-surface-container-low flex flex-col items-center p-8 overflow-auto">
        <div class="w-full max-w-[210mm] bg-surface-container-lowest rounded-sm shadow-md min-h-[600px]">
          <ResumePreview
            v-if="resume"
            :resume="resume"
            :template-id="selectedTemplateId || undefined"
          />
          <div
            v-else
            class="h-[600px] flex flex-col items-center justify-center gap-3 text-on-surface-variant text-sm"
          >
            <el-icon
              size="28"
              class="text-outline"
            >
              <Document />
            </el-icon>
            <p>加载简历中…</p>
          </div>
        </div>
        <div class="mt-3 text-[11px] text-on-surface-variant font-mono tabular-nums">
          A4 · 210 × 297mm · 模板 {{ selectedTemplateId || 'default' }}
        </div>
      </section>

      <!-- 设置面板 -->
      <section class="bg-surface-container-lowest border-l border-outline-variant p-6 overflow-y-auto flex flex-col">
        <h2 class="text-title-lg font-title-lg text-on-surface">
          导出设置
        </h2>
        <p class="mt-1 text-sm text-on-surface-variant">
          导出后可在下载中心获取。
        </p>

        <div class="mt-5 flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-on-surface-variant tracking-wide">选择模板</label>
          <select
            v-model="selectedTemplateId"
            class="w-full px-3 py-2.5 bg-surface-container-low border border-outline-variant rounded-lg text-sm text-on-surface focus:outline-none focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
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
        </div>

        <div class="mt-5 flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-on-surface-variant tracking-wide">导出格式</label>
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
          <label class="text-xs font-semibold text-on-surface-variant tracking-wide">文件名</label>
          <div class="relative">
            <input
              v-model="exportForm.fileName"
              type="text"
              class="w-full px-3 py-2.5 pr-12 bg-surface-container-low border border-outline-variant rounded-lg text-sm text-on-surface focus:outline-none focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
              placeholder="文件名"
            >
            <span class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-sm pointer-events-none font-mono tabular-nums">.{{ exportFormat === 'pdf' ? 'pdf' : exportFormat === 'word' ? 'docx' : 'md' }}</span>
          </div>
        </div>

        <button
          class="mt-6 bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center justify-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
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

        <hr class="my-6 border-none border-t border-outline-variant">

        <!-- 任务状态 -->
        <div
          v-if="exportTask"
          class="p-4 bg-surface-container-low rounded-lg"
        >
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-title-md font-title-md text-on-surface">
              导出任务
            </h3>
            <span :class="['text-[11px] px-2 py-0.5 rounded-full font-medium', taskTagClass]">
              {{ getTaskStatusText() }}
            </span>
          </div>

          <ul class="list-none p-0 mb-3 flex flex-col gap-1">
            <li
              v-if="exportTask.completedAt"
              class="flex justify-between text-xs text-on-surface-variant font-mono tabular-nums"
            >
              <span>完成时间</span><span>{{ formatDate(exportTask.completedAt) }}</span>
            </li>
            <li
              v-if="exportTask.fileSize"
              class="flex justify-between text-xs text-on-surface-variant font-mono tabular-nums"
            >
              <span>文件大小</span><span>{{ formatFileSize(exportTask.fileSize) }}</span>
            </li>
            <li
              v-if="exportTask.errorMsg"
              class="flex justify-between text-xs"
            >
              <span class="text-on-surface-variant">错误</span><span class="text-error max-w-[70%] text-right">{{ exportTask.errorMsg }}</span>
            </li>
          </ul>

          <div
            v-if="exportTask.status === 'success'"
            class="flex gap-2 flex-wrap"
          >
            <button
              class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              @click="handleDownload"
            >
              <el-icon size="14">
                <Download />
              </el-icon>
              <span>下载 PDF</span>
            </button>
            <button
              class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
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
              class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
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
import { ArrowLeft, Download, Edit, Document, Loading } from '@element-plus/icons-vue'
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
  if (status === 'pending') return 'bg-surface-container text-on-surface-variant'
  if (status === 'processing') return 'bg-primary-fixed text-primary'
  if (status === 'success') return 'bg-secondary-container text-secondary'
  if (status === 'failed') return 'bg-error-container text-error'
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
  taskPollingTimer = window.setInterval(async () => {
    try {
      const task = await pdfApi.getTask(taskId)
      exportTask.value = task
      updatePdfTask(taskId, task)
      if (task.status === 'success' || task.status === 'failed') {
        if (taskPollingTimer) { clearInterval(taskPollingTimer); taskPollingTimer = null }
        if (task.status === 'success') ElMessage.success('PDF 已生成，可以下载了。')
        else ElMessage.error('导出失败')
      }
    } catch {
      /* swallow polling error */
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
