<template>
  <div class="ai-rule-management">
    <!-- Header -->
    <div class="mb-8">
      <h2 class="text-headline-md font-bold">
        AI 规则管理
      </h2>
      <p class="text-body-md text-on-surface-variant mt-2">
        管理 AI 优化提示词、评分规则、敏感词规则及版本发布，保障 AI 输出质量与合规性。
      </p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-gutter mb-8">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-container-lowest p-4 rounded-xl border border-outline-variant shadow-sm hover:shadow-md transition-shadow"
      >
        <div class="flex items-center justify-between mb-2">
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
          <span class="text-[12px] text-secondary flex items-center font-bold"><el-icon size="14"><ArrowUp /></el-icon>{{ stat.growth }}</span>
        </div>
        <p class="text-label-md text-on-surface-variant">
          {{ stat.label }}
        </p>
        <p class="text-title-md font-bold mt-1">
          {{ stat.value }}
        </p>
        <p class="text-[10px] text-outline mt-1">
          {{ stat.compare }}
        </p>
      </div>
    </div>

    <div class="flex gap-gutter items-start">
      <!-- Left: Rule List -->
      <div class="flex-1 bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm overflow-hidden flex flex-col">
        <div class="p-4 border-b border-outline-variant flex flex-col sm:flex-row justify-between items-center gap-4 bg-surface-container-low/30">
          <h3 class="text-title-md font-bold">
            规则集列表
          </h3>
          <div class="flex gap-2">
            <el-input
              v-model="filterText"
              placeholder="搜索规则名称"
              class="w-64"
              clearable
            >
              <template #prefix>
                <el-icon size="18">
                  <Search />
                </el-icon>
              </template>
            </el-input>
            <el-select
              v-model="filterType"
              placeholder="类型"
              clearable
              class="w-28"
            >
              <el-option
                label="全部"
                value=""
              />
              <el-option
                label="评分规则"
                value="score"
              />
              <el-option
                label="提示词"
                value="prompt"
              />
              <el-option
                label="安全规则"
                value="security"
              />
              <el-option
                label="词库"
                value="dict"
              />
              <el-option
                label="匹配策略"
                value="match"
              />
              <el-option
                label="风险规则"
                value="risk"
              />
            </el-select>
          </div>
        </div>
        <el-table
          v-loading="loading"
          :data="filteredRules"
          highlight-current-row
          @row-click="selectRule"
        >
          <el-table-column
            label="规则名称"
            min-width="180"
          >
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-icon
                  :class="row.color"
                  size="20"
                >
                  <component :is="row.icon" />
                </el-icon>
                <span
                  class="font-bold"
                  :class="row.id === currentRule?.id ? 'text-primary' : ''"
                >{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            label="类型"
            prop="type"
            width="120"
          />
          <el-table-column
            label="当前版本"
            prop="version"
            width="110"
          />
          <el-table-column
            label="状态"
            width="110"
          >
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'active' ? 'success' : 'warning'"
                size="small"
              >
                {{ row.statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="最近更新"
            prop="updatedAt"
            width="160"
          />
          <el-table-column
            label="命中次数"
            prop="hits"
            width="110"
            align="right"
          />
          <el-table-column
            label="负责人"
            prop="owner"
            width="110"
          />
          <el-table-column
            label="操作"
            width="120"
            align="right"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click.stop="selectRule(row)"
              >
                编辑
              </el-button>
              <el-dropdown>
                <el-icon
                  class="text-on-surface-variant hover:text-primary cursor-pointer"
                  size="18"
                >
                  <More />
                </el-icon>
                <template #dropdown>
                  <el-dropdown-item @click="publishRule(row)">
                    发布新版本
                  </el-dropdown-item>
                  <el-dropdown-item @click="toggleRule(row)">
                    {{ row.status === 'active' ? '停用' : '启用' }}
                  </el-dropdown-item>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <div class="px-6 py-4 flex items-center justify-between bg-surface-container-low border-t border-outline-variant">
          <span class="text-label-md text-on-surface-variant">共 {{ filteredRules.length }} 条</span>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="filteredRules.length"
            :page-sizes="[10,20,50]"
            layout="sizes, prev, pager, next"
            background
            small
          />
        </div>
      </div>

      <!-- Right: Inspector -->
      <div class="w-[420px] shrink-0 bg-surface-container-lowest border border-outline-variant rounded-xl flex flex-col overflow-hidden">
        <div
          v-if="currentRule"
          class="flex flex-col h-full"
        >
          <div class="p-6 border-b border-outline-variant">
            <div class="flex justify-between items-start mb-4">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white">
                  <el-icon size="20">
                    <component :is="currentRule.icon" />
                  </el-icon>
                </div>
                <div>
                  <h3 class="text-title-md font-bold">
                    {{ currentRule.name }}
                  </h3>
                  <p class="text-[12px] text-on-surface-variant">
                    {{ currentRule.description }}
                  </p>
                </div>
                <el-tag
                  :type="currentRule.status === 'active' ? 'success' : 'warning'"
                  size="small"
                >
                  {{ currentRule.statusLabel }}
                </el-tag>
              </div>
              <div class="text-right">
                <p class="text-[10px] text-on-surface-variant">
                  当前版本
                </p>
                <el-dropdown>
                  <span class="text-primary text-[12px] font-mono font-bold cursor-pointer">{{ currentRule.version }}<el-icon
                    class="ml-1"
                    size="14"
                  ><ArrowDown /></el-icon></span>
                  <template #dropdown>
                    <el-dropdown-item
                      v-for="v in currentRule.versions"
                      :key="v"
                    >
                      {{ v }}
                    </el-dropdown-item>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <el-tabs v-model="activeTab">
              <el-tab-pane
                label="规则说明"
                name="desc"
              />
              <el-tab-pane
                label="Prompt 内容"
                name="prompt"
              />
              <el-tab-pane
                label="生效范围"
                name="scope"
              />
              <el-tab-pane
                label="版本记录"
                name="history"
              />
            </el-tabs>
          </div>
          <div class="flex-1 overflow-y-auto p-6 custom-scrollbar space-y-6">
            <div
              v-if="activeTab === 'prompt'"
              class="space-y-4"
            >
              <div class="space-y-2">
                <div class="flex justify-between items-center">
                  <label class="text-label-md font-bold">系统提示词 (System Prompt)</label>
                  <el-button
                    link
                    type="primary"
                    size="small"
                  >
                    <el-icon size="14">
                      <DocumentCopy />
                    </el-icon>复制
                  </el-button>
                </div>
                <div class="p-4 bg-surface-container-low rounded-lg border border-outline-variant font-mono text-[12px] text-on-surface-variant leading-relaxed whitespace-pre-wrap">
                  {{ currentRule.systemPrompt }}
                </div>
              </div>
              <div class="space-y-2">
                <div class="flex justify-between items-center">
                  <label class="text-label-md font-bold">用户提示词模板 (User Prompt Template)</label>
                  <el-button
                    link
                    type="primary"
                    size="small"
                  >
                    <el-icon size="14">
                      <DocumentCopy />
                    </el-icon>复制
                  </el-button>
                </div>
                <div class="p-4 bg-surface-container-low rounded-lg border border-outline-variant font-mono text-[12px] text-on-surface-variant leading-relaxed whitespace-pre-wrap">
                  {{ currentRule.userPrompt }}
                </div>
              </div>
              <div class="space-y-2">
                <div class="flex justify-between items-center">
                  <label class="text-label-md font-bold">参数配置</label>
                  <el-button
                    link
                    type="primary"
                    size="small"
                  >
                    <el-icon size="14">
                      <Edit />
                    </el-icon>编辑
                  </el-button>
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <div
                    v-for="(val, key) in currentRule.params"
                    :key="key"
                    class="flex items-center justify-between p-2 bg-surface rounded border border-outline-variant/30"
                  >
                    <span class="text-[11px] text-on-surface-variant">{{ key }}</span>
                    <span class="text-[12px] font-mono font-bold">{{ val }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div
              v-else-if="activeTab === 'desc'"
              class="space-y-4"
            >
              <p class="text-body-md text-on-surface-variant">
                {{ currentRule.description }}
              </p>
              <div class="grid grid-cols-2 gap-3">
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    规则类型
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ currentRule.type }}
                  </p>
                </div>
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    负责人
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ currentRule.owner }}
                  </p>
                </div>
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    创建时间
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ currentRule.createdAt }}
                  </p>
                </div>
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    命中次数
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ currentRule.hits }}
                  </p>
                </div>
              </div>
            </div>
            <div
              v-else-if="activeTab === 'scope'"
              class="space-y-3"
            >
              <div
                v-for="scope in currentRule.scopes"
                :key="scope"
                class="flex items-center gap-3 p-3 bg-surface-container-low rounded-lg border border-outline-variant"
              >
                <el-icon
                  class="text-primary"
                  size="18"
                >
                  <CircleCheckFilled />
                </el-icon>
                <span class="text-body-md">{{ scope }}</span>
              </div>
            </div>
            <div
              v-else-if="activeTab === 'history'"
              class="space-y-4"
            >
              <div
                v-for="(v, idx) in currentRule.versionHistory"
                :key="idx"
                class="relative pl-6 border-l border-outline-variant"
              >
                <div class="absolute left-[-5px] top-1 w-2.5 h-2.5 rounded-full bg-primary" />
                <div class="pb-4">
                  <div class="flex justify-between items-center">
                    <span class="text-body-md font-bold">{{ v.version }}</span>
                    <span class="text-label-md text-outline">{{ v.time }}</span>
                  </div>
                  <p class="text-body-md text-on-surface-variant mt-1">
                    {{ v.note }}
                  </p>
                  <p class="text-[12px] text-outline mt-1">
                    操作人：{{ v.operator }}
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-outline-variant grid grid-cols-2 gap-3">
            <el-button>保存草稿</el-button>
            <el-button type="primary">
              发布新版本
            </el-button>
            <el-button>回滚版本</el-button>
            <el-button type="success">
              <el-icon size="14">
                <MagicStick />
              </el-icon>测试运行
            </el-button>
          </div>
        </div>
        <div
          v-else
          class="flex-1 flex items-center justify-center text-on-surface-variant"
        >
          请选择一条规则查看详情
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, Search, ArrowUp, More, ArrowDown,
  DocumentCopy, Edit, CircleCheckFilled, MagicStick,
  WarningFilled, Lock, Menu,
  TrendCharts, Refresh, HelpFilled, Check
} from '@element-plus/icons-vue'

interface RuleItem {
  id: string
  name: string
  type: string
  version: string
  versions: string[]
  status: 'active' | 'pending'
  statusLabel: string
  updatedAt: string
  hits: number
  owner: string
  icon: any
  color: string
  description: string
  systemPrompt: string
  userPrompt: string
  params: Record<string, number>
  scopes: string[]
  createdAt: string
  versionHistory: { version: string; time: string; note: string; operator: string }[]
}

const loading = ref(false)
const filterText = ref('')
const filterType = ref('')
const page = ref(1)
const pageSize = ref(10)
const activeTab = ref('prompt')
const currentRule = ref<RuleItem | null>(null)

const stats = [
  { label: '生效规则集', value: '28', growth: '12.0%', compare: '较昨日', icon: Document, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '待发布更新', value: '6', growth: '2', compare: '较昨日', icon: Refresh, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '今日 AI 调用', value: '128,945', growth: '18.7%', compare: '较昨日', icon: TrendCharts, iconBg: 'bg-blue-500/10', iconColor: 'text-blue-600' },
  { label: '敏感词命中率', value: '0.68%', growth: '0.05pp', compare: '较昨日', icon: Lock, iconBg: 'bg-orange-500/10', iconColor: 'text-orange-600' },
  { label: '最新发布版本', value: 'v2.8.3', growth: '', compare: '2025-05-14 18:20', icon: Check, iconBg: 'bg-on-surface/5', iconColor: 'text-on-surface' }
]

const rules = ref<RuleItem[]>([])

const filteredRules = computed(() => {
  return rules.value.filter(r => {
    const matchText = !filterText.value || r.name.includes(filterText.value) || r.type.includes(filterText.value)
    const matchType = !filterType.value || r.type.includes(filterType.value === 'score' ? '评分' : filterType.value === 'prompt' ? '提示词' : filterType.value === 'security' ? '安全' : filterType.value === 'dict' ? '词库' : filterType.value === 'match' ? '匹配' : '风险')
    return matchText && matchType
  })
})

function selectRule(row: RuleItem) {
  currentRule.value = row
  activeTab.value = 'prompt'
}

function publishRule(row: RuleItem) {
  ElMessage.success(`已发布 ${row.name} 新版本`)
}

function toggleRule(row: RuleItem) {
  const action = row.status === 'active' ? '停用' : '启用'
  ElMessage.success(`${action} ${row.name}`)
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    rules.value = [
      {
        id: '1', name: '简历评分规则', type: '评分规则', version: 'v3.2.1', versions: ['v3.2.1', 'v3.2.0', 'v3.1.0'], status: 'active', statusLabel: '生效中', updatedAt: '2025-05-15 09:42', hits: 28732, owner: '张三',
        icon: Menu, color: 'text-primary', description: '用于对简历内容进行综合评分，输出维度得分与优化建议。',
        systemPrompt: '你是一名资深的 HR 与招聘顾问，请根据给定的职位要求和候选人简历内容，进行专业、客观的简历评分与分析。\n\n请从以下维度进行评分：匹配度、技能、经验、教育、项目经历、稳定性、亮点。\n每个维度 0-100 分，并给出简要原因与优化建议。\n最后给出综合得分 (0-100)。\n输出格式为 JSON。',
        userPrompt: '职位信息：{{job_description}}\n候选人简历：{{resume_text}}\n评分维度：{{dimensions}}\n输出语言：{{language}}\n\n请严格按JSON格式输出结果。',
        params: { temperature: 0.2, top_p: 0.85, max_tokens: 1500, presence_penalty: 0.1 },
        scopes: ['简历编辑页', 'AI 点评', '批量评分任务'], createdAt: '2024-01-10 09:30',
        versionHistory: [
          { version: 'v3.2.1', time: '2025-05-15 09:42', note: '优化项目经历量化维度权重', operator: '张三' },
          { version: 'v3.2.0', time: '2025-05-10 16:20', note: '增加稳定性评分', operator: '李四' }
        ]
      },
      {
        id: '2', name: 'AI 改写提示词', type: '提示词', version: 'v2.7.4', versions: ['v2.7.4', 'v2.7.3'], status: 'active', statusLabel: '生效中', updatedAt: '2025-05-14 16:30', hits: 46221, owner: '李四',
        icon: MagicStick, color: 'text-on-surface-variant', description: '基于用户输入的经历描述，生成更专业、更有数据支撑的简历表达。',
        systemPrompt: '你是一名专业的简历优化师，擅长把普通经历描述改写为数据驱动、成果导向的简历语言。',
        userPrompt: '经历描述：{{input}}\n目标岗位：{{job}}\n请输出 3 条优化后的描述。',
        params: { temperature: 0.7, top_p: 0.9, max_tokens: 800, presence_penalty: 0 },
        scopes: ['简历编辑器', 'AI 优化弹窗'], createdAt: '2024-01-12 14:20',
        versionHistory: [{ version: 'v2.7.4', time: '2025-05-14 16:30', note: '增加技术岗关键词优化', operator: '李四' }]
      },
      {
        id: '3', name: '敏感词拦截', type: '安全规则', version: 'v4.1.0', versions: ['v4.1.0', 'v4.0.0'], status: 'active', statusLabel: '生效中', updatedAt: '2025-05-14 11:18', hits: 3842, owner: '王五',
        icon: Lock, color: 'text-on-surface-variant', description: '识别并拦截简历中的敏感、违规信息。',
        systemPrompt: '请识别以下简历文本中是否包含个人隐私、歧视性、政治敏感等违规内容。',
        userPrompt: '简历文本：{{resume_text}}\n请输出风险等级与原因。',
        params: { temperature: 0.1, top_p: 0.5, max_tokens: 500, presence_penalty: 0 },
        scopes: ['简历保存', '简历发布'], createdAt: '2024-01-15 10:00',
        versionHistory: [{ version: 'v4.1.0', time: '2025-05-14 11:18', note: '新增身份证号正则', operator: '王五' }]
      },
      {
        id: '4', name: '行业关键词库', type: '词库', version: 'v1.9.0', versions: ['v1.9.0'], status: 'active', statusLabel: '生效中', updatedAt: '2025-05-13 20:05', hits: 21563, owner: '赵六',
        icon: HelpFilled, color: 'text-on-surface-variant', description: '按行业分类维护的高频关键词词库。',
        systemPrompt: '', userPrompt: '', params: {}, scopes: ['关键词匹配', 'AI 建议'], createdAt: '2024-02-01 11:20',
        versionHistory: [{ version: 'v1.9.0', time: '2025-05-13 20:05', note: '新增 AI 行业词库', operator: '赵六' }]
      },
      {
        id: '5', name: '职位匹配策略', type: '匹配策略', version: 'v2.3.0', versions: ['v2.3.0'], status: 'active', statusLabel: '生效中', updatedAt: '2025-05-13 17:22', hits: 19874, owner: '孙七',
        icon: TrendCharts, color: 'text-on-surface-variant', description: '根据职位描述匹配简历中的关键词与经历。',
        systemPrompt: '请对比职位描述与简历内容，输出匹配度分数及匹配项。', userPrompt: 'JD：{{jd}}\n简历：{{resume}}', params: { temperature: 0.2, top_p: 0.8, max_tokens: 1200, presence_penalty: 0 },
        scopes: ['投递匹配', 'AI 点评'], createdAt: '2024-02-05 16:45',
        versionHistory: [{ version: 'v2.3.0', time: '2025-05-13 17:22', note: '引入技能同义词', operator: '孙七' }]
      },
      {
        id: '6', name: '风险提示规则', type: '风险规则', version: 'v1.6.2', versions: ['v1.6.2', 'v1.6.1'], status: 'pending', statusLabel: '待发布', updatedAt: '2025-05-15 08:50', hits: 1287, owner: '周八',
        icon: WarningFilled, color: 'text-on-surface-variant', description: '识别简历中的潜在风险点并给出提示。',
        systemPrompt: '请分析简历中是否存在频繁跳槽、空窗期过长等风险点。', userPrompt: '简历：{{resume}}', params: { temperature: 0.3, top_p: 0.8, max_tokens: 1000, presence_penalty: 0 },
        scopes: ['AI 点评'], createdAt: '2024-03-01 09:00',
        versionHistory: [{ version: 'v1.6.2', time: '2025-05-15 08:50', note: '增加空窗期识别', operator: '周八' }]
      }
    ]
    currentRule.value = rules.value[0]
    loading.value = false
  }, 500)
})
</script>

<style scoped lang="scss">
.ai-rule-management {
  padding-bottom: var(--st-margin-page);
}
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: var(--st-outline-variant); border-radius: 4px; }
</style>
