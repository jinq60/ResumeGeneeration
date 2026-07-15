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
import { ref, watch } from 'vue'
import { Loading, Warning } from '@element-plus/icons-vue'
import axios from 'axios'

interface Props {
  resumeId?: string
  resume?: any
  templateId?: string
}

const props = defineProps<Props>()

const html = ref('')
const iframeRef = ref<HTMLIFrameElement>()
const loading = ref(false)
const error = ref('')

async function loadPreview() {
  loading.value = true
  error.value = ''

  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    const token = localStorage.getItem('access_token')

    if (props.resumeId) {
      // 通过resumeId加载
      const response = await axios.get<string>(
        `${baseURL}/resumes/${props.resumeId}/preview`,
        {
          params: props.templateId ? { templateId: props.templateId } : undefined,
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
          responseType: 'text',
          transformResponse: []
        }
      )
      html.value = response.data
    } else if (props.resume) {
      // 直接使用resume对象（用于编辑器实时预览）
      const response = await axios.post<string>(
        `${baseURL}/resumes/preview`,
        {
          resume: props.resume,
          templateId: props.templateId || props.resume.templateId
        },
        {
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
          responseType: 'text',
          transformResponse: []
        }
      )
      html.value = response.data
    } else {
      error.value = '缺少简历数据'
      return
    }
  } catch (e: any) {
    error.value = e.message || '预览加载失败，请稍后重试'
    console.error('预览加载失败', e)
  } finally {
    loading.value = false
  }
}

function handleIframeLoad() {
  // iframe加载完成后的处理
}

watch(
  () => [props.resumeId, props.resume, props.templateId],
  () => loadPreview(),
  { immediate: true }
)
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
