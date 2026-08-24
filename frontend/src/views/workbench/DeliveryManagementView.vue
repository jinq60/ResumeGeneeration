<template>
  <main class="workbench-page py-stack-lg">
    <!-- Header -->
    <div class="flex justify-between items-end mb-stack-lg">
      <div>
        <h1 class="text-headline-md font-headline-md text-on-surface">
          投递管理
        </h1>
        <p class="text-body-md font-body-md text-on-surface-variant mt-1">
          跟踪每一次投递进展，掌握求职全局
        </p>
      </div>
      <button
        class="bg-primary text-on-primary px-6 py-3 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
        @click="openCreateDialog"
      >
        <el-icon size="18">
          <Plus />
        </el-icon>
        新建投递记录
      </button>
    </div>

    <!-- Filters + Table -->
    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm overflow-hidden flex flex-col">
      <div class="p-4 flex flex-wrap items-center gap-4 border-b border-outline-variant bg-surface-container-low/30">
        <div class="w-64">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索公司 / 职位"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon size="18">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
        <div class="w-32">
          <el-input
            v-model="filters.position"
            placeholder="职位"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="w-32">
          <el-input
            v-model="filters.company"
            placeholder="公司"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="w-32">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            @change="handleSearch"
          >
            <el-option
              v-for="(label, value) in statusLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </div>
        <div class="w-96">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="w-full"
            @change="handleSearch"
          />
        </div>
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors"
          @click="resetFilters"
        >
          重置
        </button>
      </div>

      <el-table
        v-loading="loading"
        :data="deliveryList"
        highlight-current-row
        @row-click="openDetail"
      >
        <el-table-column
          label="公司"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg border border-outline-variant flex items-center justify-center bg-white overflow-hidden p-1">
                <el-icon
                  v-if="!row.channel"
                  size="24"
                >
                  <OfficeBuilding />
                </el-icon>
                <span
                  v-else
                  class="text-[10px] font-bold text-on-surface-variant px-1 text-center"
                >{{ row.channel }}</span>
              </div>
              <span class="text-title-md font-title-md text-on-surface">{{ row.company }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="职位"
          prop="position"
          min-width="180"
        />
        <el-table-column
          label="渠道"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <span class="text-body-md text-on-surface-variant">{{ row.channel || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="投递日期"
          prop="applyDate"
          width="120"
          align="center"
        />
        <el-table-column
          label="当前状态"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
              round
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="备注"
          min-width="220"
        >
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <span class="text-body-md font-body-md text-on-surface truncate">{{ row.note || '—' }}</span>
              <span
                v-if="row.interviewTime"
                class="text-label-md text-on-surface-variant"
              >面试：{{ formatDateTime(row.interviewTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label=""
          width="60"
          align="right"
        >
          <template #default="{ row }">
            <el-icon
              class="text-on-surface-variant hover:text-primary cursor-pointer"
              size="18"
              @click.stop="openDetail(row as DeliveryItem)"
            >
              <MoreFilled />
            </el-icon>
          </template>
        </el-table-column>
      </el-table>

      <div class="p-4 border-t border-outline-variant flex justify-between items-center bg-surface-container-lowest">
        <span class="text-body-md font-body-md text-on-surface-variant">共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10,20,50]"
          layout="sizes, prev, pager, next"
          background
          small
          @current-change="loadList"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="drawerVisible"
      :title="current?.company"
      size="420px"
      :with-header="false"
    >
      <div
        v-if="current"
        class="flex flex-col h-full"
      >
        <div class="flex justify-between items-start mb-stack-lg">
          <h2 class="text-title-lg font-title-lg text-on-surface">
            {{ current.company }} - {{ current.position }}
          </h2>
          <button
            class="p-2 text-on-surface-variant hover:text-primary hover:bg-surface-container-low rounded-lg transition-colors"
            @click="drawerVisible = false"
          >
            <el-icon size="18">
              <Close />
            </el-icon>
          </button>
        </div>

        <div class="flex items-start gap-4 p-4 bg-surface-container-low rounded-xl border border-outline-variant mb-stack-lg">
          <div class="w-14 h-14 rounded-lg bg-white border border-outline-variant flex items-center justify-center p-1.5 shadow-sm">
            <el-icon
              v-if="!current.channel"
              size="28"
            >
              <OfficeBuilding />
            </el-icon>
            <span
              v-else
              class="text-xs font-bold text-on-surface-variant px-1 text-center"
            >{{ current.channel }}</span>
          </div>
          <div class="flex-1">
            <div class="flex justify-between items-center">
              <span class="text-title-md font-title-md text-on-surface">{{ current.company }}</span>
              <el-tag
                :type="statusTagType(current.status)"
                size="small"
                round
              >
                {{ statusLabel(current.status) }}
              </el-tag>
            </div>
            <p class="text-body-md font-body-md text-on-surface-variant mt-1">
              {{ current.position }}
            </p>
            <p class="text-[12px] text-on-surface-variant mt-1">
              投递日期：{{ current.applyDate || '—' }}
            </p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4 mb-stack-lg">
          <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant">
            <div class="flex justify-between items-center mb-2">
              <span class="text-body-md font-body-md">JD 摘要</span>
              <el-icon size="14">
                <Document />
              </el-icon>
            </div>
            <p class="text-label-md text-on-surface-variant leading-relaxed line-clamp-4">
              {{ current.jdContent || '未填写 JD 内容' }}
            </p>
            <button
              class="mt-3 text-primary text-label-md font-label-md hover:underline"
              @click="viewJd(current)"
            >
              查看 JD
            </button>
          </div>
          <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant">
            <div class="flex justify-between items-center mb-2">
              <span class="text-body-md font-body-md">关键提醒</span>
              <el-icon size="14">
                <InfoFilled />
              </el-icon>
            </div>
            <ul class="space-y-2 text-label-md font-label-md">
              <li
                v-for="(tip, i) in statusTips(current.status)"
                :key="i"
                class="flex items-start gap-1.5"
              >
                <span
                  class="w-1.5 h-1.5 rounded-full mt-1.5"
                  :class="tip.urgent ? 'bg-error' : 'bg-secondary'"
                />
                {{ tip.text }}
              </li>
            </ul>
          </div>
        </div>

        <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant mb-stack-lg">
          <div class="flex justify-between items-center mb-3">
            <span class="text-title-md font-title-md text-on-surface">面试安排</span>
          </div>
          <div class="flex gap-3">
            <div class="w-10 h-10 rounded-lg bg-tertiary-fixed flex items-center justify-center">
              <el-icon
                class="text-tertiary"
                size="20"
              >
                <Calendar />
              </el-icon>
            </div>
            <div class="flex-1">
              <div class="text-title-md font-title-md text-on-surface">
                {{ current.interviewTime ? '已安排面试' : '暂无面试安排' }}
              </div>
              <div class="text-body-md font-body-md text-on-surface-variant mt-1">
                {{ current.interviewTime ? formatDateTime(current.interviewTime) : '可在「更新进度」中补充面试时间与地点' }}
              </div>
              <div
                v-if="current.interviewLocation"
                class="text-body-md font-body-md text-on-surface-variant"
              >
                {{ current.interviewLocation }}
              </div>
            </div>
          </div>
          <button
            v-if="current.interviewTime"
            class="w-full mt-4 border border-outline-variant text-on-surface-variant hover:bg-surface-container-low px-4 py-2 rounded-lg font-label-md transition-colors"
            @click="addToCalendar(current)"
          >
            添加到日历
          </button>
        </div>

        <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant mb-stack-lg">
          <div class="text-title-md font-title-md text-on-surface mb-3">
            使用的简历/模板
          </div>
          <button
            class="flex items-center gap-3 p-2 bg-surface-container-lowest rounded-lg border border-outline-variant w-full text-left hover:bg-surface-container transition-colors"
            @click="goResume(current)"
          >
            <div class="w-10 h-12 bg-primary-fixed rounded flex items-center justify-center overflow-hidden">
              <el-icon size="24">
                <Document />
              </el-icon>
            </div>
            <div class="flex-1">
              <div class="text-title-md font-title-md text-on-surface">
                打开关联简历
              </div>
              <div class="text-[12px] text-on-surface-variant mt-0.5">
                {{ current.resumeId }}
              </div>
            </div>
          </button>
        </div>

        <div class="grid grid-cols-2 gap-3 mt-auto">
          <button
            class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md shadow-sm hover:scale-[0.98] transition-transform"
            @click="openProgressDialog(current)"
          >
            更新进度
          </button>
          <button
            class="border border-outline-variant text-on-surface-variant hover:bg-surface-container-low px-4 py-2 rounded-lg font-label-md transition-colors"
            @click="viewJd(current)"
          >
            查看 JD
          </button>
        </div>
      </div>
    </el-drawer>

    <!-- 新建投递记录 Dialog -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建投递记录"
      width="560px"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="90px"
        label-position="left"
      >
        <el-form-item
          label="关联简历"
          prop="resumeId"
        >
          <el-select
            v-model="createForm.resumeId"
            placeholder="选择投递使用的简历"
            class="w-full"
          >
            <el-option
              v-for="resume in resumeOptions"
              :key="resume.id"
              :label="resume.title || '未命名简历'"
              :value="resume.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="公司名称"
          prop="company"
        >
          <el-input
            v-model="createForm.company"
            placeholder="请输入公司名称"
            maxlength="128"
          />
        </el-form-item>
        <el-form-item
          label="职位名称"
          prop="position"
        >
          <el-input
            v-model="createForm.position"
            placeholder="请输入职位名称"
            maxlength="128"
          />
        </el-form-item>
        <el-form-item label="投递渠道">
          <el-input
            v-model="createForm.channel"
            placeholder="如：官网 / Boss 直聘 / 内推"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="投递日期">
          <el-date-picker
            v-model="createForm.applyDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择投递日期"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-select
            v-model="createForm.status"
            class="w-full"
          >
            <el-option
              v-for="(label, value) in statusLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="JD 内容">
          <el-input
            v-model="createForm.jdContent"
            type="textarea"
            :rows="4"
            placeholder="粘贴职位描述（JD），便于后续 AI 匹配分析"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createForm.note"
            type="textarea"
            :rows="2"
            placeholder="备注（可选）"
            maxlength="512"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="submitCreate"
        >
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 更新进度 Dialog -->
    <el-dialog
      v-model="progressDialogVisible"
      title="更新进度"
      width="480px"
    >
      <el-form
        v-if="current"
        :model="progressForm"
        label-width="90px"
        label-position="left"
      >
        <el-form-item label="当前状态">
          <el-select
            v-model="progressForm.status"
            class="w-full"
          >
            <el-option
              v-for="(label, value) in statusLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="面试时间">
          <el-date-picker
            v-model="progressForm.interviewTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择面试时间"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="面试地点">
          <el-input
            v-model="progressForm.interviewLocation"
            placeholder="面试地点（可选）"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="progressForm.note"
            type="textarea"
            :rows="2"
            placeholder="备注（可选）"
            maxlength="512"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="submitProgress"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看 JD Dialog -->
    <el-dialog
      v-model="jdDialogVisible"
      :title="`JD 内容 - ${jdTarget?.company || ''}`"
      width="560px"
    >
      <div class="whitespace-pre-wrap text-body-md text-on-surface leading-relaxed max-h-[50vh] overflow-y-auto">
        {{ jdTarget?.jdContent || '该投递记录未填写 JD 内容。' }}
      </div>
      <template #footer>
        <el-button
          v-if="jdTarget?.jdContent"
          type="primary"
          @click="goMatchAnalysis(jdTarget)"
        >
          查看匹配分析
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus, Search, MoreFilled, Close,
  OfficeBuilding, Calendar, InfoFilled, Document
} from '@element-plus/icons-vue'
import { deliveryApi, DELIVERY_STATUS_LABELS, type DeliveryRecord } from '@/api/delivery'
import { resumeApi } from '@/api/resume'

type DeliveryItem = DeliveryRecord

const router = useRouter()

const loading = ref(false)
const drawerVisible = ref(false)
const current = ref<DeliveryItem | null>(null)
const filters = reactive({ keyword: '', position: '', company: '', status: '', dateRange: [] as Date[] })
const deliveryList = ref<DeliveryItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusLabels = DELIVERY_STATUS_LABELS

const createDialogVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  resumeId: '',
  company: '',
  position: '',
  channel: '',
  applyDate: '',
  status: 'delivered',
  jdContent: '',
  note: ''
})
const createRules: FormRules = {
  resumeId: [{ required: true, message: '请选择关联简历', trigger: 'change' }],
  company: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  position: [{ required: true, message: '请输入职位名称', trigger: 'blur' }]
}

const resumeOptions = ref<Array<{ id: string; title: string }>>([])

const progressDialogVisible = ref(false)
const progressForm = reactive({ status: '', interviewTime: '', interviewLocation: '', note: '' })

const jdDialogVisible = ref(false)
const jdTarget = ref<DeliveryItem | null>(null)

function statusLabel(status: string) {
  return DELIVERY_STATUS_LABELS[status] || status
}

function statusTagType(status: string) {
  switch (status) {
    case 'delivered': return 'info'
    case 'written': return 'primary'
    case 'interview1':
    case 'interview2': return 'warning'
    case 'hr': return 'success'
    case 'offer': return 'success'
    case 'rejected': return 'danger'
    default: return 'info'
  }
}

function statusTips(status: string) {
  const map: Record<string, Array<{ text: string; urgent: boolean }>> = {
    delivered: [{ text: '已投递，可在一周后跟进 HR', urgent: false }],
    written: [{ text: '笔试阶段，建议复习高频考点', urgent: false }],
    interview1: [{ text: '一面进行中，提前准备自我介绍', urgent: true }],
    interview2: [{ text: '二面进行中，深入准备项目细节', urgent: true }],
    hr: [{ text: 'HR 面进行中，准备薪资与到岗时间', urgent: false }],
    offer: [{ text: '已拿到 Offer，及时确认入职时间', urgent: false }],
    rejected: [{ text: '已结束，可复盘并投递其他机会', urgent: false }],
    withdrawn: [{ text: '已放弃该机会', urgent: false }]
  }
  return map[status] || [{ text: '保持跟进', urgent: false }]
}

function formatDateTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').substring(0, 16)
}

function openDetail(row: DeliveryItem) {
  current.value = row
  drawerVisible.value = true
}

function openCreateDialog() {
  createForm.resumeId = resumeOptions.value[0]?.id || ''
  createForm.company = ''
  createForm.position = ''
  createForm.channel = ''
  createForm.applyDate = new Date().toISOString().slice(0, 10)
  createForm.status = 'delivered'
  createForm.jdContent = ''
  createForm.note = ''
  createDialogVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await deliveryApi.create({ ...createForm })
    ElMessage.success('投递记录已创建')
    createDialogVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

function openProgressDialog(row: DeliveryItem) {
  progressForm.status = row.status
  progressForm.interviewTime = row.interviewTime || ''
  progressForm.interviewLocation = row.interviewLocation || ''
  progressForm.note = row.note || ''
  progressDialogVisible.value = true
}

async function submitProgress() {
  if (!current.value) return
  submitting.value = true
  try {
    const updated = await deliveryApi.update(current.value.id, {
      resumeId: current.value.resumeId,
      company: current.value.company,
      position: current.value.position,
      channel: current.value.channel,
      status: progressForm.status,
      applyDate: current.value.applyDate,
      jdContent: current.value.jdContent,
      note: progressForm.note,
      interviewTime: progressForm.interviewTime || undefined,
      interviewLocation: progressForm.interviewLocation || undefined
    })
    current.value = updated
    progressDialogVisible.value = false
    ElMessage.success('进度已更新')
    loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

function viewJd(row: DeliveryItem) {
  jdTarget.value = row
  jdDialogVisible.value = true
}

function goMatchAnalysis(row: DeliveryItem) {
  const jd = row.jdContent || ''
  router.push({
    path: `/workbench/resumes/${row.resumeId}/review`,
    query: jd ? { jd: encodeURIComponent(jd) } : {}
  })
}

function addToCalendar(row: DeliveryItem) {
  if (!row.interviewTime) return
  const title = `${row.company} - ${row.position} 面试`
  const start = new Date(row.interviewTime)
  const end = new Date(start.getTime() + 60 * 60 * 1000)
  const format = (d: Date) =>
    `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}${String(d.getMinutes()).padStart(2, '0')}00`
  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//ResumeGenerator//CN',
    'BEGIN:VEVENT',
    `UID:${row.id}@resume`,
    `DTSTAMP:${format(new Date())}`,
    `DTSTART:${format(start)}`,
    `DTEND:${format(end)}`,
    `SUMMARY:${title}`,
    row.interviewLocation ? `LOCATION:${row.interviewLocation}` : '',
    'END:VEVENT',
    'END:VCALENDAR'
  ].filter(Boolean).join('\r\n')
  const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${row.company}-${row.position}-面试提醒.ics`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  ElMessage.success('已生成日历文件，导入日历即可')
}

function goResume(row: DeliveryItem) {
  router.push(`/workbench/editor/${row.resumeId}`)
}

function resetFilters() {
  filters.keyword = ''
  filters.position = ''
  filters.company = ''
  filters.status = ''
  filters.dateRange = []
  page.value = 1
  loadList()
}

function handleSearch() {
  page.value = 1
  loadList()
}

function handleSizeChange() {
  page.value = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const res = await deliveryApi.list({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      company: filters.company || undefined,
      position: filters.position || undefined,
      status: filters.status || undefined,
      startDate: filters.dateRange?.[0] ? filters.dateRange[0].toISOString().slice(0, 10) : undefined,
      endDate: filters.dateRange?.[1] ? filters.dateRange[1].toISOString().slice(0, 10) : undefined
    })
    deliveryList.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载投递记录失败')
  } finally {
    loading.value = false
  }
}

async function loadResumeOptions() {
  try {
    const res = await resumeApi.list(1, 100)
    resumeOptions.value = (res.records || []).map(r => ({ id: r.id, title: r.title }))
  } catch {
    resumeOptions.value = []
  }
}

watch([page, pageSize], () => loadList())

onMounted(() => {
  loadList()
  loadResumeOptions()
})
</script>