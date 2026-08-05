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
            <div class="mt-8 flex flex-wrap gap-8">
              <div class="flex items-center gap-6">
                <div class="relative w-24 h-24">
                  <svg
                    class="w-full h-full"
                    viewBox="0 0 100 100"
                  >
                    <circle
                      class="text-surface-variant stroke-current"
                      cx="50"
                      cy="50"
                      fill="transparent"
                      r="40"
                      stroke-width="10"
                    />
                    <circle
                      class="text-primary stroke-current"
                      cx="50"
                      cy="50"
                      fill="transparent"
                      r="40"
                      stroke-linecap="round"
                      stroke-width="10"
                      :stroke-dasharray="circumference"
                      :stroke-dashoffset="progressOffset"
                      style="transform: rotate(-90deg); transform-origin: 50% 50%;"
                    />
                  </svg>
                  <div class="absolute inset-0 flex items-center justify-center flex-col">
                    <span class="text-title-md font-bold text-primary">{{ completion }}%</span>
                    <span class="text-[10px] text-on-surface-variant">完成度</span>
                  </div>
                </div>
                <div class="space-y-2">
                  <p class="text-title-md text-on-surface">
                    你的简历还有提升空间
                  </p>
                  <ul class="space-y-1">
                    <li
                      v-for="tip in tips"
                      :key="tip"
                      class="flex items-center gap-2 text-body-md text-on-surface-variant"
                    >
                      <span class="w-1.5 h-1.5 rounded-full bg-primary" />
                      {{ tip }}
                    </li>
                  </ul>
                </div>
              </div>
              <RouterLink
                to="/workbench/resumes"
                class="bg-primary hover:bg-primary-container text-on-primary px-8 py-3 rounded-lg font-bold text-body-md transition-all flex items-center gap-2 self-end"
              >
                继续编辑
                <el-icon><ArrowRight /></el-icon>
              </RouterLink>
            </div>
          </div>
          <div class="hidden lg:block">
            <img
              class="w-64 h-auto drop-shadow-2xl"
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDExuqoWJXMSEfUg1dwJj7yYbMnp4Hs2_uNDNJb-f6GttP-O6nJ2QGoHSeNnJ0NanRfWf2a7tYseevhitbpBFPjeTx-ozIj8LRYnIyhbfSx-DhT4pvvuGsFGb50aL_exfAIiKjJdONmF4W7q1vuUrE3X8_Zh9BO8UFuu30yDPh2n0LQFjITxBa5sS_euzwrfBS6-LfIJp_XFLPjyintec__17c8k-1QIl45GGYZDJJq35D5CSJ_sQCJBMG68K4N09Djpl5Tkab370c"
              alt="resume illustration"
            >
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
                :src="defaultThumbnails[0]"
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

      <!-- Activity Timeline -->
      <section class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-title-lg font-title-lg flex items-center gap-2">
            <el-icon class="text-primary">
              <Clock />
            </el-icon> 最近活动
          </h2>
          <RouterLink
            to="/workbench/delivery"
            class="text-primary text-body-md hover:underline"
          >
            查看全部 &gt;
          </RouterLink>
        </div>
        <div class="space-y-6">
          <div
            v-for="(activity, idx) in activities"
            :key="idx"
            class="flex gap-4"
          >
            <div class="flex flex-col items-center">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center"
                :class="activity.iconBg"
              >
                <el-icon
                  size="14"
                  :class="activity.iconColor"
                >
                  <component :is="activity.icon" />
                </el-icon>
              </div>
              <div
                v-if="idx < activities.length - 1"
                class="w-0.5 h-full bg-outline-variant/30 mt-2"
              />
            </div>
            <div class="pb-6 w-full flex justify-between items-start">
              <div>
                <p class="text-body-md font-semibold">
                  {{ activity.title }}
                </p>
                <p class="text-body-md text-on-surface-variant">
                  {{ activity.desc }}
                </p>
              </div>
              <span class="text-label-md text-on-surface-variant whitespace-nowrap">{{ activity.time }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Right Sidebar Area -->
    <aside class="space-y-gutter">
      <!-- AI Suggestions Card -->
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
            </el-icon> AI 建议
          </h3>
          <RouterLink
            to="/workbench/ai-review"
            class="text-primary text-label-md hover:underline"
          >
            查看更多 &gt;
          </RouterLink>
        </div>
        <div class="space-y-stack-md">
          <div
            v-for="suggestion in aiSuggestions"
            :key="suggestion.title"
            class="flex gap-3 items-start p-3 bg-surface-container-low rounded-lg border border-outline-variant/30 hover:border-primary/50 cursor-pointer transition-all"
          >
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center text-white shrink-0"
              :class="suggestion.bgClass"
            >
              <el-icon size="16">
                <component :is="suggestion.icon" />
              </el-icon>
            </div>
            <div class="flex-grow">
              <p class="text-body-md font-semibold">
                {{ suggestion.title }}
              </p>
              <p class="text-label-md text-on-surface-variant mt-1">
                {{ suggestion.desc }}
              </p>
            </div>
            <el-icon class="text-on-surface-variant">
              <ArrowRight />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- Stats Tool -->
      <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6">
        <div class="space-y-6">
          <div
            v-for="stat in stats"
            :key="stat.label"
            class="flex items-center justify-between"
          >
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center"
                :class="stat.iconBg"
              >
                <el-icon :class="stat.iconColor">
                  <component :is="stat.icon" />
                </el-icon>
              </div>
              <div>
                <p class="text-label-md text-on-surface-variant">
                  {{ stat.label }}
                </p>
                <p class="text-title-lg font-bold">
                  {{ stat.value }}
                </p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-label-md text-on-surface-variant">
                较上周
              </p>
              <p class="text-label-md text-secondary font-bold flex items-center justify-end">
                <el-icon size="10">
                  <ArrowUp />
                </el-icon> {{ stat.growth }}
              </p>
            </div>
          </div>
        </div>
        <RouterLink
          to="/workbench/delivery"
          class="w-full mt-6 py-2 border border-outline-variant text-on-surface-variant rounded-lg hover:bg-surface-container text-body-md transition-all flex items-center justify-center"
        >
          查看投递管理
        </RouterLink>
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
import type { Resume } from '@/types/resume'
import {
  Document,
  Plus,
  ArrowRight,
  Clock,
  Histogram,
  Collection,
  CircleCheck,
  CircleCheckFilled,
  MagicStick,
  Promotion,
  User,
  Upload,
  Brush,
  Reading,
  More,
  Edit,
  ArrowUp
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const resumes = ref<Resume[]>([])
const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Resume | null>(null)

const completion = 72
const radius = 40
const circumference = 2 * Math.PI * radius
const progressOffset = computed(() => circumference - (completion / 100) * circumference)

const tips = [
  '完善项目经历的量化结果',
  '补充技能证书，增强竞争力',
  '上传作品集，展示专业能力'
]

const defaultThumbnails = [
  'https://lh3.googleusercontent.com/aida-public/AB6AXuD6s0nXSHl6onAPyKyzLwz3LckJnYOQSzphf5N4g5GJxBHRdU6fPrnRMe6b0r-sFRG5RSr1uYeLSi7NTN64EIsSnn7wBnjNStwvxo3dH9yxhhWd3Tbe-vp6WZfCvmuKh_-pGyhJUlFKB1rZraiVMoh6L7hUv2_qIpJMY9CqyGOR6tAKPDtDVM5WRCOp5yXg-_q0AM_hq-pXPCHkHu8Sd8Xtto--pWLWohN8ADSbKmwm61hMmsS7Kn3iw1n049mfbF_Rk-DCJ5mIdCY',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCsgMb1HQ5Mws5r6LKw_DnCmCcYXFd7KMMd7BIZuOiXfZ-tKdL9UUCwztKv_jZNMQ4Se0uPcoZmeHStmoDlr6uzxjM4vQbvHkKo4hHoyXtuYYTYiDNM1Q8TRoCxkbwfSWVl4wXMjDPauie2DU7kh1saKkO2yW09iL9wdwbvqacEzNYBbKvH62TVVqv49_etecoKIsW0uwRv5gELTAPIhf4nEKx0MsAJhhOSqaD5LvyEHx8nvDcNg_aGQ1QLi5p7pxwNKS87bckzYQI',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuC-VO1IvkdwBnM9B0FAGb1p-267Nkd5WjO5wxbGhYcOne4nfXT0ZKPMXiucWHSxm3__B362KjZRbMpAS-OOEGz6LJFsC_zt4JFigzXz1PcRA0-kJsrrCBEtpeQxbXhEfbqDRjznV8jq4vKSnIA1ThrrHSLQkH77SA05NsfNF13Ai1wQzp7OjjABzocXGC8ysnnV3Eaq60Dqzo_7L0S0Nbyx7ovRzRSB2Fy3XFAKI7s05-lQmVeXzxbIhpNnlcmErTFGPJold6Hja4Y'
]

const displayedResumes = computed(() => resumes.value.slice(0, 3))

const activities = [
  {
    title: '导出 PDF',
    desc: '导出了简历「前端开发工程师」的 PDF 版本',
    time: '2024-05-20 15:42',
    icon: Document,
    iconBg: 'bg-error-container',
    iconColor: 'text-error'
  },
  {
    title: '修改项目经历',
    desc: '更新了简历「前端开发工程师」的项目经历',
    time: '2024-05-20 14:18',
    icon: Edit,
    iconBg: 'bg-primary-container/20',
    iconColor: 'text-primary'
  },
  {
    title: '创建新简历',
    desc: '创建了新简历「数据分析师」',
    time: '2024-05-15 11:03',
    icon: CircleCheck,
    iconBg: 'bg-secondary-container',
    iconColor: 'text-secondary'
  }
]

const aiSuggestions = [
  {
    title: '项目经历缺少量化结果',
    desc: '建议为 2 个项目补充数据指标，提升说服力',
    icon: Histogram,
    bgClass: 'bg-orange-500'
  },
  {
    title: '建议补充 GitHub 作品链接',
    desc: '完善开源项目，展示你的技术影响力',
    icon: Collection,
    bgClass: 'bg-green-600'
  },
  {
    title: '当前模板适合技术岗位',
    desc: '简洁专业的布局，更受技术面试官青睐',
    icon: CircleCheckFilled,
    bgClass: 'bg-blue-600'
  }
]

const stats = [
  { label: '本周投递', value: 18, growth: '20%', icon: Promotion, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: 'AI 优化次数', value: 7, growth: '16%', icon: MagicStick, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '面试邀请', value: 2, growth: '100%', icon: User, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' }
]

const quickActions = [
  {
    label: '导入简历',
    sub: 'Word / PDF',
    icon: Upload,
    iconBg: 'bg-primary-container/10',
    iconColor: 'text-primary',
    hoverClass: 'hover:bg-primary/5 hover:text-primary',
    onClick: () => ElMessage.info('导入功能即将上线')
  },
  {
    label: 'AI 优化',
    sub: '智能改写',
    icon: Brush,
    iconBg: 'bg-tertiary-fixed-dim/30',
    iconColor: 'text-tertiary',
    hoverClass: 'hover:bg-tertiary/5 hover:text-tertiary',
    onClick: () => router.push('/workbench/ai-review')
  },
  {
    label: '上传作品集',
    sub: '展示项目',
    icon: Upload,
    iconBg: 'bg-primary-container/10',
    iconColor: 'text-primary',
    hoverClass: 'hover:bg-primary/5 hover:text-primary',
    onClick: () => router.push('/workbench/avatar/upload')
  },
  {
    label: '职业测评',
    sub: '了解自己',
    icon: Reading,
    iconBg: 'bg-tertiary-fixed-dim/30',
    iconColor: 'text-tertiary',
    hoverClass: 'hover:bg-tertiary/5 hover:text-tertiary',
    onClick: () => ElMessage.info('职业测评即将上线')
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
