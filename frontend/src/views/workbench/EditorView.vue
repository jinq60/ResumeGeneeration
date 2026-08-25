<template>
  <div
    class="editor"
    :class="[
      editPanelCollapsed ? 'is-edit-panel-collapsed' : '',
      mobileView === 'edit' ? 'is-mobile-edit' : 'is-mobile-preview'
    ]"
  >
    <!-- Secondary Editor Header -->
    <div class="editor-secondary-header">
      <div class="editor-header-leading flex items-center gap-6">
        <button
          class="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
          @click="handleBack"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span class="header-back-label">返回简历库</span>
        </button>
        <div class="h-6 w-px bg-border" />
        <div class="flex items-center gap-2">
          <h1 class="text-headline-md font-serif text-foreground">
            {{ resume?.title || '新建简历' }}
          </h1>
          <el-icon
            class="text-muted-foreground cursor-pointer hover:text-foreground"
            @click="startRename"
          >
            <Edit />
          </el-icon>
        </div>
        <div class="editor-save-status flex items-center gap-2 text-label-md text-muted">
          <span
            class="w-2 h-2 rounded-full"
            :class="saveStatus === 'saved' ? 'bg-secondary' : 'bg-foreground'"
          />
          <span>{{ saveStatusLabel }}</span>
          <button
            v-if="saveStatus === 'error'"
            class="text-label-md text-primary hover:underline"
            @click="retry"
          >
            重试
          </button>
        </div>
      </div>
      <div class="editor-header-actions flex items-center gap-3">
        <button
          class="editor-header-icon panel-toggle-button"
          :title="editPanelCollapsed ? '展开编辑面板' : '收起编辑面板'"
          :aria-label="editPanelCollapsed ? '展开编辑面板' : '收起编辑面板'"
          @click="editPanelCollapsed = !editPanelCollapsed"
        >
          <el-icon>
            <DArrowRight v-if="editPanelCollapsed" />
            <DArrowLeft v-else />
          </el-icon>
        </button>
        <div class="flex items-center gap-1 border-r border-border pr-3">
          <button
            class="editor-header-icon"
            :disabled="!canUndo"
            title="撤销（Ctrl/Cmd + Z）"
            aria-label="撤销"
            @click="undoEdit"
          >
            <el-icon><RefreshLeft /></el-icon>
          </button>
          <button
            class="editor-header-icon"
            :disabled="!canRedo"
            title="重做（Ctrl/Cmd + Shift + Z）"
            aria-label="重做"
            @click="redoEdit"
          >
            <el-icon><RefreshRight /></el-icon>
          </button>
        </div>
        <button
          class="flex items-center gap-2 px-4 py-2 border border-border rounded-lg hover:bg-secondary transition-all text-foreground"
          @click="handlePreview"
        >
          <el-icon><View /></el-icon>
          <span class="header-action-label">预览</span>
        </button>
        <button
          class="editor-export-button flex items-center gap-2 px-6 py-2 bg-foreground rounded-lg hover:opacity-90 shadow-sm active:scale-[0.98] transition-transform"
          @click="handleExport"
        >
          <el-icon><Document /></el-icon>
          <span class="header-action-label font-bold">导出 PDF</span>
        </button>
      </div>
    </div>

    <div
      class="editor-mobile-switcher"
      role="tablist"
      aria-label="编辑器视图切换"
    >
      <button
        :class="{ 'is-active': mobileView === 'edit' }"
        role="tab"
        :aria-selected="mobileView === 'edit'"
        @click="mobileView = 'edit'"
      >
        编辑内容
      </button>
      <button
        :class="{ 'is-active': mobileView === 'preview' }"
        role="tab"
        :aria-selected="mobileView === 'preview'"
        @click="mobileView = 'preview'"
      >
        预览简历
      </button>
    </div>

    <!-- Main Workspace -->
    <main class="editor-workspace">
      <EditorModuleRail
        v-if="resume"
        :sections="resume.sections"
        :active-section-id="activeSectionId"
        :theme-color="activeThemeColor"
        @select-section="selectSection"
        @toggle-visibility="updateSectionVisibility"
        @remove-section="removeSection"
        @reorder-sections="reorderSections"
        @add-custom-section="addCustomSection"
        @select-theme="applyThemeColor"
        @open-settings="openRenderSettings"
      />

      <!-- Module form panel -->
      <section
        class="editor-form-panel"
        :class="{ 'is-collapsed': editPanelCollapsed }"
      >
        <div class="form-panel-heading">
          <el-icon><component :is="sectionIcon(activeTab)" /></el-icon>
          <strong>{{ activeSectionTitle }}</strong>
        </div>
        <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
          <div
            v-if="loading"
            class="h-full flex items-center justify-center gap-2 text-muted-foreground"
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
      <div
        class="editor-canvas"
        :class="{ 'is-panel-collapsed': editPanelCollapsed }"
      >
        <div
          ref="canvasScrollRef"
          class="canvas-scroll"
          @scroll="handleCanvasScroll"
        >
          <div
            class="a4-page-shell"
            :style="a4PageShellStyle"
          >
            <div
              class="a4-page"
              :style="a4PageStyle"
            >
              <ResumePreview
                v-if="resume"
                :resume="resume"
                @loaded="handlePreviewLoaded"
              />
              <div
                v-else
                class="a4-placeholder"
              >
                <el-icon
                  size="28"
                  class="text-muted"
                >
                  <Document />
                </el-icon>
                <p>左侧编辑的内容会实时出现在这里</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Floating Toolbar -->
        <div class="floating-toolbar">
          <div class="flex items-center gap-3 pr-8 border-r border-border">
            <button
              class="w-8 h-8 rounded-full hover:bg-secondary flex items-center justify-center"
              @click="zoomOut"
            >
              <el-icon><Minus /></el-icon>
            </button>
            <span class="font-bold text-sm">{{ zoom }}%</span>
            <button
              class="w-8 h-8 rounded-full hover:bg-secondary flex items-center justify-center"
              @click="zoomIn"
            >
              <el-icon><Plus /></el-icon>
            </button>
          </div>
          <div class="flex items-center gap-2 text-label-md text-muted-foreground">
            <button
              class="page-button"
              :disabled="currentPage <= 1"
              title="上一页"
              aria-label="上一页"
              @click="goToPage(-1)"
            >
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span>第 {{ currentPage }} 页 / 共 {{ pageCount }} 页</span>
            <button
              class="page-button"
              :disabled="currentPage >= pageCount"
              title="下一页"
              aria-label="下一页"
              @click="goToPage(1)"
            >
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div class="h-6 w-px bg-border" />
          <div
            class="flex items-center gap-2 cursor-pointer hover:text-foreground transition-colors"
            @click="aiDrawerVisible = true"
          >
            <el-icon size="14">
              <MagicStick />
            </el-icon>
            <span class="text-label-md">AI 评估</span>
          </div>
          <div
            class="flex items-center gap-2 cursor-pointer hover:text-foreground transition-colors"
            @click="runGrammarCheck"
          >
            <el-icon size="14">
              <DocumentChecked />
            </el-icon>
            <span class="text-label-md">语法检查</span>
          </div>
          <div
            class="flex items-center gap-2 cursor-pointer hover:text-foreground transition-colors"
            @click="templateDialogVisible = true"
          >
            <span class="text-label-md">模板：{{ templateName(resume?.templateId) }}</span>
            <el-icon size="12">
              <ArrowDown />
            </el-icon>
          </div>
          <div
            class="flex items-center gap-2 cursor-pointer hover:text-foreground transition-colors"
            @click="openRenderSettings"
          >
            <el-icon size="14">
              <Setting />
            </el-icon>
            <span class="text-label-md">排版</span>
          </div>
        </div>
      </div>
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
      class="flex items-center gap-2 py-2 border-b border-border/30"
    >
      <span class="text-xs text-muted w-5 text-center font-mono">{{ idx + 1 }}</span>
      <el-input
        :model-value="sec.title"
        size="small"
        style="flex: 1"
        @update:model-value="updateSectionTitle(sec.id, $event)"
      />
      <span class="text-xs text-muted w-16">
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
        :model-value="sec.visible"
        size="small"
        title="显示/隐藏"
        @update:model-value="updateSectionVisibility(sec.id, $event)"
      />
    </div>
    <el-button
      type="primary"
      plain
      class="mt-3"
      @click="addCustomSection"
    >
      <el-icon><Plus /></el-icon>
      添加自定义模块
    </el-button>
    <p class="text-xs text-muted mt-3">
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
        :class="resume?.templateId === t.id ? 'border-foreground' : 'border-border'"
        @click="selectTemplate(t)"
      >
        <img
          :src="t.thumbnailUrl"
          :alt="t.name"
          class="w-full aspect-[3/4] object-cover bg-secondary"
          @error="onThumbnailError"
        >
        <div class="p-2 text-center text-sm font-medium">
          {{ t.name }}
        </div>
      </div>
    </div>
  </el-dialog>

  <!-- 排版设置 -->
  <el-dialog
    v-model="settingsDialogVisible"
    title="排版设置"
    width="460px"
    align-center
  >
    <el-form
      :model="renderSettingsDraft"
      label-position="top"
      class="render-settings-form"
    >
      <el-form-item label="一页纸适配">
        <div class="setting-control-row">
          <el-switch v-model="renderSettingsDraft.autoOnePage" />
          <span>内容较长时自动缩小，尽量控制在一张 A4 内</span>
        </div>
      </el-form-item>
      <el-form-item label="正文字体">
        <el-select
          v-model="renderSettingsDraft.fontFamily"
          class="w-full"
        >
          <el-option
            label="思源黑体 / 微软雅黑"
            value="&quot;Noto Sans SC&quot;, &quot;Microsoft YaHei&quot;, sans-serif"
          />
          <el-option
            label="Arial 无衬线"
            value="Arial, sans-serif"
          />
          <el-option
            label="思源黑体"
            value="&quot;Source Han Sans SC&quot;, &quot;Noto Sans SC&quot;, sans-serif"
          />
          <el-option
            label="宋体"
            value="&quot;SimSun&quot;, serif"
          />
        </el-select>
      </el-form-item>
      <div class="grid grid-cols-2 gap-3">
        <el-form-item label="字号（pt）">
          <el-input-number
            v-model="renderSettingsDraft.baseFontSize"
            :min="8"
            :max="16"
            :step="0.5"
            controls-position="right"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="行高">
          <el-input-number
            v-model="renderSettingsDraft.lineHeight"
            :min="1"
            :max="2.2"
            :step="0.1"
            :precision="1"
            controls-position="right"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="页面边距（mm）">
          <el-input-number
            v-model="renderSettingsDraft.pagePadding"
            :min="8"
            :max="30"
            :step="1"
            controls-position="right"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="模块间距（px）">
          <el-input-number
            v-model="renderSettingsDraft.sectionSpacing"
            :min="4"
            :max="32"
            :step="1"
            controls-position="right"
            class="w-full"
          />
        </el-form-item>
      </div>
      <el-form-item label="主题色">
        <div class="setting-control-row">
          <el-color-picker v-model="renderSettingsDraft.accentColor" />
          <span class="font-mono text-sm">{{ renderSettingsDraft.accentColor }}</span>
        </div>
      </el-form-item>
    </el-form>
    <p class="render-settings-note">
      设置会同步应用到实时预览、PDF 和 Word 导出。
    </p>
    <template #footer>
      <el-button @click="resetRenderSettings">
        恢复建议值
      </el-button>
      <el-button @click="settingsDialogVisible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        @click="confirmRenderSettings"
      >
        应用设置
      </el-button>
    </template>
  </el-dialog>

  <!-- AI 评估抽屉 -->
  <el-drawer
    v-model="aiDrawerVisible"
    title="AI 简历评估"
    size="380px"
    @open="loadAiAssessment"
  >
    <div
      v-if="aiAssessmentLoading"
      class="flex items-center justify-center gap-2 text-muted-foreground py-16"
    >
      <el-icon class="is-spin">
        <Loading />
      </el-icon>
      <span class="text-sm">正在加载最新点评…</span>
    </div>

    <template v-else-if="aiAssessment">
      <div class="flex items-baseline gap-1 mb-2">
        <span class="text-4xl font-bold text-foreground">{{ aiAssessment.overallScore ?? '—' }}</span>
        <span class="text-muted text-xl">/ 100</span>
      </div>
      <p class="text-label-md text-muted-foreground mb-4">
        {{ aiAssessmentComment }}
      </p>
      <div class="h-1.5 w-full bg-background-container rounded-full overflow-hidden mb-6">
        <div
          class="h-full bg-foreground"
          :style="{ width: Math.min(100, aiAssessment.overallScore ?? 0) + '%' }"
        />
      </div>
      <div class="space-y-4">
        <div
          v-if="aiAssessment.highlights?.length"
          class="p-4 border border-border rounded-lg"
        >
          <div class="flex items-center gap-2 text-sm font-semibold mb-2 text-muted-foreground">
            <el-icon size="14">
              <Opportunity />
            </el-icon>
            <span>已做得好</span>
          </div>
          <ul class="text-xs space-y-2 text-muted-foreground">
            <li
              v-for="(h, i) in aiAssessment.highlights"
              :key="i"
            >
              {{ h }}
            </li>
          </ul>
        </div>
        <div
          v-if="aiAssessment.suggestions?.length"
          class="p-4 border border-border rounded-lg"
        >
          <div class="flex items-center gap-2 text-sm font-semibold mb-2 text-destructive">
            <el-icon size="14">
              <WarningFilled />
            </el-icon>
            <span>最值得修改</span>
          </div>
          <ul class="text-xs space-y-2 text-muted-foreground">
            <li
              v-for="(s, i) in aiAssessment.suggestions.slice(0, 5)"
              :key="i"
            >
              {{ s.title || s.advice || s.problem }}
            </li>
          </ul>
        </div>
        <div
          v-if="aiAssessment.missingSkills?.length"
          class="p-4 border border-border rounded-lg"
        >
          <div class="flex items-center gap-2 text-sm font-semibold mb-2 text-muted-foreground">
            <el-icon size="14">
              <CollectionTag />
            </el-icon>
            <span>建议补充的技能</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(skill, i) in aiAssessment.missingSkills"
              :key="i"
              class="bg-secondary text-muted-foreground px-2.5 py-1 text-xs rounded-full"
            >{{ skill }}</span>
          </div>
        </div>
      </div>
    </template>

    <el-empty
      v-else
      description="暂无 AI 点评结果，去生成一份点评报告吧"
      :image-size="90"
    />

    <div class="mt-6">
      <el-button
        type="primary"
        class="w-full"
        @click="goAiReview"
      >
        查看 AI 点评详情
      </el-button>
    </div>
  </el-drawer>

  <!-- AI 语法检查抽屉 -->
  <el-drawer
    v-model="grammarDrawerVisible"
    title="语法检查"
    size="420px"
  >
    <div
      v-if="grammarLoading"
      class="grammar-loading"
    >
      <el-icon class="is-spin">
        <Loading />
      </el-icon>
      <span>正在检查简历表达…</span>
    </div>
    <el-alert
      v-else-if="grammarResult?.status === 'unavailable'"
      title="语法检查暂不可用"
      :description="grammarResult.message || '请先配置 AI 供应商。'"
      type="info"
      :closable="false"
      show-icon
    />
    <el-empty
      v-else-if="!grammarResult || grammarResult.issues.length === 0"
      description="暂未发现明显表达问题"
      :image-size="90"
    />
    <div
      v-else
      class="grammar-issues"
    >
      <div class="grammar-summary">
        找到 {{ grammarResult.issues.length }} 处可优化表达
      </div>
      <button
        v-for="(issue, index) in grammarResult.issues"
        :key="`${issue.sectionType}-${issue.field}-${index}`"
        class="grammar-issue"
        type="button"
        @click="focusGrammarIssue(issue)"
      >
        <div class="grammar-issue-head">
          <span>{{ grammarFieldLabel(issue) }}</span>
          <el-tag
            size="small"
            :type="grammarSeverityType(issue.severity)"
          >
            {{ grammarSeverityLabel(issue.severity) }}
          </el-tag>
        </div>
        <p class="grammar-original">
          “{{ issue.originalText }}”
        </p>
        <p class="grammar-suggestion">
          {{ issue.suggestion }}
        </p>
        <p class="grammar-explanation">
          {{ issue.explanation }}
        </p>
      </button>
    </div>
    <template #footer>
      <el-button
        :loading="grammarLoading"
        type="primary"
        plain
        @click="runGrammarCheck"
      >
        重新检查
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  View,
  Hide,
  Document,
  User,
  School,
  DocumentChecked,
  Briefcase,
  CollectionTag,
  CirclePlus,
  Plus,
  Minus,
  ArrowDown,
  ArrowUp,
  ArrowRight,
  DArrowLeft,
  DArrowRight,
  Menu,
  RefreshLeft,
  RefreshRight,
  WarningFilled,
  Opportunity,
  MagicStick,
  Loading,
  Setting,
  Delete
} from '@element-plus/icons-vue'
import { resumeApi, type GrammarCheckResponse, type GrammarIssue } from '@/api/resume'
import { templateApi } from '@/api/template'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'
import type { Template } from '@/api/template'
import type { Resume, Section } from '@/types/resume'
import {
  DEFAULT_RENDER_SETTINGS,
  normalizeRenderSettings,
  type EffectiveRenderSettings
} from '@/utils/renderSettings'
import ResumePreview from '@/components/preview/ResumePreview.vue'
import ProfileForm from '@/components/editor/ProfileForm.vue'
import EducationForm from '@/components/editor/EducationForm.vue'
import ProjectForm from '@/components/editor/ProjectForm.vue'
import WorkForm from '@/components/editor/WorkForm.vue'
import SkillForm from '@/components/editor/SkillForm.vue'
import IntroductionForm from '@/components/editor/IntroductionForm.vue'
import CustomForm from '@/components/editor/CustomForm.vue'
import EditorModuleRail from '@/components/editor/EditorModuleRail.vue'
import { useAutoSave } from '@/composables/useAutoSave'
import { useResumeDraft } from '@/composables/useResumeDraft'
import { useResumeHistory } from '@/composables/useResumeHistory'
import { useResumeConflict, isVersionConflictError } from '@/composables/useResumeConflict'

const router = useRouter()
const route = useRoute()

const resume = ref<Resume | null>(null)
const activeTab = ref('profile')
const activeSectionId = ref('profile')
const loading = ref(true)
const desktopDefaultZoom = typeof window !== 'undefined' && window.innerWidth <= 1440 ? 60 : 85
const zoom = ref(desktopDefaultZoom)
const editPanelCollapsed = ref(false)
const mobileView = ref<'edit' | 'preview'>('edit')
const currentPage = ref(1)
const pageCount = ref(1)
const canvasScrollRef = ref<HTMLElement | null>(null)
const aiAssessment = ref<{
  overallScore?: number
  highlights?: string[]
  suggestions?: Array<{ title?: string; advice?: string; problem?: string }>
  missingSkills?: string[]
} | null>(null)
const aiAssessmentLoading = ref(false)
const renameVisible = ref(false)
const renameTitle = ref('')
const sectionsDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const aiDrawerVisible = ref(false)
const settingsDialogVisible = ref(false)
const renderSettingsDraft = ref<EffectiveRenderSettings>({ ...DEFAULT_RENDER_SETTINGS })
const grammarDrawerVisible = ref(false)
const grammarLoading = ref(false)
const grammarResult = ref<GrammarCheckResponse | null>(null)
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

const { saveStatus, triggerSave, flush, retry } = useAutoSave()
const { resolveConflict } = useResumeConflict()
const saveError = ref('')
const { canUndo, canRedo, reset: resetHistory, record, undo, redo } = useResumeHistory<Resume>()
const draftStorage = useResumeDraft<Resume>()

const A4_PAGE_HEIGHT_PX = 1123
const DRAFT_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000

const a4PageScale = computed(() => zoom.value / 100)
const a4PageShellStyle = computed(() => ({
  width: `${210 * a4PageScale.value}mm`,
  minHeight: `${297 * a4PageScale.value}mm`
}))
const a4PageStyle = computed(() => ({
  transform: `scale(${a4PageScale.value})`
}))
const activeSection = computed(() => resume.value?.sections.find(section => section.id === activeSectionId.value))
const activeSectionTitle = computed(() => activeSection.value?.title || typeLabel(activeTab.value))
const activeThemeColor = computed(() => normalizeRenderSettings(resume.value?.renderSettings).accentColor)

const saveStatusLabel = computed(() => {
  switch (saveStatus.value) {
    case 'saving': return '正在保存'
    case 'saved': return '已保存 · 刚刚'
    case 'error': return saveError.value ? `保存失败：${saveError.value}` : '未能保存，请重试'
    default: return '尚未修改'
  }
})

function cloneResumeState(value: Resume): Resume {
  if (typeof structuredClone === 'function') {
    return structuredClone(value)
  }
  return JSON.parse(JSON.stringify(value)) as Resume
}

function applyResumeChange(
  change: (current: Resume) => void,
  historyGroup = activeTab.value,
  shouldSave = true
) {
  if (!resume.value) return

  const previous = cloneResumeState(resume.value)
  change(resume.value)

  if (JSON.stringify(previous) === JSON.stringify(resume.value)) return

  record(previous, historyGroup)
  draftStorage.save(resume.value.id, cloneResumeState(resume.value))
  if (shouldSave) triggerAutoSave()
}

async function restoreDraftIfAvailable(serverResume: Resume) {
  const draft = draftStorage.load(serverResume.id)
  if (!draft) return

  if (Date.now() - draft.savedAt > DRAFT_MAX_AGE_MS) {
    draftStorage.clear(serverResume.id)
    return
  }

  if (JSON.stringify(draft.data) === JSON.stringify(serverResume)) {
    draftStorage.clear(serverResume.id)
    return
  }

  try {
    await ElMessageBox.confirm(
      '检测到一份尚未同步到服务器的本地草稿，是否恢复？',
      '恢复本地草稿',
      {
        confirmButtonText: '恢复草稿',
        cancelButtonText: '使用服务器版本',
        type: 'warning'
      }
    )
    resume.value = cloneResumeState(draft.data)
    resetActiveSection(resume.value)
    resetHistory()
    record(serverResume, 'draft-restore')
    triggerAutoSave()
  } catch {
    draftStorage.clear(serverResume.id)
  }
}

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
      resetActiveSection(newResume)
      resetHistory()
      router.replace(`/workbench/editor/${newResume.id}`)
    } catch (e: any) {
      ElMessage.error(e.message || '创建简历失败')
    } finally {
      loading.value = false
    }
    return
  }

  try {
    const serverResume = await resumeApi.get(id)
    resume.value = serverResume
    resetActiveSection(serverResume)
    resetHistory()
    await restoreDraftIfAvailable(serverResume)
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

async function loadAiAssessment() {
  if (!resume.value) return
  aiAssessmentLoading.value = true
  aiAssessment.value = null
  try {
    const review = await resumeApi.getLatestReview(resume.value.id)
    if (review?.overallScore != null) {
      aiAssessment.value = review
    }
  } catch {
    // 点评数据加载失败时展示空状态
  } finally {
    aiAssessmentLoading.value = false
  }
}

const aiAssessmentComment = computed(() => {
  const score = aiAssessment.value?.overallScore
  if (score == null) return '暂无 AI 点评结果'
  if (score >= 80) return '整体不错，继续优化可显著提升竞争力'
  if (score >= 60) return '简历基础扎实，按建议优化后可明显提升匹配度'
  return '建议根据下方点评逐项优化，快速提升竞争力'
})

function startRename() {
  if (!resume.value) return
  renameTitle.value = resume.value.title
  renameVisible.value = true
}

async function confirmRename() {
  if (resume.value && renameTitle.value.trim()) {
    try {
      const title = renameTitle.value.trim()
      await resumeApi.rename(resume.value.id, title)
      applyResumeChange((current) => {
        current.title = title
      }, 'title', false)
      draftStorage.clear(resume.value.id)
      ElMessage.success('重命名成功')
      renameVisible.value = false
    } catch {
      ElMessage.error('重命名失败')
    }
  }
}

function handleBasicInfoUpdate(data: { title?: string; targetPosition?: string; sections?: Section[] }) {
  applyResumeChange((current) => {
    if (data.title) current.title = data.title
    if (data.targetPosition !== undefined) current.targetPosition = data.targetPosition
    if (data.sections) current.sections = data.sections
  }, 'profile')
}

async function runGrammarCheck() {
  if (!resume.value) return
  grammarDrawerVisible.value = true
  grammarLoading.value = true
  grammarResult.value = null
  try {
    grammarResult.value = await resumeApi.grammarCheck(resume.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '语法检查失败，请稍后重试')
  } finally {
    grammarLoading.value = false
  }
}

function focusGrammarIssue(issue: GrammarIssue) {
  const supportedTabs = tabs.map((tab) => tab.name)
  if (supportedTabs.includes(issue.sectionType)) {
    activeTab.value = issue.sectionType
    const matchingSection = resume.value?.sections.find(section => section.type === issue.sectionType)
    if (matchingSection) activeSectionId.value = matchingSection.id
  }
  grammarDrawerVisible.value = false
}

function grammarFieldLabel(issue: GrammarIssue) {
  const sectionLabel = typeLabel(issue.sectionType)
  return issue.itemIndex === undefined
    ? `${sectionLabel} · ${issue.field}`
    : `${sectionLabel} ${issue.itemIndex + 1} · ${issue.field}`
}

function grammarSeverityLabel(severity: string) {
  return severity === 'high' ? '重点' : severity === 'low' ? '轻微' : '建议'
}

function grammarSeverityType(severity: string) {
  return severity === 'high' ? 'danger' : severity === 'low' ? 'info' : 'warning'
}

function handleSectionsUpdate(sections: Section[]) {
  applyResumeChange((current) => {
    current.sections = sections
  })
}

function openRenderSettings() {
  if (!resume.value) return
  renderSettingsDraft.value = normalizeRenderSettings(resume.value.renderSettings)
  settingsDialogVisible.value = true
}

function resetRenderSettings() {
  renderSettingsDraft.value = { ...DEFAULT_RENDER_SETTINGS }
}

function confirmRenderSettings() {
  if (!resume.value) return
  const settings = { ...renderSettingsDraft.value }
  applyResumeChange((current) => {
    current.renderSettings = settings
  }, 'render-settings')
  settingsDialogVisible.value = false
  ElMessage.success('排版设置已应用')
}

/**
 * 乐观锁冲突（业务码 2012 / HTTP 409）处理：
 * 询问用户是否加载服务器最新版本；确认前先把当前草稿备份到本地存储以便恢复。
 */
async function handleSaveConflict() {
  if (!resume.value) return
  await resolveConflict({
    backupDraft: () => {
      if (resume.value) {
        draftStorage.save(resume.value.id, cloneResumeState(resume.value))
      }
    },
    fetchLatest: async () => {
      const current = resume.value!
      const serverResume = await resumeApi.get(current.id)
      resume.value = serverResume
      resetActiveSection(serverResume)
      resetHistory()
      record(serverResume, 'conflict-restore')
      saveError.value = ''
    }
  })
}

function triggerAutoSave() {
  if (resume.value) {
    triggerSave(async () => {
      saveError.value = ''
      const snapshot = cloneResumeState(resume.value!)
      try {
        await resumeApi.update(snapshot.id, {
          title: snapshot.title,
          targetPosition: snapshot.targetPosition,
          templateId: snapshot.templateId,
          sections: snapshot.sections,
          renderSettings: snapshot.renderSettings || undefined,
          // 期望版本号：后端 V13 乐观锁校验，冲突返回业务码 2012/HTTP 409
          version: (snapshot as unknown as { version?: number }).version
        })
        if (resume.value && JSON.stringify(resume.value) === JSON.stringify(snapshot)) {
          draftStorage.clear(snapshot.id)
        }
      } catch (e: any) {
        saveError.value = e.message || '未知错误'
        if (isVersionConflictError(e)) {
          void handleSaveConflict()
        }
        throw e
      }
    })
  }
}

function zoomIn() {
  if (zoom.value < 150) zoom.value += 10
}

function zoomOut() {
  if (zoom.value > 60) zoom.value -= 10
}

let previewInitialized = false
function handlePreviewLoaded(contentHeight: number) {
  const nextCount = Math.max(1, Math.ceil(contentHeight / A4_PAGE_HEIGHT_PX))
  const countChanged = nextCount !== pageCount.value
  pageCount.value = nextCount
  if (!previewInitialized) {
    // 首次加载定位到第一页
    previewInitialized = true
    currentPage.value = 1
  } else if (countChanged) {
    // 仅页数变化时同步，保留用户当前页（越界时收敛到有效范围），
    // 避免预览每次刷新回调都把用户正在查看的页码强制归 1
    currentPage.value = Math.min(pageCount.value, Math.max(1, currentPage.value))
  }
}

function handleCanvasScroll(event: Event) {
  const target = event.currentTarget as HTMLElement
  const pageHeight = A4_PAGE_HEIGHT_PX * a4PageScale.value
  currentPage.value = Math.min(
    pageCount.value,
    Math.max(1, Math.floor((target.scrollTop + pageHeight / 2) / pageHeight) + 1)
  )
}

function goToPage(delta: number) {
  const nextPage = Math.min(pageCount.value, Math.max(1, currentPage.value + delta))
  if (nextPage === currentPage.value) return

  currentPage.value = nextPage
  canvasScrollRef.value?.scrollTo({
    top: (nextPage - 1) * A4_PAGE_HEIGHT_PX * a4PageScale.value,
    behavior: 'smooth'
  })
}

function updateSectionTitle(sectionId: string, title: string) {
  applyResumeChange((current) => {
    const section = current.sections.find((item) => item.id === sectionId)
    if (section) section.title = title
  }, 'section-title')
}

function updateSectionVisibility(sectionId: string, visible: string | number | boolean) {
  if (sectionId === 'profile') return
  applyResumeChange((current) => {
    const section = current.sections.find((item) => item.id === sectionId)
    if (section) section.visible = Boolean(visible)
  }, 'section-visibility')
}

function resetActiveSection(current: Resume) {
  const profile = current.sections.find(section => section.type === 'profile') || current.sections[0]
  if (!profile) return
  activeSectionId.value = profile.id
  activeTab.value = profile.type
}

function selectSection(sectionId: string) {
  const section = resume.value?.sections.find(item => item.id === sectionId)
  if (!section) return
  activeSectionId.value = section.id
  activeTab.value = section.type
}

function applyThemeColor(color: string) {
  if (!resume.value) return
  applyResumeChange((current) => {
    current.renderSettings = { ...normalizeRenderSettings(current.renderSettings), accentColor: color }
  }, 'theme-color')
}

function reorderSections(orderedIds: string[]) {
  if (!resume.value) return
  const byId = new Map(resume.value.sections.map(section => [section.id, section]))
  const profile = resume.value.sections.filter(section => section.type === 'profile')
  const ordered = orderedIds.map(id => byId.get(id)).filter((section): section is Section => Boolean(section))
  const remaining = resume.value.sections.filter(section => section.type !== 'profile' && !orderedIds.includes(section.id))
  handleSectionsUpdate([...profile, ...ordered, ...remaining].map((section, index) => ({ ...section, order: index })))
}

function sectionIcon(type: string) {
  const iconMap: Record<string, typeof User> = {
    profile: User,
    education: School,
    project: DocumentChecked,
    work: Briefcase,
    skill: CollectionTag,
    introduction: CirclePlus,
    custom: CirclePlus
  }
  return iconMap[type] || CirclePlus
}

function removeSection(sectionId: string) {
  if (!resume.value) return
  const target = resume.value.sections.find(section => section.id === sectionId)
  if (!target || target.type === 'profile') return
  const remaining = resume.value.sections.filter(section => section.id !== sectionId)
  handleSectionsUpdate(remaining)
  if (activeSectionId.value === sectionId) activeSectionId.value = 'profile'
  if (activeTab.value === target.type && !remaining.some(section => section.type === target.type)) {
    activeTab.value = 'profile'
  }
}

const draggedSectionId = ref<string | null>(null)

function startSectionDrag(sectionId: string, event: DragEvent) {
  draggedSectionId.value = sectionId
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', sectionId)
  }
}

function dropSection(targetId: string) {
  if (!resume.value || !draggedSectionId.value || draggedSectionId.value === targetId) return
  const sections = [...resume.value.sections]
  const fromIndex = sections.findIndex(section => section.id === draggedSectionId.value)
  const toIndex = sections.findIndex(section => section.id === targetId)
  if (fromIndex < 0 || toIndex < 0) return
  const [moved] = sections.splice(fromIndex, 1)
  sections.splice(toIndex, 0, moved)
  handleSectionsUpdate(sections.map((section, index) => ({ ...section, order: index })))
  draggedSectionId.value = null
}

function addCustomSection() {
  if (!resume.value) return
  const count = resume.value.sections.filter(section => section.type === 'custom').length
  const section: Section = {
    id: `custom_${Date.now()}`,
    type: 'custom',
    title: `自定义模块 ${count + 1}`,
    order: resume.value.sections.length,
    visible: true,
    data: { content: '' }
  }
  handleSectionsUpdate([...resume.value.sections, section])
  activeTab.value = 'custom'
  activeSectionId.value = section.id
  sectionsDialogVisible.value = false
}

function undoEdit() {
  if (!resume.value) return
  const snapshot = undo(resume.value)
  if (!snapshot) return
  resume.value = snapshot
  draftStorage.save(snapshot.id, cloneResumeState(snapshot))
  triggerAutoSave()
}

function redoEdit() {
  if (!resume.value) return
  const snapshot = redo(resume.value)
  if (!snapshot) return
  resume.value = snapshot
  draftStorage.save(snapshot.id, cloneResumeState(snapshot))
  triggerAutoSave()
}

function isEditableTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false
  return Boolean(
    target.isContentEditable ||
    target.closest('[contenteditable="true"]') ||
    target instanceof HTMLInputElement ||
    target instanceof HTMLTextAreaElement ||
    target instanceof HTMLSelectElement
  )
}

function handleEditorKeydown(event: KeyboardEvent) {
  const modifierPressed = event.ctrlKey || event.metaKey
  if (!modifierPressed) return

  const key = event.key.toLowerCase()
  if (key === 's') {
    event.preventDefault()
    flush()
    return
  }

  if (isEditableTarget(event.target)) return

  if (key === 'z' && !event.shiftKey) {
    event.preventDefault()
    undoEdit()
  } else if (key === 'y' || (key === 'z' && event.shiftKey)) {
    event.preventDefault()
    redoEdit()
  }
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
  applyResumeChange((current) => {
    current.templateId = t.id
  }, 'template')
  templateDialogVisible.value = false
  ElMessage.success(`已切换模板：${t.name}`)
}

function onThumbnailError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img && img.src !== TEMPLATE_PLACEHOLDER) {
    img.src = TEMPLATE_PLACEHOLDER
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleEditorKeydown)
  loadResume()
  loadTemplates()
})

watch(() => loading.value, (isLoading) => {
  if (!isLoading && route.query.grammar === '1' && resume.value) {
    runGrammarCheck()
    router.replace({ query: {} })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEditorKeydown)
})
</script>

<style scoped lang="scss">
.editor {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: calc(100vh - 2 * var(--spacing-margin-page));
  background: #f6f6f4;
  overflow: hidden;
  margin: calc(-1 * var(--spacing-margin-page));
}

.editor-secondary-header {
  height: 64px;
  flex-shrink: 0;
  background: #fff;
  border-bottom: 1px solid #e7e7e4;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.editor-header-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted-foreground);
  transition: background-color 160ms ease, color 160ms ease;

  &:hover:not(:disabled) {
    background: var(--color-secondary);
    color: var(--color-foreground);
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

.editor-header-leading,
.editor-header-actions {
  min-width: 0;
}

.editor-header-leading {
  flex: 1;
}

.editor-header-actions {
  flex-shrink: 0;
}

.editor-export-button,
.editor-export-button .el-icon,
.editor-export-button .header-action-label {
  color: #fff;
}

.editor-mobile-switcher {
  display: none;
}

.editor-workspace {
  flex: 1;
  display: flex;
  overflow: hidden;
  overflow-x: hidden;
  margin-top: 16px;
  border-top: 1px solid #e4e4e1;
}

.editor-tabs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: #fafaf8;
  border-right: 1px solid #e4e4e1;
  overflow-y: auto;
  overflow-x: hidden;
  flex-shrink: 0;
}

.editor-tab {
  flex-shrink: 0;
  width: 100%;
  min-height: 58px;
  justify-content: flex-start;
  padding: 0 14px !important;
  border: 1px solid #e3e3df;
  border-radius: 14px !important;
  background: #fff;
  color: #22221f;

  &:hover {
    border-color: #b8b8b1;
    background: #fff;
  }

  &.bg-secondary {
    border: 2px solid #20201d;
    background: #fff;
  }
}

.editor-tab-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.editor-tab-delete,
.editor-tab-visibility {
  flex: 0 0 auto;
  color: #777771;
}

.editor-tab-delete:hover {
  color: #d92d20;
}

.editor-form-panel {
  width: clamp(390px, 31vw, 520px);
  border-right: 1px solid var(--color-border);
  background: #fffdf9;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 180ms ease, opacity 180ms ease;

  &.is-collapsed {
    width: 0;
    opacity: 0;
    overflow: hidden;
    border-right: 0;
    pointer-events: none;
  }
}

.form-panel-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid #e8e7e2;
  background: #fff;
  color: #22211d;
  font-size: 16px;
}

.editor-form-panel > .flex-1 {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 4px;
}

.editor-canvas {
  flex: 1;
  background: #f0f1f0;
  display: flex;
  flex-direction: column;
  position: relative;
  min-width: 0;
}

.canvas-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 32px 40px;
  display: flex;
  justify-content: center;
}

.a4-page-shell {
  flex: 0 0 auto;
  transition: width 160ms ease, min-height 160ms ease;
}

.a4-page {
  width: 210mm;
  min-height: 297mm;
  box-sizing: border-box;
  padding: 0;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform-origin: top center;
  transition: transform 160ms ease;
}

.page-button {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted-foreground);

  &:hover:not(:disabled) {
    background: var(--color-secondary);
    color: var(--color-foreground);
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

.grammar-loading {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-muted-foreground);
}

.setting-control-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
}

.render-settings-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.render-settings-note {
  margin: 0;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--color-secondary);
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
}

.grammar-summary {
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: var(--radius-md);
  color: var(--color-muted-foreground);
  background: var(--color-secondary);
  font-size: 13px;
}

.grammar-issues {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.grammar-issue {
  width: 100%;
  padding: 14px;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  transition: border-color 160ms ease, box-shadow 160ms ease;

  &:hover {
    border-color: var(--color-secondary);
    box-shadow: var(--shadow-sm);
  }
}

.grammar-issue-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--color-foreground);
  font-size: 13px;
  font-weight: 600;
}

.grammar-original,
.grammar-suggestion,
.grammar-explanation {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.grammar-original {
  color: var(--color-destructive);
}

.grammar-suggestion {
  color: var(--color-muted-foreground);
}

.grammar-explanation {
  color: var(--color-muted-foreground);
}

.a4-placeholder {
  width: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-stack-md);
  color: var(--color-muted);
}

.floating-toolbar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--color-border);
  padding: 12px 24px;
  border-radius: 9999px;
  box-shadow: var(--shadow-md);
  display: flex;
  align-items: center;
  gap: 20px;
  z-index: 40;
}

.is-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1280px) {
  .editor-form-panel {
    width: 360px;
  }
}

@media (max-width: 768px) {
  .editor {
    margin: -12px;
    height: calc(100vh - 24px);
  }

  .editor-secondary-header {
    min-height: 56px;
    height: auto;
    padding: 8px 12px 8px 56px;
    gap: 8px;
    flex-wrap: wrap;
  }

  .editor-header-leading {
    gap: 8px;
    width: 100%;
  }

  .editor-header-leading > .h-6 {
    display: none;
  }

  .editor-header-leading h1 {
    max-width: 32vw;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .editor-save-status {
    margin-left: auto;
  }

  .editor-header-actions {
    width: 100%;
    justify-content: flex-end;
    gap: 6px;
  }

  .panel-toggle-button {
    display: none;
  }

  .header-back-label,
  .header-action-label {
    display: none;
  }

  .editor-mobile-switcher {
    display: flex;
    gap: 4px;
    padding: 8px 12px;
    background: var(--color-background);
    border-bottom: 1px solid var(--color-border);

    button {
      flex: 1;
      padding: 8px 12px;
      border-radius: var(--radius-md);
      color: var(--color-muted-foreground);
      font-size: 13px;
      font-weight: 600;

      &.is-active {
        color: var(--color-foreground);
        background: var(--color-secondary);
      }
    }
  }

  .editor-workspace {
    position: relative;
  }

  .editor-form-panel {
    display: none;
    position: static;
    width: 100%;
    grid-template-columns: 1fr;
    border-right: 0;

    &.is-collapsed {
      width: 100%;
      opacity: 1;
      overflow: visible;
      pointer-events: auto;
    }
  }

  .editor-tabs {
    flex-direction: row;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
    overflow-x: auto;
    overflow-y: hidden;
  }

  .editor-tab {
    width: auto;
  }

  .editor-canvas {
    display: none;
    width: 100%;
  }

  .editor.is-mobile-edit .editor-form-panel {
    display: flex;
  }

  .editor.is-mobile-preview .editor-canvas {
    display: flex;
  }

  .a4-page-shell {
    width: 100% !important;
    min-height: auto !important;
  }
  .a4-page {
    width: 100%;
    min-height: auto;
    padding: 16px;
    transform: scale(1) !important;
  }

  .floating-toolbar {
    left: 12px;
    right: 12px;
    bottom: 12px;
    transform: none;
    max-width: none;
    padding: 8px 12px;
    gap: 10px;
    overflow-x: auto;
    justify-content: flex-start;
    white-space: nowrap;
  }
}
</style>
