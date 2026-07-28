<template>
  <MainLayout>
    <main class="max-w-[1440px] mx-auto px-margin-page py-stack-lg">
      <header class="flex justify-between items-end mb-stack-lg gap-gutter">
        <div class="flex flex-col">
          <h1 class="text-headline-md font-headline-md text-on-surface">
            下载中心
          </h1>
          <p class="text-body-md text-on-surface-variant mt-1">
            查看并管理你的 PDF 导出与头像优化任务
          </p>
        </div>
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 flex items-center gap-2"
          @click="loadTasks"
        >
          <el-icon size="14">
            <Refresh />
          </el-icon>
          <span>刷新</span>
        </button>
      </header>

      <div class="max-w-[1080px] mx-auto w-full flex flex-col gap-stack-lg">
        <div class="flex gap-2 bg-surface-container-lowest p-2 rounded-xl border border-outline-variant/30 w-fit">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="[
              'flex items-center gap-2 px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
              activeTab === tab.value
                ? 'bg-primary text-on-primary'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            ]"
            @click="activeTab = tab.value"
          >
            <el-icon size="16">
              <component :is="tab.icon" />
            </el-icon>
            <span>{{ tab.label }}</span>
            <span
              :class="[
                'min-w-[18px] h-[18px] px-1.5 rounded-full text-[11px] font-bold inline-flex items-center justify-center',
                activeTab === tab.value ? 'bg-white/25 text-white' : 'bg-surface-container text-on-surface-variant'
              ]"
            >
              {{ tab.count }}
            </span>
          </button>
        </div>

        <div class="flex flex-col gap-stack-md">
          <!-- PDF 列表 -->
          <template v-if="activeTab === 'pdf'">
            <div
              v-for="task in pdfTasks"
              :key="task.taskId"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-5 flex items-center gap-4 hover:shadow-sm transition-all"
            >
              <div class="w-12 h-12 rounded-lg bg-error-container text-error flex items-center justify-center flex-shrink-0">
                <el-icon size="24">
                  <Document />
                </el-icon>
              </div>
              <div class="flex-1 min-w-0 flex flex-col gap-1.5">
                <div class="text-body-md font-semibold text-on-surface truncate">
                  {{ task.fileName || '简历.pdf' }}
                </div>
                <div class="flex items-center gap-3 text-label-md text-on-surface-variant flex-wrap">
                  <span :class="statusClass(task.status)">
                    {{ getStatusText(task.status) }}
                  </span>
                  <span>{{ formatDate(task.createdAt) }}</span>
                  <span v-if="task.fileSize">{{ formatFileSize(task.fileSize) }}</span>
                </div>
                <div
                  v-if="task.errorMsg"
                  class="text-label-md text-error"
                >
                  {{ task.errorMsg }}
                </div>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button
                  v-if="task.status === 'success'"
                  class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
                  @click="downloadPdf(task)"
                >
                  <el-icon size="14">
                    <Download />
                  </el-icon>
                  <span>下载</span>
                </button>
                <button
                  v-if="task.status === 'success' || task.status === 'failed'"
                  class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
                  @click="removePdf(task.taskId)"
                >
                  {{ task.status === 'failed' ? '移除' : '删除' }}
                </button>
              </div>
            </div>
            <div
              v-if="pdfTasks.length === 0"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 flex flex-col items-center gap-4 text-center text-on-surface-variant"
            >
              <el-icon size="48">
                <Document />
              </el-icon>
              <p class="text-body-md">
                暂无 PDF 导出任务
              </p>
              <RouterLink
                to="/resumes"
                class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              >
                去导出简历
              </RouterLink>
            </div>
          </template>

          <!-- 头像优化列表 -->
          <template v-if="activeTab === 'avatar'">
            <div
              v-for="task in avatarTasks"
              :key="task.taskId"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-5 flex items-center gap-4 hover:shadow-sm transition-all"
            >
              <div class="w-[72px] h-24 rounded-md bg-surface-container flex items-center justify-center overflow-hidden flex-shrink-0">
                <img
                  v-if="task.resultImageUrl"
                  class="w-full h-full object-cover"
                  :src="task.resultImageUrl"
                  alt="优化后"
                >
                <el-icon
                  v-else
                  size="24"
                  class="text-outline"
                >
                  <Picture />
                </el-icon>
              </div>
              <div class="flex-1 min-w-0 flex flex-col gap-1.5">
                <div class="text-body-md font-semibold text-on-surface">
                  一寸照优化
                </div>
                <div class="flex items-center gap-3 text-label-md text-on-surface-variant flex-wrap">
                  <span :class="statusClass(task.status)">
                    {{ getStatusText(task.status) }}
                  </span>
                  <span>{{ formatDate(task.createdAt) }}</span>
                </div>
                <div
                  v-if="task.errorMsg"
                  class="text-label-md text-error"
                >
                  {{ task.errorMsg }}
                </div>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button
                  v-if="task.status === 'success' && task.resultImageUrl"
                  class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
                  @click="downloadAvatar(task)"
                >
                  <el-icon size="14">
                    <Download />
                  </el-icon>
                  <span>下载</span>
                </button>
                <button
                  v-if="task.status === 'success' || task.status === 'failed'"
                  class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
                  @click="removeAvatar(task.taskId)"
                >
                  删除
                </button>
              </div>
            </div>
            <div
              v-if="avatarTasks.length === 0"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 flex flex-col items-center gap-4 text-center text-on-surface-variant"
            >
              <el-icon size="48">
                <Picture />
              </el-icon>
              <p class="text-body-md">
                暂无头像优化任务
              </p>
              <RouterLink
                to="/avatar/upload"
                class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              >
                去上传头像
              </RouterLink>
            </div>
          </template>
        </div>
      </div>
    </main>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Picture, Download, Refresh } from '@element-plus/icons-vue'
import { pdfApi } from '@/api/pdf'
import type { PdfTask } from '@/api/pdf'
import type { AvatarTask } from '@/api/avatar'
import {
  getPdfTasks,
  getAvatarTasks,
  removePdfTask,
  removeAvatarTask
} from '@/utils/download'
import MainLayout from '@/components/layout/MainLayout.vue'

const activeTab = ref<'pdf' | 'avatar'>('pdf')
const pdfTasks = ref<PdfTask[]>([])
const avatarTasks = ref<AvatarTask[]>([])

const samplePdfTasks: PdfTask[] = [
  { taskId: 'pdf-sample-1', status: 'success', fileName: 'Java 后端工程师.pdf', fileSize: 245760, createdAt: new Date(Date.now() - 1000 * 60 * 60).toISOString(), completedAt: new Date(Date.now() - 1000 * 60 * 55).toISOString() },
  { taskId: 'pdf-sample-2', status: 'processing', fileName: '产品经理.pdf', createdAt: new Date(Date.now() - 1000 * 60 * 5).toISOString() }
]

const sampleAvatarTasks: AvatarTask[] = [
  { taskId: 'avatar-sample-1', status: 'success', sourceImageUrl: 'https://placehold.co/300x400/2962FF/fff?text=Original', resultImageUrl: 'https://placehold.co/300x400/27866F/fff?text=Optimized', createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), completedAt: new Date(Date.now() - 1000 * 60 * 60 * 1.9).toISOString() }
]

const tabs = computed(() => [
  { value: 'pdf' as const, label: 'PDF 导出', icon: Document, count: pdfTasks.value.length },
  { value: 'avatar' as const, label: '头像优化', icon: Picture, count: avatarTasks.value.length }
])

function loadTasks() {
  pdfTasks.value = getPdfTasks()
  avatarTasks.value = getAvatarTasks()
  if (pdfTasks.value.length === 0 && avatarTasks.value.length === 0) {
    pdfTasks.value = samplePdfTasks
    avatarTasks.value = sampleAvatarTasks
  }
  ElMessage.success('已刷新')
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    pending: '等待处理',
    processing: '处理中',
    success: '完成',
    failed: '失败'
  }
  return map[status] || status
}

function statusClass(status: string): string {
  const base = 'text-[10px] font-bold px-2 py-0.5 rounded border '
  switch (status) {
    case 'pending':
      return base + 'bg-surface-container text-on-surface-variant border-outline-variant'
    case 'processing':
      return base + 'bg-primary/10 text-primary border-primary/20'
    case 'success':
      return base + 'bg-secondary/10 text-secondary border-secondary/20'
    case 'failed':
      return base + 'bg-error/10 text-error border-error/20'
    default:
      return base + 'bg-surface-container text-on-surface-variant border-outline-variant'
  }
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString()
}

function formatFileSize(bytes?: number): string {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

async function downloadPdf(task: PdfTask) {
  if (!task.taskId) return
  try {
    const response = await pdfApi.download(task.taskId)
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = task.fileName || '简历.pdf'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e: any) {
    ElMessage.error(e.message || '下载失败')
  }
}

function downloadAvatar(task: AvatarTask) {
  if (!task.resultImageUrl) return
  const link = document.createElement('a')
  link.href = task.resultImageUrl
  link.download = 'optimized-avatar.jpg'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success('下载成功')
}

function removePdf(taskId: string) {
  removePdfTask(taskId)
  pdfTasks.value = getPdfTasks()
}

function removeAvatar(taskId: string) {
  removeAvatarTask(taskId)
  avatarTasks.value = getAvatarTasks()
}

onMounted(() => {
  loadTasks()
})
</script>
