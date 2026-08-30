<template>
  <div class="flex flex-col min-h-screen bg-surface-container-low">
    <header class="px-margin-page py-stack-lg pb-stack-md flex justify-between items-center">
      <div class="flex items-center gap-3">
        <button
          class="w-8 h-8 bg-surface-container-lowest border border-outline-variant rounded-lg flex items-center justify-center text-on-surface-variant hover:text-on-surface transition-colors"
          title="返回"
          @click="router.back()"
        >
          <el-icon size="16">
            <ArrowLeft />
          </el-icon>
        </button>
        <h1 class="text-headline-md font-headline-md text-on-surface">
          AI 简历点评
        </h1>
      </div>
      <span
        v-if="resumeId"
        class="text-[11px] text-on-surface-variant font-mono tabular-nums"
      >简历 ID：{{ resumeId }}</span>
    </header>

    <div class="workbench-page flex-1 px-margin-page pb-margin-page flex flex-col gap-gutter w-full">
      <!-- 输入卡 -->
      <section class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
        <h2 class="text-title-lg font-title-lg text-on-surface">
          目标岗位描述
        </h2>
        <p class="mt-1 text-sm text-on-surface-variant">
          粘贴目标 JD，AI 会据此给出针对性的修改建议。
        </p>

        <textarea
          v-model="reviewForm.jobDescription"
          class="mt-4 w-full px-4 py-4 bg-surface-container-low border border-outline-variant rounded-lg text-sm text-on-surface leading-relaxed resize-y focus:outline-none focus:bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
          rows="6"
          placeholder="请粘贴目标岗位的职位描述（JD）..."
        />

        <div class="mt-4 flex flex-wrap items-center gap-3">
          <button
            class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="analyzing || optimizing"
            @click="handleAnalyze"
          >
            <el-icon
              v-if="analyzing"
              class="animate-spin"
              size="14"
            >
              <Loading />
            </el-icon>
            <el-icon
              v-else
              size="14"
            >
              <MagicStick />
            </el-icon>
            <span>{{ analyzing ? '分析中…' : '开始点评' }}</span>
          </button>
          <button
            class="bg-secondary text-on-secondary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="analyzing || optimizing || !reviewForm.jobDescription.trim()"
            title="AI 将逐模块给出针对该 JD 的改写建议与匹配度分析"
            @click="handleOptimize()"
          >
            <el-icon
              v-if="optimizing"
              class="animate-spin"
              size="14"
            >
              <Loading />
            </el-icon>
            <el-icon
              v-else
              size="14"
            >
              <Aim />
            </el-icon>
            <span>{{ optimizing ? '优化分析中…' : 'JD 匹配深度优化' }}</span>
          </button>
        </div>

        <p
          v-if="analyzing || optimizing"
          class="mt-3 text-xs text-on-surface-variant"
        >
          AI 生成需要几秒钟到一分钟，期间你的简历内容不会丢失。
        </p>
      </section>

      <!-- 结果卡 -->
      <section
        v-if="reviewResult"
        class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm"
      >
        <header class="flex items-end justify-between pb-4 mb-5 border-b border-outline-variant">
          <h2 class="text-title-lg font-title-lg text-on-surface">
            分析结果
          </h2>
          <div class="flex items-baseline gap-1 bg-primary-fixed px-3.5 py-2 rounded-lg">
            <span class="text-[28px] font-bold text-primary leading-none tracking-tight font-mono tabular-nums">{{ reviewResult.overallScore || '—' }}</span>
            <span class="text-xs text-on-surface-variant">/ 100</span>
          </div>
        </header>

        <!-- 分项评分 -->
        <div
          v-if="reviewResult.dimensionScores"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-on-surface tracking-wide pl-2 border-l-[3px] border-primary mb-3">
            分项评分
          </h3>
          <div class="flex flex-col gap-3">
            <div
              v-for="(score, key) in (reviewResult.dimensionScores as Record<string, number>)"
              :key="key"
              class="flex flex-col gap-1.5"
            >
              <div class="flex justify-between">
                <span class="text-sm text-on-surface-variant">{{ getDimensionLabel(key) }}</span>
                <span class="text-sm font-semibold text-on-surface font-mono tabular-nums">{{ score }}</span>
              </div>
              <div class="h-1.5 rounded-full bg-surface-container overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-300"
                  :style="{ width: score + '%' }"
                  :class="getScoreColorClass(score)"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 亮点 -->
        <div
          v-if="reviewResult.highlights?.length"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-secondary tracking-wide pl-2 border-l-[3px] border-secondary mb-3">
            已做得好
          </h3>
          <ul class="list-none p-0 flex flex-col gap-2">
            <li
              v-for="(h, i) in reviewResult.highlights"
              :key="i"
              class="px-3 py-2 bg-secondary-container border-l-2 border-secondary rounded-sm text-sm text-on-surface-variant"
            >
              {{ h }}
            </li>
          </ul>
        </div>

        <!-- 修改建议（按优先级排序：最值得先改 → 可增强 → 已做得好） -->
        <div
          v-if="reviewResult.suggestions?.length"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-error tracking-wide pl-2 border-l-[3px] border-error mb-3">
            最值得先改
          </h3>
          <ul class="list-none p-0 flex flex-col gap-3">
            <li
              v-for="(s, i) in sortedSuggestions"
              :key="i"
              class="flex gap-3 p-3 bg-surface-container-low rounded-lg"
            >
              <div
                class="shrink-0 text-[10px] font-semibold px-2 py-0.5 rounded-full tracking-wide h-fit"
                :class="priorityTagClass(s)"
              >
                {{ priorityLabel(s) }}
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-[11px] text-on-surface-variant mb-0.5">
                  {{ s.sectionType || '通用建议' }}
                </div>
                <div class="text-sm font-semibold text-on-surface">
                  {{ s.title }}
                </div>
                <p
                  v-if="s.problem"
                  class="text-xs text-on-surface-variant leading-relaxed mt-1"
                >
                  {{ s.problem }}
                </p>
                <p
                  v-if="s.advice"
                  class="text-xs text-on-surface leading-relaxed mt-1"
                >
                  {{ s.advice }}
                </p>
              </div>
            </li>
          </ul>
        </div>

        <footer class="mt-6 pt-4 border-t border-outline-variant flex justify-end gap-2">
          <button
            class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
            @click="handleReapply"
          >
            重新分析
          </button>
          <button
            class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="applicableMissingSkills.length === 0"
            :title="applicableMissingSkills.length === 0 ? '缺失技能来自 JD 匹配深度优化，请先运行 JD 优化' : '将 JD 优化识别的缺失技能补充到简历技能清单'"
            @click="handleApplySuggestions"
          >
            <el-icon size="14">
              <Check />
            </el-icon>
            <span>应用建议到简历</span>
          </button>
        </footer>
      </section>

      <!-- JD 匹配深度优化结果卡 -->
      <section
        v-if="optimizeResult && optimizeResult.status !== 'failed'"
        class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm"
      >
        <header class="flex items-end justify-between pb-4 mb-5 border-b border-outline-variant">
          <div>
            <h2 class="text-title-lg font-title-lg text-on-surface">
              JD 匹配深度优化
            </h2>
            <p
              v-if="optimizePending"
              class="mt-1 text-xs text-on-surface-variant"
            >
              AI 正在逐模块分析简历与岗位的匹配度…
            </p>
          </div>
          <div
            v-if="optimizeResult.matchScore != null"
            class="flex items-baseline gap-1 bg-secondary-container px-3.5 py-2 rounded-lg"
          >
            <span class="text-[28px] font-bold text-secondary leading-none tracking-tight font-mono tabular-nums">{{ optimizeResult.matchScore }}</span>
            <span class="text-xs text-on-surface-variant">/ 100 匹配度</span>
          </div>
        </header>

        <!-- 匹配维度评分 -->
        <div
          v-if="optimizeResult.dimensionScores && Object.keys(optimizeResult.dimensionScores).length"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-on-surface tracking-wide pl-2 border-l-[3px] border-secondary mb-3">
            匹配维度
          </h3>
          <div class="flex flex-col gap-3">
            <div
              v-for="(score, key) in (optimizeResult.dimensionScores as Record<string, number>)"
              :key="key"
              class="flex flex-col gap-1.5"
            >
              <div class="flex justify-between">
                <span class="text-sm text-on-surface-variant">{{ getOptimizeDimensionLabel(String(key)) }}</span>
                <span class="text-sm font-semibold text-on-surface font-mono tabular-nums">{{ score }}</span>
              </div>
              <div class="h-1.5 rounded-full bg-surface-container overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-300 bg-secondary"
                  :style="{ width: score + '%' }"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 逐模块优化建议 -->
        <div
          v-if="optimizeResult.optimizations?.length"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-on-surface tracking-wide pl-2 border-l-[3px] border-primary mb-3">
            逐模块改写建议
          </h3>
          <ul class="list-none p-0 flex flex-col gap-3">
            <li
              v-for="(opt, i) in optimizeResult.optimizations"
              :key="i"
              class="p-3 bg-surface-container-low rounded-lg"
            >
              <div class="text-[11px] text-on-surface-variant mb-1">
                {{ sectionTypeLabel(opt.sectionType) }}
              </div>
              <p
                v-if="opt.originalSummary"
                class="text-xs text-on-surface-variant leading-relaxed line-through decoration-outline mb-2"
              >
                {{ opt.originalSummary }}
              </p>
              <p class="text-sm text-on-surface leading-relaxed whitespace-pre-wrap">
                {{ opt.optimizedContent }}
              </p>
              <p
                v-if="opt.reasoning"
                class="text-xs text-secondary mt-2"
              >
                理由：{{ opt.reasoning }}
              </p>
            </li>
          </ul>
        </div>

        <!-- 整体建议 -->
        <div
          v-if="optimizeResult.recommendations?.length"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-on-surface tracking-wide pl-2 border-l-[3px] border-primary mb-3">
            整体提升建议
          </h3>
          <ol class="list-decimal pl-5 flex flex-col gap-1.5 text-sm text-on-surface-variant leading-relaxed">
            <li
              v-for="(rec, i) in optimizeResult.recommendations"
              :key="i"
            >
              {{ rec }}
            </li>
          </ol>
        </div>

        <!-- 缺失技能 -->
        <div
          v-if="optimizeResult.missingSkills?.length"
          class="mb-5"
        >
          <h3 class="text-sm font-semibold text-on-surface tracking-wide pl-2 border-l-[3px] border-error mb-3">
            相对目标 JD 缺失的技能
          </h3>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(skill, i) in optimizeResult.missingSkills"
              :key="i"
              class="bg-error-container text-error px-2.5 py-1 text-xs rounded-full"
            >{{ skill }}</span>
          </div>
        </div>

        <footer
          v-if="!optimizePending"
          class="mt-6 pt-4 border-t border-outline-variant flex justify-end"
        >
          <button
            class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
            @click="handleOptimize()"
          >
            重新优化
          </button>
        </footer>
      </section>

      <!-- 优化失败提示 -->
      <section
        v-if="optimizeResult?.status === 'failed'"
        class="bg-error-container border border-error/30 rounded-xl p-6 shadow-sm"
      >
        <h2 class="text-title-md font-bold text-error">
          JD 优化生成失败
        </h2>
        <p class="mt-1 text-sm text-on-surface-variant">
          {{ optimizeResult.errorMsg || '请稍后重试，或先使用上方点评功能。' }}
        </p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, MagicStick, Loading, Check, Aim } from '@element-plus/icons-vue'
import { resumeApi, type ResumeOptimizeResult } from '@/api/resume'

const router = useRouter()
const route = useRoute()
const resumeId = (route.params.id || route.query.resumeId) as string
const analyzing = ref(false)
const optimizing = ref(false)

// 轮询取消令牌：离开页面或重新发起时使旧轮询失效（防止组件卸载后继续请求）
let reviewPollToken = 0
let optimizePollToken = 0

interface ReviewSuggestion {
  priority?: 'high' | 'medium' | 'low'
  sectionType?: string
  title?: string
  problem?: string
  advice?: string
}

interface ReviewResult {
  reviewId?: string
  overallScore?: number
  dimensionScores?: Record<string, number>
  highlights?: string[]
  suggestions?: ReviewSuggestion[]
}

function safeDecode(value: string): string {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

const reviewForm = ref({ jobDescription: (route.query.jd as string) ? safeDecode(route.query.jd as string) : '' })
const reviewResult = ref<ReviewResult | null>(null)
const optimizeResult = ref<ResumeOptimizeResult | null>(null)

const optimizePending = computed(() =>
  optimizeResult.value?.status === 'pending' || optimizeResult.value?.status === 'processing'
)

const optimizeDimensionLabels: Record<string, string> = {
  skills: '技能匹配',
  experience: '经历相关度',
  education: '教育背景',
  projects: '项目契合度'
}

function getOptimizeDimensionLabel(k: string) {
  return optimizeDimensionLabels[k] || dimensionLabels[k] || k
}

const sectionTypeLabels: Record<string, string> = {
  profile: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skill: '技能清单',
  introduction: '自我介绍',
  custom: '自定义模块'
}

function sectionTypeLabel(t: string) {
  return sectionTypeLabels[t] || t
}

const dimensionLabels: Record<string, string> = {
  content: '内容完整性',
  structure: '结构合理性',
  language: '语言表达',
  matching: '岗位匹配度',
  highlight: '亮点突出度',
  completeness: '完整性',
  expression: '表达能力',
  match: '匹配度'
}

function getDimensionLabel(k: string) {
  return dimensionLabels[k] || k
}

function getScoreColorClass(score: number) {
  if (score >= 80) return 'bg-secondary'
  if (score >= 60) return 'bg-[#D9892B]'
  return 'bg-error'
}

function priorityOf(s: ReviewSuggestion): 'high' | 'medium' | 'low' {
  return s.priority || 'medium'
}

function priorityLabel(s: ReviewSuggestion) {
  const p = priorityOf(s)
  return p === 'high' ? '优先改' : p === 'low' ? '可增强' : '中优先'
}

function priorityTagClass(s: ReviewSuggestion) {
  const p = priorityOf(s)
  if (p === 'high') return 'bg-error-container text-error'
  if (p === 'low') return 'bg-secondary-container text-secondary'
  return 'bg-primary-fixed text-primary'
}

const sortedSuggestions = computed(() => {
  if (!reviewResult.value?.suggestions) return []
  const order: Record<string, number> = { high: 0, medium: 1, low: 2 }
  return [...reviewResult.value.suggestions].sort((a, b) =>
    (order[priorityOf(a)] ?? 9) - (order[priorityOf(b)] ?? 9))
})

async function handleAnalyze() {
  if (!reviewForm.value.jobDescription.trim()) {
    ElMessage.warning('请输入目标岗位描述')
    return
  }
  analyzing.value = true
  reviewResult.value = null
  const token = ++reviewPollToken
  try {
    // 点评为异步任务，响应中携带本次任务的 reviewId（历史最近一次成功点评不会混淆）
    const created = await resumeApi.review(resumeId, { jobDescription: reviewForm.value.jobDescription })
    const myReviewId: string | undefined = created?.reviewId
    // 轮询最新结果（最长 60 秒，页面离开/重新发起即取消）。
    // latest 接口只返回 status=success 的点评，因此 reviewId 与本次一致即代表本次任务完成，
    // 同时规避了 overallScore 为 0 时真值判断导致的永久轮询问题。
    for (let i = 0; i < 60; i++) {
      if (token !== reviewPollToken) return
      const latest = await resumeApi.getLatestReview(resumeId)
      if (latest?.reviewId && myReviewId && latest.reviewId === myReviewId) {
        reviewResult.value = latest as ReviewResult
        break
      }
      await new Promise((r) => setTimeout(r, 1000))
    }
    if (token !== reviewPollToken) return
    if (reviewResult.value) {
      ElMessage.success('分析完成')
    } else {
      ElMessage.warning('分析仍在进行中，请稍后在点评中心查看')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '暂时无法生成点评；你的简历内容不会丢失。')
  } finally {
    if (token === reviewPollToken) {
      analyzing.value = false
    }
  }
}

/**
 * 发起 JD 匹配深度优化任务并轮询结果。
 * 后端限制每用户最多 3 个进行中任务（6004），失败时给出明确提示。
 */
async function handleOptimize() {
  if (!reviewForm.value.jobDescription.trim()) {
    ElMessage.warning('请输入目标岗位描述')
    return
  }
  optimizing.value = true
  optimizeResult.value = null
  const token = ++optimizePollToken
  try {
    const created = await resumeApi.createOptimizeTask(resumeId, {
      jobDescription: reviewForm.value.jobDescription.trim()
    })
    // 轮询任务状态（最长 120 秒：LLM 逐模块改写耗时较长）
    for (let i = 0; i < 120; i++) {
      if (token !== optimizePollToken) return
      const task = await resumeApi.getOptimizeTask(resumeId, created.taskId)
      optimizeResult.value = task
      if (task.status === 'success' || task.status === 'failed') break
      await new Promise((r) => setTimeout(r, 1000))
    }
    if (token !== optimizePollToken) return
    if (optimizeResult.value?.status === 'success') {
      ElMessage.success('优化分析完成')
    } else if (optimizeResult.value?.status !== 'failed') {
      ElMessage.warning('优化生成较慢，可稍后点击重新优化查看')
    }
  } catch (e: any) {
    optimizeResult.value = null
    // 业务码 6004 = AI_CONCURRENT_LIMIT_EXCEEDED（api-spec §13 + ResultCode.java）
    // 直接读 e.code，避免「message 文本里含 '6004'」之类的脆弱匹配。
    if (e?.code === 6004) {
      ElMessage.warning('你已有多个优化任务在进行中，请稍后再试')
    } else {
      ElMessage.error(e?.message || '暂时无法生成优化分析；你的简历内容不会丢失。')
    }
  } finally {
    if (token === optimizePollToken) {
      optimizing.value = false
    }
  }
}

onMounted(async () => {
  // 进入页面时回显最近一次优化结果（无则静默忽略）
  try {
    const latest = await resumeApi.getLatestOptimize(resumeId)
    if (latest && latest.status !== 'pending' && latest.status !== 'processing') {
      optimizeResult.value = latest
    }
  } catch {
    // 静默：历史结果加载失败不影响主流程
  }
})

onBeforeUnmount(() => {
  ++reviewPollToken
  ++optimizePollToken
})

// 点评响应 DTO 没有 missingSkills 字段；可应用的缺失技能只来自 JD 匹配深度优化结果。
const applicableMissingSkills = computed(() =>
  (optimizeResult.value?.missingSkills || []).filter((s) => s && s.trim())
)

function handleApplySuggestions() {
  const skills = applicableMissingSkills.value
  if (skills.length === 0) {
    ElMessage.info('请先运行「JD 匹配深度优化」，再应用缺失技能建议')
    return
  }
  applyMissingSkillsToResume(skills)
}

async function applyMissingSkillsToResume(skills: string[]) {
  const { resumeApi } = await import('@/api/resume')
  try {
    const resume = await resumeApi.get(resumeId)
    const sections = (resume.sections || []).map((section: any) => ({ ...section }))
    const skillSection = sections.find((s: any) => s.type === 'skill')

    const mergeInto = (items: Array<{ name: string; level?: string; highlight?: boolean }>) => {
      const existing = new Set(items.map((item) => item.name.trim().toLowerCase()))
      const additions = skills.filter((skill) => !existing.has(skill.trim().toLowerCase()))
      return [...items, ...additions.map((skill) => ({ name: skill.trim(), highlight: false }))]
    }

    if (skillSection) {
      const data: Array<{ category: string; items: Array<{ name: string; level?: string; highlight?: boolean }> }> = Array.isArray(skillSection.data) ? skillSection.data : []
      if (data.length === 0) {
        skillSection.data = [{ category: '补充技能', items: mergeInto([]) }]
      } else {
        data[0].items = mergeInto(data[0].items || [])
        skillSection.data = data
      }
    } else {
      sections.push({
        id: `skill_${Date.now()}`,
        type: 'skill',
        title: '技能清单',
        order: sections.length,
        visible: true,
        data: [{ category: '补充技能', items: mergeInto([]) }]
      })
    }

    await resumeApi.update(resumeId, { sections })
    ElMessage.success(`已自动补充 ${skills.length} 项缺失技能到技能清单`)
    ElMessageBox.confirm('技能已写入简历，是否立即前往编辑器查看？', '应用成功', {
      confirmButtonText: '去编辑器',
      cancelButtonText: '稍后再说',
      type: 'success'
    }).then(() => {
      router.push(`/workbench/editor/${resumeId}`)
    }).catch(() => {})
  } catch (e: any) {
    ElMessage.error(e.message || '应用建议失败，请稍后重试')
  }
}

function handleReapply() {
  reviewResult.value = null
  handleAnalyze()
}
</script>
