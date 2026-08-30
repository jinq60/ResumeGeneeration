<template>
  <div class="introduction-form magic-form">
    <!-- SkillPanel / SelfEvaluationPanel 都是 rounded-lg border p-4 bg-card，这里保持同款卡片 -->
    <div class="magic-card">
      <el-form :model="formData" label-width="84px" size="default" class="magic-inner-form">
        <div class="magic-grid-2">
          <el-form-item label="写作风格" class="magic-field">
            <el-select v-model="formData.style" placeholder="请选择写作风格" style="width: 100%" class="magic-input">
              <el-option label="简洁正式" value="concise_formal" />
              <el-option label="技术导向" value="tech_oriented" />
              <el-option label="学生风格" value="student" />
              <el-option label="资深风格" value="senior" />
              <el-option label="考研复试" value="postgraduate" />
              <el-option label="项目导向" value="project" />
            </el-select>
          </el-form-item>
          <el-form-item label="字数限制" class="magic-field">
            <el-input-number v-model="formData.maxWords" :min="50" :max="500" :step="10" placeholder="字数限制" style="width: 100%" class="magic-input magic-number" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="关键词" class="magic-field">
          <el-input v-model="formData.keywordsText" type="textarea" :rows="2" placeholder="请输入关键词，用逗号分隔" class="magic-input magic-textarea" />
        </el-form-item>
        <el-form-item label="自我介绍" required class="magic-field magic-field-editor">
          <div class="magic-editor-wrap">
            <div class="magic-editor-toolbar">
              <span class="magic-editor-label">支持加粗、斜体、列表等富文本</span>
              <AiWriterButton v-if="resumeId" :resume-id="resumeId" section-type="introduction" field="content" :get-original-text="() => formData.content" @apply="applyAiContent" />
            </div>
            <RichTextEditor :model-value="formData.contentHtml" placeholder="请输入自我介绍内容" @update:model-value="handleRichTextChange" />
            <div class="word-count magic-word-count">
              <span>当前字数：{{ currentWordCount }} / {{ formData.maxWords || 500 }}</span>
              <span v-if="currentWordCount > (formData.maxWords || 500)" class="is-over">超出 {{ currentWordCount - (formData.maxWords || 500) }} 字</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="magic-card">
      <div class="magic-card-head">
        <h4>快速模板</h4>
        <span class="magic-hint">选择后点击应用模板</span>
      </div>
      <div class="magic-divider" />
      <div class="magic-inner-pad">
        <el-radio-group v-model="selectedTemplate" class="magic-radio-group">
          <el-radio label="student" class="magic-radio">学生求职</el-radio>
          <el-radio label="tech" class="magic-radio">技术岗位</el-radio>
          <el-radio label="senior" class="magic-radio">资深岗位</el-radio>
          <el-radio label="postgraduate" class="magic-radio">考研复试</el-radio>
        </el-radio-group>
        <button type="button" class="magic-add-btn magic-add-btn-sm" @click="applyTemplate">应用模板</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useSectionSync } from '@/composables/useSectionSync'
import AiWriterButton from './AiWriterButton.vue'
import RichTextEditor from './RichTextEditor.vue'
import { plainTextToRichHtml, richTextToPlainText } from '@/utils/richText'
import type { Section } from '@/types/resume'

interface Props {
  sections: Section[]
  resumeId?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', sections: Section[]): void
}>()

const formData = ref<{
  content: string
  contentHtml: string
  keywords: string[]
  keywordsText: string
  style: string
  maxWords: number
}>({
  content: '',
  contentHtml: '',
  keywords: [],
  keywordsText: '',
  style: 'concise_formal',
  maxWords: 300
})

const selectedTemplate = ref('')

const currentWordCount = computed(() => {
  return formData.value.content.length
})

const templates = {
  student: {
    style: 'student',
    content: '本人是一名即将毕业的大学生，在校期间学习成绩优异，积极参与各类实践活动。具备良好的团队协作能力和沟通能力，学习能力强，能够快速适应新环境。希望能有机会加入贵公司，为公司的发展贡献自己的力量。',
    keywords: ['学习能力强', '团队协作', '沟通能力']
  },
  tech: {
    style: 'tech_oriented',
    content: '热爱技术，具备扎实的编程基础和丰富项目经验。熟练掌握多种编程语言和开发框架，对新技术有强烈的学习热情。具备良好的问题解决能力和代码规范意识，能够独立完成复杂功能的开发。',
    keywords: ['编程基础', '项目经验', '问题解决']
  },
  senior: {
    style: 'senior',
    content: '拥有多年行业经验，在相关领域积累了丰富的技术积累和项目管理经验。具备优秀的团队领导能力和技术决策能力，能够带领团队完成复杂项目。注重代码质量和团队协作，致力于推动技术创新。',
    keywords: ['行业经验', '项目管理', '团队领导']
  },
  postgraduate: {
    style: 'postgraduate',
    content: '学习成绩优秀，专业基础扎实，具备较强的科研能力和学术素养。在校期间积极参与科研项目，发表过学术论文。对学术研究有浓厚兴趣，希望在研究生阶段继续深造，为学科发展做出贡献。',
    keywords: ['学习成绩', '科研能力', '学术素养']
  }
}

// 从sections中提取自我介绍
function extractIntroduction() {
  if (props.sections) {
    const introductionSection = props.sections.find((s) => s.type === 'introduction')
    if (introductionSection && introductionSection.data) {
      const data = introductionSection.data
      formData.value.content = data.content || ''
      formData.value.contentHtml = data.contentHtml || plainTextToRichHtml(formData.value.content)
      formData.value.style = data.style || 'concise_formal'
      formData.value.maxWords = data.maxWords || 300
      formData.value.keywords = data.keywords || []
      formData.value.keywordsText = formData.value.keywords.join(', ')
    }
  }
}

function handleRichTextChange(html: string) {
  formData.value.contentHtml = html
  formData.value.content = richTextToPlainText(html)
}

function applyAiContent(content: string) {
  formData.value.content = content
  formData.value.contentHtml = plainTextToRichHtml(content)
}

function applyTemplate() {
  const template = templates[selectedTemplate.value as keyof typeof templates]
  if (template) {
    formData.value.style = template.style
    formData.value.content = template.content
    formData.value.contentHtml = plainTextToRichHtml(template.content)
    formData.value.keywords = template.keywords
    formData.value.keywordsText = template.keywords.join(', ')
    ElMessage.success('模板应用成功')
  }
}

// 初始化
extractIntroduction()

// 与父组件 sections 双向同步：外部变更时重新提取，自身 emit 的回传自动忽略
useSectionSync(
  formData,
  () => props.sections,
  extractIntroduction,
  () => {
    const introductionData = {
      content: formData.value.content,
      contentHtml: formData.value.contentHtml,
      keywords: formData.value.keywordsText ? formData.value.keywordsText.split(',').map((k) => k.trim()).filter((k) => k) : [],
      style: formData.value.style,
      maxWords: formData.value.maxWords
    }

    emit('update', props.sections.map((section) => {
      if (section.type === 'introduction') {
        return {
          ...section,
          data: introductionData
        }
      }
      return section
    }))
  },
  () => props.sections?.find((s) => s.type === 'introduction')?.data
)
</script>

<style scoped lang="scss">
.introduction-form.magic-form { display: flex; flex-direction: column; gap: 12px; padding: 4px 0 8px; }
.magic-card { background: hsl(var(--st-card)); border: 1px solid hsl(var(--st-border)); border-radius: var(--st-radius-lg); box-shadow: var(--st-shadow-sm); overflow: hidden; }
.magic-inner-form { padding: 16px; }
.magic-card-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; padding: 14px 16px 12px; }
.magic-card-head h4 { margin: 0; font-size: 13.5px; font-weight: 600; color: hsl(var(--st-foreground)); }
.magic-hint { font-size: 11px; color: hsl(var(--st-muted-foreground)); }
.magic-divider { height: 1px; background: hsl(var(--st-border)); margin: 0 16px; }
.magic-inner-pad { padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; }
.magic-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
@media (max-width: 640px) { .magic-grid-2 { grid-template-columns: 1fr; } }
.magic-field { margin-bottom: 10px !important; }
.magic-field :deep(.el-form-item__label) { font-size: 12.5px; font-weight: 500; color: hsl(var(--st-foreground)); }
.magic-input :deep(.el-input__wrapper), .magic-input :deep(.el-select .el-input__wrapper) { min-height: 36px; height: 36px; background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset, var(--st-shadow-xs); }
.magic-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-input :deep(.el-input__inner)::placeholder, .magic-input :deep(.el-textarea__inner)::placeholder { color: hsl(var(--st-muted)); }
.magic-textarea :deep(.el-textarea__inner) { background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset; padding: 8px 10px; font-size: 13px; color: hsl(var(--st-foreground)); }
.magic-textarea :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-number :deep(.el-input-number__increase), .magic-number :deep(.el-input-number__decrease) { background: hsl(var(--st-secondary)); border-color: hsl(var(--st-border)); }
.magic-number :deep(.el-input__wrapper) { background: hsl(var(--st-background)); }
.magic-field-editor :deep(.el-form-item__content) { display: block; }
.magic-editor-wrap { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.magic-editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.magic-editor-label { font-size: 11px; color: hsl(var(--st-muted-foreground)); }
.magic-word-count { display: flex; align-items: center; gap: 8px; font-size: 12px; color: hsl(var(--st-muted-foreground)); }
.magic-word-count .is-over { color: hsl(var(--st-destructive)); font-weight: 600; }
.magic-radio-group { display: flex; flex-wrap: wrap; gap: 12px 18px; }
.magic-radio :deep(.el-radio__label) { font-size: 13px; color: hsl(var(--st-foreground)); }
.magic-radio :deep(.el-radio__input.is-checked .el-radio__inner) { background: hsl(var(--st-foreground)); border-color: hsl(var(--st-foreground)); }
.magic-radio :deep(.el-radio__input.is-checked + .el-radio__label) { color: hsl(var(--st-foreground)); }
.magic-add-btn { width: 100%; height: 40px; display: inline-flex; align-items: center; justify-content: center; background: hsl(var(--st-primary)); color: hsl(var(--st-primary-foreground)); border: 0; border-radius: 9999px; font-size: 13.5px; font-weight: 500; cursor: pointer; box-shadow: var(--st-shadow-sm); }
.magic-add-btn:hover { opacity: 0.92; }
.magic-add-btn-sm { height: 38px; }
</style>
