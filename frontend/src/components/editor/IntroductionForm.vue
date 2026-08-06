<template>
  <div class="introduction-form">
    <el-form
      :model="formData"
      label-width="100px"
      size="default"
    >
      <el-form-item label="写作风格">
        <el-select
          v-model="formData.style"
          placeholder="请选择写作风格"
          style="width: 100%"
        >
          <el-option
            label="简洁正式"
            value="concise_formal"
          />
          <el-option
            label="技术导向"
            value="tech_oriented"
          />
          <el-option
            label="学生风格"
            value="student"
          />
          <el-option
            label="资深风格"
            value="senior"
          />
          <el-option
            label="考研复试"
            value="postgraduate"
          />
          <el-option
            label="项目导向"
            value="project"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="字数限制">
        <el-input-number
          v-model="formData.maxWords"
          :min="50"
          :max="500"
          :step="10"
          placeholder="字数限制"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="关键词">
        <el-input
          v-model="formData.keywordsText"
          type="textarea"
          :rows="2"
          placeholder="请输入关键词，用逗号分隔"
        />
      </el-form-item>

      <el-form-item
        label="自我介绍"
        required
      >
        <div class="w-full flex flex-col gap-1">
          <div class="flex justify-end">
            <AiWriterButton
              v-if="resumeId"
              :resume-id="resumeId"
              section-type="introduction"
              field="content"
              :get-original-text="() => formData.content"
              @apply="applyAiContent"
            />
          </div>
          <RichTextEditor
            :model-value="formData.contentHtml"
            placeholder="请输入自我介绍内容"
            @update:model-value="handleRichTextChange"
          />
        </div>
      </el-form-item>

      <el-form-item label="字数统计">
        <div class="word-count">
          当前字数：{{ currentWordCount }} / {{ formData.maxWords || 500 }}
        </div>
      </el-form-item>

      <el-divider>快速模板</el-divider>

      <el-form-item label="选择模板">
        <el-radio-group v-model="selectedTemplate">
          <el-radio label="student">
            学生求职
          </el-radio>
          <el-radio label="tech">
            技术岗位
          </el-radio>
          <el-radio label="senior">
            资深岗位
          </el-radio>
          <el-radio label="postgraduate">
            考研复试
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-button
        type="primary"
        plain
        @click="applyTemplate"
      >
        应用模板
      </el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useSectionSync } from '@/composables/useSectionSync'
import AiWriterButton from './AiWriterButton.vue'
import RichTextEditor from './RichTextEditor.vue'
import { plainTextToRichHtml, richTextToPlainText } from '@/utils/richText'

interface Props {
  sections: any[]
  resumeId?: string
}

const props = defineProps<Props>()
const emit = defineEmits(['update'])

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
    const introductionSection = props.sections.find((s: any) => s.type === 'introduction')
    if (introductionSection && introductionSection.data) {
      formData.value.content = introductionSection.data.content || ''
      formData.value.contentHtml = introductionSection.data.contentHtml || plainTextToRichHtml(formData.value.content)
      formData.value.style = introductionSection.data.style || 'concise_formal'
      formData.value.maxWords = introductionSection.data.maxWords || 300
      formData.value.keywords = introductionSection.data.keywords || []
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
      keywords: formData.value.keywordsText ? formData.value.keywordsText.split(',').map(k => k.trim()).filter(k => k) : [],
      style: formData.value.style,
      maxWords: formData.value.maxWords
    }

    emit('update', props.sections.map((section: any) => {
      if (section.type === 'introduction') {
        return {
          ...section,
          data: introductionData
        }
      }
      return section
    }))
  }
)
</script>

<style scoped lang="scss">
.introduction-form {
  padding: 16px 0;
}

.el-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }
}

.word-count {
  font-size: 13px;
  color: #606266;
  
  &.warning {
    color: #e6a23c;
  }
  
  &.danger {
    color: #f56c6c;
  }
}
</style>
