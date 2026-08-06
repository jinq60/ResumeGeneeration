<template>
  <main class="max-w-[1440px] mx-auto px-margin-page py-stack-lg">
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
        @click="addDelivery"
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
          >
            <template #prefix>
              <el-icon size="18">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
        <div class="w-32">
          <el-select
            v-model="filters.position"
            placeholder="全部职位"
            clearable
          >
            <el-option
              label="全部职位"
              value=""
            />
            <el-option
              label="后端开发"
              value="backend"
            />
            <el-option
              label="前端开发"
              value="frontend"
            />
            <el-option
              label="产品经理"
              value="pm"
            />
          </el-select>
        </div>
        <div class="w-32">
          <el-select
            v-model="filters.company"
            placeholder="全部公司"
            clearable
          >
            <el-option
              label="全部公司"
              value=""
            />
            <el-option
              label="腾讯"
              value="tencent"
            />
            <el-option
              label="字节跳动"
              value="bytedance"
            />
            <el-option
              label="华为"
              value="huawei"
            />
            <el-option
              label="小米"
              value="xiaomi"
            />
          </el-select>
        </div>
        <div class="w-32">
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
          >
            <el-option
              label="全部状态"
              value=""
            />
            <el-option
              label="已投递"
              value="delivered"
            />
            <el-option
              label="笔试"
              value="written"
            />
            <el-option
              label="一面"
              value="interview1"
            />
            <el-option
              label="HR 面"
              value="hr"
            />
            <el-option
              label="Offer"
              value="offer"
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
          type="selection"
          width="48"
          align="center"
        />
        <el-table-column
          label="公司"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg border border-outline-variant flex items-center justify-center bg-white overflow-hidden p-1">
                <el-icon
                  v-if="!row.logo"
                  size="24"
                >
                  <OfficeBuilding />
                </el-icon>
                <img
                  v-else
                  :src="row.logo"
                  class="max-w-full max-h-full"
                >
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
          label="匹配度"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <span class="font-bold text-secondary">{{ row.match }}%</span>
          </template>
        </el-table-column>
        <el-table-column
          label="投递日期"
          prop="date"
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
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="下一步动作"
          min-width="220"
        >
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <span class="text-body-md font-body-md text-on-surface">{{ row.nextAction }}</span>
              <span
                class="text-label-md"
                :class="row.isUrgent ? 'text-error' : 'text-on-surface-variant'"
              >{{ row.nextTime }}</span>
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
              @click.stop="openDetail(row)"
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
              v-if="!current.logo"
              size="28"
            >
              <OfficeBuilding />
            </el-icon>
            <img
              v-else
              :src="current.logo"
              class="max-w-full"
            >
          </div>
          <div class="flex-1">
            <div class="flex justify-between items-center">
              <span class="text-title-md font-title-md text-on-surface">{{ current.company }}</span>
              <el-tag
                :type="statusTagType(current.status)"
                size="small"
                round
              >
                {{ current.statusLabel }}
              </el-tag>
            </div>
            <p class="text-body-md font-body-md text-on-surface-variant mt-1">
              {{ current.position }}
            </p>
            <p class="text-[12px] text-on-surface-variant mt-1">
              {{ current.dept }}
            </p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4 mb-stack-lg">
          <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant">
            <div class="flex justify-between items-center mb-2">
              <span class="text-body-md font-body-md">JD 匹配度</span>
              <el-icon size="14">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="text-headline-md font-headline-md text-secondary">
              {{ current.match }}%
            </div>
            <div class="w-full bg-outline-variant h-1.5 rounded-full mt-3">
              <div
                class="bg-secondary h-full rounded-full"
                :style="{ width: current.match + '%' }"
              />
            </div>
            <button class="mt-3 text-primary text-label-md font-label-md hover:underline">
              查看匹配分析
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
                v-for="(tip, i) in current.tips"
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
                {{ current.interviewTitle }}
              </div>
              <div class="text-body-md font-body-md text-on-surface-variant mt-1">
                {{ current.interviewTime }}
              </div>
              <div class="text-body-md font-body-md text-on-surface-variant">
                {{ current.interviewLocation }}
              </div>
              <div class="text-body-md font-body-md text-on-surface-variant">
                面试官：{{ current.interviewer }}
              </div>
            </div>
          </div>
          <button class="w-full mt-4 border border-outline-variant text-on-surface-variant hover:bg-surface-container-low px-4 py-2 rounded-lg font-label-md transition-colors">
            添加到日历
          </button>
        </div>

        <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant mb-stack-lg">
          <div class="text-title-md font-title-md text-on-surface mb-3">
            使用的简历/模板
          </div>
          <div class="flex items-center gap-3 p-2 bg-surface-container-lowest rounded-lg border border-outline-variant">
            <div class="w-10 h-12 bg-primary-fixed rounded flex items-center justify-center overflow-hidden">
              <el-icon size="24">
                <Document />
              </el-icon>
            </div>
            <div class="flex-1">
              <div class="text-title-md font-title-md text-on-surface">
                {{ current.resumeTitle }}
              </div>
              <div class="text-[12px] text-on-surface-variant mt-0.5">
                更新于 {{ current.resumeUpdated }}
              </div>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 mt-auto">
          <button class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md shadow-sm hover:scale-[0.98] transition-transform">
            更新进度
          </button>
          <button class="border border-outline-variant text-on-surface-variant hover:bg-surface-container-low px-4 py-2 rounded-lg font-label-md transition-colors">
            查看 JD
          </button>
        </div>
      </div>
    </el-drawer>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus, Search, MoreFilled, Close,
  OfficeBuilding, Calendar, InfoFilled, Document, ArrowDown
} from '@element-plus/icons-vue'

interface DeliveryItem {
  id: string
  company: string
  logo: string
  position: string
  dept: string
  match: number
  date: string
  status: string
  statusLabel: string
  nextAction: string
  nextTime: string
  isUrgent: boolean
  interviewTitle: string
  interviewTime: string
  interviewLocation: string
  interviewer: string
  resumeTitle: string
  resumeUpdated: string
  tips: { text: string; urgent: boolean }[]
}

const loading = ref(false)
const drawerVisible = ref(false)
const current = ref<DeliveryItem | null>(null)
const filters = reactive({ keyword: '', position: '', company: '', status: '', dateRange: [] as Date[] })
const deliveryList = ref<DeliveryItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

function statusTagType(status: string) {
  switch (status) {
    case 'delivered': return 'info'
    case 'written': return 'primary'
    case 'interview1': return 'warning'
    case 'hr': return 'success'
    case 'offer': return 'success'
    default: return 'info'
  }
}

function openDetail(row: DeliveryItem) {
  current.value = row
  drawerVisible.value = true
}

function addDelivery() {
  ElMessage.info('新建投递记录功能待接入')
}

function resetFilters() {
  filters.keyword = ''
  filters.position = ''
  filters.company = ''
  filters.status = ''
  filters.dateRange = []
  loadList()
}

function loadList() {
  loading.value = true
  // 投递管理后端接口待接入，当前展示空列表
  setTimeout(() => {
    deliveryList.value = []
    total.value = 0
    loading.value = false
  }, 300)
}

watch(filters, () => {
  page.value = 1
  loadList()
}, { deep: true })

onMounted(loadList)
</script>
