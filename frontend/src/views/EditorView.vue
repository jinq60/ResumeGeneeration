<template>
  <div class="editor-view">
    <div class="editor-header">
      <div class="header-left">
        <el-button
          :icon="ArrowLeft"
          @click="handleBack"
        >
          返回
        </el-button>
        <h2>{{ resume?.title || '新建简历' }}</h2>
      </div>
      <div class="header-right">
        <el-button
          :icon="View"
          @click="handlePreview"
        >
          预览
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </div>
    </div>

    <div class="editor-content">
      <div class="editor-left">
        <el-tabs
          v-model="activeTab"
          class="editor-tabs"
        >
          <el-tab-pane
            label="基本信息"
            name="basic"
          >
            <BasicInfoForm
              v-if="resume"
              :resume="resume"
              @update="handleBasicInfoUpdate"
            />
          </el-tab-pane>
          <el-tab-pane
            label="教育经历"
            name="education"
          >
            <EducationForm
              v-if="resume"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </el-tab-pane>
          <el-tab-pane
            label="项目经历"
            name="project"
          >
            <ProjectForm
              v-if="resume"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </el-tab-pane>
          <el-tab-pane
            label="工作经历"
            name="work"
          >
            <WorkForm
              v-if="resume"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </el-tab-pane>
          <el-tab-pane
            label="技能"
            name="skill"
          >
            <SkillForm
              v-if="resume"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </el-tab-pane>
          <el-tab-pane
            label="自我介绍"
            name="introduction"
          >
            <IntroductionForm
              v-if="resume"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </el-tab-pane>
          <el-tab-pane
            label="自定义模块"
            name="custom"
          >
            <CustomForm
              v-if="resume"
              :sections="resume.sections"
              @update="handleSectionsUpdate"
            />
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="editor-right">
        <ResumePreview
          v-if="resume"
          :resume="resume"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, View, Check } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import type { Resume } from '@/types/resume'
import ResumePreview from '@/components/preview/ResumePreview.vue'
import { useAutoSave } from '@/composables/useAutoSave'

// 占位组件，后续实现
const BasicInfoForm = defineComponent({
  props: ['resume'],
  emits: ['update'],
  template: '<div class="placeholder-form">基本信息表单（待实现）</div>'
})

const EducationForm = defineComponent({
  props: ['sections'],
  emits: ['update'],
  template: '<div class="placeholder-form">教育经历表单（待实现）</div>'
})

const ProjectForm = defineComponent({
  props: ['sections'],
  emits: ['update'],
  template: '<div class="placeholder-form">项目经历表单（待实现）</div>'
})

const WorkForm = defineComponent({
  props: ['sections'],
  emits: ['update'],
  template: '<div class="placeholder-form">工作经历表单（待实现）</div>'
})

const SkillForm = defineComponent({
  props: ['sections'],
  emits: ['update'],
  template: '<div class="placeholder-form">技能表单（待实现）</div>'
})

const IntroductionForm = defineComponent({
  props: ['sections'],
  emits: ['update'],
  template: '<div class="placeholder-form">自我介绍表单（待实现）</div>'
})

const CustomForm = defineComponent({
  props: ['sections'],
  emits: ['update'],
  template: '<div class="placeholder-form">自定义模块表单（待实现）</div>'
})

const router = useRouter()
const route = useRoute()

const resume = ref<Resume | null>(null)
const activeTab = ref('basic')
const saving = ref(false)

// 使用自动保存
const { saveStatus, triggerSave } = useAutoSave()

async function loadResume() {
  const id = route.params.id as string
  if (!id) {
    // 创建新简历
    try {
      const newResume = await resumeApi.create({
        title: '我的简历',
        scene: 'campus_recruitment',
        templateId: 'default'
      })
      resume.value = newResume
      router.replace(`/resumes/${newResume.id}/edit`)
    } catch (e: any) {
      ElMessage.error(e.message || '创建简历失败')
    }
    return
  }

  // 加载现有简历
  try {
    resume.value = await resumeApi.get(id)
  } catch (e: any) {
    ElMessage.error(e.message || '加载简历失败')
  }
}

function handleBack() {
  router.push('/resumes')
}

function handlePreview() {
  if (resume.value) {
    router.push(`/resumes/${resume.value.id}/preview`)
  }
}

async function handleSave() {
  if (!resume.value) return

  saving.value = true
  try {
    await resumeApi.update(resume.value.id, {
      title: resume.value.title,
      targetPosition: resume.value.targetPosition,
      sections: resume.value.sections
    })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleBasicInfoUpdate(data: any) {
  if (resume.value) {
    Object.assign(resume.value, data)
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
      await resumeApi.update(resume.value!.id, {
        title: resume.value!.title,
        targetPosition: resume.value!.targetPosition,
        sections: resume.value!.sections
      })
    })
  }
}

// 监听保存状态变化，显示提示
watch(saveStatus, (newStatus) => {
  if (newStatus === 'saved') {
    // 可以在这里显示自动保存成功的提示
  }
})

onMounted(() => {
  loadResume()
})
</script>

<style scoped lang="scss">
.editor-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: white;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    h2 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }

  .header-right {
    display: flex;
    gap: 12px;
  }
}

.editor-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.editor-left {
  width: 45%;
  border-right: 1px solid #e4e7ed;
  background: white;
  display: flex;
  flex-direction: column;

  .editor-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;

    :deep(.el-tabs__content) {
      flex: 1;
      overflow-y: auto;
    }
  }
}

.editor-right {
  width: 55%;
  background: #f5f7fa;
  overflow-y: auto;
}

.placeholder-form {
  padding: 24px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
