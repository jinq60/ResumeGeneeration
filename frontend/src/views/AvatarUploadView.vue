<template>
  <MainLayout>
    <div class="flex flex-col min-h-screen bg-surface-container-low">
      <header class="px-margin-page py-stack-lg pb-stack-md flex justify-between items-center">
        <div class="flex items-center gap-3">
          <button
            class="w-8 h-8 bg-surface-container-lowest border border-outline-variant rounded-lg flex items-center justify-center text-on-surface-variant hover:text-on-surface transition-colors"
            title="返回"
            @click="router.back()"
          >
            <el-icon size="16">
              <ArrowLeft />
            </el-icon>
          </button>
          <h1 class="text-headline-md font-headline-md text-on-surface">
            头像上传与一寸照优化
          </h1>
        </div>
      </header>

      <div class="flex-1 max-w-[1080px] w-full mx-auto px-margin-page pb-margin-page grid grid-cols-1 lg:grid-cols-2 gap-gutter">
        <!-- 上传 + 设置 -->
        <section class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm flex flex-col">
          <h2 class="text-title-lg font-title-lg text-on-surface">
            上传自拍照
          </h2>
          <p class="mt-1 text-sm text-on-surface-variant">
            JPG / PNG / WEBP，≤ 10 MB
          </p>

          <el-upload
            class="avatar-uploader mt-4"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            accept="image/*"
            drag
          >
            <div
              v-if="!imageUrl"
              class="flex flex-col items-center gap-2 text-center"
            >
              <el-icon
                size="40"
                class="text-outline"
              >
                <UploadFilled />
              </el-icon>
              <p class="text-sm font-medium text-on-surface">
                点击或拖拽上传
              </p>
              <p class="text-xs text-on-surface-variant">
                AI 将帮你裁剪为一寸证件照
              </p>
            </div>
            <img
              v-else
              :src="imageUrl"
              class="max-w-[200px] max-h-[200px] rounded-lg shadow-sm"
              alt="头像"
            >
          </el-upload>

          <div
            v-if="imageUrl"
            class="mt-3"
          >
            <button
              class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
              @click="handleReupload"
            >
              重新上传
            </button>
          </div>

          <hr class="mt-5 border-t border-outline-variant">

          <h3 class="mt-4 text-sm font-semibold text-on-surface">
            优化设置
          </h3>

          <div class="mt-4 flex flex-col gap-2 items-start">
            <label class="text-xs font-semibold text-on-surface-variant tracking-wide">背景类型</label>
            <div class="flex flex-wrap gap-2">
              <label
                v-for="bg in ['white', 'blue', 'red']"
                :key="bg"
                class="relative cursor-pointer"
              >
                <input
                  v-model="optimizeForm.backgroundType"
                  type="radio"
                  :value="bg"
                  class="peer sr-only"
                >
                <span
                  :class="[
                    'inline-block px-3.5 py-1.5 rounded-full text-sm font-medium border border-outline-variant bg-surface-container-low text-on-surface-variant transition-colors peer-checked:bg-primary peer-checked:border-primary peer-checked:text-on-primary',
                    bgChipClass[bg]
                  ]"
                >{{ bgLabel(bg) }}</span>
              </label>
            </div>
          </div>

          <div class="mt-4 flex flex-col gap-2 items-start">
            <label class="text-xs font-semibold text-on-surface-variant tracking-wide">照片风格</label>
            <div class="flex flex-wrap gap-2">
              <label
                v-for="st in ['formal', 'natural', 'professional']"
                :key="st"
                class="relative cursor-pointer"
              >
                <input
                  v-model="optimizeForm.style"
                  type="radio"
                  :value="st"
                  class="peer sr-only"
                >
                <span class="inline-block px-3.5 py-1.5 rounded-full text-sm font-medium border border-outline-variant bg-surface-container-low text-on-surface-variant transition-colors peer-checked:bg-primary peer-checked:border-primary peer-checked:text-on-primary">{{ styleLabel(st) }}</span>
              </label>
            </div>
          </div>

          <div class="mt-4 flex flex-row justify-between items-center gap-4">
            <div>
              <div class="text-sm font-medium text-on-surface">
                保持身份特征
              </div>
              <div class="text-xs text-on-surface-variant">
                面部五官不变形
              </div>
            </div>
            <el-switch v-model="optimizeForm.keepIdentity" />
          </div>
          <div class="mt-4 flex flex-row justify-between items-center gap-4">
            <div>
              <div class="text-sm font-medium text-on-surface">
                增强清晰度
              </div>
              <div class="text-xs text-on-surface-variant">
                轻微超分
              </div>
            </div>
            <el-switch v-model="optimizeForm.enhanceQuality" />
          </div>
          <div class="mt-4 flex flex-row justify-between items-center gap-4">
            <div>
              <div class="text-sm font-medium text-on-surface">
                去除背景
              </div>
              <div class="text-xs text-on-surface-variant">
                抠图并替换背景色
              </div>
            </div>
            <el-switch v-model="optimizeForm.removeBackground" />
          </div>
          <div class="mt-4 flex flex-row justify-between items-center gap-4">
            <div>
              <div class="text-sm font-medium text-on-surface">
                提亮肤色
              </div>
              <div class="text-xs text-on-surface-variant">
                自然美颜
              </div>
            </div>
            <el-switch v-model="optimizeForm.brightenSkin" />
          </div>

          <button
            class="mt-5 bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center justify-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed self-stretch"
            :disabled="!imageUrl || optimizing"
            @click="handleOptimize"
          >
            <el-icon
              v-if="optimizing"
              class="animate-spin"
              size="14"
            >
              <Loading />
            </el-icon>
            <el-icon
              v-else
              size="14"
            >
              <MagicStick />
            </el-icon>
            <span>{{ optimizing ? '优化中…' : '开始优化' }}</span>
          </button>
        </section>

        <!-- 结果卡 -->
        <section
          v-if="optimizeTask"
          class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm flex flex-col"
        >
          <h2 class="text-title-lg font-title-lg text-on-surface">
            优化结果
          </h2>

          <div class="flex justify-between my-5">
            <div :class="['flex items-center gap-1.5 text-[11px] text-on-surface-variant flex-1', { 'text-primary': stepIndex === 0 }]">
              <span
                class="w-2 h-2 rounded-full"
                :class="stepIndex > 0 ? 'bg-secondary' : stepIndex === 0 ? 'bg-primary' : 'bg-surface-container'"
              /><span>上传</span>
            </div>
            <div :class="['flex items-center gap-1.5 text-[11px] text-on-surface-variant flex-1', { 'text-primary': stepIndex === 1 }]">
              <span
                class="w-2 h-2 rounded-full"
                :class="stepIndex > 1 ? 'bg-secondary' : stepIndex === 1 ? 'bg-primary' : 'bg-surface-container'"
              /><span>处理中</span>
            </div>
            <div :class="['flex items-center gap-1.5 text-[11px] text-on-surface-variant flex-1', { 'text-primary': stepIndex === 2 }]">
              <span
                class="w-2 h-2 rounded-full"
                :class="stepIndex > 2 ? 'bg-secondary' : stepIndex === 2 ? 'bg-primary' : 'bg-surface-container'"
              /><span>完成</span>
            </div>
          </div>

          <div
            v-if="optimizeTask.status === 'processing'"
            class="py-8 flex flex-col items-center gap-3 text-on-surface-variant text-sm"
          >
            <el-icon
              class="animate-spin text-primary"
              size="28"
            >
              <Loading />
            </el-icon>
            <p>AI 正在优化你的头像，请稍候…</p>
          </div>

          <div
            v-if="optimizeTask.status === 'success'"
            class="flex items-center gap-3 mb-4"
          >
            <div class="flex-1 text-center">
              <span class="block mb-1.5 text-[11px] text-on-surface-variant font-medium">原图</span>
              <img
                :src="optimizeTask.sourceImageUrl"
                class="w-full max-w-[200px] rounded-lg shadow-sm mx-auto"
                alt="原图"
              >
            </div>
            <div class="text-lg text-primary">
              →
            </div>
            <div class="flex-1 text-center">
              <span class="block mb-1.5 text-[11px] text-on-surface-variant font-medium">优化后</span>
              <img
                :src="optimizeTask.resultImageUrl"
                class="w-full max-w-[200px] rounded-lg shadow-sm mx-auto"
                alt="优化后"
              >
            </div>
          </div>

          <div
            v-if="optimizeTask.status === 'success'"
            class="flex flex-wrap gap-2 justify-center"
          >
            <button
              class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              @click="handleApplyToResume"
            >
              <el-icon size="14">
                <Check />
              </el-icon>
              <span>应用到简历</span>
            </button>
            <button
              class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
              @click="handleDownload"
            >
              下载
            </button>
            <button
              class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
              @click="handleReoptimize"
            >
              重新优化
            </button>
          </div>

          <div
            v-if="optimizeTask.status === 'failed'"
            class="py-6 flex flex-col items-center gap-1.5 text-on-surface-variant"
          >
            <span class="w-8 h-8 rounded-full bg-error-container text-error flex items-center justify-center font-bold">!</span>
            <p>优化失败</p>
            <p class="text-xs text-error mb-2">
              {{ optimizeTask.errorMsg }}
            </p>
            <button
              class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              @click="handleReoptimize"
            >
              重试
            </button>
          </div>
        </section>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, UploadFilled, MagicStick, Loading, Check } from '@element-plus/icons-vue'
import { avatarApi } from '@/api/avatar'
import type { OptimizeAvatarRequest, AvatarTask } from '@/api/avatar'
import MainLayout from '@/components/layout/MainLayout.vue'
import { addAvatarTask, updateAvatarTask } from '@/utils/download'

const router = useRouter()
const route = useRoute()
const resumeId = route.query.resumeId as string | undefined
const imageUrl = ref('')
const uploadedImageUrl = ref('')
const optimizing = ref(false)
const optimizeTask = ref<AvatarTask | null>(null)

const bgChipClass: Record<string, string> = {
  white: 'peer-checked:bg-surface-container-lowest peer-checked:text-on-surface',
  blue: 'peer-checked:bg-primary-fixed peer-checked:text-on-surface',
  red: 'peer-checked:bg-error-container peer-checked:text-on-surface'
}

const optimizeForm = ref<OptimizeAvatarRequest>({
  sourceImageUrl: '',
  backgroundType: 'white',
  style: 'formal',
  keepIdentity: true,
  enhanceQuality: true,
  removeBackground: true,
  brightenSkin: false
})

let taskPollingTimer: number | null = null

const stepIndex = computed(() => {
  if (!optimizeTask.value) return 0
  const s = optimizeTask.value.status
  if (s === 'pending') return 0
  if (s === 'processing') return 1
  if (s === 'success') return 2
  return 0
})

function bgLabel(bg: string) {
  return ({ white: '白底', blue: '蓝底', red: '红底' } as Record<string, string>)[bg] || bg
}
function styleLabel(st: string) {
  return ({ formal: '职业照', natural: '自然', professional: '商务' } as Record<string, string>)[st] || st
}

function beforeUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

async function handleFileChange(file: { raw: File }) {
  const reader = new FileReader()
  reader.onload = (e) => { imageUrl.value = e.target?.result as string }
  reader.readAsDataURL(file.raw)
  try {
    const response = await avatarApi.upload(file.raw, resumeId)
    uploadedImageUrl.value = response.sourceImageUrl
    optimizeForm.value.sourceImageUrl = response.sourceImageUrl
    ElMessage.success('上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
    imageUrl.value = ''
  }
}

function handleReupload() {
  imageUrl.value = ''
  uploadedImageUrl.value = ''
  optimizeForm.value.sourceImageUrl = ''
  optimizeTask.value = null
}

async function handleOptimize() {
  if (!uploadedImageUrl.value) {
    ElMessage.warning('请先上传头像')
    return
  }
  optimizing.value = true
  optimizeTask.value = null
  try {
    const response = await avatarApi.optimize(optimizeForm.value)
    const task: AvatarTask = {
      taskId: response.taskId,
      status: response.status as AvatarTask['status'],
      sourceImageUrl: uploadedImageUrl.value,
      createdAt: new Date().toISOString()
    }
    optimizeTask.value = task
    addAvatarTask(task)
    startTaskPolling(response.taskId)
    ElMessage.success('优化任务已创建')
  } catch (e: any) {
    ElMessage.error(e.message || '创建优化任务失败')
  } finally {
    optimizing.value = false
  }
}

function startTaskPolling(taskId: string) {
  if (taskPollingTimer) clearInterval(taskPollingTimer)
  taskPollingTimer = window.setInterval(async () => {
    try {
      const task = await avatarApi.getTask(taskId)
      optimizeTask.value = task
      updateAvatarTask(taskId, task)
      if (task.status === 'success' || task.status === 'failed') {
        if (taskPollingTimer) { clearInterval(taskPollingTimer); taskPollingTimer = null }
        if (task.status === 'success') ElMessage.success('优化完成')
        else ElMessage.error('优化失败')
      }
    } catch {
      /* swallow polling error */
    }
  }, 2000)
}

function handleReoptimize() {
  optimizeTask.value = null
  handleOptimize()
}

function handleApplyToResume() {
  if (!optimizeTask.value?.resultImageUrl) return
  ElMessage.success('已应用回简历')
  if (resumeId) router.push(`/editor/${resumeId}`)
  else router.back()
}

function handleDownload() {
  if (!optimizeTask.value?.resultImageUrl) return
  const link = document.createElement('a')
  link.href = optimizeTask.value.resultImageUrl
  link.download = 'optimized-avatar.jpg'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success('下载成功')
}

onUnmounted(() => {
  if (taskPollingTimer) clearInterval(taskPollingTimer)
})
</script>

<style scoped>
.avatar-uploader :deep(.el-upload-dragger) {
  width: 100%;
  padding: 32px 16px;
  background: #f2f4f7;
  border: 2px dashed #c1c6d7;
  border-radius: 1rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.avatar-uploader :deep(.el-upload-dragger:hover) {
  border-color: #0057c2;
  background: #d9e2ff;
}
</style>
