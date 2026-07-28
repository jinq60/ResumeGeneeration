<template>
  <MainLayout>
    <main class="max-w-[1440px] mx-auto px-margin-page py-stack-lg flex flex-col lg:flex-row gap-stack-lg">
      <!-- Main Content Area -->
      <div class="flex-1">
        <!-- Header Section -->
        <div class="flex justify-between items-center mb-stack-lg">
          <h1 class="font-headline-md text-headline-md text-on-surface">
            我的简历
          </h1>
          <button
            class="bg-primary text-on-primary px-6 py-2.5 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
            @click="goCreate"
          >
            <el-icon size="18">
              <Plus />
            </el-icon>
            新建简历
          </button>
        </div>

        <!-- Tabs & Search -->
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-stack-lg bg-surface-container-lowest p-2 rounded-xl border border-outline-variant/30">
          <div class="flex gap-2">
            <button
              v-for="t in tabs"
              :key="t.value"
              :class="[
                'px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
                currentTab === t.value
                  ? 'bg-primary-fixed text-primary'
                  : 'text-on-surface-variant hover:bg-surface-container-low'
              ]"
              @click="currentTab = t.value"
            >
              {{ t.label }}
            </button>
          </div>
          <div class="flex items-center gap-2 w-full sm:w-auto">
            <div class="relative flex-1 sm:w-64">
              <input
                v-model="keyword"
                class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                placeholder="搜索简历名称"
                type="text"
              >
              <el-icon
                class="absolute right-2 top-2 text-outline text-lg"
                @click="handleSearch"
              >
                <Search />
              </el-icon>
            </div>
            <button class="p-2 border border-outline-variant rounded-lg hover:bg-surface-container-low">
              <el-icon class="text-on-surface-variant">
                <Filter />
              </el-icon>
            </button>
          </div>
        </div>

        <!-- Resume Grid -->
        <div
          v-if="loading"
          class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-stack-lg"
        >
          <div
            v-for="i in 3"
            :key="i"
            class="aspect-[3/4] bg-surface-container-lowest rounded-xl border border-outline-variant animate-pulse p-4"
          >
            <div class="w-full h-3/4 bg-surface-container-low rounded-lg mb-4" />
            <div class="h-4 bg-surface-container rounded w-3/4 mb-2" />
            <div class="h-3 bg-surface-container rounded w-1/2" />
          </div>
        </div>

        <div
          v-else-if="filteredResumes.length === 0"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 text-center"
        >
          <div class="w-16 h-16 mx-auto mb-4 bg-surface-container-low rounded-full flex items-center justify-center">
            <el-icon
              size="28"
              class="text-outline"
            >
              <Document />
            </el-icon>
          </div>
          <h3 class="text-title-md font-bold text-on-surface mb-2">
            还没有简历
          </h3>
          <p class="text-body-md text-on-surface-variant mb-6">
            创建第一份简历，开始你的求职之旅
          </p>
          <button
            class="bg-primary text-on-primary px-6 py-2.5 rounded-lg font-label-md flex items-center gap-2 mx-auto"
            @click="goCreate"
          >
            <el-icon><Plus /></el-icon>
            新建简历
          </button>
        </div>

        <div
          v-else
          class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-stack-lg"
        >
          <!-- Resume Cards -->
          <div
            v-for="(resume, idx) in filteredResumes"
            :key="resume.id"
            class="resume-card group bg-surface-container-lowest rounded-xl border border-outline-variant hover:shadow-xl transition-all duration-300 relative overflow-hidden"
          >
            <div class="aspect-[3/4] bg-surface-container-low relative overflow-hidden p-4">
              <span
                v-if="idx === 0"
                class="absolute top-2 left-2 z-10 bg-primary/10 text-primary text-[10px] font-bold px-2 py-0.5 rounded border border-primary/20"
              >默认</span>
              <div class="w-full h-full bg-white shadow-md rounded-sm p-4 origin-top transition-transform group-hover:scale-[1.02]">
                <img
                  class="w-full h-full object-contain"
                  :src="resume.thumbnail || defaultThumbnails[idx % defaultThumbnails.length]"
                  :alt="resume.title"
                >
              </div>
              <!-- Hover Actions Overlay -->
              <div class="resume-overlay absolute inset-0 bg-black/40 backdrop-blur-sm flex flex-col items-center justify-center gap-3 opacity-0 transition-opacity duration-300">
                <button
                  class="w-32 bg-white text-on-surface py-2 rounded-lg font-label-md flex items-center justify-center gap-2 hover:bg-primary hover:text-white transition-all"
                  @click.stop="openEditor(resume)"
                >
                  <el-icon><Edit /></el-icon> 编辑简历
                </button>
                <div class="flex gap-2">
                  <button
                    class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-lg hover:bg-white/40 transition-all flex items-center justify-center"
                    @click.stop="handleCopy(resume)"
                  >
                    <el-icon><CopyDocument /></el-icon>
                  </button>
                  <button
                    class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-lg hover:bg-white/40 transition-all flex items-center justify-center"
                    @click.stop="handleExport(resume)"
                  >
                    <el-icon><Download /></el-icon>
                  </button>
                  <button
                    class="w-10 h-10 bg-white/20 backdrop-blur-md text-white rounded-lg hover:bg-error transition-all flex items-center justify-center"
                    @click.stop="handleDelete(resume)"
                  >
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
              </div>
            </div>
            <div class="p-4 border-t border-outline-variant/30 flex justify-between items-start">
              <div>
                <h3 class="font-title-md text-title-md text-on-surface mb-1">
                  {{ resume.title || '未命名简历' }}
                </h3>
                <p class="text-label-md font-label-md text-on-surface-variant flex items-center gap-1">
                  <el-icon size="12">
                    <Grid />
                  </el-icon> 模板: {{ sceneLabel(resume.scene) }} · {{ resume.templateId || '默认' }}
                </p>
                <p class="text-[11px] text-outline mt-2">
                  {{ formatTime(resume.updatedAt || resume.createdAt) }}
                </p>
              </div>
              <el-dropdown
                trigger="click"
                @command="(cmd: string) => handleResumeAction(cmd, resume)"
                @click.stop
              >
                <button
                  class="p-1 hover:bg-surface-container rounded transition-colors"
                  @click.stop
                >
                  <el-icon><More /></el-icon>
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

          <!-- Create New Card -->
          <button
            class="aspect-[3/4] bg-background border-2 border-dashed border-outline-variant rounded-xl flex flex-col items-center justify-center group hover:border-primary transition-colors cursor-pointer"
            @click="goCreate"
          >
            <div class="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center group-hover:bg-primary group-hover:text-white transition-all mb-4">
              <el-icon size="24">
                <Plus />
              </el-icon>
            </div>
            <span class="font-title-md text-on-surface">新建简历</span>
            <span class="text-label-md text-on-surface-variant">选择模板，快速创建</span>
          </button>
        </div>
      </div>

      <!-- Sidebar / Inspector Drawer -->
      <aside class="w-full lg:w-[360px] flex flex-col gap-stack-lg">
        <!-- Preparedness Score Card -->
        <div class="bg-surface-container-lowest rounded-2xl border border-outline-variant p-6 shadow-sm">
          <div class="flex justify-between items-center mb-6">
            <div class="flex items-center gap-2">
              <h2 class="font-title-md text-title-md">
                求职准备度
              </h2>
              <el-icon class="text-outline text-sm">
                <InfoFilled />
              </el-icon>
            </div>
            <button class="text-primary text-label-md font-label-md flex items-center gap-1 hover:underline">
              查看建议 <el-icon size="12">
                <ArrowRight />
              </el-icon>
            </button>
          </div>
          <div class="flex items-center gap-stack-lg">
            <div class="relative w-32 h-32 flex items-center justify-center">
              <svg class="w-full h-full transform -rotate-90">
                <circle
                  cx="64"
                  cy="64"
                  fill="transparent"
                  r="56"
                  stroke="#eceef1"
                  stroke-width="10"
                />
                <circle
                  cx="64"
                  cy="64"
                  fill="transparent"
                  r="56"
                  stroke="#0057c2"
                  stroke-dasharray="351.8"
                  :stroke-dashoffset="preparednessOffset"
                  stroke-linecap="round"
                  stroke-width="10"
                />
              </svg>
              <div class="absolute inset-0 flex flex-col items-center justify-center">
                <span class="text-3xl font-extrabold text-primary">72</span>
                <span class="text-[10px] text-outline">/100</span>
              </div>
            </div>
            <div class="flex-1 space-y-4">
              <p class="text-body-md text-on-surface-variant leading-relaxed">
                整体表现良好，继续优化可提升竞争力
              </p>
              <div
                v-for="metric in metrics"
                :key="metric.label"
                class="space-y-2"
              >
                <div class="flex justify-between items-center">
                  <span class="text-[11px] text-outline">{{ metric.label }}</span>
                  <span class="text-[11px] font-bold">{{ metric.value }}/100</span>
                </div>
                <div class="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                  <div
                    class="h-full bg-primary"
                    :style="{ width: metric.value + '%' }"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity -->
        <div class="bg-surface-container-lowest rounded-2xl border border-outline-variant p-6 shadow-sm">
          <div class="flex justify-between items-center mb-4">
            <h2 class="font-title-md text-title-md">
              最近活动
            </h2>
            <button class="text-outline text-label-md font-label-md flex items-center gap-1 hover:text-primary transition-colors">
              查看全部 <el-icon size="12">
                <ArrowRight />
              </el-icon>
            </button>
          </div>
          <div class="space-y-4">
            <div
              v-for="activity in recentActivities"
              :key="activity.title"
              class="flex items-center gap-3 group cursor-pointer"
            >
              <div
                class="w-10 h-10 rounded-lg flex items-center justify-center"
                :class="activity.iconBg"
              >
                <el-icon :class="activity.iconColor">
                  <component :is="activity.icon" />
                </el-icon>
              </div>
              <div class="flex-1">
                <h4 class="text-body-md font-bold group-hover:text-primary transition-colors">
                  {{ activity.title }}
                </h4>
                <p class="text-[11px] text-outline">
                  {{ activity.desc }}
                </p>
              </div>
              <span class="text-[11px] text-outline whitespace-nowrap">{{ activity.time }}</span>
            </div>
          </div>
        </div>

        <!-- Task List -->
        <div class="bg-surface-container-lowest rounded-2xl border border-outline-variant p-6 shadow-sm">
          <div class="flex justify-between items-center mb-6">
            <h2 class="font-title-md text-title-md">
              待办清单
            </h2>
            <span class="bg-surface-container text-[11px] px-2 py-0.5 rounded font-bold">共 {{ tasks.length }} 项</span>
          </div>
          <div class="space-y-4">
            <div
              v-for="task in tasks"
              :key="task.title"
              class="flex items-start gap-3"
            >
              <div
                class="mt-1 w-5 h-5 flex items-center justify-center rounded"
                :class="task.done ? 'bg-primary text-on-primary' : 'border-2 border-outline-variant'"
              >
                <el-icon
                  v-if="task.done"
                  size="12"
                >
                  <Check />
                </el-icon>
              </div>
              <div class="flex-1">
                <h4
                  class="text-body-md font-bold text-on-surface"
                  :class="{ 'line-through decoration-outline-variant': task.done }"
                >
                  {{ task.title }}
                </h4>
                <p class="text-[11px] text-outline">
                  {{ task.desc }}
                </p>
              </div>
              <button class="text-primary text-[11px] font-bold hover:underline">
                {{ task.action }}
              </button>
            </div>
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
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/components/layout/MainLayout.vue'
import { resumeApi } from '@/api/resume'
import type { Resume } from '@/types/resume'
import {
  Plus,
  Search,
  Filter,
  Document,
  Edit,
  CopyDocument,
  Download,
  Delete,
  Grid,
  More,
  InfoFilled,
  ArrowRight,
  Check,
  MagicStick
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const resumes = ref<Resume[]>([])
const keyword = ref((route.query.q as string) || '')
const currentTab = ref<'recent' | 'all' | 'published' | 'draft'>('recent')
const renameVisible = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Resume | null>(null)

const tabs = [
  { label: '最近编辑', value: 'recent' as const },
  { label: '全部简历', value: 'all' as const },
  { label: '已发布', value: 'published' as const },
  { label: '草稿', value: 'draft' as const }
]

const defaultThumbnails = [
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCOKj7X1S0R5U_u0nG3A4p1GqEu9u_GNOZVKTd4T6xvYvDm0F1VqaS4LGCZj4tF1fkF-2THPw0YR8cqLXT-6MLpQ4rx5EGmDI8-ZNMbLRtOCpHpcOdf1VpmljjcbehBf3czKgk__B6UzrpQmplktbopo4M2zC-0brztk1JBGhVe2P0VPvbWUkvwYyhk39K1jzLAPaOyaMIaFN8IorkLVG5Y2WrD0ul2MhsmfTXl9Hqaa8EASVlpkbF4xc6NSJwfNXz2B9agZkg7ajM',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuBfnwrMHF9STQqnsUPe29L-T-2Huoh7HoQftk7fePaIHBHT8_Y3hO_8y_1VfMxPEp3UpAsMefcqMCvVc3TwpLcYVf33GVDccBLQDJ_g7TLXKEa0HrOph1tjB1q2bCyd2KFhIzcEDodhpr0OVRNa2BUBDoWXNEFMx9bVdhpvbZ6zM-9mURgPstGFunrxsUd6aq7GzlLivQpUuESWHg659JOTHJckhZBJktZwmmqOHsSExE0WkL-nexFpC-bTgjs8B7oSrtPo0t2Rm0A',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuB4y6WXHAS8AHzR3LwrQ98p5P0c3f5O8eHtDaoM0JrFl5ZuyspkuQcra8dW0zRN1UoGDIo9oa_1Ba4E1RN6HVaADWNOMPONyap-Gc_fsnDpa4VojWc3M03N5gX4F46TCKSovh4h5xx3VosGxAhbJsLYQ8hHmj9ugWCl_N7nSHeQ7OkNP9WECtdHTOIBHtGWX0-b4HpTJybUO03TzKQX_tGMH2098fOQ9TnCRjr5HPviAJKV6gX-BNabHcb5BFT5c05bSyqZCw95qlk'
]

const preparednessOffset = computed(() => {
  const circumference = 2 * Math.PI * 56
  return circumference - (72 / 100) * circumference
})

const metrics = [
  { label: '内容完整度', value: 80 },
  { label: '关键词匹配度', value: 65 },
  { label: '项目亮点', value: 70 }
]

const recentActivities = [
  { title: '前端开发工程师', desc: '已导出 PDF', time: '今天 15:42', icon: Document, iconBg: 'bg-error-container/30', iconColor: 'text-error' },
  { title: '产品实习生', desc: '已复制简历', time: '05-18 10:30', icon: CopyDocument, iconBg: 'bg-primary-fixed/30', iconColor: 'text-primary' },
  { title: '运营专员', desc: 'AI 优化完成', time: '05-16 09:22', icon: MagicStick, iconBg: 'bg-tertiary-fixed/30', iconColor: 'text-tertiary' }
]

const tasks = [
  { title: '完善项目经历', desc: '补充2个关键项目，突出技术成果', done: true, action: '去完善' },
  { title: '添加技能标签', desc: '补充与目标职位匹配的技能', done: false, action: '去完善' },
  { title: '优化个人简介', desc: '让个人亮点更吸引HR', done: false, action: '去完善' },
  { title: '生成投递信', desc: '为意向职位生成个性化投递信', done: false, action: '去生成' }
]

const filteredResumes = computed(() => {
  let list = [...resumes.value]
  if (currentTab.value === 'recent') {
    list.sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
  } else if (currentTab.value === 'published') {
    list = list.filter(r => r.targetPosition)
  } else if (currentTab.value === 'draft') {
    list = list.filter(r => !r.targetPosition)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(r =>
      (r.title || '').toLowerCase().includes(kw) ||
      (r.targetPosition || '').toLowerCase().includes(kw)
    )
  }
  return list
})

function sceneLabel(s: string) {
  const map: Record<string, string> = {
    campus_recruitment: '校招',
    internship: '实习',
    social_recruitment: '社招',
    postgraduate_reexam: '考研',
    project_application: '项目',
    custom: '自定义'
  }
  return map[s] || '默认'
}

function formatTime(d: string) {
  if (!d) return ''
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 86400000)
  if (diff === 0) return '今天 ' + new Date(d).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff === 1) return '昨天 ' + new Date(d).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff < 7) return `${diff}天前`
  return new Date(d).toLocaleDateString('zh-CN')
}

function handleSearch() {
  if (keyword.value.trim()) {
    router.replace({ query: { q: keyword.value.trim() } })
  } else {
    router.replace({ query: {} })
  }
}

function goCreate() {
  router.push('/templates')
}

function openEditor(resume: Resume) {
  router.push(`/editor/${resume.id}`)
}

function handleExport(resume: Resume) {
  router.push(`/resumes/${resume.id}/export`)
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
    const data = await resumeApi.list(1, 50) as any
    resumes.value = data?.records || (Array.isArray(data) ? data : [])
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
.resume-card:hover .resume-overlay {
  opacity: 1;
}
</style>
