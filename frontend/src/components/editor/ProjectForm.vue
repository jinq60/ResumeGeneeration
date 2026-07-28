<template>
  <div class="project-form">
    <div
      v-for="(item, index) in projectList"
      :key="item.id || index"
      class="project-item"
    >
      <div class="item-header">
        <h4>项目经历 {{ index + 1 }}</h4>
        <el-button
          v-if="projectList.length > 1"
          type="danger"
          size="small"
          text
          @click="removeProject(index)"
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
          label="项目名称"
          required
        >
          <el-input
            v-model="item.name"
            placeholder="请输入项目名称"
          />
        </el-form-item>

        <el-form-item label="担任角色">
          <el-input
            v-model="item.role"
            placeholder="例如：项目负责人、前端开发、后端开发"
          />
        </el-form-item>

        <el-form-item label="项目类型">
          <el-select
            v-model="item.type"
            placeholder="请选择项目类型"
            style="width: 100%"
          >
            <el-option
              label="科研项目"
              value="research"
            />
            <el-option
              label="课程项目"
              value="course"
            />
            <el-option
              label="企业项目"
              value="enterprise"
            />
            <el-option
              label="竞赛项目"
              value="competition"
            />
            <el-option
              label="开源项目"
              value="open_source"
            />
            <el-option
              label="个人项目"
              value="personal"
            />
            <el-option
              label="其他"
              value="other"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="项目时间">
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

        <el-form-item label="技术栈">
          <el-input
            v-model="item.techStackText"
            type="textarea"
            :rows="2"
            placeholder="请输入技术栈，用逗号分隔，例如：Vue.js, React, Node.js"
          />
        </el-form-item>

        <el-form-item label="项目背景">
          <el-input
            v-model="item.background"
            type="textarea"
            :rows="3"
            placeholder="请描述项目背景和目的"
          />
        </el-form-item>

        <el-form-item label="职责描述">
          <el-input
            v-model="item.responsibility"
            type="textarea"
            :rows="3"
            placeholder="请描述你在项目中的职责"
          />
        </el-form-item>

        <el-form-item label="项目描述">
          <el-input
            v-model="item.descriptionText"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述，用换行分隔多个要点"
          />
        </el-form-item>

        <el-form-item label="项目成就">
          <el-input
            v-model="item.achievementsText"
            type="textarea"
            :rows="3"
            placeholder="请输入项目成就，用换行分隔多个要点"
          />
        </el-form-item>

        <el-form-item label="项目链接">
          <el-input
            v-model="item.link"
            placeholder="请输入项目链接（可选）"
          />
        </el-form-item>

        <el-form-item label="GitHub链接">
          <el-input
            v-model="item.github"
            placeholder="请输入GitHub链接（可选）"
          />
        </el-form-item>
      </el-form>
    </div>

    <el-button
      type="primary"
      plain
      style="width: 100%; margin-top: 16px"
      @click="addProject"
    >
      <el-icon><Plus /></el-icon>
      添加项目经历
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'

interface Props {
  sections: any[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update'])

const projectList = ref<any[]>([])

// 从sections中提取项目经历
function extractProjects() {
  if (props.sections) {
    const projectSection = props.sections.find((s: any) => s.type === 'project')
    if (projectSection && projectSection.data) {
      projectList.value = projectSection.data.map((item: any) => ({
        ...item,
        dateRange: item.startDate && item.endDate ? [item.startDate, item.endDate] : [],
        techStackText: item.techStack ? item.techStack.join(', ') : '',
        descriptionText: item.description ? item.description.join('\n') : '',
        achievementsText: item.achievements ? item.achievements.join('\n') : ''
      }))
    }
  }
  
  // 如果没有项目经历，添加一个默认的
  if (projectList.value.length === 0) {
    addProject()
  }
}

function addProject() {
  projectList.value.push({
    name: '',
    role: '',
    type: '',
    dateRange: [],
    startDate: '',
    endDate: '',
    techStack: [],
    background: '',
    responsibility: '',
    description: [],
    achievements: [],
    link: '',
    github: '',
    techStackText: '',
    descriptionText: '',
    achievementsText: ''
  })
}

function removeProject(index: number) {
  projectList.value.splice(index, 1)
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

// 监听变化，触发更新
watch(projectList, (newList) => {
  const projectData = newList.map(item => ({
    ...item,
    techStack: item.techStackText ? item.techStackText.split(',').map((t: string) => t.trim()).filter((t: string) => t) : [],
    description: item.descriptionText ? item.descriptionText.split('\n').map((d: string) => d.trim()).filter((d: string) => d) : [],
    achievements: item.achievementsText ? item.achievementsText.split('\n').map((a: string) => a.trim()).filter((a: string) => a) : []
  }))

  emit('update', props.sections.map((section: any) => {
    if (section.type === 'project') {
      return {
        ...section,
        data: projectData
      }
    }
    return section
  }))
}, { deep: true })

// 初始化
extractProjects()
</script>

<style scoped lang="scss">
.project-form {
  padding: 16px 0;
}

.project-item {
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
