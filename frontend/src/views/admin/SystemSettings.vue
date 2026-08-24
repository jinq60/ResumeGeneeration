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
        </div>
        <p class="text-label-md text-on-surface-variant">
          {{ stat.label }}
        </p>
        <p class="text-title-md font-bold mt-1">
          {{ stat.value }}
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
              @keyup.enter="handleSearch"
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
              @change="handleSearch"
            >
              <el-option
                label="全部"
                value=""
              />
              <el-option
                v-for="(label, value) in ruleTypeLabels"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </div>
        </div>
        <el-table
          v-loading="loading"
          :data="rules"
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
                  :class="ruleIconColor(row.ruleType)"
                  size="20"
                >
                  <component :is="ruleIcon(row.ruleType)" />
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
            width="120"
          >
            <template #default="{ row }">
              {{ ruleTypeLabels[row.ruleType] || row.ruleType }}
            </template>
          </el-table-column>
          <el-table-column
            label="当前版本"
            width="110"
          >
            <template #default="{ row }">
              v{{ row.version }}
            </template>
          </el-table-column>
          <el-table-column
            label="状态"
            width="110"
          >
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'active' ? 'success' : row.status === 'disabled' ? 'info' : 'warning'"
                size="small"
              >
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="最近更新"
            width="160"
          >
            <template #default="{ row }">
              {{ formatDate(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="120"
            align="right"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click.stop="selectRule(row as RuleItem)"
              >
                编辑
              </el-button>
              <el-dropdown @command="(cmd: string) => handleRuleCommand(cmd, row as RuleItem)">
                <el-icon
                  class="text-on-surface-variant hover:text-primary cursor-pointer"
                  size="18"
                >
                  <More />
                </el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="publish">
                      发布新版本
                    </el-dropdown-item>
                    <el-dropdown-item command="toggle">
                      {{ row.status === 'active' ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      divided
                    >
                      删除
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
            :page-sizes="[10,20,50]"
            layout="sizes, prev, pager, next"
            background
            small
            @current-change="loadList"
            @size-change="handleSizeChange"
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
                    <component :is="ruleIcon(currentRule.ruleType)" />
                  </el-icon>
                </div>
                <div>
                  <h3 class="text-title-md font-bold">
                    {{ currentRule.name }}
                  </h3>
                  <p class="text-[12px] text-on-surface-variant">
                    {{ currentRule.description || '暂无说明' }}
                  </p>
                </div>
                <el-tag
                  :type="currentRule.status === 'active' ? 'success' : currentRule.status === 'disabled' ? 'info' : 'warning'"
                  size="small"
                >
                  {{ statusLabel(currentRule.status) }}
                </el-tag>
              </div>
              <div class="text-right">
                <p class="text-[10px] text-on-surface-variant">
                  当前版本
                </p>
                <el-dropdown
                  v-if="ruleVersions.length > 0"
                  @command="rollbackTo"
                >
                  <span class="text-primary text-[12px] font-mono font-bold cursor-pointer">v{{ currentRule.version }}<el-icon
                    class="ml-1"
                    size="14"
                  ><ArrowDown /></el-icon></span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="v in ruleVersions"
                        :key="v.id"
                        :command="v.version"
                        :disabled="v.status === 'active'"
                      >
                        v{{ v.version }}（{{ statusLabel(v.status) }}）
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <span
                  v-else
                  class="text-primary text-[12px] font-mono font-bold"
                >v{{ currentRule.version }}</span>
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
                    @click="copyText(currentRule.systemPrompt || '')"
                  >
                    <el-icon size="14">
                      <DocumentCopy />
                    </el-icon>复制
                  </el-button>
                </div>
                <div class="p-4 bg-surface-container-low rounded-lg border border-outline-variant font-mono text-[12px] text-on-surface-variant leading-relaxed whitespace-pre-wrap">
                  {{ currentRule.systemPrompt || '（空）' }}
                </div>
              </div>
              <div class="space-y-2">
                <div class="flex justify-between items-center">
                  <label class="text-label-md font-bold">用户提示词模板 (User Prompt Template)</label>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="copyText(currentRule.userPrompt || '')"
                  >
                    <el-icon size="14">
                      <DocumentCopy />
                    </el-icon>复制
                  </el-button>
                </div>
                <div class="p-4 bg-surface-container-low rounded-lg border border-outline-variant font-mono text-[12px] text-on-surface-variant leading-relaxed whitespace-pre-wrap">
                  {{ currentRule.userPrompt || '（空）' }}
                </div>
              </div>
              <div class="space-y-2">
                <div class="flex justify-between items-center">
                  <label class="text-label-md font-bold">参数配置</label>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="openEditParams"
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
                  <div
                    v-if="!currentRule.params || Object.keys(currentRule.params).length === 0"
                    class="col-span-2 text-[12px] text-on-surface-variant"
                  >
                    暂无参数配置
                  </div>
                </div>
              </div>
            </div>
            <div
              v-else-if="activeTab === 'desc'"
              class="space-y-4"
            >
              <p class="text-body-md text-on-surface-variant">
                {{ currentRule.description || '暂无说明' }}
              </p>
              <div class="grid grid-cols-2 gap-3">
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    规则类型
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ ruleTypeLabels[currentRule.ruleType] || currentRule.ruleType }}
                  </p>
                </div>
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    创建时间
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ formatDate(currentRule.createdAt) }}
                  </p>
                </div>
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    发布时间
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ currentRule.publishedAt ? formatDate(currentRule.publishedAt) : '—' }}
                  </p>
                </div>
                <div class="p-3 bg-surface-container-low rounded-lg border border-outline-variant">
                  <p class="text-label-md text-on-surface-variant">
                    创建人
                  </p>
                  <p class="text-body-md font-bold mt-1">
                    {{ currentRule.createdBy || '—' }}
                  </p>
                </div>
              </div>
            </div>
            <div
              v-else-if="activeTab === 'history'"
              class="space-y-4"
            >
              <div
                v-for="v in ruleVersions"
                :key="v.id"
                class="relative pl-6 border-l border-outline-variant"
              >
                <div
                  class="absolute left-[-5px] top-1 w-2.5 h-2.5 rounded-full"
                  :class="v.status === 'active' ? 'bg-primary' : 'bg-outline-variant'"
                />
                <div class="pb-4">
                  <div class="flex justify-between items-center">
                    <span class="text-body-md font-bold">v{{ v.version }}</span>
                    <span class="text-label-md text-outline">{{ formatDate(v.updatedAt) }}</span>
                  </div>
                  <p class="text-body-md text-on-surface-variant mt-1">
                    {{ statusLabel(v.status) }}
                  </p>
                  <p class="text-[12px] text-outline mt-1">
                    创建人：{{ v.createdBy || '—' }}
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-outline-variant grid grid-cols-2 gap-3">
            <el-button @click="openEditPrompt">
              保存草稿
            </el-button>
            <el-button
              type="primary"
              @click="publishRule(currentRule)"
            >
              发布新版本
            </el-button>
            <el-button
              :disabled="ruleVersions.length <= 1"
              @click="openRollbackDialog"
            >
              回滚版本
            </el-button>
            <el-button
              type="success"
              @click="openTestDialog"
            >
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

    <!-- 编辑 Prompt / 保存草稿 Dialog -->
    <el-dialog
      v-model="editPromptVisible"
      title="编辑规则内容（保存为草稿）"
      width="640px"
    >
      <el-form
        v-if="currentRule"
        :model="draftForm"
        label-position="top"
      >
        <el-form-item label="系统提示词 (System Prompt)">
          <el-input
            v-model="draftForm.systemPrompt"
            type="textarea"
            :rows="6"
            placeholder="请输入系统提示词"
          />
        </el-form-item>
        <el-form-item label="用户提示词模板 (User Prompt Template)">
          <el-input
            v-model="draftForm.userPrompt"
            type="textarea"
            :rows="5"
            placeholder="支持 {{input}} / {{resume_text}} / {{jd}} 等占位符"
          />
        </el-form-item>
        <el-form-item label="规则说明">
          <el-input
            v-model="draftForm.description"
            type="textarea"
            :rows="2"
            maxlength="512"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editPromptVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="saveDraft"
        >
          保存草稿
        </el-button>
      </template>
    </el-dialog>

    <!-- 参数编辑 Dialog -->
    <el-dialog
      v-model="editParamsVisible"
      title="参数配置"
      width="420px"
    >
      <el-form
        v-if="currentRule"
        label-width="130px"
      >
        <el-form-item label="temperature">
          <el-input-number
            v-model="paramsDraft.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            :precision="1"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="top_p">
          <el-input-number
            v-model="paramsDraft.top_p"
            :min="0"
            :max="1"
            :step="0.05"
            :precision="2"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="max_tokens">
          <el-input-number
            v-model="paramsDraft.max_tokens"
            :min="100"
            :max="8000"
            :step="100"
            class="w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editParamsVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="saveParams"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 回滚 Dialog -->
    <el-dialog
      v-model="rollbackVisible"
      title="回滚版本"
      width="420px"
    >
      <p class="text-body-md text-on-surface-variant mb-4">
        选择要恢复的历史版本，回滚后该版本将生效。
      </p>
      <el-select
        v-model="rollbackVersion"
        class="w-full"
      >
        <el-option
          v-for="v in ruleVersions"
          :key="v.id"
          :label="`v${v.version}（${statusLabel(v.status)}）`"
          :value="v.version"
          :disabled="v.status === 'active'"
        />
      </el-select>
      <template #footer>
        <el-button @click="rollbackVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="confirmRollback"
        >
          回滚
        </el-button>
      </template>
    </el-dialog>

    <!-- 测试运行 Dialog -->
    <el-dialog
      v-model="testVisible"
      title="测试运行"
      width="560px"
    >
      <el-form label-position="top">
        <el-form-item label="示例输入（可选，将替换 {{input}} / {{resume_text}} / {{jd}}）">
          <el-input
            v-model="testSample"
            type="textarea"
            :rows="4"
            placeholder="例如一段简历文本或职位描述"
          />
        </el-form-item>
        <el-form-item label="测试结果">
          <div
            v-if="testResult"
            class="w-full p-4 bg-surface-container-low rounded-lg border border-outline-variant font-mono text-[12px] whitespace-pre-wrap leading-relaxed"
          >
            <span
              v-if="testResult.model"
              class="text-on-surface-variant"
            >[模型 {{ testResult.model }}] </span>
            {{ testResult.output }}
          </div>
          <div
            v-else
            class="text-label-md text-on-surface-variant"
          >
            点击「运行测试」调用已配置的 LLM，未配置时返回占位结果。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">
          关闭
        </el-button>
        <el-button
          type="primary"
          :loading="testing"
          @click="runTest"
        >
          运行测试
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Search, More, ArrowDown,
  DocumentCopy, Edit, MagicStick,
  WarningFilled, Lock, Menu,
  TrendCharts, Refresh, HelpFilled, Check
} from '@element-plus/icons-vue'
import { aiRuleApi, RULE_TYPE_LABELS, type AiRule, type AiRuleStats } from '@/api/admin/aiRules'

type RuleItem = AiRule

const loading = ref(false)
const filterText = ref('')
const filterType = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('prompt')
const currentRule = ref<RuleItem | null>(null)
const rules = ref<RuleItem[]>([])
const ruleVersions = ref<RuleItem[]>([])

const ruleTypeLabels = RULE_TYPE_LABELS

const stats = ref([
  { label: '生效规则集', value: '-', icon: Document, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '草稿待发布', value: '-', icon: Refresh, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '已停用', value: '-', icon: Lock, iconBg: 'bg-orange-500/10', iconColor: 'text-orange-600' },
  { label: '规则总数', value: '-', icon: Menu, iconBg: 'bg-blue-500/10', iconColor: 'text-blue-600' },
  { label: '今日发布', value: '-', icon: Check, iconBg: 'bg-on-surface/5', iconColor: 'text-on-surface' }
])

const editPromptVisible = ref(false)
const draftForm = reactive({ systemPrompt: '', userPrompt: '', description: '' })

const editParamsVisible = ref(false)
const paramsDraft = reactive({ temperature: 0.2, top_p: 0.9, max_tokens: 1500 })

const rollbackVisible = ref(false)
const rollbackVersion = ref(0)

const testVisible = ref(false)
const testing = ref(false)
const testSample = ref('')
const testResult = ref<{ ok: boolean; output: string; model?: string } | null>(null)

function statusLabel(status: string) {
  switch (status) {
    case 'active': return '生效中'
    case 'disabled': return '已停用'
    case 'draft': return '草稿'
    default: return status
  }
}

function ruleIcon(ruleType: string) {
  switch (ruleType) {
    case 'score': return Menu
    case 'prompt': return MagicStick
    case 'security': return Lock
    case 'dict': return HelpFilled
    case 'match': return TrendCharts
    case 'risk': return WarningFilled
    default: return Document
  }
}

function ruleIconColor(ruleType: string) {
  switch (ruleType) {
    case 'score': return 'text-primary'
    case 'security': return 'text-orange-600'
    case 'risk': return 'text-error'
    default: return 'text-on-surface-variant'
  }
}

function formatDate(date: string | null) {
  return date ? date.replace('T', ' ').substring(0, 19) : '-'
}

async function copyText(text: string) {
  if (!text) {
    ElMessage.info('该字段为空')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await aiRuleApi.list({
      page: page.value,
      size: pageSize.value,
      keyword: filterText.value || undefined,
      ruleType: filterType.value || undefined
    })
    rules.value = res.records || []
    total.value = res.total || 0
    if (currentRule.value) {
      currentRule.value = rules.value.find(r => r.id === currentRule.value?.id) || null
      if (currentRule.value) {
        await loadVersions(currentRule.value.id)
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载规则列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const s: AiRuleStats = await aiRuleApi.stats()
    stats.value[0].value = String(s.active)
    stats.value[1].value = String(s.draft)
    stats.value[2].value = String(s.disabled)
    stats.value[3].value = String(s.total)
    stats.value[4].value = String(s.todayPublished)
  } catch {
    // 统计失败保持占位
  }
}

function handleSearch() {
  page.value = 1
  loadList()
}

function handleSizeChange() {
  page.value = 1
  loadList()
}

async function loadVersions(id: string) {
  try {
    ruleVersions.value = await aiRuleApi.versions(id) || []
  } catch {
    ruleVersions.value = []
  }
}

async function selectRule(row: RuleItem) {
  currentRule.value = row
  activeTab.value = 'prompt'
  await loadVersions(row.id)
}

async function handleRuleCommand(cmd: string, row: RuleItem) {
  if (cmd === 'publish') {
    currentRule.value = row
    await publishRule(row)
  } else if (cmd === 'toggle') {
    await toggleRule(row)
  } else if (cmd === 'delete') {
    await deleteRule(row)
  }
}

async function publishRule(row: RuleItem) {
  try {
    await ElMessageBox.confirm(
      `确定发布「${row.name}」新版本吗？发布后新版本将生效，旧版本停用保留。`,
      '发布新版本',
      { type: 'warning' }
    )
    const updated = await aiRuleApi.publish(row.id, {
      name: row.name,
      ruleType: row.ruleType,
      description: row.description,
      systemPrompt: row.systemPrompt,
      userPrompt: row.userPrompt,
      params: row.params || {}
    })
    ElMessage.success(`已发布 v${updated.version}`)
    await loadList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '发布失败')
  }
}

async function toggleRule(row: RuleItem) {
  const nextStatus = row.status === 'active' ? 'disabled' : 'active'
  try {
    await aiRuleApi.toggle(row.id, nextStatus)
    ElMessage.success(nextStatus === 'active' ? '已启用' : '已停用')
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function deleteRule(row: RuleItem) {
  try {
    await ElMessageBox.confirm(`确定删除规则「${row.name}」吗？`, '删除规则', { type: 'warning' })
    await aiRuleApi.remove(row.id)
    ElMessage.success('已删除')
    await loadList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function openEditPrompt() {
  if (!currentRule.value) return
  draftForm.systemPrompt = currentRule.value.systemPrompt || ''
  draftForm.userPrompt = currentRule.value.userPrompt || ''
  draftForm.description = currentRule.value.description || ''
  editPromptVisible.value = true
}

async function saveDraft() {
  if (!currentRule.value) return
  try {
    const updated = await aiRuleApi.saveDraft(currentRule.value.id, {
      name: currentRule.value.name,
      ruleType: currentRule.value.ruleType,
      description: draftForm.description,
      systemPrompt: draftForm.systemPrompt,
      userPrompt: draftForm.userPrompt,
      params: currentRule.value.params || {}
    })
    currentRule.value = updated
    editPromptVisible.value = false
    ElMessage.success('草稿已保存')
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

function openEditParams() {
  if (!currentRule.value) return
  const p = currentRule.value.params || {}
  paramsDraft.temperature = Number(p.temperature) || 0.2
  paramsDraft.top_p = Number(p.top_p) || 0.9
  paramsDraft.max_tokens = Number(p.max_tokens) || 1500
  editParamsVisible.value = true
}

async function saveParams() {
  if (!currentRule.value) return
  try {
    const updated = await aiRuleApi.saveDraft(currentRule.value.id, {
      name: currentRule.value.name,
      ruleType: currentRule.value.ruleType,
      description: currentRule.value.description,
      systemPrompt: currentRule.value.systemPrompt,
      userPrompt: currentRule.value.userPrompt,
      params: {
        temperature: paramsDraft.temperature,
        top_p: paramsDraft.top_p,
        max_tokens: paramsDraft.max_tokens
      }
    })
    currentRule.value = updated
    editParamsVisible.value = false
    ElMessage.success('参数已保存')
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

function openRollbackDialog() {
  if (!currentRule.value) return
  const latest = ruleVersions.value.find(v => v.status === 'active') || ruleVersions.value[0]
  rollbackVersion.value = latest?.version || 0
  rollbackVisible.value = true
}

async function rollbackTo(version: number) {
  if (!currentRule.value) return
  rollbackVersion.value = version
  try {
    await aiRuleApi.rollback(currentRule.value.id, version)
    ElMessage.success(`已回滚到 v${version}`)
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '回滚失败')
  }
}

async function confirmRollback() {
  if (!currentRule.value || !rollbackVersion.value) return
  try {
    await aiRuleApi.rollback(currentRule.value.id, rollbackVersion.value)
    rollbackVisible.value = false
    ElMessage.success(`已回滚到 v${rollbackVersion.value}`)
    await loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '回滚失败')
  }
}

function openTestDialog() {
  testSample.value = ''
  testResult.value = null
  testVisible.value = true
}

async function runTest() {
  if (!currentRule.value) return
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await aiRuleApi.testRun(currentRule.value.id, testSample.value || undefined)
  } catch (e: any) {
    testResult.value = { ok: false, output: e.message || '测试失败' }
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadStats()
  loadList()
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