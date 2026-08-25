<template>
  <div class="resume-preview-container">
    <div
      v-if="loading"
      class="loading-state"
    >
      <el-icon
        class="is-loading"
        :size="32"
      >
        <Loading />
      </el-icon>
      <p>正在加载预览...</p>
    </div>

    <div
      v-else-if="error"
      class="error-state"
    >
      <el-icon
        :size="32"
        color="#f56c6c"
      >
        <Warning />
      </el-icon>
      <p>{{ error }}</p>
      <el-button
        type="primary"
        size="small"
        @click="loadPreview"
      >
        重试
      </el-button>
    </div>

    <iframe
      v-else
      ref="iframeRef"
      class="resume-preview"
      :srcdoc="html"
      frameborder="0"
      sandbox="allow-same-origin"
      @load="handleIframeLoad"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { Loading, Warning } from '@element-plus/icons-vue'
import { fetchResumePreview, fetchLivePreview } from '@/api/preview'
import type { Resume } from '@/types/resume'

interface Props {
  resumeId?: string
  resume?: Resume
  templateId?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  loaded: [contentHeight: number]
}>()

const html = ref('')
const iframeRef = ref<HTMLIFrameElement>()
const loading = ref(false)
const error = ref('')

let debounceTimer: ReturnType<typeof setTimeout> | null = null
let loadSeq = 0

async function loadPreview() {
  loading.value = true
  error.value = ''

  const seq = ++loadSeq
  try {
    if (props.resumeId && !props.resume) {
      html.value = await fetchResumePreview(props.resumeId, props.templateId)
    } else if (props.resume) {
      html.value = await fetchLivePreview(props.resume, props.templateId || props.resume.templateId)
    } else {
      error.value = '缺少简历数据'
      return
    }
  } catch (e: any) {
    if (seq === loadSeq) {
      error.value = e.message || '预览加载失败，请稍后重试'
      console.error('预览加载失败', e)
    }
  } finally {
    if (seq === loadSeq) {
      loading.value = false
    }
  }
}

function scheduleLoadPreview() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(loadPreview, 300)
}

function handleIframeLoad() {
  const document = iframeRef.value?.contentDocument
  const fittedHeight = applyOnePageFit(document)
  if (fittedHeight !== null) {
    emit('loaded', fittedHeight)
    return
  }
  const contentHeight = Math.max(
    document?.documentElement?.scrollHeight || 0,
    document?.body?.scrollHeight || 0
  )
  emit('loaded', contentHeight)
}

function applyOnePageFit(document: Document | null | undefined): number | null {
  if (!document || typeof document.querySelector !== 'function') {
    return null
  }
  const page = document.querySelector<HTMLElement>('.resume-page[data-auto-one-page="true"]')
  if (!page || !page.style) {
    return null
  }

  page.style.setProperty('--resume-fit-scale', '1')
  const originalMinHeight = page.style.minHeight
  page.style.minHeight = '0'
  const contentHeight = page.scrollHeight
  page.style.minHeight = originalMinHeight

  const a4Height = 297 * 96 / 25.4
  const scale = Math.min(1, a4Height / Math.max(contentHeight, 1))
  page.style.setProperty('--resume-fit-scale', String(scale))

  const measuredHeight = page.getBoundingClientRect().height
  return Math.max(measuredHeight, contentHeight * scale)
}

// 深监听 resume 内容（编辑器原地修改 sections 时也能触发），避免引用陷阱
watch(
  () => props.resume ? JSON.stringify(props.resume) : null,
  () => {
    if (props.resumeId && !props.resume) {
      loadPreview()
    } else if (props.resume) {
      scheduleLoadPreview()
    }
  },
  { immediate: true }
)

watch(
  () => [props.resumeId, props.templateId],
  () => {
    // 模板切换（无论是否携带 resume 对象）都需重新加载，保证预览与导出模板一致
    loadPreview()
  }
)

onUnmounted(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
  }
})
</script>

<style scoped lang="scss">
.resume-preview-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 24px;
  background: #f5f7fa;
  min-height: 100%;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: #909399;

  p {
    margin: 16px 0 0;
    font-size: 14px;
  }
}

.resume-preview {
  width: 210mm;
  min-height: 297mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: block;
  border: none;
}
</style>
