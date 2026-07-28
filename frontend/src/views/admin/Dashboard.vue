<template>
  <div class="admin-dashboard">
    <!-- Quick Stats Row -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-gutter">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-container-lowest p-5 rounded-xl border border-outline-variant flex flex-col gap-2 hover:shadow-md transition-shadow"
      >
        <div class="flex justify-between items-start">
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center"
            :class="stat.iconBg"
          >
            <el-icon :class="stat.iconColor">
              <component :is="stat.icon" />
            </el-icon>
          </div>
        </div>
        <div>
          <p class="text-label-md text-on-surface-variant">
            {{ stat.label }}
          </p>
          <h3 class="text-headline-md font-headline-md mt-1">
            {{ stat.value }}
          </h3>
        </div>
        <div class="flex items-center gap-1 mt-2 text-[12px]">
          <span class="text-secondary flex items-center font-bold">
            <el-icon size="14"><ArrowUp /></el-icon>{{ stat.growth }}
          </span>
          <span class="text-outline">较昨日</span>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-gutter">
      <!-- Trend Chart -->
      <div class="lg:col-span-2 bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col gap-6">
        <div class="flex justify-between items-center flex-wrap gap-4">
          <h4 class="text-title-md font-bold">
            数据趋势 (近7天)
          </h4>
          <div class="flex items-center gap-4 flex-wrap">
            <div class="flex items-center gap-2 text-[12px]">
              <span class="w-3 h-3 rounded-full bg-primary" />
              <span class="text-on-surface-variant">新增用户</span>
            </div>
            <div class="flex items-center gap-2 text-[12px]">
              <span class="w-3 h-3 rounded-full bg-secondary" />
              <span class="text-on-surface-variant">活跃用户</span>
            </div>
            <div class="flex items-center gap-2 text-[12px]">
              <span class="w-3 h-3 rounded-full bg-tertiary" />
              <span class="text-on-surface-variant">简历导出</span>
            </div>
            <el-select
              v-model="trendRange"
              size="small"
              class="w-24"
            >
              <el-option
                label="近7天"
                value="7"
              />
              <el-option
                label="近30天"
                value="30"
              />
            </el-select>
          </div>
        </div>
        <div class="flex-1 relative min-h-[280px] chart-grid rounded-lg border border-outline-variant/30 p-4">
          <svg
            class="w-full h-full"
            viewBox="0 0 800 240"
          >
            <path
              d="M0,180 Q100,160 200,170 T400,150 T600,165 T800,140"
              fill="none"
              stroke="#0057c2"
              stroke-linecap="round"
              stroke-width="3"
            />
            <path
              d="M0,120 Q100,110 200,130 T400,100 T600,115 T800,90"
              fill="none"
              stroke="#266d00"
              stroke-linecap="round"
              stroke-width="3"
            />
            <path
              d="M0,210 Q100,195 200,205 T400,185 T600,198 T800,175"
              fill="none"
              stroke="#7431d3"
              stroke-linecap="round"
              stroke-width="3"
            />
            <line
              stroke="#e0e3e6"
              stroke-dasharray="4"
              x1="133"
              x2="133"
              y1="0"
              y2="240"
            />
            <line
              stroke="#e0e3e6"
              stroke-dasharray="4"
              x1="266"
              x2="266"
              y1="0"
              y2="240"
            />
            <line
              stroke="#e0e3e6"
              stroke-dasharray="4"
              x1="400"
              x2="400"
              y1="0"
              y2="240"
            />
            <line
              stroke="#e0e3e6"
              stroke-dasharray="4"
              x1="533"
              x2="533"
              y1="0"
              y2="240"
            />
            <line
              stroke="#e0e3e6"
              stroke-dasharray="4"
              x1="666"
              x2="666"
              y1="0"
              y2="240"
            />
            <circle
              cx="533"
              cy="115"
              fill="#266d00"
              r="5"
              stroke="white"
              stroke-width="2"
            />
          </svg>
          <!-- Tooltip -->
          <div class="absolute top-20 left-[540px] bg-white border border-outline-variant shadow-lg rounded-lg p-3 z-10 w-36 hidden lg:block">
            <p class="text-[10px] font-bold text-on-surface-variant mb-2">
              05-13 (周二)
            </p>
            <div class="space-y-1">
              <div class="flex justify-between text-[11px]">
                <span class="text-primary">●</span><span>新增用户</span><span>1,152</span>
              </div>
              <div class="flex justify-between text-[11px]">
                <span class="text-secondary">●</span><span>活跃用户</span><span>8,431</span>
              </div>
              <div class="flex justify-between text-[11px]">
                <span class="text-tertiary">●</span><span>简历导出</span><span>4,982</span>
              </div>
            </div>
          </div>
          <div class="absolute bottom-2 left-0 right-0 flex justify-between px-4 text-[10px] text-outline">
            <span>05-09</span><span>05-10</span><span>05-11</span><span>05-12</span><span>05-13</span><span>05-14</span><span class="text-primary font-bold">05-15 (今天)</span>
          </div>
        </div>
      </div>

      <!-- Distribution Chart -->
      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col gap-6">
        <h4 class="text-title-md font-bold">
          用户来源分布
        </h4>
        <div class="flex-1 flex flex-col items-center justify-center py-4">
          <div class="relative w-48 h-48 mb-6">
            <svg
              class="w-full h-full transform -rotate-90"
              viewBox="0 0 36 36"
            >
              <circle
                cx="18"
                cy="18"
                fill="transparent"
                r="16"
                stroke="#f2f4f7"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                fill="transparent"
                r="16"
                stroke="#7431d3"
                stroke-dasharray="100 100"
                stroke-dashoffset="51.8"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                fill="transparent"
                r="16"
                stroke="#0057c2"
                stroke-dasharray="18.6 100"
                stroke-dashoffset="33.2"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                fill="transparent"
                r="16"
                stroke="#266d00"
                stroke-dasharray="12.4 100"
                stroke-dashoffset="14.6"
                stroke-width="4"
              />
              <circle
                cx="18"
                cy="18"
                fill="transparent"
                r="16"
                stroke="#ffdad6"
                stroke-dasharray="8.7 100"
                stroke-dashoffset="2.2"
                stroke-width="4"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center text-center">
              <span class="text-[10px] text-on-surface-variant">总用户</span>
              <span class="text-title-md font-bold">128,945</span>
            </div>
          </div>
          <div class="w-full space-y-2">
            <div
              v-for="source in sources"
              :key="source.label"
              class="flex items-center justify-between text-body-md"
            >
              <div class="flex items-center gap-2">
                <span
                  class="w-2 h-2 rounded-full"
                  :class="source.color"
                />
                {{ source.label }}
              </div>
              <div class="flex gap-4">
                <span class="font-medium text-right">{{ source.percent }}%</span>
                <span class="text-outline text-[12px] w-12 text-right">({{ source.count }})</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tables Row -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-gutter pb-12">
      <!-- Top Templates -->
      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col">
        <div class="flex justify-between items-center mb-6">
          <h4 class="text-title-md font-bold">
            热门模板排行
          </h4>
          <button class="text-primary text-[12px] font-bold flex items-center gap-1 hover:underline">
            更多 <el-icon size="14">
              <ArrowRight />
            </el-icon>
          </button>
        </div>
        <table class="w-full">
          <thead class="border-b border-outline-variant text-left">
            <tr class="text-label-md text-on-surface-variant">
              <th class="pb-3 font-medium">
                排名
              </th>
              <th class="pb-3 font-medium">
                模板名称
              </th>
              <th class="pb-3 font-medium text-right">
                分类
              </th>
              <th class="pb-3 font-medium text-right">
                排序
              </th>
            </tr>
          </thead>
          <tbody class="text-body-md">
            <tr
              v-for="(tpl, idx) in topTemplates"
              :key="tpl.id"
              class="border-b border-outline-variant/30 hover:bg-surface-container-low transition-colors"
            >
              <td class="py-4 text-center">
                <span
                  class="w-6 h-6 flex items-center justify-center rounded-full text-[12px] font-bold"
                  :class="rankClass(idx)"
                >
                  {{ idx + 1 }}
                </span>
              </td>
              <td class="py-4">
                {{ tpl.name }}
              </td>
              <td class="py-4 text-right">
                {{ tpl.category }}
              </td>
              <td class="py-4 text-right">
                {{ tpl.sortOrder }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pending Actions -->
      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col">
        <div class="flex justify-between items-center mb-6">
          <h4 class="text-title-md font-bold">
            待处理事项
          </h4>
          <button class="text-primary text-[12px] font-bold flex items-center gap-1 hover:underline">
            查看全部 <el-icon size="14">
              <ArrowRight />
            </el-icon>
          </button>
        </div>
        <div class="space-y-4">
          <div
            v-for="pending in pendingActions"
            :key="pending.title"
            class="flex items-center gap-4 p-4 rounded-lg bg-surface hover:shadow-sm transition-shadow"
          >
            <div
              class="w-10 h-10 rounded-lg flex items-center justify-center"
              :class="pending.iconBg"
            >
              <el-icon :class="pending.iconColor">
                <component :is="pending.icon" />
              </el-icon>
            </div>
            <div class="flex-1">
              <div class="font-bold text-body-md">
                {{ pending.title }}
              </div>
              <div class="text-[12px] text-outline">
                {{ pending.desc }}
              </div>
            </div>
            <div class="text-right">
              <div
                class="font-bold"
                :class="pending.countClass"
              >
                {{ pending.count }}
              </div>
              <button class="text-[12px] text-primary font-medium">
                去处理
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Live Feed -->
      <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex flex-col">
        <div class="flex justify-between items-center mb-6">
          <h4 class="text-title-md font-bold">
            实时动态
          </h4>
          <button class="text-primary text-[12px] font-bold flex items-center gap-1 hover:underline">
            更多 <el-icon size="14">
              <ArrowRight />
            </el-icon>
          </button>
        </div>
        <div class="space-y-6 relative before:absolute before:left-8 before:top-2 before:bottom-0 before:w-[1px] before:bg-outline-variant">
          <div
            v-for="feed in liveFeeds"
            :key="feed.time"
            class="relative flex gap-6"
          >
            <div class="w-16 text-right text-[12px] text-outline pt-1">
              {{ feed.time }}
            </div>
            <div
              class="w-6 h-6 flex items-center justify-center rounded-full z-10 text-white border-4 border-white ring-1"
              :class="feed.iconBg"
            >
              <el-icon size="12">
                <component :is="feed.icon" />
              </el-icon>
            </div>
            <div class="flex-1">
              <div class="text-body-md">
                <span
                  class="px-2 py-0.5 rounded text-[10px] mr-2"
                  :class="feed.tagClass"
                >{{ feed.tag }}</span>
                {{ feed.text }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer Anchor -->
    <footer class="w-full py-6 px-margin-page bg-on-surface text-surface-variant mt-auto border-t border-outline/20 rounded-lg">
      <div class="flex flex-col md:flex-row justify-between items-center gap-4">
        <div class="flex items-center gap-6">
          <span class="text-title-md font-bold text-white">智能简历</span>
          <nav class="hidden md:flex gap-4">
            <a
              class="text-label-md hover:text-white transition-colors"
              href="#"
            >产品功能</a>
            <a
              class="text-label-md hover:text-white transition-colors"
              href="#"
            >模板中心</a>
            <a
              class="text-label-md hover:text-white transition-colors"
              href="#"
            >AI 点评</a>
            <a
              class="text-label-md hover:text-white transition-colors"
              href="#"
            >价格套餐</a>
          </nav>
        </div>
        <div class="text-[12px] text-outline-variant text-center md:text-right">
          <p>© 2016-2024 智能简历 (北京) 科技有限公司 版权所有</p>
          <p class="mt-1">
            数据统计时间: 2025-05-15 10:30:00
          </p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User,
  UserFilled,
  Document,
  Cpu,
  Wallet,
  ArrowUp,
  ArrowRight,
  WarningFilled,
  MagicStick,
  Check
} from '@element-plus/icons-vue'
import { userApi } from '@/api/admin/users'
import { resumeApi } from '@/api/admin/resumes'
import { templateApi, type Template } from '@/api/admin/templates'

const trendRange = ref('7')

const stats = reactive([
  { label: '今日新增用户', value: '-', growth: '', icon: User, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '活跃用户', value: '-', growth: '', icon: UserFilled, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' },
  { label: '简历总数', value: '-', growth: '', icon: Document, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '模板总数', value: '-', growth: '', icon: Cpu, iconBg: 'bg-on-tertiary-fixed-variant/10', iconColor: 'text-on-tertiary-fixed-variant' },
  { label: '今日新增简历', value: '-', growth: '', icon: Wallet, iconBg: 'bg-primary/10', iconColor: 'text-primary' }
])

const sources = [
  { label: '微信小程序', percent: '48.2', count: '62,100', color: 'bg-tertiary' },
  { label: '百度自然搜索', percent: '18.6', count: '23,945', color: 'bg-primary' },
  { label: '抖音', percent: '12.4', count: '15,955', color: 'bg-secondary' },
  { label: '直接访问', percent: '8.7', count: '11,220', color: 'bg-error-container' }
]

const topTemplates = ref<Template[]>([])

function rankClass(idx: number) {
  if (idx === 0) return 'bg-yellow-400/20 text-yellow-700'
  if (idx === 1) return 'bg-slate-400/20 text-slate-700'
  if (idx === 2) return 'bg-amber-400/20 text-amber-700'
  return 'text-outline'
}

const pendingActions = [
  { title: '违规内容待审核', desc: '包含简历内容、个人信息等', count: '—', countClass: 'text-error', icon: WarningFilled, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { title: '模板待发布', desc: '已提交审核的模板', count: '—', countClass: 'text-primary', icon: Document, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { title: 'AI 提示词更新', desc: '系统提示词需要优化更新', count: '—', countClass: 'text-tertiary', icon: MagicStick, iconBg: 'bg-on-tertiary-fixed-variant/10', iconColor: 'text-on-tertiary-fixed-variant' },
  { title: '退款工单待处理', desc: '用户申请退款待处理', count: '—', countClass: 'text-error', icon: Wallet, iconBg: 'bg-error/10', iconColor: 'text-error' }
]

const liveFeeds = [
  { time: '10:29', tag: '用户注册', tagClass: 'bg-secondary/10 text-secondary', text: '用户 189****6621 通过微信小程序注册', icon: User, iconBg: 'bg-secondary' },
  { time: '10:28', tag: '简历生成', tagClass: 'bg-primary/10 text-primary', text: '用户 王** 生成了新简历《产品经理简历》', icon: Document, iconBg: 'bg-primary' },
  { time: '10:27', tag: 'AI 优化', tagClass: 'bg-tertiary/10 text-tertiary', text: '用户 李** 使用了 AI 优化功能', icon: MagicStick, iconBg: 'bg-tertiary' },
  { time: '10:26', tag: '内容审核', tagClass: 'bg-error/10 text-error', text: '管理员 张三 审核通过了 2 条简历内容', icon: Check, iconBg: 'bg-error' },
  { time: '10:25', tag: '订单支付', tagClass: 'bg-yellow-500/10 text-yellow-700', text: '用户 138****2210 购买了年度会员', icon: Wallet, iconBg: 'bg-yellow-500' }
]

onMounted(async () => {
  try {
    const [userStats, resumeStats, templateStats] = await Promise.all([
      userApi.getUserStats(),
      resumeApi.getResumeStats(),
      templateApi.getTemplateStats()
    ])
    stats[0].value = String(userStats.todayNewUsers)
    stats[1].value = String(userStats.activeUsers)
    stats[2].value = String(resumeStats.totalResumes)
    stats[3].value = String(templateStats.totalTemplates)
    stats[4].value = String(resumeStats.todayNewResumes)
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计数据失败')
  }

  try {
    const tplRes = await templateApi.getTemplates({ page: 1, size: 5, status: 'active' })
    topTemplates.value = tplRes.records
  } catch (e: any) {
    ElMessage.error(e.message || '加载热门模板失败')
  }
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--st-gutter);
}

.chart-grid {
  background-image: radial-gradient(#c1c6d7 0.5px, transparent 0.5px);
  background-size: 20px 20px;
}

footer {
  --tw-bg-opacity: 1;
  background-color: #191c1e;
}

footer a {
  color: rgba(255, 255, 255, 0.62);
  text-decoration: none;
}
</style>
