<template>
  <div class="admin-delivery-data">
    <div class="mb-8">
      <h2 class="text-headline-md font-bold">
        投递数据
      </h2>
      <p class="text-body-md text-on-surface-variant mt-2">
        统计平台用户的简历投递情况、进度分布与热门岗位
      </p>
    </div>

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

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-gutter mb-8">
      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col gap-4">
        <h4 class="text-title-md font-bold">
          投递阶段分布
        </h4>
        <div class="flex-1 flex items-center justify-center py-6">
          <div class="relative w-48 h-48">
            <svg
              class="w-full h-full transform -rotate-90"
              viewBox="0 0 36 36"
            >
              <circle
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                stroke="#f2f4f7"
                stroke-width="4"
              />
              <circle
                v-for="seg in donutSegments"
                :key="seg.label"
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                :stroke="seg.color"
                :stroke-dasharray="seg.percent + ' ' + (100 - seg.percent)"
                :stroke-dashoffset="'-' + seg.offset"
                stroke-width="4"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center text-center">
              <span class="text-[10px] text-on-surface-variant">总投递</span>
              <span class="text-title-md font-bold">{{ stats[0]?.value || 0 }}</span>
            </div>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-2 text-[12px]">
          <div
            v-for="seg in donutSegments"
            :key="seg.label"
            class="flex items-center gap-2"
          >
            <span
              class="w-2 h-2 rounded-full"
              :style="{ background: seg.color }"
            />{{ seg.label }} ({{ seg.count }})
          </div>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col gap-4">
        <h4 class="text-title-md font-bold">
          热门投递岗位 TOP5
        </h4>
        <div
          v-if="topJobs.length === 0"
          class="flex-1 flex items-center justify-center text-on-surface-variant"
        >
          暂无投递数据
        </div>
        <div
          v-for="(job, idx) in topJobs"
          v-else
          :key="job.name"
          class="flex items-center gap-4"
        >
          <span
            class="w-6 h-6 flex items-center justify-center rounded-full text-[10px] font-bold"
            :class="idx < 3 ? 'bg-primary text-white' : 'bg-surface-container text-on-surface-variant'"
          >{{ idx + 1 }}</span>
          <div class="flex-1">
            <div class="flex justify-between text-body-md font-bold mb-1">
              <span>{{ job.name }}</span>
              <span>{{ job.count }} 次</span>
            </div>
            <div class="h-2 bg-surface-container rounded-full overflow-hidden">
              <div
                class="h-full bg-primary rounded-full"
                :style="{ width: job.percent + '%' }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden">
      <div class="p-4 border-b border-outline-variant flex justify-between items-center">
        <h3 class="text-title-md font-bold">
          最近投递记录
        </h3>
        <el-button
          type="primary"
          @click="exportData"
        >
          导出数据
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="deliveryList"
      >
        <el-table-column
          label="用户ID"
          prop="userId"
          min-width="160"
        />
        <el-table-column
          label="公司"
          prop="company"
        />
        <el-table-column
          label="职位"
          prop="position"
        />
        <el-table-column
          label="渠道"
          prop="channel"
          width="100"
        >
          <template #default="{ row }">
            {{ row.channel || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          label="当前状态"
          width="120"
        >
          <template #default="{ row }">
            {{ statusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column
          label="投递时间"
          prop="applyDate"
          width="120"
        />
      </el-table>
      <div class="px-6 py-4 flex justify-between items-center bg-surface-container-low border-t border-outline-variant">
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Filter, UserFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { adminDeliveryApi, type DeliveryStats } from '@/api/admin/deliveries'
import { DELIVERY_STATUS_LABELS } from '@/api/delivery'
import { adminDownload } from '@/utils/adminDownload'

const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref([
  { label: '总投递数', value: '0', icon: Promotion, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '面试中', value: '0', icon: UserFilled, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' },
  { label: 'Offer 数', value: '0', icon: CircleCheckFilled, iconBg: 'bg-on-tertiary-fixed-variant/10', iconColor: 'text-on-tertiary-fixed-variant' },
  { label: '已结束', value: '0', icon: Filter, iconBg: 'bg-surface-container-high/10', iconColor: 'text-on-surface-variant' }
])

const topJobs = ref<DeliveryStats['topJobs']>([])
const statusCounts = ref<Record<string, number>>({})

const donutColors = ['#0057c2', '#266d00', '#7431d3', '#d9892b', '#191c1e']

const donutSegments = computed(() => {
  const groups: Array<{ label: string; statuses: string[]; color: string }> = [
    { label: '已投递', statuses: ['delivered'], color: donutColors[0] },
    { label: '筛选中', statuses: ['written'], color: donutColors[1] },
    { label: '面试中', statuses: ['interview1', 'interview2', 'hr'], color: donutColors[2] },
    { label: 'Offer', statuses: ['offer'], color: donutColors[3] },
    { label: '结束', statuses: ['rejected', 'withdrawn'], color: donutColors[4] }
  ]
  const totalCount = groups.reduce((sum, g) => sum + g.statuses.reduce((s, st) => s + (statusCounts.value[st] || 0), 0), 0)
  let offset = 0
  return groups.map(g => {
    const count = g.statuses.reduce((s, st) => s + (statusCounts.value[st] || 0), 0)
    const percent = totalCount === 0 ? 0 : Math.round(count * 1000 / totalCount) / 10
    const seg = { label: g.label, color: g.color, count, percent, offset }
    offset += percent
    return seg
  })
})

function statusLabel(status: string) {
  return DELIVERY_STATUS_LABELS[status] || status
}

function handleSizeChange() {
  page.value = 1
  loadList()
}

async function loadStats() {
  try {
    const s: DeliveryStats = await adminDeliveryApi.stats()
    statusCounts.value = s.statusCounts || {}
    topJobs.value = s.topJobs || []
    stats.value[0].value = String(s.totalDeliveries || 0)
    const interviewing = (s.statusCounts?.['interview1'] || 0) + (s.statusCounts?.['interview2'] || 0) + (s.statusCounts?.['hr'] || 0)
    stats.value[1].value = String(interviewing)
    stats.value[2].value = String(s.statusCounts?.['offer'] || 0)
    stats.value[3].value = String((s.statusCounts?.['rejected'] || 0) + (s.statusCounts?.['withdrawn'] || 0))
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计数据失败')
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await adminDeliveryApi.list({ page: page.value, size: pageSize.value })
    deliveryList.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载投递记录失败')
  } finally {
    loading.value = false
  }
}

async function exportData() {
  try {
    const fileName = await adminDownload(adminDeliveryApi.exportUrl(), '投递数据.csv')
    ElMessage.success(`已导出 ${fileName}`)
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

interface DeliveryItem {
  id: string
  userId: string
  company: string
  position: string
  channel?: string
  status: string
  applyDate?: string
}

const deliveryList = ref<DeliveryItem[]>([])

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped lang="scss">
.admin-delivery-data {
  padding-bottom: var(--st-margin-page);
}
</style>