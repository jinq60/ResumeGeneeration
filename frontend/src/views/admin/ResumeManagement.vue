<template>
  <div class="admin-resume-management">
    <div class="mb-8">
      <h2 class="text-headline-md font-bold">
        简历管理
      </h2>
      <p class="text-body-md text-on-surface-variant mt-2">
        查看平台用户创建的简历内容、状态与导出情况
      </p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-gutter mb-8">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant flex items-center gap-4 hover:shadow-md transition-shadow"
      >
        <div
          class="w-12 h-12 rounded-xl flex items-center justify-center"
          :class="stat.iconBg"
        >
          <el-icon
            :class="stat.iconColor"
            size="28"
          >
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div>
          <p class="text-label-md text-on-surface-variant">
            {{ stat.label }}
          </p>
          <p class="text-headline-md font-bold mt-1">
            {{ stat.value }}
          </p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 mb-8">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-gutter">
        <div class="space-y-2">
          <label class="text-label-md font-bold text-on-surface-variant">关键词搜索</label>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索简历标题、目标岗位"
            clearable
          >
            <template #prefix>
              <el-icon size="18">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
        <div class="space-y-2 flex flex-col justify-end">
          <div class="flex gap-2">
            <el-button
              type="primary"
              class="flex-1"
              @click="handleSearch"
            >
              <el-icon size="16">
                <Search />
              </el-icon>查询
            </el-button>
            <el-button @click="resetFilters">
              重置
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm">
      <el-table
        v-loading="loading"
        :data="resumeList"
      >
        <el-table-column
          label="简历ID"
          prop="id"
          width="120"
        />
        <el-table-column
          label="简历标题"
          prop="title"
          min-width="160"
        />
        <el-table-column
          label="用户ID"
          prop="userId"
          min-width="120"
        />
        <el-table-column
          label="目标岗位"
          prop="targetPosition"
          min-width="140"
        />
        <el-table-column
          label="场景"
          prop="scene"
          width="120"
        >
          <template #default="{ row }">
            {{ sceneLabel(row.scene) }}
          </template>
        </el-table-column>
        <el-table-column
          label="模板"
          prop="templateName"
          width="140"
        />
        <el-table-column
          label="导出次数"
          prop="exportCount"
          width="100"
          align="center"
        />
        <el-table-column
          label="更新时间"
          prop="updatedAt"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="140"
          align="right"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="previewResume(row as Resume)"
            >
              预览
            </el-button>
            <el-dropdown @command="(cmd: string) => downloadResume(cmd, row as Resume)">
              <el-button link>
                下载
                <el-icon
                  class="ml-1"
                  size="12"
                >
                  <ArrowDown />
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="word">
                    Word (.docx)
                  </el-dropdown-item>
                  <el-dropdown-item command="markdown">
                    Markdown (.md)
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="pdf"
                    divided
                  >
                    PDF（异步生成）
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <div class="px-6 py-4 flex items-center justify-between bg-surface-container-low border-t border-outline-variant">
        <span class="text-label-md text-on-surface-variant">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10,20,50,100]"
          layout="sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Search, Download, View, ArrowDown } from '@element-plus/icons-vue'
import { resumeApi, type Resume, type ResumeStats } from '@/api/admin/resumes'
import { adminDownload } from '@/utils/adminDownload'

const loading = ref(false)
const filters = reactive({ keyword: '' })
const resumeList = ref<Resume[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = reactive([
  { label: '简历总数', value: '-', icon: Document, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '今日新增', value: '-', icon: Document, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' },
  { label: '活跃简历', value: '-', icon: Download, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '已删除', value: '-', icon: View, iconBg: 'bg-on-tertiary-fixed-variant/10', iconColor: 'text-on-tertiary-fixed-variant' }
])

onMounted(() => {
  loadStats()
  loadResumes()
})

async function loadStats() {
  try {
    const s: ResumeStats = await resumeApi.getResumeStats()
    stats[0].value = String(s.totalResumes)
    stats[1].value = String(s.todayNewResumes)
    stats[2].value = String(s.activeResumes)
    stats[3].value = String(s.deletedResumes)
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计数据失败')
  }
}

async function loadResumes() {
  loading.value = true
  try {
    const res = await resumeApi.getResumes({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword || undefined
    })
    resumeList.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载简历列表失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  page.value = 1
  loadResumes()
}

/** 查询：先回到第一页再加载（watch 会因 page 变化触发一次，避免重复请求） */
function handleSearch() {
  if (page.value === 1) {
    loadResumes()
  } else {
    page.value = 1
  }
}

function sceneLabel(scene: string) {
  const map: Record<string, string> = {
    campus_recruitment: '校园招聘',
    social_recruitment: '社会招聘',
    internship: '实习',
    postgraduate_reexam: '考研复试',
    project_application: '项目申请',
    custom: '自定义'
  }
  return map[scene] || scene
}

function formatDate(date: string | null) {
  return date ? date.replace('T', ' ').substring(0, 19) : '-'
}

function previewResume(row: Resume) {
  window.open(resumeApi.previewUrl(row.id), '_blank')
}

async function downloadResume(format: string, row: Resume) {
  try {
    if (format === 'word') {
      const fileName = await adminDownload(resumeApi.exportWordUrl(row.id), '简历.docx')
      ElMessage.success(`已导出 ${fileName}`)
    } else if (format === 'markdown') {
      const fileName = await adminDownload(resumeApi.exportMarkdownUrl(row.id), '简历.md')
      ElMessage.success(`已导出 ${fileName}`)
    } else if (format === 'pdf') {
      const { taskId } = await resumeApi.exportPdf(row.id)
      ElMessage.info('PDF 正在后台生成，请稍候…')
      // 轮询任务状态（最长 120 秒）
      for (let i = 0; i < 120; i++) {
        await new Promise(r => setTimeout(r, 1000))
        const task = await resumeApi.getPdfTask(taskId)
        if (task.status === 'success') {
          const fileName = await adminDownload(resumeApi.pdfDownloadUrl(taskId), task.fileName || '简历.pdf')
          ElMessage.success(`已导出 ${fileName}`)
          return
        }
        if (task.status === 'failed') {
          ElMessage.error(task.errorMsg || 'PDF 生成失败')
          return
        }
      }
      ElMessage.warning('PDF 生成超时，请稍后在用户下载中心查看')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '下载失败')
  }
}

watch([page, pageSize], loadResumes)
</script>

<style scoped lang="scss">
.admin-resume-management {
  padding-bottom: var(--st-margin-page);
}
</style>
