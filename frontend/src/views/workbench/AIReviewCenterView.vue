<template>
  <main class="workbench-page py-stack-lg">
    <!-- Header Section -->
    <div class="flex flex-col gap-2 mb-stack-lg">
      <h1 class="text-headline-md font-headline-md text-on-surface">
        AI 简历优化
      </h1>
      <p class="text-body-md text-on-surface-variant">
        基于目标岗位描述，AI 会从内容、结构、匹配度等维度给出专业建议
      </p>
    </div>

    <!-- Hero Section -->
    <section class="grid grid-cols-1 md:grid-cols-2 gap-gutter mb-stack-lg">
      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm flex items-center gap-4">
        <div class="w-14 h-14 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
          <el-icon size="28">
            <MagicStick />
          </el-icon>
        </div>
        <div>
          <div class="text-title-md font-bold text-on-surface">
            6 大维度
          </div>
          <div class="text-label-md text-on-surface-variant mt-0.5">
            内容 / 结构 / 语言 / 匹配度 / 亮点 / 完整度
          </div>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm flex items-center gap-4">
        <div class="w-14 h-14 rounded-lg bg-secondary/10 text-secondary flex items-center justify-center shrink-0">
          <el-icon size="28">
            <CircleCheckFilled />
          </el-icon>
        </div>
        <div>
          <div class="text-title-md font-bold text-on-surface">
            即时建议
          </div>
          <div class="text-label-md text-on-surface-variant mt-0.5">
            生成可落地的修改建议与缺失技能
          </div>
        </div>
      </div>
    </section>

    <div class="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-gutter">
      <!-- Review Form -->
      <section class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm">
        <h2 class="text-title-lg font-title-lg text-on-surface">
          开始一次点评
        </h2>
        <p class="text-body-md text-on-surface-variant mt-1">
          选择一份简历并粘贴目标岗位的 JD，AI 将在几秒内生成点评报告。
        </p>

        <div class="mt-stack-lg flex flex-col gap-2">
          <label class="text-label-md font-bold text-on-surface-variant">
            选择简历
          </label>
          <select
            v-model="selectedResumeId"
            class="w-full h-[42px] bg-surface-container-low border border-outline-variant rounded-lg px-3 text-body-md text-on-surface focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none"
          >
            <option value="">
              请选择要点评的简历
            </option>
            <option
              v-for="resume in resumes"
              :key="resume.id"
              :value="resume.id"
            >
              {{ resume.title }}
            </option>
          </select>
          <p
            v-if="!loading && resumes.length === 0"
            class="text-body-md text-on-surface-variant"
          >
            暂无简历，<RouterLink
              to="/workbench/resumes/create"
              class="text-primary hover:underline"
            >
              去创建
            </RouterLink>
          </p>
        </div>

        <div class="mt-stack-lg flex flex-col gap-2">
          <label class="text-label-md font-bold text-on-surface-variant">
            目标岗位描述（JD）
          </label>
          <textarea
            v-model="jobDescription"
            class="w-full bg-surface-container-low border border-outline-variant rounded-lg px-3 py-2 text-body-md text-on-surface resize-y focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none"
            rows="6"
            placeholder="请粘贴目标岗位的职位描述..."
          />
        </div>

        <button
          class="mt-stack-lg bg-primary text-on-primary px-5 py-2.5 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
          :disabled="!selectedResumeId || !jobDescription.trim() || analyzing"
          @click="startReview"
        >
          <el-icon
            v-if="analyzing"
            class="animate-spin"
            size="16"
          >
            <Loading />
          </el-icon>
          <el-icon
            v-else
            size="16"
          >
            <MagicStick />
          </el-icon>
          <span>{{ analyzing ? '准备中...' : '开始点评' }}</span>
        </button>
      </section>

      <!-- Recent Resumes -->
      <section class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm">
        <h2 class="text-title-lg font-title-lg text-on-surface">
          最近简历
        </h2>
        <div
          v-if="loading"
          class="mt-4"
        >
          <el-skeleton
            :rows="3"
            animated
          />
        </div>
        <ul
          v-else
          class="mt-4 space-y-1"
        >
          <li
            v-for="resume in resumes.slice(0, 5)"
            :key="resume.id"
            class="flex items-center gap-4 p-3 rounded-lg hover:bg-surface-container-low cursor-pointer transition-colors"
            @click="selectResume(resume.id)"
          >
            <div class="flex-1 min-w-0">
              <div class="text-body-md font-semibold text-on-surface truncate">
                {{ resume.title }}
              </div>
              <div class="flex gap-2 text-label-md text-on-surface-variant mt-0.5">
                <span>{{ formatScene(resume.scene) }}</span>
                <span>{{ resume.updatedAt }}</span>
              </div>
            </div>
            <button
              class="bg-primary text-on-primary text-label-md font-bold px-3 py-1 rounded-full shrink-0 hover:scale-[0.98] transition-transform"
              @click.stop="goReview(resume.id)"
            >
              点评
            </button>
          </li>
          <li
            v-if="resumes.length === 0"
            class="text-center text-body-md text-on-surface-variant py-6"
          >
            暂无简历
          </li>
        </ul>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, CircleCheckFilled, Loading } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import type { Resume } from '@/types/resume'

const router = useRouter()
const resumes = ref<Resume[]>([])
const loading = ref(true)
const selectedResumeId = ref('')
const jobDescription = ref('')
const analyzing = ref(false)

const sceneMap: Record<string, string> = {
  campus_recruitment: '校园招聘',
  social_recruitment: '社会招聘',
  postgraduate: '考研复试',
  internship: '实习',
  internal: '内部晋升',
  custom: '自定义'
}

function formatScene(scene?: string) {
  return sceneMap[scene || ''] || scene || '未设置'
}

async function loadResumes() {
  loading.value = true
  try {
    const res = await resumeApi.list()
    resumes.value = res.records || []
  } catch (e: any) {
    ElMessage.error(e.message || '简历加载失败')
    resumes.value = []
  } finally {
    loading.value = false
  }
}

function selectResume(id: string) {
  selectedResumeId.value = id
}

function goReview(id: string) {
  router.push({ path: `/workbench/resumes/${id}/review` })
}

function startReview() {
  if (!selectedResumeId.value) {
    ElMessage.warning('请选择简历')
    return
  }
  if (!jobDescription.value.trim()) {
    ElMessage.warning('请输入目标岗位描述')
    return
  }
  analyzing.value = true
  setTimeout(() => {
    analyzing.value = false
    router.push({
      path: `/workbench/resumes/${selectedResumeId.value}/review`,
      query: { jd: encodeURIComponent(jobDescription.value.trim()) }
    })
  }, 400)
}

onMounted(() => { loadResumes() })
</script>

