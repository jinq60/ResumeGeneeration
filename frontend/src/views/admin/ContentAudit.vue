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
          <span
            class="text-[12px] flex items-center gap-0.5"
            :class="stat.growthClass"
          >
            <el-icon size="14"><component :is="stat.growthIcon" /></el-icon>{{ stat.growth }}
          </span>
        </div>
      </div>
    </div>

    <div class="flex gap-gutter items-start">
      <!-- Left List -->
      <div class="flex-1 flex flex-col gap-gutter">
        <!-- Filters -->
        <div class="bg-surface-container-lowest rounded-xl p-4 border border-outline-variant shadow-sm">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            <div class="lg:col-span-1 space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">搜索</label>
              <el-input
                v-model="filters.keyword"
                placeholder="简历名称 / 用户昵称"
                clearable
              />
            </div>
            <div class="space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">简历类型</label>
              <el-select
                v-model="filters.type"
                placeholder="全部"
                clearable
              >
                <el-option
                  label="全部"
                  value=""
                />
                <el-option
                  label="标准简历"
                  value="standard"
                />
                <el-option
                  label="设计简历"
                  value="design"
                />
              </el-select>
            </div>
            <div class="space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">审核状态</label>
              <el-select
                v-model="filters.status"
                placeholder="全部"
                clearable
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
            <div class="space-y-2">
              <label class="text-label-md font-bold text-on-surface-variant">公开状态</label>
              <el-select
                v-model="filters.public"
                placeholder="全部"
                clearable
              >
                <el-option
                  label="全部"
                  value=""
                />
                <el-option
                  label="公开"
                  value="public"
                />
                <el-option
                  label="未公开"
                  value="private"
                />
              </el-select>
            </div>
            <div class="space-y-2 flex flex-col justify-end">
              <div class="flex gap-2">
                <el-button
                  type="primary"
                  class="flex-1"
                  @click="loadList"
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
              type="selection"
              width="48"
            />
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
                  >{{ row.title }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              label="所属用户"
              min-width="160"
            >
              <template #default="{ row }">
                <div>
                  <p class="font-medium">
                    {{ row.userName }}
                  </p>
                  <p class="text-[12px] text-on-surface-variant">
                    ID: {{ row.userId }}
                  </p>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              label="岗位类型"
              prop="jobType"
              width="130"
            />
            <el-table-column
              label="更新时间"
              prop="updatedAt"
              width="170"
            />
            <el-table-column
              label="状态"
              width="110"
            >
              <template #default="{ row }">
                <el-tag
                  :type="statusTagType(row.status)"
                  size="small"
                >
                  {{ row.statusLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="风险等级"
              width="110"
            >
              <template #default="{ row }">
                <el-tag
                  :type="riskTagType(row.risk)"
                  size="small"
                >
                  {{ row.riskLabel }}
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
                  @click.stop="selectItem(row)"
                >
                  查看
                </el-button>
                <el-button
                  link
                  type="primary"
                  @click.stop="auditItem(row)"
                >
                  审核
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="px-6 py-4 bg-surface-container-low border-t border-outline-variant flex items-center justify-between">
            <span class="text-label-md text-on-surface-variant">共 {{ total }} 条，当前显示 1-10 条</span>
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10,20,50]"
              layout="sizes, prev, pager, next"
              background
              small
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
                {{ currentItem.title }}
              </h2>
              <el-tag
                :type="statusTagType(currentItem.status)"
                size="small"
              >
                {{ currentItem.statusLabel }}
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
            <!-- User info -->
            <div class="flex items-center gap-4 p-4 bg-surface-container-low rounded-xl border border-outline-variant">
              <el-avatar
                :size="48"
                :src="currentItem.avatarUrl"
              >
                <el-icon size="20">
                  <User />
                </el-icon>
              </el-avatar>
              <div class="flex-1">
                <div class="flex items-center justify-between">
                  <h3 class="font-bold">
                    {{ currentItem.userName }}
                  </h3>
                  <span class="text-[12px] text-primary font-medium">{{ currentItem.vip }}</span>
                </div>
                <p class="text-[12px] text-on-surface-variant">
                  ID: {{ currentItem.userId }}
                </p>
                <div class="mt-2 flex gap-2">
                  <span class="px-1.5 py-0.5 bg-white border border-outline-variant rounded text-[10px]">标准简历</span>
                  <span class="px-1.5 py-0.5 bg-white border border-outline-variant rounded text-[10px]">用户上传</span>
                </div>
              </div>
            </div>

            <!-- Preview thumbnail -->
            <div class="relative group cursor-pointer overflow-hidden rounded-xl border border-outline-variant shadow-sm aspect-[3/4] bg-surface-container flex items-center justify-center">
              <img
                v-if="currentItem.thumbnail"
                :src="currentItem.thumbnail"
                class="w-full h-full object-cover"
              >
              <div
                v-else
                class="flex flex-col items-center gap-3 text-outline"
              >
                <el-icon size="48">
                  <Document />
                </el-icon>
                <span class="text-label-md">暂无预览</span>
              </div>
              <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <el-button type="primary">
                  <el-icon size="18">
                    <View />
                  </el-icon>预览全文
                </el-button>
              </div>
            </div>

            <!-- AI insights -->
            <div class="space-y-4">
              <div class="flex items-center gap-2">
                <el-icon
                  class="text-tertiary"
                  size="20"
                >
                  <MagicStick />
                </el-icon>
                <h4 class="text-title-md font-bold">
                  问题点摘要 (AI 智能识别)
                </h4>
              </div>
              <div
                v-for="issue in currentItem.issues"
                :key="issue.title"
                class="p-4 rounded-xl border"
                :class="issueBorderClass(issue.level)"
              >
                <div class="flex items-center gap-2 mb-2">
                  <el-icon
                    size="18"
                    :class="issueColorClass(issue.level)"
                  >
                    <component :is="issue.icon" />
                  </el-icon>
                  <span
                    class="font-bold"
                    :class="issueColorClass(issue.level)"
                  >{{ issue.title }}</span>
                </div>
                <p class="text-body-md text-on-surface-variant">
                  {{ issue.desc }}
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
            <el-button @click="preview(currentItem)">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, Search, WarningFilled, CircleCheckFilled, ArrowUp, ArrowDown,
  Close, User, View, MagicStick, InfoFilled, Delete
} from '@element-plus/icons-vue'

interface AuditItem {
  id: string
  title: string
  userName: string
  userId: string
  avatarUrl: string
  vip: string
  jobType: string
  updatedAt: string
  status: 'pending' | 'approved' | 'rejected' | 'warning'
  statusLabel: string
  risk: 'low' | 'medium' | 'high'
  riskLabel: string
  thumbnail: string
  issues: { title: string; desc: string; level: 'low' | 'medium' | 'high'; icon: any }[]
}

const loading = ref(false)
const filters = reactive({ keyword: '', type: '', status: '', public: '' })
const auditList = ref<AuditItem[]>([])
const currentItem = ref<AuditItem | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(12894)

const stats = [
  { label: '简历总数', value: '128,945', growth: '12.4%', growthIcon: ArrowUp, growthClass: 'text-secondary', icon: Document, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '待审核', value: '2,346', growth: '8.7%', growthIcon: ArrowUp, growthClass: 'text-secondary', icon: WarningFilled, iconBg: 'bg-amber-100', iconColor: 'text-amber-600' },
  { label: '已公开', value: '96,182', growth: '10.6%', growthIcon: ArrowUp, growthClass: 'text-secondary', icon: CircleCheckFilled, iconBg: 'bg-emerald-100', iconColor: 'text-emerald-600' },
  { label: '风险预警', value: '1,275', growth: '5.2%', growthIcon: ArrowUp, growthClass: 'text-secondary', icon: WarningFilled, iconBg: 'bg-rose-100', iconColor: 'text-rose-600' },
  { label: '今日删除', value: '382', growth: '3.6%', growthIcon: ArrowDown, growthClass: 'text-error', icon: Delete, iconBg: 'bg-surface-container-high', iconColor: 'text-on-surface-variant' }
]

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
function issueBorderClass(level: string) {
  return level === 'high' ? 'border-error/20 bg-error/5' : level === 'medium' ? 'border-amber-200 bg-amber-50' : 'border-emerald-200 bg-emerald-50'
}
function issueColorClass(level: string) {
  return level === 'high' ? 'text-error' : level === 'medium' ? 'text-amber-600' : 'text-emerald-600'
}

function selectItem(row: AuditItem) {
  currentItem.value = row
}

function auditItem(row: AuditItem) {
  currentItem.value = row
}

function approve(row: AuditItem) {
  ElMessage.success(`已通过 ${row.title}`)
  loadList()
}

function reject(row: AuditItem) {
  ElMessage.warning(`已驳回 ${row.title}`)
  loadList()
}

function markWarning(row: AuditItem) {
  ElMessage.warning(`已标记风险 ${row.title}`)
}

function preview(row: AuditItem) {
  ElMessage.info(`预览 ${row.title}`)
}

function resetFilters() {
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  filters.public = ''
  loadList()
}

function loadList() {
  loading.value = true
  setTimeout(() => {
    auditList.value = [
      { id: 'A1001', title: '产品经理求职简历', userName: '王小明', userId: '188****6621', avatarUrl: '', vip: '普通会员', jobType: '产品 / 经理', updatedAt: '2025-05-15 10:28', status: 'pending', statusLabel: '待审核', risk: 'low', riskLabel: '低风险', thumbnail: '', issues: [{ title: '个人信息泄露风险', desc: '简历中包含了完整身份证号，建议脱敏处理。', level: 'medium', icon: InfoFilled }] },
      { id: 'A1002', title: '资深前端工程师简历', userName: '李雨桐', userId: '157****3310', avatarUrl: '', vip: '高级会员', jobType: '技术 / 前端', updatedAt: '2025-05-15 09:56', status: 'approved', statusLabel: '已通过', risk: 'low', riskLabel: '低风险', thumbnail: '', issues: [] },
      { id: 'A1003', title: '运维工程师简历', userName: '张伟', userId: '139****8822', avatarUrl: '', vip: '普通会员', jobType: '技术 / 运维', updatedAt: '2025-05-15 09:31', status: 'rejected', statusLabel: '已驳回', risk: 'medium', riskLabel: '中风险', thumbnail: '', issues: [{ title: '敏感公司信息', desc: '包含前公司内部机密项目代号，建议删除。', level: 'medium', icon: WarningFilled }] },
      { id: 'A1004', title: 'UI设计师作品简历', userName: '陈一凡', userId: '186****7721', avatarUrl: '', vip: 'SVIP', jobType: '设计 / UI', updatedAt: '2025-05-15 09:12', status: 'warning', statusLabel: '风险预警', risk: 'high', riskLabel: '高风险', thumbnail: '', issues: [{ title: '违规联系方式', desc: '简历中放置了外部引流二维码。', level: 'high', icon: WarningFilled }] }
    ]
    currentItem.value = auditList.value[0]
    loading.value = false
  }, 500)
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.content-audit {
  padding-bottom: var(--st-margin-page);
}
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: var(--st-outline-variant); border-radius: 4px; }
</style>
