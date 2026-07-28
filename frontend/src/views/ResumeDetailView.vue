<template>
  <MainLayout>
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
            to="/resumes"
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
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick, Download, Edit, Document } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import type { Resume } from '@/types/resume'
import ResumePreview from '@/components/preview/ResumePreview.vue'
import MainLayout from '@/components/layout/MainLayout.vue'

const router = useRouter()
const route = useRoute()
const resumeId = route.params.id as string

const loading = ref(false)
const resume = ref<Resume | null>(null)

const sampleResume: Resume = {
  id: resumeId,
  userId: 'u1',
  title: 'Java 后端工程师',
  scene: 'social_recruitment',
  templateId: 'tpl-1',
  createdAt: '2024-05-20T08:00:00Z',
  updatedAt: '2024-05-22T10:30:00Z',
  sections: [
    {
      id: 's1',
      type: 'profile',
      title: '个人信息',
      order: 1,
      visible: true,
      data: {
        name: '张一航',
        phone: '138****1234',
        email: 'zhang@example.com',
        city: '上海',
        targetPosition: 'Java 后端工程师',
        showGender: false,
        showAge: false,
        showSalary: false,
        showAvatar: false
      }
    },
    {
      id: 's2',
      type: 'education',
      title: '教育经历',
      order: 2,
      visible: true,
      data: [
        { school: '上海交通大学', degree: '本科', major: '软件工程', startDate: '2018-09', endDate: '2022-06' }
      ]
    },
    {
      id: 's3',
      type: 'work',
      title: '工作经历',
      order: 3,
      visible: true,
      data: [
        { company: '星云科技', position: 'Java 后端工程师', startDate: '2022-07', endDate: '2024-05', description: ['负责企业协作产品后端开发', '参与微服务架构设计与落地'] }
      ]
    },
    {
      id: 's4',
      type: 'skill',
      title: '技能',
      order: 4,
      visible: true,
      data: { category: '后端', items: [{ name: 'Java' }, { name: 'Spring Boot' }, { name: 'MySQL' }] }
    },
    {
      id: 's5',
      type: 'introduction',
      title: '自我介绍',
      order: 5,
      visible: true,
      data: { content: '具备扎实的 Java 后端开发经验，熟悉微服务架构与分布式系统设计。' }
    }
  ]
}

async function loadResume() {
  loading.value = true
  try {
    resume.value = await resumeApi.get(resumeId)
  } catch (e: any) {
    ElMessage.warning('后端连接失败，已加载示例简历用于预览')
    resume.value = sampleResume
  } finally {
    loading.value = false
  }
}

function goEdit() {
  router.push(`/editor/${resumeId}`)
}

function goExport() {
  router.push(`/resumes/${resumeId}/export`)
}

function goReview() {
  router.push(`/resumes/${resumeId}/review`)
}

onMounted(() => { loadResume() })
</script>

