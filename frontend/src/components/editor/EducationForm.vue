<template>
  <div class="education-form magic-form">
    <div
      v-for="(item, index) in educationList"
      :key="item.id || index"
      class="education-item magic-item group"
    >
      <div class="magic-drag-col" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></div>
      <div class="magic-item-main">
        <div class="magic-item-header">
          <div class="magic-item-title">
            <h4>{{ item.school || `教育经历 ${index + 1}` }}</h4>
            <p v-if="item.major || item.degree" class="magic-item-sub">{{ [item.major, item.degree].filter(Boolean).join(' · ') }}</p>
          </div>
          <div class="magic-item-actions">
            <button type="button" class="magic-action-sm" title="显示/隐藏"><el-icon :size="14"><View /></el-icon></button>
            <button v-if="educationList.length > 1" type="button" class="magic-delete-sm" title="删除" @click="removeEducation(index)"><el-icon :size="14"><Delete /></el-icon></button>
            <span class="magic-chevron"><el-icon :size="14"><ArrowDown /></el-icon></span>
          </div>
        </div>
        <div class="magic-divider" />
        <el-form :model="item" label-width="84px" size="default" class="magic-form-grid">
          <div class="magic-grid-2">
            <el-form-item label="学校" required class="magic-field">
              <el-input v-model="item.school" placeholder="请输入学校名称" class="magic-input" />
            </el-form-item>
            <el-form-item label="专业" required class="magic-field">
              <el-input v-model="item.major" placeholder="请输入专业" class="magic-input" />
            </el-form-item>
          </div>
          <div class="magic-grid-2">
            <el-form-item label="学历" required class="magic-field">
              <el-select v-model="item.degree" placeholder="请选择学历" style="width: 100%" class="magic-input">
                <el-option label="博士" value="博士" />
                <el-option label="硕士" value="硕士" />
                <el-option label="本科" value="本科" />
                <el-option label="专科" value="专科" />
                <el-option label="高中" value="高中" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item label="学院" class="magic-field">
              <el-input v-model="item.college" placeholder="请输入学院（可选）" class="magic-input" />
            </el-form-item>
          </div>
          <div class="magic-grid-2">
            <el-form-item label="入学时间" required class="magic-field">
              <el-date-picker v-model="item.startDate" type="month" placeholder="选择入学时间" format="YYYY-MM" value-format="YYYY-MM" style="width: 100%" class="magic-input" />
            </el-form-item>
            <el-form-item label="毕业时间" class="magic-field">
              <el-date-picker v-model="item.endDate" type="month" placeholder="选择毕业时间" format="YYYY-MM" value-format="YYYY-MM" style="width: 100%" class="magic-input" />
            </el-form-item>
          </div>
          <div class="magic-grid-2">
            <el-form-item label="GPA" class="magic-field">
              <el-input v-model="item.gpa" placeholder="例如：3.8/4.0 或 88/100" class="magic-input" />
            </el-form-item>
            <el-form-item label="排名" class="magic-field">
              <el-input v-model="item.rank" placeholder="例如：专业前10%" class="magic-input" />
            </el-form-item>
          </div>
          <el-form-item label="荣誉奖项" class="magic-field">
            <el-input v-model="item.honorsText" type="textarea" :rows="2" placeholder="请输入荣誉奖项，用逗号分隔" class="magic-input magic-textarea" />
          </el-form-item>
          <el-form-item label="主修课程" class="magic-field">
            <el-input v-model="item.coursesText" type="textarea" :rows="2" placeholder="请输入主修课程，用逗号分隔" class="magic-input magic-textarea" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <button type="button" class="magic-add-btn" @click="addEducation">
      <el-icon><Plus /></el-icon>
      添加教育经历
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown, Delete, Hide, Plus, Rank, View } from '@element-plus/icons-vue'
import type { EducationItem, Section } from '@/types/resume'
import { useSectionSync } from '@/composables/useSectionSync'

interface Props {
  sections: Section[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', sections: Section[]): void
}>()

const educationList = ref<EducationItem[]>([])

function nextEducationId() {
  return `edu_${Date.now()}_${Math.floor(Math.random() * 1000)}`
}

// 从sections中提取教育经历
function extractEducation() {
  if (props.sections) {
    const educationSection = props.sections.find((s) => s.type === 'education')
    if (educationSection && Array.isArray(educationSection.data)) {
      educationList.value = educationSection.data.map((item) => ({
        ...item,
        id: item.id || nextEducationId(),
        honorsText: item.honors ? item.honors.join(', ') : '',
        coursesText: item.courses ? item.courses.join(', ') : ''
      }))
    }
  }

  // 如果没有教育经历，添加一个默认的
  if (educationList.value.length === 0) {
    addEducation()
  }
}

function addEducation() {
  educationList.value.push({
    id: nextEducationId(),
    school: '',
    degree: '',
    major: '',
    college: '',
    startDate: '',
    endDate: '',
    gpa: '',
    rank: '',
    honors: [],
    courses: [],
    honorsText: '',
    coursesText: ''
  })
}

function removeEducation(index: number) {
  educationList.value.splice(index, 1)
}

// 初始化
extractEducation()

// 与父组件 sections 双向同步：外部变更时重新提取，自身 emit 的回传自动忽略
useSectionSync(
  educationList,
  () => props.sections,
  extractEducation,
  () => {
    const educationData = educationList.value.map((item) => ({
      ...item,
      honors: item.honorsText ? item.honorsText.split(',').map((h) => h.trim()).filter((h) => h) : [],
      courses: item.coursesText ? item.coursesText.split(',').map((c) => c.trim()).filter((c) => c) : []
    }))

    emit('update', props.sections.map((section) => {
      if (section.type === 'education') {
        return {
          ...section,
          data: educationData
        }
      }
      return section
    }))
  },
  () => props.sections?.find((s) => s.type === 'education')?.data
)
</script>

<style scoped lang="scss">
.education-form.magic-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0 8px;
}

.magic-item {
  display: flex;
  overflow: hidden;
  background: hsl(var(--st-card));
  border: 1px solid hsl(var(--st-border));
  border-radius: var(--st-radius-lg);
  box-shadow: var(--st-shadow-sm);
  transition: border-color 160ms ease, box-shadow 160ms ease;
}
.magic-item:hover { border-color: hsl(var(--st-foreground) / 0.14); box-shadow: var(--st-shadow-md); }

.magic-drag-col {
  width: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid hsl(var(--st-border));
  background: transparent;
  cursor: grab;
  color: hsl(var(--st-muted));
  transition: background 160ms ease, color 160ms ease;
}
.magic-item:hover .magic-drag-col { background: hsl(var(--st-secondary) / 0.5); color: hsl(var(--st-foreground) / 0.7); }
.magic-drag-icon { font-size: 14px; }

.magic-item-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.magic-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}
.magic-item-title h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--st-foreground));
  line-height: 1.4;
}
.magic-item-sub { margin: 2px 0 0; font-size: 12px; color: hsl(var(--st-muted-foreground)); }
.magic-item-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.magic-action-sm, .magic-delete-sm {
  width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center;
  border-radius: 9999px; border: 1px solid transparent; background: transparent; cursor: pointer;
  color: hsl(var(--st-muted-foreground)); transition: all 160ms ease;
}
.magic-action-sm:hover { background: hsl(var(--st-secondary)); color: hsl(var(--st-foreground)); border-color: hsl(var(--st-border)); }
.magic-delete-sm:hover { background: hsl(0 84% 97%); color: #dc2626; border-color: hsl(0 84% 88%); }
.magic-chevron { width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; color: hsl(var(--st-muted)); }
.magic-divider { height: 1px; background: hsl(var(--st-border)); margin: 0 16px; }

.magic-form-grid { padding: 16px; }
.magic-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
@media (max-width: 640px) { .magic-grid-2 { grid-template-columns: 1fr; } }

.magic-field { margin-bottom: 10px !important; }
.magic-field :deep(.el-form-item__label) {
  font-size: 12.5px;
  font-weight: 500;
  color: hsl(var(--st-foreground));
  line-height: 32px;
}
.magic-input :deep(.el-input__wrapper),
.magic-input :deep(.el-select .el-input__wrapper),
.magic-input :deep(.el-date-editor.el-input__wrapper) {
  min-height: 36px; height: 36px;
  background: hsl(var(--st-background));
  border-radius: 0.5rem;
  box-shadow: 0 0 0 1px hsl(var(--st-input)) inset, var(--st-shadow-xs);
}
.magic-input :deep(.el-input__wrapper.is-focus),
.magic-input :deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset;
  background: hsl(var(--st-card));
}
.magic-input :deep(.el-input__inner)::placeholder,
.magic-input :deep(.el-textarea__inner)::placeholder { color: hsl(var(--st-muted)); }
.magic-textarea :deep(.el-textarea__inner) {
  background: hsl(var(--st-background));
  border-radius: 0.5rem;
  box-shadow: 0 0 0 1px hsl(var(--st-input)) inset;
  padding: 8px 10px;
  font-size: 13px;
  color: hsl(var(--st-foreground));
}
.magic-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset;
  background: hsl(var(--st-card));
}

.magic-add-btn {
  width: 100%;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: hsl(var(--st-primary));
  color: hsl(var(--st-primary-foreground));
  border: 0;
  border-radius: 9999px;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: var(--st-shadow-sm);
  transition: opacity 160ms ease, transform 160ms ease;
}
.magic-add-btn:hover { opacity: 0.92; }
.magic-add-btn:active { transform: scale(0.99); }
</style>
