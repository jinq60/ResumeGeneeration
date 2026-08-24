<template>
  <div class="content-audit">
    <!-- Header -->
    <div class="mb-8">
      <h2 class="text-headline-md font-bold">
        内容审核
      </h2>
      <p class="text-body-md text-on-surface-variant mt-2">
        审核用户简历内容，识别违规与风险信息，保障平台合规
      </p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-gutter mb-8">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-container-lowest p-4 rounded-xl border border-outline-variant shadow-sm hover:shadow-md transition-shadow"
      >
        <div class="flex items-center gap-3 mb-2">
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center"
            :class="stat.iconBg"
          >
            <el-icon
              :class="stat.iconColor"
              size="20"
            >
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <span class="text-label-md text-on-surface-variant">{{ stat.label }}</span>
        </div>
        <div class="flex items-baseline justify-between">
          <h3 class="text-headline-md font-bold">
            {{ stat.value }}
          </h3>
        </div>
      </div>
    </div>

    <div class="flex gap-gutter items-start">
      <!-- Left List -->
      <div class="flex-1 flex flex-col gap-gutter">
        <!-- Filters -->
        <div class="bg-surface-container-lowest rounded-xl p-4 border border-outline-variant shadow-sm">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div class="space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">搜索</label>
              <el-input
                v-model="filters.keyword"
                placeholder="简历名称 / 用户ID"
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">风险等级</label>
              <el-select
                v-model="filters.riskLevel"
                placeholder="全部"
                clearable
                @change="handleSearch"
              >
                <el-option
                  label="全部"
                  value=""
                />
                <el-option
                  label="低风险"
                  value="low"
                />
                <el-option
                  label="中风险"
                  value="medium"
                />
                <el-option
                  label="高风险"
                  value="high"
                />
              </el-select>
            </div>
            <div class="space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">审核状态</label>
              <el-select
                v-model="filters.status"
                placeholder="全部"
                clearable
                @change="handleSearch"
              >
                <el-option
                  label="全部"
                  value=""
                />
                <el-option
                  label="待审核"
                  value="pending"
                />
                <el-option
                  label="已通过"
                  value="approved"
                />
                <el-option
                  label="已驳回"
                  value="rejected"
                />
                <el-option
                  label="风险预警"
                  value="warning"
                />
              </el-select>
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
                  </el-icon>搜索
                </el-button>
                <el-button @click="resetFilters">
                  重置
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Table -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm overflow-hidden">
          <el-table
            v-loading="loading"
            :data="auditList"
            highlight-current-row
            @row-click="selectItem"
          >
            <el-table-column
              label="简历名称"
              min-width="180"
            >
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <el-icon
                    class="text-primary"
                    size="20"
                  >
                    <Document />
                  </el-icon>
                  <span
                    class="font-bold"
                    :class="row.id === currentItem?.id ? 'text-primary' : ''"
                  >{{ row.targetTitle || '未命名简历' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              label="所属用户"
              min-width="160"
            >
              <template #default="{ row }">
                <div>
                  <p class="text-[12px] text-on-surface-variant">
                    ID: {{ row.userId }}
                  </p>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              label="提交时间"
              prop="createdAt"
              width="170"
            >
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column
              label="状态"
              width="110"
            >
              <template #default="{ row }">
                <el-tag
                  :type="statusTagType(row.status)"
                  size="small"
                >
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="风险等级"
              width="110"
            >
              <template #default="{ row }">
                <el-tag
                  :type="riskTagType(row.riskLevel)"
                  size="small"
                >
                  {{ riskLabel(row.riskLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="150"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click.stop="selectItem(row as AuditItem)"
                >
                  查看
                </el-button>
                <el-button
                  link
                  type="primary"
                  @click.stop="auditItem(row as AuditItem)"
                >
                  审核
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="px-6 py-4 bg-surface-container-low border-t border-outline-variant flex items-center justify-between">
            <span class="text-label-md text-on-surface-variant">共 {{ total }} 条</span>
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
      </div>

      <!-- Right Inspector -->
      <div class="w-[400px] shrink-0 bg-surface-container-lowest border border-outline-variant rounded-xl flex flex-col overflow-hidden sticky top-4">
        <div
          v-if="currentItem"
          class="flex flex-col h-full"
        >
          <div class="p-6 border-b border-outline-variant flex items-center justify-between">
            <div class="flex items-center gap-3">
              <h2 class="text-title-md font-bold">
                {{ currentItem.targetTitle || '未命名简历' }}
              </h2>
              <el-tag
                :type="statusTagType(currentItem.status)"
                size="small"
              >
                {{ statusLabel(currentItem.status) }}
              </el-tag>
            </div>
            <el-button
              text
              @click="currentItem = null"
            >
              <el-icon size="18">
                <Close />
              </el-icon>
            </el-button>
          </div>
          <div class="p-6 flex-1 overflow-y-auto custom-scrollbar space-y-6">
            <div class="flex items-center gap-4 p-4 bg-surface-container-low rounded-xl border border-outline-variant">
              <el-avatar
                :size="48"
              >
                <el-icon size="20">
                  <User />
                </el-icon>
              </el-avatar>
              <div class="flex-1">
                <div class="flex items-center justify-between">
                  <h3 class="font-bold">
                    用户 {{ currentItem.userId }}
                  </h3>
                </div>
                <div class="mt-2 flex gap-2">
                  <span class="px-1.5 py-0.5 bg-white border border-outline-variant rounded text-[10px]">
                    风险等级：{{ riskLabel(currentItem.riskLevel) }}
                  </span>
                </div>
              </div>
            </div>

            <div class="relative group cursor-pointer overflow-hidden rounded-xl border border-outline-variant shadow-sm aspect-[3/4] bg-surface-container flex items-center justify-center">
              <div class="flex flex-col items-center gap-3 text-outline">
                <el-icon size="48">
                  <Document />
                </el-icon>
                <span class="text-label-md">简历预览</span>
              </div>
              <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <el-button
                  type="primary"
                  @click="previewFull(currentItem)"
                >
                  <el-icon size="18">
                    <View />
                  </el-icon>预览全文
                </el-button>
              </div>
            </div>

            <div
              v-if="currentItem.reviewNote || currentItem.reviewerId"
              class="space-y-4"
            >
              <div class="flex items-center gap-2">
                <el-icon
                  class="text-tertiary"
                  size="20"
                >
                  <MagicStick />
                </el-icon>
                <h4 class="text-title-md font-bold">
                  审核记录
                </h4>
              </div>
              <div class="p-4 rounded-xl border border-outline-variant bg-surface-container-low">
                <p class="text-body-md text-on-surface-variant">
                  审核人：{{ currentItem.reviewerId }} · {{ formatDate(currentItem.reviewedAt) }}
                </p>
                <p
                  v-if="currentItem.reviewNote"
                  class="text-body-md text-on-surface-variant mt-1"
                >
                  备注：{{ currentItem.reviewNote }}
                </p>
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-outline-variant grid grid-cols-2 gap-3">
            <el-button
              type="success"
              @click="approve(currentItem)"
            >
              通过
            </el-button>
            <el-button
              type="danger"
              @click="reject(currentItem)"
            >
              驳回
            </el-button>
            <el-button @click="markWarning(currentItem)">
              标记风险
            </el-button>
            <el-button @click="previewFull(currentItem)">
              预览
            </el-button>
          </div>
        </div>
        <div
          v-else
          class="flex-1 flex items-center justify-center text-on-surface-variant p-6"
        >
          请选择一条审核记录查看详情
        </div>
      </div>
    </div>

    <!-- 标记风险 Dialog -->
    <el-dialog
      v-model="warningDialogVisible"
      title="标记风险"
      width="420px"
    >
      <el-form label-width="90px">
        <el-form-item label="风险等级">
          <el-select
            v-model="warningRiskLevel"
            class="w-full"
          >
            <el-option
              label="低风险"
              value="low"
            />
            <el-option
              label="中风险"
              value="medium"
            />
            <el-option
              label="高风险"
              value="high"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="warningNote"
            type="textarea"
            :rows="3"
            placeholder="风险说明（可选）"
            maxlength="512"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warningDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="confirmMarkWarning"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, Search, WarningFilled, CircleCheckFilled, Close, User, View, MagicStick, Delete
} from '@element-plus/icons-vue'
import { auditApi, adminResumePreviewUrl, type AuditItem, type AuditStats } from '@/api/admin/audits'

const loading = ref(false)
const filters = reactive({ keyword: '', riskLevel: '', status: '' })
const auditList = ref<AuditItem[]>([])
const currentItem = ref<AuditItem | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const warningDialogVisible = ref(false)
const warningRiskLevel = ref('medium')
const warningNote = ref('')

const stats = ref([
  { label: '待审核', value: '-', icon: WarningFilled, iconBg: 'bg-amber-100', iconColor: 'text-amber-600' },
  { label: '已通过', value: '-', icon: CircleCheckFilled, iconBg: 'bg-emerald-100', iconColor: 'text-emerald-600' },
  { label: '风险预警', value: '-', icon: WarningFilled, iconBg: 'bg-rose-100', iconColor: 'text-rose-600' },
  { label: '已驳回', value: '-', icon: Close, iconBg: 'bg-surface-container-high', iconColor: 'text-on-surface-variant' },
  { label: '今日已审', value: '-', icon: Delete, iconBg: 'bg-surface-container-high', iconColor: 'text-on-surface-variant' }
])

const statusLabelsMap: Record<string, string> = {
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  warning: '风险预警'
}

const riskLabelsMap: Record<string, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险'
}

function statusLabel(status: string) {
  return statusLabelsMap[status] || status
}

function riskLabel(risk: string) {
  return riskLabelsMap[risk] || risk
}

function statusTagType(status: string) {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'warning': return 'danger'
    default: return 'info'
  }
}
function riskTagType(risk: string) {
  switch (risk) {
    case 'low': return 'success'
    case 'medium': return 'warning'
    case 'high': return 'danger'
    default: return 'info'
  }
}

function formatDate(date: string | null | undefined) {
  return date ? date.replace('T', ' ').substring(0, 19) : '-'
}

function selectItem(row: AuditItem) {
  currentItem.value = row
}

function auditItem(row: AuditItem) {
  currentItem.value = row
}

function previewFull(row: AuditItem) {
  if (!row.targetId) return
  window.open(adminResumePreviewUrl(row.targetId), '_blank')
}

async function approve(row: AuditItem) {
  try {
    await auditApi.approve(row.id)
    ElMessage.success(`已通过 ${row.targetTitle || row.id}`)
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function reject(row: AuditItem) {
  try {
    await auditApi.reject(row.id)
    ElMessage.warning(`已驳回 ${row.targetTitle || row.id}`)
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function markWarning(row: AuditItem) {
  warningRiskLevel.value = row.riskLevel || 'medium'
  warningNote.value = ''
  warningDialogVisible.value = true
}

async function confirmMarkWarning() {
  if (!currentItem.value) return
  try {
    await auditApi.markWarning(currentItem.value.id, warningRiskLevel.value, warningNote.value || undefined)
    warningDialogVisible.value = false
    ElMessage.warning('已标记风险')
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.riskLevel = ''
  filters.status = ''
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

async function loadStats() {
  try {
    const s: AuditStats = await auditApi.stats()
    stats.value[0].value = String(s.pending)
    stats.value[1].value = String(s.approved)
    stats.value[2].value = String(s.warning)
    stats.value[3].value = String(s.rejected)
    stats.value[4].value = String(s.todayReviewed)
  } catch {
    // 统计失败保持占位
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await auditApi.list({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      riskLevel: filters.riskLevel || undefined
    })
    auditList.value = res.records || []
    total.value = res.total || 0
    if (currentItem.value) {
      currentItem.value = auditList.value.find(i => i.id === currentItem.value?.id) || null
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载审核列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped lang="scss">
.content-audit {
  padding-bottom: var(--st-margin-page);
}
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: var(--st-outline-variant); border-radius: 4px; }
</style>