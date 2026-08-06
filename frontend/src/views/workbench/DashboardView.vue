<template>
  <main class="max-w-[1440px] mx-auto p-margin-page grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-gutter">
    <!-- Left Content Area -->
    <div class="space-y-gutter">
      <!-- Welcome Area -->
      <section class="relative overflow-hidden rounded-xl bg-gradient-to-r from-primary-container/10 to-tertiary-container/5 p-8 border border-outline-variant/30">
        <div class="relative z-10 grid md:grid-cols-[1fr_auto] gap-8 items-center">
          <div>
            <h1 class="text-headline-md font-headline-md text-on-background flex items-center gap-2">
              你好，{{ userStore.nickname || '求职者' }} 👋
            </h1>
            <p class="text-body-lg text-on-surface-variant mt-2">
              继续完善你的简历，让下一次投递更有把握
            </p>
            <div class="mt-8 flex flex-wrap items-center gap-4">
              <RouterLink
                to="/workbench/resumes"
                class="bg-primary hover:bg-primary-container text-on-primary px-8 py-3 rounded-lg font-bold text-body-md transition-all flex items-center gap-2"
              >
                继续编辑
                <el-icon><ArrowRight /></el-icon>
              </RouterLink>
              <RouterLink
                to="/workbench/ai-review"
                class="border border-outline-variant hover:bg-surface-container-low text-on-surface-variant px-6 py-3 rounded-lg font-bold text-body-md transition-all flex items-center gap-2"
              >
                <el-icon><MagicStick /></el-icon>
                AI 点评
              </RouterLink>
            </div>
          </div>
        </div>
        <div class="absolute -right-20 -bottom-20 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />
      </section>

      <!-- Resume List Area -->
      <section>
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-title-lg font-title-lg flex items-center gap-2">
            <el-icon class="text-primary">
              <Document />
            </el-icon> 我的简历
          </h2>
          <RouterLink
            to="/workbench/resumes"
            class="text-primary text-body-md hover:underline"
          >
            全部简历 &gt;
          </RouterLink>
        </div>

        <div
          v-if="loading"
          class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-stack-md"
        >
          <div
            v-for="i in 4"
            :key="i"
            class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 h-[300px] animate-pulse"
          >
            <div class="bg-surface-container-low rounded-lg h-40 mb-3" />
            <div class="h-4 bg-surface-container rounded w-3/4 mb-2" />
            <div class="h-3 bg-surface-container rounded w-1/2" />
          </div>
        </div>

        <div
          v-else
          class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-stack-md"
        >
          <!-- New Resume Entry -->
          <button
            class="border-2 border-dashed border-outline-variant rounded-xl flex flex-col items-center justify-center p-6 bg-surface-container-low hover:bg-surface-container hover:border-primary transition-all cursor-pointer group h-[300px] text-left"
            @click="createResume"
          >
            <div class="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary group-hover:scale-110 transition-transform mb-4">
              <el-icon size="24">
                <Plus />
              </el-icon>
            </div>
            <p class="text-title-md">
              新建简历
            </p>
            <p class="text-label-md text-on-surface-variant mt-1">
              选择模板，快速创建
            </p>
          </button>

          <!-- Resume Cards -->
          <div
            v-for="resume in displayedResumes"
            :key="resume.id"
            class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 st-resume-card-hover transition-all cursor-pointer h-[300px] flex flex-col"
            @click="openEditor(resume)"
          >
            <div class="bg-surface-container-low rounded-lg overflow-hidden h-40 mb-3 border border-outline-variant/30">
              <img
                class="w-full h-full object-cover"
                :src="TEMPLATE_PLACEHOLDER"
                :alt="resume.title"
              >
            </div>
            <div class="flex-grow">
              <p class="text-title-md text-on-surface truncate">
                {{ resume.title || '未命名简历' }}
              </p>
              <p class="text-label-md text-on-surface-variant mt-1">
                {{ formatTime(resume.updatedAt || resume.createdAt) }}
              </p>
            </div>
            <div class="flex justify-between items-center mt-2 pt-2 border-t border-outline-variant/20">
              <span
                class="text-label-md"
                :class="resume.targetPosition ? 'text-primary font-bold' : 'text-on-surface-variant'"
              >
                {{ resume.targetPosition || sceneLabel(resume.scene) }}
              </span>
              <el-dropdown
                trigger="click"
                @command="(cmd: string) => handleResumeAction(cmd, resume)"
                @click.stop
              >
                <button
                  class="p-1 hover:bg-surface-container rounded-md"
                  @click.stop
                >
                  <el-icon class="text-body-md">
                    <More />
                  </el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item command="copy">
                      复制
                    </el-dropdown-item>
                    <el-dropdown-item command="rename">
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      divided
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Right Sidebar Area -->
    <aside class="space-y-gutter">
      <!-- AI Capabilities Card -->
      <div class="bg-surface-container-lowest border-2 border-primary/20 rounded-xl p-6 relative overflow-hidden group">
        <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <el-icon class="text-6xl text-primary">
            <MagicStick />
          </el-icon>
        </div>
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-title-md flex items-center gap-2">
            <el-icon class="text-primary">
              <MagicStick />
            </el-icon> AI 能力
          </h3>
          <RouterLink
            to="/workbench/ai-review"
            class="text-primary text-label-md hover:underline"
          >
            查看更多 &gt;
          </RouterLink>
        </div>
        <div class="space-y-stack-md">
          <button
            class="w-full flex gap-3 items-start p-3 bg-surface-container-low rounded-lg border border-outline-variant/30 hover:border-primary/50 cursor-pointer transition-all text-left"
            @click="router.push('/workbench/ai-review')"
          >
            <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white shrink-0 bg-orange-500">
              <el-icon size="16">
                <Histogram />
              </el-icon>
            </div>
            <div class="flex-grow">
              <p class="text-body-md font-semibold text-on-surface">
                AI 简历点评
              </p>
              <p class="text-label-md text-on-surface-variant mt-1">
                多维度评估竞争力，给出可执行建议
              </p>
            </div>
            <el-icon class="text-on-surface-variant">
              <ArrowRight />
            </el-icon>
          </button>
          <button
            class="w-full flex gap-3 items-start p-3 bg-surface-container-low rounded-lg border border-outline-variant/30 hover:border-primary/50 cursor-pointer transition-all text-left"
            @click="router.push('/workbench/ai-review')"
          >
            <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white shrink-0 bg-green-600">
              <el-icon size="16">
                <Collection />
              </el-icon>
            </div>
            <div class="flex-grow">
              <p class="text-body-md font-semibold text-on-surface">
                JD 匹配优化
              </p>
              <p class="text-label-md text-on-surface-variant mt-1">
                对照岗位描述优化简历关键词
              </p>
            </div>
            <el-icon class="text-on-surface-variant">
              <ArrowRight />
            </el-icon>
          </button>
          <button
            class="w-full flex gap-3 items-start p-3 bg-surface-container-low rounded-lg border border-outline-variant/30 hover:border-primary/50 cursor-pointer transition-all text-left"
            @click="router.push('/workbench/resumes')"
          >
            <div class="w-8 h-8 rounded-lg flex items-center justify-center text-white shrink-0 bg-blue-600">
              <el-icon size="16">
                <CircleCheckFilled />
              </el-icon>
            </div>
            <div class="flex-grow">
              <p class="text-body-md font-semibold text-on-surface">
                语法检查
              </p>
              <p class="text-label-md text-on-surface-variant mt-1">
                编辑器内检查错别字与表达问题
              </p>
            </div>
            <el-icon class="text-on-surface-variant">
              <ArrowRight />
            </el-icon>
          </button>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6">
        <h3 class="text-title-md mb-6">
          快捷操作
        </h3>
        <div class="grid grid-cols-2 gap-4">
          <button
            v-for="action in quickActions"
            :key="action.label"
            class="flex flex-col items-center justify-center p-4 bg-surface-container-low rounded-xl transition-all group"
            :class="action.hoverClass"
            @click="action.onClick"
          >
            <div
              class="w-10 h-10 rounded-lg flex items-center justify-center mb-2 group-hover:scale-110 transition-transform"
              :class="action.iconBg"
            >
              <el-icon :class="action.iconColor">
                <component :is="action.icon" />
              </el-icon>
            </div>
            <span class="text-label-md font-semibold">{{ action.label }}</span>
            <span class="text-[10px] text-on-surface-variant">{{ action.sub }}</span>
          </button>
        </div>
      </div>
    </aside>
  </main>

  <!-- Rename Dialog -->
  <el-dialog
    v-model="renameVisible"
    title="重命名简历"
    width="420px"
    align-center
  >
    <el-input
      v-model="renameTitle"
      placeholder="输入新标题"
    />
    <template #footer>
      <el-button @click="renameVisible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        @click="handleRenameConfirm"
      >
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { resumeApi } from '@/api/resume'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'
import type { Resume } from '@/types/resume'
import {
  Document,
  Plus,
  ArrowRight,
  Histogram,
  Collection,
  CircleCheckFilled,
  MagicStick,
  Upload,
  More
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const resumes = ref<Resume[]>([])
const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Resume | null>(null)

const displayedResumes = computed(() => resumes.value.slice(0, 3))

const quickActions = [
  {
    label: '导入简历',
    sub: 'Markdown / JSON',
    icon: Upload,
    iconBg: 'bg-primary-container/10',
    iconColor: 'text-primary',
    hoverClass: 'hover:bg-primary/5 hover:text-primary',
    onClick: () => router.push('/workbench/resumes')
  },
  {
    label: 'AI 优化',
    sub: '智能改写',
    icon: MagicStick,
    iconBg: 'bg-tertiary-fixed-dim/30',
    iconColor: 'text-tertiary',
    hoverClass: 'hover:bg-tertiary/5 hover:text-tertiary',
    onClick: () => router.push('/workbench/ai-review')
  },
  {
    label: '头像管理',
    sub: '上传与优化',
    icon: Upload,
    iconBg: 'bg-primary-container/10',
    iconColor: 'text-primary',
    hoverClass: 'hover:bg-primary/5 hover:text-primary',
    onClick: () => router.push('/workbench/avatar/upload')
  }
]

function sceneLabel(scene: string) {
  const map: Record<string, string> = {
    campus_recruitment: '校招',
    internship: '实习',
    social_recruitment: '社招',
    postgraduate_reexam: '考研',
    project_application: '项目',
    custom: '自定义'
  }
  return map[scene] || '简历'
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const diffDays = Math.floor((Date.now() - d.getTime()) / 86400000)
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return d.toLocaleDateString('zh-CN')
}

function createResume() {
  router.push('/workbench/templates')
}

function openEditor(resume: Resume) {
  router.push(`/workbench/editor/${resume.id}`)
}

function handleResumeAction(cmd: string, resume: Resume) {
  if (cmd === 'edit') openEditor(resume)
  else if (cmd === 'copy') handleCopy(resume)
  else if (cmd === 'rename') handleRename(resume)
  else if (cmd === 'delete') handleDelete(resume)
}

async function handleCopy(resume: Resume) {
  try {
    await resumeApi.duplicate(resume.id)
    ElMessage.success('已复制')
    fetchResumes()
  } catch {
    ElMessage.error('复制失败')
  }
}

async function handleDelete(resume: Resume) {
  try {
    await ElMessageBox.confirm('确定删除该简历？删除后不可恢复。', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resumeApi.remove(resume.id)
    ElMessage.success('已删除')
    fetchResumes()
  } catch {
    // cancelled
  }
}

function handleRename(resume: Resume) {
  renameTarget.value = resume
  renameTitle.value = resume.title
  renameVisible.value = true
}

async function handleRenameConfirm() {
  if (renameTarget.value && renameTitle.value.trim()) {
    try {
      await resumeApi.rename(renameTarget.value.id, renameTitle.value.trim())
      ElMessage.success('重命名成功')
      renameVisible.value = false
      fetchResumes()
    } catch {
      ElMessage.error('重命名失败')
    }
  }
}

async function fetchResumes() {
  try {
    const data = await resumeApi.list(1, 4)
    resumes.value = data?.records || []
  } catch {
    // ignore
  }
}

onMounted(async () => {
  loading.value = true
  await fetchResumes()
  loading.value = false
})
</script>

<style scoped lang="scss">
.st-resume-card-hover {
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--st-shadow-card);
  }
}
</style>
