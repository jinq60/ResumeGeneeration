<template>
  <div class="editor">
    <!-- Secondary Editor Header -->
    <div class="editor-secondary-header">
      <div class="flex items-center gap-6">
        <button
          class="flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors"
          @click="handleBack"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span>返回简历库</span>
        </button>
        <div class="h-6 w-px bg-outline-variant" />
        <div class="flex items-center gap-2">
          <h1 class="text-title-lg font-title-lg text-on-surface">
            {{ resume?.title || '新建简历' }}
          </h1>
          <el-icon
            class="text-on-surface-variant cursor-pointer hover:text-primary"
            @click="startRename"
          >
            <Edit />
          </el-icon>
        </div>
        <div class="flex items-center gap-2 text-label-md text-outline">
          <span
            class="w-2 h-2 rounded-full"
            :class="saveStatus === 'saved' ? 'bg-secondary' : 'bg-primary'"
          />
          <span>{{ saveStatusLabel }}</span>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <button
          class="flex items-center gap-2 px-4 py-2 border border-outline-variant rounded-lg hover:bg-surface-container-high transition-all text-on-surface"
          @click="handlePreview"
        >
          <el-icon><View /></el-icon>
          <span>预览</span>
        </button>
        <button
          class="flex items-center gap-2 px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 shadow-sm active:scale-[0.98] transition-transform"
          @click="handleExport"
        >
          <el-icon><Document /></el-icon>
          <span class="font-bold">导出 PDF</span>
        </button>
      </div>
    </div>

    <!-- Main Workspace -->
    <main class="editor-workspace">
      <!-- Left Sidebar Navigation -->
      <aside class="editor-sidebar">
        <div class="flex-1 flex flex-col gap-1">
          <button
            v-for="tab in tabs"
            :key="tab.name"
            :class="[
              'flex items-center gap-3 px-4 py-3 mx-2 rounded-lg transition-colors text-left',
              activeTab === tab.name
                ? 'bg-primary-container text-on-primary-container'
                : 'text-surface-variant hover:bg-white/10'
            ]"
            @click="activeTab = tab.name"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
            <span class="text-label-md">{{ tab.label }}</span>
          </button>
        </div>
        <div class="px-4 py-4 mt-auto border-t border-white/10">
          <button class="flex items-center gap-3 text-surface-variant hover:text-white transition-colors">
            <el-icon><Setting /></el-icon>
            <span class="text-label-md">编辑器设置</span>
          </button>
        </div>
      </aside>

      <!-- Left Edit Panel -->
      <section class="editor-form-panel">
        <div class="p-6 border-b border-outline-variant flex justify-between items-center">
          <div class="flex items-center gap-2">
            <el-icon class="text-primary">
              <component :is="currentTab?.icon" />
            </el-icon>
            <h2 class="text-title-md">
              {{ currentTab?.label }}
            </h2>
          </div>
          <button
            v-if="currentTab && !['profile', 'introduction', 'custom'].includes(currentTab.name)"
            class="text-primary flex items-center gap-1 font-semibold text-label-md hover:bg-primary/5 px-2 py-1 rounded"
            @click="addSectionItem"
          >
            <el-icon size="12">
              <Plus />
            </el-icon>
            添加
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
          <div
            v-if="loading"
            class="h-full flex items-center justify-center gap-2 text-on-surface-variant"
          >
            <el-icon
              class="is-spin"
              size="20"
            >
              <Loading />
            </el-icon>
            <span>正在加载简历…</span>
          </div>
          <template v-else-if="resume">
            <ProfileForm
              v-show="activeTab === 'profile'"
              :resume="resume"
              @update="handleBasicInfoUpdate"
            />
            <EducationForm
              v-show="activeTab === 'education'"
              :sections="resume.sections"
              :resume-id="resume.id"
              @update="handleSectionsUpdate"
            />
            <ProjectForm
              v-show="activeTab === 'project'"
              :sections="resume.sections"
              :resume-id="resume.id"
              @update="handleSectionsUpdate"
            />
            <WorkForm
              v-show="activeTab === 'work'"
              :sections="resume.sections"
              :resume-id="resume.id"
              @update="handleSectionsUpdate"
            />
            <SkillForm
              v-show="activeTab === 'skill'"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
            <IntroductionForm
              v-show="activeTab === 'introduction'"
              :sections="resume.sections"
              :resume-id="resume.id"
              @update="handleSectionsUpdate"
            />
            <CustomForm
              v-show="activeTab === 'custom'"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </template>
        </div>
      </section>

      <!-- Middle Canvas Area -->
      <div class="editor-canvas">
        <div class="canvas-scroll">
          <div class="a4-page">
            <ResumePreview
              v-if="resume"
              :resume="resume"
            />
            <div
              v-else
              class="a4-placeholder"
            >
              <el-icon
                size="28"
                class="text-outline"
              >
                <Document />
              </el-icon>
              <p>左侧编辑的内容会实时出现在这里</p>
            </div>
          </div>
        </div>

        <!-- Floating Toolbar -->
        <div class="floating-toolbar">
          <div class="flex items-center gap-3 pr-8 border-r border-outline-variant">
            <button
              class="w-8 h-8 rounded-full hover:bg-surface-container-high flex items-center justify-center"
              @click="zoomOut"
            >
              <el-icon><Minus /></el-icon>
            </button>
            <span class="font-bold text-sm">{{ zoom }}%</span>
            <button
              class="w-8 h-8 rounded-full hover:bg-surface-container-high flex items-center justify-center"
              @click="zoomIn"
            >
              <el-icon><Plus /></el-icon>
            </button>
          </div>
          <div class="flex items-center gap-4 text-label-md text-on-surface-variant">
            第 1 页 / 共 1 页
          </div>
          <div class="h-6 w-px bg-outline-variant" />
        <div class="flex items-center gap-4">
          <button
            class="flex items-center gap-2 px-3 py-2 border border-outline-variant rounded-lg hover:bg-surface-container-high transition-all text-on-surface text-sm"
            @click="sectionsDialogVisible = true"
          >
            <el-icon size="14"><Menu /></el-icon>
            <span>模块管理</span>
          </button>
          <div
            class="flex items-center gap-2 cursor-pointer hover:text-primary transition-colors"
            @click="templateDialogVisible = true"
          >
            <span class="text-label-md">模板：{{ templateName(resume?.templateId) }}</span>
            <el-icon size="12">
              <ArrowDown />
            </el-icon>
          </div>
          <el-icon class="cursor-pointer hover:text-primary">
            <Files />
          </el-icon>
        </div>
        </div>
      </div>

      <!-- Right Inspector Sidebar -->
      <aside class="editor-inspector custom-scrollbar">
        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center gap-2">
            <h3 class="text-title-md">
              AI 简历评估
            </h3>
            <el-tooltip
              content="基于当前简历内容给出的优化建议"
              placement="top"
            >
              <el-icon class="text-outline text-sm cursor-help">
                <InfoFilled />
              </el-icon>
            </el-tooltip>
          </div>
          <button
            class="text-primary text-label-md flex items-center gap-1 hover:underline"
            @click="goAiReview"
          >
            查看详情 <el-icon size="12">
              <ArrowRight />
            </el-icon>
          </button>
        </div>

        <div class="flex items-baseline gap-1 mb-2">
          <span class="text-4xl font-bold text-primary">{{ aiScore }}</span>
          <span class="text-outline text-xl">/ 100</span>
        </div>
        <p class="text-label-md text-on-surface-variant mb-4">
          整体不错，继续优化可显著提升竞争力
        </p>
        <div class="h-1.5 w-full bg-surface-container rounded-full overflow-hidden mb-8">
          <div
            class="h-full bg-primary"
            :style="{ width: aiScore + '%' }"
          />
        </div>

        <div class="space-y-6">
          <!-- Critical Updates -->
          <div class="bg-error/5 border border-error/20 rounded-xl p-4 cursor-pointer hover:bg-error/10 transition-colors group">
            <div class="flex justify-between items-center mb-3">
              <div class="flex items-center gap-2 text-error font-bold">
                <el-icon size="14">
                  <WarningFilled />
                </el-icon>
                <span>最值得修改 (2)</span>
              </div>
              <el-icon
                size="14"
                class="text-error group-hover:translate-x-1 transition-transform"
              >
                <ArrowRight />
              </el-icon>
            </div>
            <ul class="text-xs space-y-3 text-on-surface">
              <li class="relative pl-4 before:content-[''] before:absolute before:left-0 before:top-1.5 before:w-1.5 before:h-1.5 before:bg-error before:rounded-full">
                <span class="font-semibold block mb-0.5">在项目经历中补充量化结果</span>
                <p class="text-outline">
                  用数据展示你的影响力。
                </p>
              </li>
              <li class="relative pl-4 before:content-[''] before:absolute before:left-0 before:top-1.5 before:w-1.5 before:h-1.5 before:bg-error before:rounded-full">
                <span class="font-semibold block mb-0.5">把“会员管理”改为具体成果表述</span>
                <p class="text-outline">
                  突出你带来的业务成效与价值。
                </p>
              </li>
            </ul>
          </div>

          <!-- Potential Improvements -->
          <div class="bg-secondary-container/5 border border-secondary-container/20 rounded-xl p-4 cursor-pointer hover:bg-secondary-container/10 transition-colors group">
            <div class="flex justify-between items-center mb-3 text-secondary">
              <div class="flex items-center gap-2 font-bold">
                <el-icon size="14">
                  <Opportunity />
                </el-icon>
                <span>可增强 (3)</span>
              </div>
              <el-icon
                size="14"
                class="group-hover:translate-x-1 transition-transform"
              >
                <ArrowRight />
              </el-icon>
            </div>
            <ul class="text-xs space-y-3 text-on-surface">
              <li class="relative pl-4 before:content-[''] before:absolute before:left-0 before:top-1.5 before:w-1.5 before:h-1.5 before:bg-secondary before:rounded-full">
                <span class="font-semibold block mb-0.5">在个人简介中加入技术优势关键词</span>
                <p class="text-outline">
                  例如：性能优化、工程化、可视化等。
                </p>
              </li>
              <li class="relative pl-4 before:content-[''] before:absolute before:left-0 before:top-1.5 before:w-1.5 before:h-1.5 before:bg-secondary before:rounded-full">
                <span class="font-semibold block mb-0.5">补充一个代表性 GitHub 链接或技术作品</span>
                <p class="text-outline">
                  增加技术影响力和可信度。
                </p>
              </li>
            </ul>
          </div>

          <!-- Good Practices -->
          <div class="bg-primary/5 border border-primary/20 rounded-xl p-4">
            <div class="flex justify-between items-center mb-3 text-primary">
              <div class="flex items-center gap-2 font-bold">
                <el-icon size="14">
                  <CircleCheckFilled />
                </el-icon>
                <span>已做得好 (3)</span>
              </div>
            </div>
            <ul class="text-xs space-y-2 text-on-surface-variant">
              <li class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 bg-primary rounded-full" /> 结构清晰，模块完整，重点突出
              </li>
              <li class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 bg-primary rounded-full" /> 项目经历有量化结果
              </li>
              <li class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 bg-primary rounded-full" /> 技能区已覆盖主流技术栈
              </li>
            </ul>
          </div>
        </div>

        <div class="mt-8 pt-6 border-t border-outline-variant">
          <h4 class="text-label-md font-bold mb-4 text-on-surface-variant uppercase tracking-wider">
            快速操作
          </h4>
          <div class="grid grid-cols-3 gap-3">
            <button
              class="flex flex-col items-center justify-center p-3 rounded-xl border border-outline-variant hover:border-primary hover:bg-primary/5 transition-all group"
              @click="goAiReview"
            >
              <el-icon class="text-primary mb-2 group-hover:scale-110 transition-transform">
                <MagicStick />
              </el-icon>
              <span class="text-[10px] whitespace-nowrap">AI 优化</span>
            </button>
            <button
              class="flex flex-col items-center justify-center p-3 rounded-xl border border-outline-variant hover:border-primary hover:bg-primary/5 transition-all group"
              @click="activeTab = 'project'"
            >
              <el-icon class="text-primary mb-2 group-hover:scale-110 transition-transform">
                <Reading />
              </el-icon>
              <span class="text-[10px] whitespace-nowrap">改项目</span>
            </button>
            <button
              class="flex flex-col items-center justify-center p-3 rounded-xl border border-outline-variant hover:border-primary hover:bg-primary/5 transition-all group"
              @click="ElMessage.info('优秀案例即将上线')"
            >
              <el-icon class="text-primary mb-2 group-hover:scale-110 transition-transform">
                <Trophy />
              </el-icon>
              <span class="text-[10px] whitespace-nowrap">优秀案例</span>
            </button>
          </div>
        </div>
      </aside>
    </main>
  </div>

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
        @click="confirmRename"
      >
        确认
      </el-button>
    </template>
  </el-dialog>

  <!-- 模块管理：排序 + 显隐 + 标题 -->
  <el-dialog
    v-model="sectionsDialogVisible"
    title="模块管理"
    width="520px"
    align-center
  >
    <div
      v-for="(sec, idx) in resume?.sections || []"
      :key="sec.id"
      class="flex items-center gap-2 py-2 border-b border-outline-variant/30"
    >
      <span class="text-xs text-outline w-5 text-center font-mono">{{ idx + 1 }}</span>
      <el-input
        v-model="sec.title"
        size="small"
        style="flex: 1"
        @change="triggerAutoSave()"
      />
      <span class="text-xs text-outline w-16">
        {{ typeLabel(sec.type) }}
      </span>
      <el-button
        size="small"
        :disabled="idx === 0"
        title="上移"
        @click="moveSection(idx, -1)"
      >
        <el-icon><ArrowUp /></el-icon>
      </el-button>
      <el-button
        size="small"
        :disabled="idx === (resume?.sections?.length || 1) - 1"
        title="下移"
        @click="moveSection(idx, 1)"
      >
        <el-icon><ArrowDown /></el-icon>
      </el-button>
      <el-switch
        v-model="sec.visible"
        size="small"
        title="显示/隐藏"
        @change="triggerAutoSave()"
      />
    </div>
    <p class="text-xs text-outline mt-3">
      隐藏的模块不会出现在预览与导出 PDF 中；个人信息模块不可隐藏。
    </p>
  </el-dialog>

  <!-- 模板切换 -->
  <el-dialog
    v-model="templateDialogVisible"
    title="切换简历模板"
    width="720px"
    align-center
  >
    <div class="grid grid-cols-3 gap-3">
      <div
        v-for="t in templates"
        :key="t.id"
        class="cursor-pointer rounded-xl border-2 overflow-hidden transition-all hover:-translate-y-0.5 hover:shadow-md"
        :class="resume?.templateId === t.id ? 'border-primary' : 'border-outline-variant'"
        @click="selectTemplate(t)"
      >
        <img
          :src="t.thumbnailUrl"
          :alt="t.name"
          class="w-full aspect-[3/4] object-cover bg-surface-container-low"
          @error="onThumbnailError"
        >
        <div class="p-2 text-center text-sm font-medium">
          {{ t.name }}
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElTooltip } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  View,
  Document,
  User,
  School,
  DocumentChecked,
  Briefcase,
  CollectionTag,
  CirclePlus,
  Setting,
  Plus,
  Minus,
  ArrowDown,
  ArrowUp,
  Menu,
  Files,
  InfoFilled,
  ArrowRight,
  WarningFilled,
  Opportunity,
  CircleCheckFilled,
  MagicStick,
  Reading,
  Trophy,
  Loading
} from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import { templateApi } from '@/api/template'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'
import type { Template } from '@/api/template'
import type { Resume } from '@/types/resume'
import ResumePreview from '@/components/preview/ResumePreview.vue'
import ProfileForm from '@/components/editor/ProfileForm.vue'
import EducationForm from '@/components/editor/EducationForm.vue'
import ProjectForm from '@/components/editor/ProjectForm.vue'
import WorkForm from '@/components/editor/WorkForm.vue'
import SkillForm from '@/components/editor/SkillForm.vue'
import IntroductionForm from '@/components/editor/IntroductionForm.vue'
import CustomForm from '@/components/editor/CustomForm.vue'
import { useAutoSave } from '@/composables/useAutoSave'

const router = useRouter()
const route = useRoute()

const resume = ref<Resume | null>(null)
const activeTab = ref('profile')
const loading = ref(true)
const zoom = ref(100)
const aiScore = ref(82)
const renameVisible = ref(false)
const renameTitle = ref('')
const sectionsDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const templates = ref<Template[]>([])
const templateNameMap = ref<Record<string, string>>({})

interface TabDef {
  name: string
  label: string
  icon: typeof User
}

const tabs: TabDef[] = [
  { name: 'profile', label: '基本信息', icon: User },
  { name: 'education', label: '教育背景', icon: School },
  { name: 'project', label: '项目经历', icon: DocumentChecked },
  { name: 'work', label: '工作经历', icon: Briefcase },
  { name: 'skill', label: '技能清单', icon: CollectionTag },
  { name: 'introduction', label: '个人简介', icon: CirclePlus },
  { name: 'custom', label: '补充信息', icon: CirclePlus }
]

const currentTab = computed(() => tabs.find(t => t.name === activeTab.value))

const { saveStatus, triggerSave } = useAutoSave()
const saveError = ref('')

const saveStatusLabel = computed(() => {
  switch (saveStatus.value) {
    case 'saving': return '正在保存'
    case 'saved': return '已保存 · 刚刚'
    case 'error': return saveError.value ? `保存失败：${saveError.value}` : '未能保存，请重试'
    default: return '尚未修改'
  }
})

async function loadResume() {
  loading.value = true
  const id = route.params.id as string
  if (!id) {
    try {
      // 未携带 id 时先取第一个可用模板，避免使用不存在的模板 ID
      const templates = await templateApi.list()
      const templateId = templates[0]?.id
      if (!templateId) {
        ElMessage.error('暂无可用模板，请先在模板中心选择')
        loading.value = false
        return
      }
      const newResume = await resumeApi.create({
        title: '我的简历',
        scene: 'campus_recruitment',
        templateId
      })
      resume.value = newResume
      router.replace(`/workbench/editor/${newResume.id}`)
    } catch (e: any) {
      ElMessage.error(e.message || '创建简历失败')
    }
    return
  }

  try {
    resume.value = await resumeApi.get(id)
  } catch (e: any) {
    ElMessage.error(e.message || '加载简历失败')
  } finally {
    loading.value = false
  }
}

function handleBack() {
  router.push('/workbench/resumes')
}

function handlePreview() {
  if (resume.value) {
    // 新标签页导航无法携带 Authorization 头，改为站内详情/预览页
    router.push(`/workbench/resumes/${resume.value.id}`)
  }
}

function handleExport() {
  if (!resume.value) return
  router.push(`/workbench/resumes/${resume.value.id}/export`)
}

function goAiReview() {
  if (!resume.value) return
  router.push(`/workbench/resumes/${resume.value.id}/review`)
}

function startRename() {
  if (!resume.value) return
  renameTitle.value = resume.value.title
  renameVisible.value = true
}

async function confirmRename() {
  if (resume.value && renameTitle.value.trim()) {
    try {
      await resumeApi.rename(resume.value.id, renameTitle.value.trim())
      resume.value.title = renameTitle.value.trim()
      ElMessage.success('重命名成功')
      renameVisible.value = false
    } catch {
      ElMessage.error('重命名失败')
    }
  }
}

function handleBasicInfoUpdate(data: { title?: string; targetPosition?: string; sections?: any[] }) {
  if (resume.value) {
    if (data.title) resume.value.title = data.title
    if (data.targetPosition !== undefined) resume.value.targetPosition = data.targetPosition
    if (data.sections) resume.value.sections = data.sections
    triggerAutoSave()
  }
}

function handleSectionsUpdate(sections: any[]) {
  if (resume.value) {
    resume.value.sections = sections
    triggerAutoSave()
  }
}

function triggerAutoSave() {
  if (resume.value) {
    triggerSave(async () => {
      saveError.value = ''
      try {
        await resumeApi.update(resume.value!.id, {
          title: resume.value!.title,
          targetPosition: resume.value!.targetPosition,
          templateId: resume.value!.templateId,
          sections: resume.value!.sections
        })
      } catch (e: any) {
        saveError.value = e.message || '未知错误'
        throw e
      }
    })
  }
}

function addSectionItem() {
  // Form components handle add internally; this button can focus the panel.
  ElMessage.info('请在下方表单中添加条目')
}

function zoomIn() {
  if (zoom.value < 150) zoom.value += 10
}

function zoomOut() {
  if (zoom.value > 60) zoom.value -= 10
}

function typeLabel(type: string): string {
  const map: Record<string, string> = {
    profile: '基本信息',
    education: '教育背景',
    project: '项目经历',
    work: '工作经历',
    skill: '技能清单',
    introduction: '个人简介',
    custom: '补充信息'
  }
  return map[type] || type
}

function moveSection(index: number, delta: number) {
  if (!resume.value) return
  const sections = [...resume.value.sections]
  const target = index + delta
  if (target < 0 || target >= sections.length) return
  const moved = sections.splice(index, 1)[0]
  sections.splice(target, 0, moved)
  sections.forEach((s, i) => { s.order = i })
  handleSectionsUpdate(sections)
}

function templateName(id?: string): string {
  if (!id) return '默认'
  return templateNameMap.value[id] || id
}

async function loadTemplates() {
  try {
    templates.value = await templateApi.list()
    templateNameMap.value = Object.fromEntries(templates.value.map(t => [t.id, t.name]))
  } catch {
    // 模板列表加载失败时退化为显示模板 ID
  }
}

function selectTemplate(t: Template) {
  if (!resume.value) return
  resume.value.templateId = t.id
  templateDialogVisible.value = false
  ElMessage.success(`已切换模板：${t.name}`)
  triggerAutoSave()
}

function onThumbnailError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img && img.src !== TEMPLATE_PLACEHOLDER) {
    img.src = TEMPLATE_PLACEHOLDER
  }
}

onMounted(() => {
  loadResume()
  loadTemplates()
})
</script>

<style scoped lang="scss">
.editor {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: calc(100vh - 2 * var(--st-margin-page));
  background: var(--st-surface);
  overflow: hidden;
  margin: calc(-1 * var(--st-margin-page));
}

.editor-secondary-header {
  height: 56px;
  flex-shrink: 0;
  background: var(--st-surface);
  border-bottom: 1px solid var(--st-outline-variant);
  padding: 0 var(--st-margin-page);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.editor-workspace {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-sidebar {
  width: var(--st-sidebar-width);
  background: var(--st-on-primary-fixed);
  border-right: 1px solid var(--st-outline-variant);
  display: flex;
  flex-direction: column;
  padding: var(--st-stack-sm) 0;
  flex-shrink: 0;

  .text-surface-variant {
    color: rgba(255, 255, 255, 0.72);
  }
}

.editor-form-panel {
  width: 480px;
  border-right: 1px solid var(--st-outline-variant);
  background: var(--st-surface-container-lowest);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--st-outline-variant);
  border-radius: 4px;
}

.editor-canvas {
  flex: 1;
  background: var(--st-surface-container-low);
  display: flex;
  flex-direction: column;
  position: relative;
  min-width: 0;
}

.canvas-scroll {
  flex: 1;
  overflow-y: auto;
  padding: var(--st-stack-lg);
  display: flex;
  justify-content: center;
}

.a4-page {
  width: 210mm;
  min-height: 297mm;
  padding: 20mm;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform-origin: top center;
}

.a4-placeholder {
  width: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--st-stack-md);
  color: var(--st-outline);
}

.floating-toolbar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--st-outline-variant);
  padding: 12px 24px;
  border-radius: 9999px;
  box-shadow: var(--st-shadow-md);
  display: flex;
  align-items: center;
  gap: 24px;
  z-index: 40;
}

.editor-inspector {
  width: 360px;
  background: var(--st-surface-container-lowest);
  border-left: 1px solid var(--st-outline-variant);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding: var(--st-margin-page);
  overflow-y: auto;
}

.is-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1280px) {
  .editor-sidebar,
  .editor-inspector {
    display: none;
  }
  .editor-form-panel {
    width: 360px;
  }
}

@media (max-width: 768px) {
  .editor-form-panel {
    position: absolute;
    inset: 0;
    width: 100%;
    z-index: 30;
  }
  .a4-page {
    width: 100%;
    min-height: auto;
    padding: 16px;
  }
}
</style>
