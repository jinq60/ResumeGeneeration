<template>
  <div class="work-form">
    <div
      v-for="(item, index) in workList"
      :key="item.id || index"
      class="work-item"
    >
      <div class="item-header">
        <h4>工作经历 {{ index + 1 }}</h4>
        <el-button
          v-if="workList.length > 1"
          type="danger"
          size="small"
          text
          @click="removeWork(index)"
        >
          删除
        </el-button>
      </div>

      <el-form
        :model="item"
        label-width="100px"
        size="default"
      >
        <el-form-item
          label="公司名称"
          required
        >
          <el-input
            v-model="item.company"
            placeholder="请输入公司名称"
          />
        </el-form-item>

        <el-form-item
          label="职位"
          required
        >
          <el-input
            v-model="item.position"
            placeholder="请输入职位"
          />
        </el-form-item>

        <el-form-item label="部门">
          <el-input
            v-model="item.department"
            placeholder="请输入部门（可选）"
          />
        </el-form-item>

        <el-form-item label="工作类型">
          <el-select
            v-model="item.type"
            placeholder="请选择工作类型"
            style="width: 100%"
          >
            <el-option
              label="全职"
              value="full_time"
            />
            <el-option
              label="实习"
              value="internship"
            />
            <el-option
              label="兼职"
              value="part_time"
            />
            <el-option
              label="校园兼职"
              value="campus_job"
            />
            <el-option
              label="研究助理"
              value="research_assistant"
            />
            <el-option
              label="志愿者"
              value="volunteer"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="工作城市">
          <el-input
            v-model="item.city"
            placeholder="请输入工作城市"
          />
        </el-form-item>

        <el-form-item
          label="工作时间"
          required
        >
          <el-date-picker
            v-model="item.dateRange"
            type="monthrange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM"
            value-format="YYYY-MM"
            style="width: 100%"
            @change="handleDateRangeChange(item)"
          />
        </el-form-item>

        <el-form-item label="工作描述">
          <div class="w-full flex flex-col gap-1">
            <div class="flex justify-end">
              <AiWriterButton
                v-if="resumeId"
                :resume-id="resumeId"
                section-type="work"
                field="description"
                :get-original-text="() => item.descriptionText"
                @apply="(content: string) => applyAiContent(item, content)"
              />
            </div>
            <RichTextEditor
              :model-value="item.descriptionHtml"
              placeholder="请输入工作描述，用换行分隔多个要点"
              @update:model-value="updateRichText(item, 'description', $event)"
            />
          </div>
        </el-form-item>

        <el-form-item label="工作成就">
          <RichTextEditor
            :model-value="item.achievementsHtml"
            placeholder="请输入工作成就，用换行分隔多个要点"
            @update:model-value="updateRichText(item, 'achievements', $event)"
          />
        </el-form-item>

        <el-form-item label="技术栈">
          <el-input
            v-model="item.techStackText"
            type="textarea"
            :rows="2"
            placeholder="请输入技术栈，用逗号分隔"
          />
        </el-form-item>

        <el-form-item label="离职原因">
          <el-input
            v-model="item.leaveReason"
            type="textarea"
            :rows="2"
            placeholder="请输入离职原因（可选）"
          />
        </el-form-item>

        <el-form-item label="显示离职原因">
          <el-switch v-model="item.showLeaveReason" />
        </el-form-item>
      </el-form>
    </div>

    <el-button
      type="primary"
      plain
      style="width: 100%; margin-top: 16px"
      @click="addWork"
    >
      <el-icon><Plus /></el-icon>
      添加工作经历
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
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

const workList = ref<any[]>([])

// 从sections中提取工作经历
function extractWork() {
  if (props.sections) {
    const workSection = props.sections.find((s: any) => s.type === 'work')
    if (workSection && workSection.data) {
      workList.value = workSection.data.map((item: any) => ({
        ...item,
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

function handleDateRangeChange(item: any) {
  if (item.dateRange && item.dateRange.length === 2) {
    item.startDate = item.dateRange[0]
    item.endDate = item.dateRange[1]
  } else {
    item.startDate = ''
    item.endDate = ''
  }
}

function updateRichText(item: any, field: 'description' | 'achievements', html: string) {
  item[`${field}Html`] = html
  item[`${field}Text`] = richTextToPlainText(html)
}

function applyAiContent(item: any, content: string) {
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
    const workData = workList.value.map((item: any) => ({
      ...item,
      description: item.descriptionText ? item.descriptionText.split('\n').map((d: string) => d.trim()).filter((d: string) => d) : [],
      achievements: item.achievementsText ? item.achievementsText.split('\n').map((a: string) => a.trim()).filter((a: string) => a) : [],
      techStack: item.techStackText ? item.techStackText.split(',').map((t: string) => t.trim()).filter((t: string) => t) : []
    }))

    emit('update', props.sections.map((section: any) => {
      if (section.type === 'work') {
        return {
          ...section,
          data: workData
        }
      }
      return section
    }))
  }
)
</script>

<style scoped lang="scss">
.work-form {
  padding: 16px 0;
}

.work-item {
  background: #f9fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

.el-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }
}
</style>
