<template>
  <div class="work-form magic-form">
    <div v-for="(item, index) in workList" :key="item.id || index" class="work-item magic-item group">
      <div class="magic-drag-col" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></div>
      <div class="magic-item-main">
        <div class="magic-item-header">
          <div class="magic-item-title">
            <h4>{{ item.company || `工作经历 ${index + 1}` }}</h4>
            <p v-if="item.position" class="magic-item-sub">{{ item.position }}</p>
          </div>
          <div class="magic-item-actions">
            <button type="button" class="magic-action-sm" title="显示/隐藏"><el-icon :size="14"><View /></el-icon></button>
            <button v-if="workList.length > 1" type="button" class="magic-delete-sm" title="删除" @click="removeWork(index)"><el-icon :size="14"><Delete /></el-icon></button>
            <span class="magic-chevron"><el-icon :size="14"><ArrowDown /></el-icon></span>
          </div>
        </div>
        <div class="magic-divider" />
        <el-form :model="item" label-width="84px" size="default" class="magic-form-grid">
          <div class="magic-grid-2">
            <el-form-item label="公司名称" required class="magic-field">
              <el-input v-model="item.company" placeholder="请输入公司名称" class="magic-input" />
            </el-form-item>
            <el-form-item label="职位" required class="magic-field">
              <el-input v-model="item.position" placeholder="请输入职位" class="magic-input" />
            </el-form-item>
          </div>
          <div class="magic-grid-2">
            <el-form-item label="部门" class="magic-field">
              <el-input v-model="item.department" placeholder="请输入部门（可选）" class="magic-input" />
            </el-form-item>
            <el-form-item label="工作类型" class="magic-field">
              <el-select v-model="item.type" placeholder="请选择工作类型" style="width: 100%" class="magic-input">
                <el-option label="全职" value="full_time" />
                <el-option label="实习" value="internship" />
                <el-option label="兼职" value="part_time" />
                <el-option label="校园兼职" value="campus_job" />
                <el-option label="研究助理" value="research_assistant" />
                <el-option label="志愿者" value="volunteer" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="工作城市" class="magic-field">
            <el-input v-model="item.city" placeholder="请输入工作城市" class="magic-input" />
          </el-form-item>
          <el-form-item label="工作时间" required class="magic-field">
            <el-date-picker v-model="item.dateRange" type="monthrange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" format="YYYY-MM" value-format="YYYY-MM" style="width: 100%" class="magic-input" @change="handleDateRangeChange(item)" />
          </el-form-item>
          <el-form-item label="工作描述" class="magic-field magic-field-editor">
            <div class="magic-editor-wrap">
              <div class="magic-editor-toolbar">
                <span class="magic-editor-label">详细描述</span>
                <AiWriterButton v-if="resumeId" :resume-id="resumeId" section-type="work" field="description" :get-original-text="() => item.descriptionText ?? ''" @apply="(content: string) => applyAiContent(item, content)" />
              </div>
              <RichTextEditor :model-value="item.descriptionHtml ?? ''" placeholder="请输入工作描述，用换行分隔多个要点" @update:model-value="updateRichText(item, 'description', $event)" />
            </div>
          </el-form-item>
          <el-form-item label="工作成就" class="magic-field magic-field-editor">
            <RichTextEditor :model-value="item.achievementsHtml ?? ''" placeholder="请输入工作成就，用换行分隔多个要点" @update:model-value="updateRichText(item, 'achievements', $event)" />
          </el-form-item>
          <el-form-item label="技术栈" class="magic-field">
            <el-input v-model="item.techStackText" type="textarea" :rows="2" placeholder="请输入技术栈，用逗号分隔" class="magic-input magic-textarea" />
          </el-form-item>
          <el-form-item label="离职原因" class="magic-field">
            <el-input v-model="item.leaveReason" type="textarea" :rows="2" placeholder="请输入离职原因（可选）" class="magic-input magic-textarea" />
          </el-form-item>
          <el-form-item label="显示离职原因" class="magic-field">
            <el-switch v-model="item.showLeaveReason" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <button type="button" class="magic-add-btn" @click="addWork">
      <el-icon><Plus /></el-icon>
      添加工作经历
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown, Delete, Plus, Rank, View } from '@element-plus/icons-vue'
import { useSectionSync } from '@/composables/useSectionSync'
import AiWriterButton from './AiWriterButton.vue'
import RichTextEditor from './RichTextEditor.vue'
import { plainTextToRichHtml, richTextToPlainText } from '@/utils/richText'
import type { Section, WorkItem } from '@/types/resume'

/**
 * 编辑态扩展字段：dateRange 用于 el-date-picker 月份范围，*Text 用于 textarea↔数组镜像。
 * 提交前会投影回 WorkItem（去除这些 UI 字段）。
 */
type WorkItemDraft = WorkItem & {
  dateRange?: string[]
  descriptionText?: string
  achievementsText?: string
  techStackText?: string
}

interface Props {
  sections: Section[]
  resumeId?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', sections: Section[]): void
}>()

const workList = ref<WorkItemDraft[]>([])

function nextWorkId() {
  return `work_${Date.now()}_${Math.floor(Math.random() * 1000)}`
}

// 从sections中提取工作经历
function extractWork() {
  if (props.sections) {
    const workSection = props.sections.find((s) => s.type === 'work')
    if (workSection && Array.isArray(workSection.data)) {
      workList.value = (workSection.data as WorkItem[]).map((item) => ({
        ...item,
        id: item.id || nextWorkId(),
        dateRange: item.startDate && item.endDate ? [item.startDate, item.endDate] : [],
        descriptionText: item.description ? item.description.join('\n') : '',
        descriptionHtml: item.descriptionHtml || plainTextToRichHtml(item.description ? item.description.join('\n') : ''),
        achievementsText: item.achievements ? item.achievements.join('\n') : '',
        achievementsHtml: item.achievementsHtml || plainTextToRichHtml(item.achievements ? item.achievements.join('\n') : ''),
        techStackText: item.techStack ? item.techStack.join(', ') : ''
      }))
    }
  }

  // 如果没有工作经历，添加一个默认的
  if (workList.value.length === 0) {
    addWork()
  }
}

function addWork() {
  workList.value.push({
    id: nextWorkId(),
    company: '',
    position: '',
    department: '',
    type: '',
    city: '',
    dateRange: [],
    startDate: '',
    endDate: '',
    description: [],
    achievements: [],
    techStack: [],
    descriptionHtml: '',
    achievementsHtml: '',
    leaveReason: '',
    showLeaveReason: false,
    descriptionText: '',
    achievementsText: '',
    techStackText: ''
  })
}

function removeWork(index: number) {
  workList.value.splice(index, 1)
}

function handleDateRangeChange(item: WorkItemDraft) {
  if (item.dateRange && item.dateRange.length === 2) {
    item.startDate = item.dateRange[0]
    item.endDate = item.dateRange[1]
  } else {
    item.startDate = ''
    item.endDate = ''
  }
}

function updateRichText(item: WorkItemDraft, field: 'description' | 'achievements', html: string) {
  item[`${field}Html`] = html
  item[`${field}Text`] = richTextToPlainText(html)
}

function applyAiContent(item: WorkItemDraft, content: string) {
  item.descriptionText = content
  item.descriptionHtml = plainTextToRichHtml(content)
}

// 初始化
extractWork()

// 与父组件 sections 双向同步：外部变更时重新提取，自身 emit 的回传自动忽略
useSectionSync(
  workList,
  () => props.sections,
  extractWork,
  () => {
    const workData: WorkItem[] = workList.value.map((item) => ({
      ...item,
      description: item.descriptionText ? item.descriptionText.split('\n').map((d) => d.trim()).filter((d) => d) : [],
      achievements: item.achievementsText ? item.achievementsText.split('\n').map((a) => a.trim()).filter((a) => a) : [],
      techStack: item.techStackText ? item.techStackText.split(',').map((t) => t.trim()).filter((t) => t) : []
    }))

    emit('update', props.sections.map((section) => {
      if (section.type === 'work') {
        return {
          ...section,
          data: workData
        }
      }
      return section
    }))
  },
  () => props.sections?.find((s) => s.type === 'work')?.data
)
</script>

<style scoped lang="scss">
.work-form.magic-form { display: flex; flex-direction: column; gap: 12px; padding: 4px 0 8px; }
.magic-item { display: flex; overflow: hidden; background: hsl(var(--st-card)); border: 1px solid hsl(var(--st-border)); border-radius: var(--st-radius-lg); box-shadow: var(--st-shadow-sm); transition: border-color 160ms ease, box-shadow 160ms ease; }
.magic-item:hover { border-color: hsl(var(--st-foreground) / 0.14); box-shadow: var(--st-shadow-md); }
.magic-drag-col { width: 44px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-right: 1px solid hsl(var(--st-border)); background: transparent; cursor: grab; color: hsl(var(--st-muted)); transition: background 160ms ease, color 160ms ease; }
.magic-item:hover .magic-drag-col { background: hsl(var(--st-secondary) / 0.5); color: hsl(var(--st-foreground) / 0.7); }
.magic-drag-icon { font-size: 14px; }
.magic-item-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.magic-item-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 16px; }
.magic-item-title h4 { margin: 0; font-size: 14px; font-weight: 600; color: hsl(var(--st-foreground)); }
.magic-item-sub { margin: 2px 0 0; font-size: 12px; color: hsl(var(--st-muted-foreground)); }
.magic-item-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.magic-action-sm, .magic-delete-sm { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border-radius: 9999px; border: 1px solid transparent; background: transparent; cursor: pointer; color: hsl(var(--st-muted-foreground)); transition: all 160ms ease; }
.magic-action-sm:hover { background: hsl(var(--st-secondary)); color: hsl(var(--st-foreground)); border-color: hsl(var(--st-border)); }
.magic-delete-sm:hover { background: hsl(0 84% 97%); color: #dc2626; border-color: hsl(0 84% 88%); }
.magic-chevron { width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; color: hsl(var(--st-muted)); }
.magic-divider { height: 1px; background: hsl(var(--st-border)); margin: 0 16px; }
.magic-form-grid { padding: 16px; }
.magic-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
@media (max-width: 640px) { .magic-grid-2 { grid-template-columns: 1fr; } }
.magic-field { margin-bottom: 10px !important; }
.magic-field :deep(.el-form-item__label) { font-size: 12.5px; font-weight: 500; color: hsl(var(--st-foreground)); line-height: 32px; }
.magic-input :deep(.el-input__wrapper), .magic-input :deep(.el-select .el-input__wrapper), .magic-input :deep(.el-date-editor.el-input__wrapper) { min-height: 36px; height: 36px; background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset, var(--st-shadow-xs); }
.magic-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-input :deep(.el-input__inner)::placeholder, .magic-input :deep(.el-textarea__inner)::placeholder { color: hsl(var(--st-muted)); }
.magic-textarea :deep(.el-textarea__inner) { background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset; padding: 8px 10px; font-size: 13px; color: hsl(var(--st-foreground)); }
.magic-textarea :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-field-editor :deep(.el-form-item__content) { display: block; }
.magic-editor-wrap { width: 100%; display: flex; flex-direction: column; gap: 6px; }
.magic-editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.magic-editor-label { font-size: 11px; color: hsl(var(--st-muted-foreground)); }
.magic-add-btn { width: 100%; height: 42px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; background: hsl(var(--st-primary)); color: hsl(var(--st-primary-foreground)); border: 0; border-radius: 9999px; font-size: 13.5px; font-weight: 500; cursor: pointer; box-shadow: var(--st-shadow-sm); transition: opacity 160ms ease; }
.magic-add-btn:hover { opacity: 0.92; }
</style>
