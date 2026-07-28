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
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                stroke="#0057c2"
                stroke-dasharray="28 100"
                stroke-dashoffset="0"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                stroke="#266d00"
                stroke-dasharray="22 100"
                stroke-dashoffset="-28"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                stroke="#7431d3"
                stroke-dasharray="18 100"
                stroke-dashoffset="-50"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                stroke="#d9892b"
                stroke-dasharray="16 100"
                stroke-dashoffset="-68"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                r="16"
                fill="transparent"
                stroke="#191c1e"
                stroke-dasharray="16 100"
                stroke-dashoffset="-84"
                stroke-width="4"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center text-center">
              <span class="text-[10px] text-on-surface-variant">总投递</span>
              <span class="text-title-md font-bold">128,945</span>
            </div>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-2 text-[12px]">
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-primary" />已投递
          </div>
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-secondary" />筛选中
          </div>
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-tertiary" />面试中
          </div>
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-orange-500" />Offer
          </div>
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-on-surface" />结束
          </div>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col gap-4">
        <h4 class="text-title-md font-bold">
          热门投递岗位 TOP5
        </h4>
        <div class="space-y-4">
          <div
            v-for="(job, idx) in topJobs"
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
    </div>

    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden">
      <div class="p-4 border-b border-outline-variant flex justify-between items-center">
        <h3 class="text-title-md font-bold">
          最近投递记录
        </h3>
        <el-button type="primary">
          导出数据
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="deliveryList"
      >
        <el-table-column
          label="用户"
          min-width="160"
        >
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <el-avatar
                :size="32"
                :src="row.avatarUrl"
              >
                <el-icon size="16">
                  <User />
                </el-icon>
              </el-avatar>
              <span>{{ row.userName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="公司"
          prop="company"
        />
        <el-table-column
          label="职位"
          prop="position"
        />
        <el-table-column
          label="匹配度"
          prop="match"
          width="100"
        >
          <template #default="{ row }">
            <span class="font-bold text-secondary">{{ row.match }}%</span>
          </template>
        </el-table-column>
        <el-table-column
          label="当前状态"
          prop="status"
          width="120"
        />
        <el-table-column
          label="投递时间"
          prop="time"
          width="170"
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
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Promotion, Filter, UserFilled, CircleCheckFilled, User } from '@element-plus/icons-vue'

const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(48231)

const stats = [
  { label: '总投递数', value: '128,945', icon: Promotion, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '筛选中', value: '32,401', icon: Filter, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '面试中', value: '8,642', icon: UserFilled, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' },
  { label: 'Offer 数', value: '2,156', icon: CircleCheckFilled, iconBg: 'bg-on-tertiary-fixed-variant/10', iconColor: 'text-on-tertiary-fixed-variant' }
]

const topJobs = [
  { name: '后端开发工程师', count: 18234, percent: 100 },
  { name: '前端开发工程师', count: 15234, percent: 84 },
  { name: '产品经理', count: 12456, percent: 68 },
  { name: '算法工程师', count: 9876, percent: 54 },
  { name: 'UI 设计师', count: 7654, percent: 42 }
]

interface DeliveryItem {
  userName: string
  avatarUrl: string
  company: string
  position: string
  match: number
  status: string
  time: string
}

const deliveryList = ref<DeliveryItem[]>([])

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    deliveryList.value = [
      { userName: '张一航', avatarUrl: '', company: '腾讯', position: '后端开发工程师', match: 89, status: '一面', time: '2025-05-15 10:28' },
      { userName: '李雨桐', avatarUrl: '', company: '字节跳动', position: '后端开发工程师', match: 86, status: '笔试', time: '2025-05-15 09:56' },
      { userName: '张伟', avatarUrl: '', company: '华为', position: '云计算开发工程师', match: 83, status: 'HR 面', time: '2025-05-14 16:54' },
      { userName: '陈一凡', avatarUrl: '', company: '小米', position: 'Java 开发工程师', match: 80, status: '已投递', time: '2025-05-14 09:12' }
    ]
    loading.value = false
  }, 500)
})
</script>

<style scoped lang="scss">
.admin-delivery-data {
  padding-bottom: var(--st-margin-page);
}
</style>
