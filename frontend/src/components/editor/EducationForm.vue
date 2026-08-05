<template>
  <div class="education-form">
    <div
      v-for="(item, index) in educationList"
      :key="item.id || index"
      class="education-item"
    >
      <div class="item-header">
        <h4>教育经历 {{ index + 1 }}</h4>
        <el-button
          v-if="educationList.length > 1"
          type="danger"
          size="small"
          text
          @click="removeEducation(index)"
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
          label="学校"
          required
        >
          <el-input
            v-model="item.school"
            placeholder="请输入学校名称"
          />
        </el-form-item>

        <el-form-item
          label="学历"
          required
        >
          <el-select
            v-model="item.degree"
            placeholder="请选择学历"
            style="width: 100%"
          >
            <el-option
              label="博士"
              value="博士"
            />
            <el-option
              label="硕士"
              value="硕士"
            />
            <el-option
              label="本科"
              value="本科"
            />
            <el-option
              label="专科"
              value="专科"
            />
            <el-option
              label="高中"
              value="高中"
            />
            <el-option
              label="其他"
              value="其他"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          label="专业"
          required
        >
          <el-input
            v-model="item.major"
            placeholder="请输入专业"
          />
        </el-form-item>

        <el-form-item label="学院">
          <el-input
            v-model="item.college"
            placeholder="请输入学院（可选）"
          />
        </el-form-item>

        <el-form-item
          label="入学时间"
          required
        >
          <el-date-picker
            v-model="item.startDate"
            type="month"
            placeholder="选择入学时间"
            format="YYYY-MM"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="毕业时间">
          <el-date-picker
            v-model="item.endDate"
            type="month"
            placeholder="选择毕业时间"
            format="YYYY-MM"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="GPA">
          <el-input
            v-model="item.gpa"
            placeholder="例如：3.8/4.0 或 88/100"
          />
        </el-form-item>

        <el-form-item label="排名">
          <el-input
            v-model="item.rank"
            placeholder="例如：专业前10%"
          />
        </el-form-item>

        <el-form-item label="荣誉奖项">
          <el-input
            v-model="item.honorsText"
            type="textarea"
            :rows="2"
            placeholder="请输入荣誉奖项，用逗号分隔"
          />
        </el-form-item>

        <el-form-item label="主修课程">
          <el-input
            v-model="item.coursesText"
            type="textarea"
            :rows="2"
            placeholder="请输入主修课程，用逗号分隔"
          />
        </el-form-item>
      </el-form>
    </div>

    <el-button
      type="primary"
      plain
      style="width: 100%; margin-top: 16px"
      @click="addEducation"
    >
      <el-icon><Plus /></el-icon>
      添加教育经历
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { EducationItem } from '@/types/resume'
import { useSectionSync } from '@/composables/useSectionSync'

interface Props {
  sections: any[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update'])

const educationList = ref<EducationItem[]>([])

// 从sections中提取教育经历
function extractEducation() {
  if (props.sections) {
    const educationSection = props.sections.find((s: any) => s.type === 'education')
    if (educationSection && educationSection.data) {
      educationList.value = educationSection.data.map((item: any) => ({
        ...item,
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
    const educationData = educationList.value.map(item => ({
      ...item,
      honors: item.honorsText ? item.honorsText.split(',').map(h => h.trim()).filter(h => h) : [],
      courses: item.coursesText ? item.coursesText.split(',').map(c => c.trim()).filter(c => c) : []
    }))

    emit('update', props.sections.map((section: any) => {
      if (section.type === 'education') {
        return {
          ...section,
          data: educationData
        }
      }
      return section
    }))
  }
)
</script>

<style scoped lang="scss">
.education-form {
  padding: 16px 0;
}

.education-item {
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
