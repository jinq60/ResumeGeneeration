<template>
  <div class="resume-list-view">
    <div class="header">
      <h2>我的简历</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建简历
      </el-button>
    </div>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="resumes.length === 0" class="empty">
      <el-empty description="暂无简历，点击上方按钮创建">
        <el-button type="primary" @click="handleCreate">创建简历</el-button>
      </el-empty>
    </div>

    <div v-else class="resume-list">
      <el-card
        v-for="resume in resumes"
        :key="resume.id"
        class="resume-card"
        shadow="hover"
        @click="handleEdit(resume.id)"
      >
        <div class="resume-card-header">
          <h3 class="resume-title">{{ resume.title }}</h3>
          <div class="resume-actions" @click.stop>
            <el-dropdown @command="(cmd) => handleAction(cmd, resume)">
              <el-button circle :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">重命名</el-dropdown-item>
                  <el-dropdown-item command="duplicate">复制</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="resume-info">
          <p class="resume-scene">{{ getSceneText(resume.scene) }}</p>
          <p class="resume-position" v-if="resume.targetPosition">
            目标岗位：{{ resume.targetPosition }}
          </p>
          <p class="resume-time">
            更新于 {{ formatDate(resume.updatedAt) }}
          </p>
        </div>

        <div class="resume-footer">
          <el-button type="primary" size="small" @click.stop="handleEdit(resume.id)">
            编辑
          </el-button>
          <el-button size="small" @click.stop="handlePreview(resume.id)">
            预览
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名简历" width="400px">
      <el-input v-model="newTitle" placeholder="请输入新标题" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenameConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import type { Resume } from '@/types/resume'

const router = useRouter()

const loading = ref(false)
const resumes = ref<Resume[]>([])
const renameDialogVisible = ref(false)
const currentResumeId = ref('')
const newTitle = ref('')

const SCENE_MAP: Record<string, string> = {
  campus_recruitment: '校园招聘',
  internship: '实习',
  social_recruitment: '社会招聘',
  postgraduate_reexam: '考研复试',
  project_application: '项目申请',
  custom: '自定义'
}

function getSceneText(scene: string): string {
  return SCENE_MAP[scene] || scene
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return date.toLocaleDateString()
}

async function loadResumes() {
  loading.value = true
  try {
    const response = await resumeApi.list(1, 20)
    resumes.value = response.list || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载简历列表失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  router.push('/resumes/create')
}

function handleEdit(id: string) {
  router.push(`/resumes/${id}/edit`)
}

function handlePreview(id: string) {
  router.push(`/resumes/${id}/preview`)
}

function handleAction(command: string, resume: Resume) {
  currentResumeId.value = resume.id
  newTitle.value = resume.title

  if (command === 'rename') {
    renameDialogVisible.value = true
  } else if (command === 'duplicate') {
    handleDuplicate(resume.id)
  } else if (command === 'delete') {
    handleDelete(resume.id)
  }
}

async function handleRenameConfirm() {
  if (!newTitle.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  try {
    await resumeApi.rename(currentResumeId.value, newTitle.value)
    ElMessage.success('重命名成功')
    renameDialogVisible.value = false
    await loadResumes()
  } catch (e: any) {
    ElMessage.error(e.message || '重命名失败')
  }
}

async function handleDuplicate(id: string) {
  try {
    const result = await resumeApi.duplicate(id)
    ElMessage.success('复制成功')
    await loadResumes()
  } catch (e: any) {
    ElMessage.error(e.message || '复制失败')
  }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm('确定要删除这份简历吗？删除后无法恢复。', '确认删除', {
      type: 'warning'
    })
    await resumeApi.remove(id)
    ElMessage.success('删除成功')
    await loadResumes()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadResumes()
})
</script>

<style scoped lang="scss">
.resume-list-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 24px;
    color: #303133;
  }
}

.loading {
  padding: 24px;
}

.empty {
  padding: 48px 24px;
}

.resume-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.resume-card {
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-4px);
  }
}

.resume-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.resume-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
  font-weight: 500;
}

.resume-actions {
  opacity: 0;
  transition: opacity 0.2s;

  .resume-card:hover & {
    opacity: 1;
  }
}

.resume-info {
  margin-bottom: 16px;

  p {
    margin: 4px 0;
    font-size: 14px;
    color: #606266;
  }

  .resume-scene {
    color: #409eff;
    font-weight: 500;
  }

  .resume-time {
    color: #909399;
    font-size: 12px;
  }
}

.resume-footer {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}
</style>
