<template>
  <main class="workbench-page py-stack-lg">
    <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-stack-lg">
      <div class="flex items-center gap-3">
        <button
          class="w-8 h-8 bg-surface-container-lowest border border-outline-variant rounded-lg flex items-center justify-center text-on-surface hover:text-primary hover:bg-surface-container-low transition-colors"
          title="返回"
          @click="router.back()"
        >
          <el-icon size="16">
            <ArrowLeft />
          </el-icon>
        </button>
        <div>
          <h1 class="text-title-lg font-title-lg text-on-surface">
            {{ resume?.title || '简历详情' }}
          </h1>
          <p class="text-label-md text-on-surface-variant mt-0.5">
            仅你可见的预览，可继续编辑或导出 PDF
          </p>
        </div>
      </div>
      <div class="flex flex-wrap gap-2">
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 flex items-center gap-2 transition-colors"
          @click="openShareDialog"
        >
          <el-icon size="14">
            <Share />
          </el-icon>
          <span>分享</span>
        </button>
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 flex items-center gap-2 transition-colors"
          @click="goReview"
        >
          <el-icon size="14">
            <MagicStick />
          </el-icon>
          <span>AI 点评</span>
        </button>
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 flex items-center gap-2 transition-colors"
          @click="goExport"
        >
          <el-icon size="14">
            <Download />
          </el-icon>
          <span>导出 PDF</span>
        </button>
        <button
          class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
          @click="goEdit"
        >
          <el-icon size="14">
            <Edit />
          </el-icon>
          <span>编辑简历</span>
        </button>
      </div>
    </header>

    <div class="max-w-[960px] mx-auto">
      <div
        v-if="loading"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm"
      >
        <el-skeleton
          :rows="6"
          animated
        />
      </div>
      <div
        v-else-if="!resume"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 text-center text-on-surface-variant"
      >
        <el-icon size="48">
          <Document />
        </el-icon>
        <p class="text-body-md mt-4 mb-6">
          简历不存在或已被删除
        </p>
        <RouterLink
          to="/workbench/resumes"
          class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md inline-flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
        >
          返回简历库
        </RouterLink>
      </div>
      <div
        v-else
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm"
      >
        <ResumePreview :resume="resume" />
      </div>
    </div>
  </main>

  <!-- 分享对话框 -->
  <el-dialog
    v-model="shareDialogVisible"
    title="分享简历"
    width="480px"
    align-center
  >
    <div
      v-if="shareInfo"
      class="flex flex-col gap-4"
    >
      <el-switch
        v-model="shareEnabled"
        active-text="公开分享"
        inactive-text="已关闭"
        @change="handleShareToggle"
      />
      <template v-if="shareEnabled">
        <el-input
          :model-value="shareUrl"
          readonly
        >
          <template #append>
            <el-button @click="copyShareUrl">
              复制链接
            </el-button>
          </template>
        </el-input>
        <div class="flex items-center justify-between text-sm">
          <span class="text-on-surface-variant">隐藏联系方式</span>
          <el-switch
            v-model="shareForm.hideContact"
            @change="handleShareToggle(true)"
          />
        </div>
        <div class="flex items-center justify-between text-sm">
          <span class="text-on-surface-variant">过期时间</span>
          <span class="text-on-surface">
            {{ shareExpiryLabel }}
          </span>
        </div>
        <div class="text-xs text-on-surface-variant">
          {{ shareForm.hideContact
            ? '分享页将隐藏手机号、邮箱和个人链接，仅展示简历正文。'
            : '链接对所有人可见，包含简历中的联系方式。关闭分享或到期后链接立即失效。' }}
        </div>
        <RouterLink
          :to="`/share/${shareInfo.token}`"
          target="_blank"
          class="text-primary text-sm hover:underline"
        >
          预览分享页 →
        </RouterLink>
      </template>
      <template v-else>
        <el-switch
          v-model="shareForm.hideContact"
          active-text="隐藏联系方式"
          inactive-text="展示联系方式"
        />
        <el-select
          v-model="shareForm.expiryOption"
          class="w-full"
          placeholder="过期时间"
        >
          <el-option
            label="永久有效"
            value="forever"
          />
          <el-option
            label="7 天后过期"
            value="7d"
          />
          <el-option
            label="30 天后过期"
            value="30d"
          />
        </el-select>
        <div class="text-xs text-on-surface-variant">
          开启分享后，链接对所有人可见。可选择隐藏联系方式或设置有效期。
        </div>
      </template>
    </div>
    <div
      v-else-if="shareLoading"
      class="py-4 text-center text-on-surface-variant text-sm"
    >
      加载中…
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick, Download, Edit, Document, Share } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import { shareApi, type ShareInfo } from '@/api/share'
import type { Resume } from '@/types/resume'
import ResumePreview from '@/components/preview/ResumePreview.vue'

const router = useRouter()
const route = useRoute()
const resumeId = route.params.id as string

const loading = ref(false)
const resume = ref<Resume | null>(null)

const shareDialogVisible = ref(false)
const shareLoading = ref(false)
const shareInfo = ref<ShareInfo | null>(null)
const shareForm = ref<{ hideContact: boolean; expiryOption: 'forever' | '7d' | '30d' }>({
  hideContact: false,
  expiryOption: 'forever'
})

const shareEnabled = computed({
  get: () => !!shareInfo.value && shareInfo.value.status === 'active',
  set: () => { /* 由 handleShareToggle 处理 */ }
})

const shareExpiryLabel = computed(() => {
  if (!shareInfo.value?.expiresAt) return '永久有效'
  // 使用本地日期差异，避免 UTC 跨日导致 Math.ceil 多算 1 天。
  const now = new Date()
  const exp = new Date(shareInfo.value.expiresAt)
  const days = Math.max(
    0,
    Math.round((exp.getTime() - now.getTime()) / 86_400_000)
  )
  return `${days} 天后过期`
})

const shareUrl = computed(() => {
  if (!shareInfo.value) return ''
  return `${window.location.origin}${shareInfo.value.url}`
})

function resolveExpiresAt(option: 'forever' | '7d' | '30d'): string | undefined {
  if (option === 'forever') return undefined
  const days = option === '7d' ? 7 : 30
  // 选「本地正午」避免 UTC 时区偏移把过期时间提前/推后 1 天。
  // 后端按 ISO datetime 解析，校验「晚于当前时间」即可。
  const exp = new Date()
  exp.setDate(exp.getDate() + days)
  exp.setHours(12, 0, 0, 0)
  return exp.toISOString()
}

async function openShareDialog() {
  shareDialogVisible.value = true
  shareLoading.value = true
  shareForm.value = { hideContact: false, expiryOption: 'forever' }
  try {
    shareInfo.value = await shareApi.get(resumeId)
    if (shareInfo.value) {
      shareForm.value.hideContact = !!shareInfo.value.hideContact
      shareForm.value.expiryOption = shareInfo.value.expiresAt ? '7d' : 'forever'
    }
  } catch (e: any) {
    shareInfo.value = null
    ElMessage.error(e.message || '查询分享状态失败')
  } finally {
    shareLoading.value = false
  }
}

async function handleShareToggle(enabled: string | number | boolean) {
  try {
    if (enabled) {
      shareInfo.value = await shareApi.create(resumeId, {
        hideContact: shareForm.value.hideContact,
        expiresAt: resolveExpiresAt(shareForm.value.expiryOption)
      })
      ElMessage.success('分享已开启')
    } else {
      await shareApi.revoke(resumeId)
      shareInfo.value = { ...shareInfo.value!, status: 'revoked' } as ShareInfo
      ElMessage.success('分享已关闭')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
    // 回滚开关状态
    shareInfo.value = null
  }
}

async function copyShareUrl() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

async function loadResume() {
  loading.value = true
  try {
    resume.value = await resumeApi.get(resumeId)
  } catch (e: any) {
    // 不再回退到示例简历，避免误导用户；由错误提示 + 空状态展示真实结果
    resume.value = null
    ElMessage.error(e.message || '加载简历失败')
  } finally {
    loading.value = false
  }
}

function goEdit() {
  router.push(`/workbench/editor/${resumeId}`)
}

function goExport() {
  router.push(`/workbench/resumes/${resumeId}/export`)
}

function goReview() {
  router.push(`/workbench/resumes/${resumeId}/review`)
}

onMounted(() => { loadResume() })
</script>

