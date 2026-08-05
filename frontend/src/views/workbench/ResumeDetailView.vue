<template>
  <main class="max-w-[1440px] mx-auto px-margin-page py-stack-lg">
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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick, Download, Edit, Document } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import type { Resume } from '@/types/resume'
import ResumePreview from '@/components/preview/ResumePreview.vue'

const router = useRouter()
const route = useRoute()
const resumeId = route.params.id as string

const loading = ref(false)
const resume = ref<Resume | null>(null)

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

