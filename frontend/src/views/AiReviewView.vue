<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import AppShell from '@/components/atelier/AppShell.vue'
  import AtButton from '@/components/atelier/AtButton.vue'
  import { useI18n } from 'vue-i18n'
  import client from '@/api/client'

  const { t } = useI18n()

  const route = useRoute()
  const router = useRouter()
  const resumeId = computed(() => (route.params.id as string) || 'demo')

  const previewHtml = ref('')
  const grammarLoading = ref(false)
  const grammarError = ref('')
  const grammarRes = ref<any>(null)

  const optimizeRes = ref<any>(null)
  const optimizeLoading = ref(false)
  const optimizeError = ref('')
  const jobDescription = ref('')
  const showJdInput = ref(false)
  const polling = ref(false)

  const toastMsg = ref('')
  let toastTimer: any = null
  function toast(m: string) {
    toastMsg.value = m
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => (toastMsg.value = ''), 2500)
  }

  // --- derived ---
  const overallScore = computed(() => {
    if (optimizeRes.value?.matchScore != null) return optimizeRes.value.matchScore
    if (grammarRes.value) return 84
    return 84
  })
  // circular dash offset: 188 is circumference (2*PI*30)
  const dashOffset = computed(() => {
    const s = overallScore.value
    // 188 * (1 - s/100)
    return Math.round(188 * (1 - s / 100))
  })

  const dimCards = computed(() => {
    const ds: Record<string, number> = optimizeRes.value?.dimensionScores || {
      Impact: 78,
      Clarity: 92,
      Relevance: 85,
    }
    return [
      { key: 'Impact', label: 'Impact', score: ds.Impact ?? ds.impact ?? 78, icon: 'trending_up' },
      {
        key: 'Clarity',
        label: 'Clarity',
        score: ds.Clarity ?? ds.clarity ?? 92,
        icon: 'visibility',
      },
      {
        key: 'Relevance',
        label: 'Relevance',
        score: ds.Relevance ?? ds.relevance ?? 85,
        icon: 'target',
      },
    ]
  })

  const breakdown = computed(() => {
    const ds: Record<string, number> = optimizeRes.value?.dimensionScores || {}
    return [
      { label: 'Structure', value: ds.Structure ?? ds.structure ?? 100, dot: 'bg-black' },
      { label: 'Impact', value: ds.Impact ?? ds.impact ?? 75, dot: 'bg-[#FF3B1F]' },
      { label: 'Grammar', value: ds.Grammar ?? ds.grammar ?? 95, dot: 'bg-black' },
    ]
  })

  const optimizations = computed(() => {
    if (optimizeRes.value?.optimizations?.length) return optimizeRes.value.optimizations
    // mock fallback fused from both screens
    return [
      {
        sectionId: 's1',
        sectionType: 'work',
        originalSummary: 'Designed user interfaces for mobile applications.',
        optimizedContent:
          'Spearheaded a Q3 marketing campaign resulting in a 24% increase in sales revenue.',
        reasoning: 'Add metrics to this bullet point to quantify your impact.',
        tag: 'Impact',
      },
      {
        sectionId: 's2',
        sectionType: 'skill',
        originalSummary: 'Missing specific technologies',
        optimizedContent: 'Add React, TypeScript, Figma to skills where applicable',
        reasoning: "Missing 'React' keyword for this JD. Consider adding if applicable.",
        tag: 'Keywords',
      },
    ]
  })

  const grammarIssues = computed(() => {
    if (grammarRes.value?.issues?.length) return grammarRes.value.issues
    return [
      {
        severity: 'Critical',
        sectionType: 'Experience',
        field: 'content',
        explanation: 'Subject-verb agreement error in the "Experience" section.',
        originalText: '',
        suggestion: '',
      },
      {
        severity: 'Stylistic',
        sectionType: 'Experience',
        field: 'content',
        explanation:
          'Overuse of the word "managed". Consider synonyms like "directed" or "coordinated".',
        originalText: '',
        suggestion: '',
      },
    ]
  })

  const missingSkills = computed(() => optimizeRes.value?.missingSkills || [])
  const recommendations = computed(() => optimizeRes.value?.recommendations || [])

  async function loadPreview() {
    if (resumeId.value === 'demo') return
    try {
      const { data } = await client.get(`/resumes/${resumeId.value}/preview`, {
        responseType: 'text' as any,
      })
      // backend wraps? try unwrap
      const html = (data as any)?.data ?? data
      if (typeof html === 'string' && html.includes('<')) previewHtml.value = html
    } catch {}
  }

  async function loadLatestOptimize() {
    if (resumeId.value === 'demo') return
    try {
      const { data } = await client.get(`/resumes/${resumeId.value}/optimize/latest`)
      if (data?.data) optimizeRes.value = data.data
    } catch {}
  }

  async function runGrammarCheck() {
    if (resumeId.value === 'demo') {
      toast('演示简历：展示模拟诊断结果')
      grammarRes.value = {
        status: 'completed',
        model: 'mock',
        checkedAt: new Date().toISOString(),
        issues: grammarIssues.value,
      }
      return
    }
    grammarLoading.value = true
    grammarError.value = ''
    try {
      const { data } = await client.post(`/resumes/${resumeId.value}/grammar-check`)
      // R wrapper: code===200 + data is GrammarCheckResponse{issues:[{sectionType,field,severity,suggestion}]}
      if ((data as any).code !== undefined && (data as any).code !== 200) throw new Error((data as any).message || '检查失败')
      grammarRes.value = (data as any).data
      toast('语法检查完成')
    } catch (e: any) {
      grammarError.value = e?.response?.data?.message || e.message || '检查失败'
      toast(grammarError.value)
    } finally {
      grammarLoading.value = false
    }
  }

  async function runOptimize() {
    if (optimizeLoading.value || polling.value) return
    if (!jobDescription.value.trim() && resumeId.value !== 'demo') {
      showJdInput.value = true
      toast('请输入目标 JD 以获得精准优化')
      return
    }
    if (resumeId.value === 'demo') {
      optimizeLoading.value = true
      setTimeout(() => {
        optimizeRes.value = {
          taskId: 'mock',
          resumeId: 'demo',
          status: 'success',
          matchScore: 92,
          dimensionScores: { Impact: 75, Clarity: 92, Relevance: 85, Structure: 100, Grammar: 95 },
          optimizations: optimizations.value,
          missingSkills: ['React', 'TypeScript'],
          recommendations: ['量化成果', '补充关键词'],
          createdAt: new Date().toISOString(),
        }
        optimizeLoading.value = false
        toast('One-click Polish 完成（演示数据）')
      }, 900)
      return
    }
    optimizeLoading.value = true
    optimizeError.value = ''
    try {
      const { data } = await client.post(`/resumes/${resumeId.value}/optimize`, {
        jobDescription: jobDescription.value,
      })
      const taskId = data.data?.taskId
      if (!taskId) {
        optimizeRes.value = data.data
        return
      }
      polling.value = true
      let attempts = 0
      const iv = setInterval(async () => {
        attempts++
        try {
          const r = await client.get(`/resumes/${resumeId.value}/optimize/${taskId}`)
          const task = r.data.data
          if (task?.status === 'success' || task?.status === 'failed' || attempts > 30) {
            clearInterval(iv)
            polling.value = false
            optimizeLoading.value = false
            if (task?.status === 'success') {
              optimizeRes.value = task
              toast('优化完成')
            } else if (task?.status === 'failed') {
              optimizeError.value = task.errorMsg || '优化失败'
              toast(optimizeError.value)
            } else {
              optimizeRes.value = task
            }
          } else {
            optimizeRes.value = task
          }
        } catch (e: any) {
          if (attempts > 30) {
            clearInterval(iv)
            polling.value = false
            optimizeLoading.value = false
          }
        }
      }, 1500)
    } catch (e: any) {
      optimizeError.value = e?.response?.data?.message || e.message || '优化失败'
      toast(optimizeError.value)
      optimizeLoading.value = false
    }
  }

  function applySuggestion(idx: number | string) {
    const i = Number(idx)
    const src = (grammarRes.value?.issues as any[]) ?? (grammarIssues.value as any[])
    const item: any = src?.[i]
    const sug: string | undefined = item?.suggestion || item?.advice
    if (sug) {
      navigator.clipboard?.writeText(sug).catch(() => {})
      toast(`已复制建议: ${sug.slice(0, 36)}${sug.length > 36 ? '…' : ''}`)
    } else {
      toast(`已应用建议 #${i + 1}`)
    }
    // 非 demo 简历可在此接入 PUT /resumes/{id} 回写，需 version；当前仅复制避免误写
  }
  function ignoreSuggestion(idx: number | string) {
    toast(`已忽略 #${Number(idx) + 1}`)
  }

  onMounted(() => {
    loadPreview()
    loadLatestOptimize()
  })
</script>

<template>
  <AppShell>
    <template #actions>
      <div class="flex items-center gap-3">
        <span
          class="hidden md:inline-flex items-center gap-2 font-mono text-[11px] tracking-[0.06em] uppercase text-graphite"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-vermilion animate-pulse"></span>
          {{ t('ai.title') }}
        </span>
        <AtButton
          variant="ghost"
          size="sm"
          @click="router.push(resumeId !== 'demo' ? `/editor/${resumeId}` : '/workbench')"
          >{{ t('common.back') }}</AtButton
        >
        <AtButton variant="ai" size="sm" @click="runGrammarCheck" :disabled="grammarLoading as any">
          <span
            v-if="grammarLoading"
            class="w-3 h-3 border border-white/40 border-t-white rounded-full animate-spin"
          ></span>
          {{ grammarLoading ? t('common.loading') : t('ai.grammar') }}
        </AtButton>
      </div>
    </template>

    <!-- toast -->
    <div
      v-if="toastMsg"
      class="fixed top-[60px] left-1/2 -translate-x-1/2 z-50 bg-ink text-paper text-sm px-4 py-2 rounded-full shadow-xl border border-black"
    >
      {{ toastMsg }}
    </div>

    <div class="flex flex-col lg:flex-row min-h-[calc(100vh-56px)] bg-[#EAE8E3]/20 overflow-hidden">
      <!-- A4 Canvas Area (Left) -->
      <div class="flex-1 overflow-y-auto relative flex justify-center py-8 px-6 lg:px-8">
        <!-- Margin Rail Left -->
        <div
          class="w-16 absolute left-0 top-0 bottom-0 border-r border-line/30 hidden lg:flex flex-col items-center py-8"
        >
          <span
            class="font-mono text-[11px] tracking-[0.06em] text-[#777871] rotate-180"
            style="writing-mode: vertical-rl"
            >RES-{{ resumeId.slice(0, 4).toUpperCase() }}</span
          >
          <div class="w-px h-32 bg-line/50 mt-6"></div>
          <span class="mt-auto font-mono text-[10px] text-graphite">210 × 297</span>
        </div>

        <!-- A4 Paper -->
        <div
          class="w-full max-w-[794px] bg-white shadow-paper relative transition-transform duration-300 hover:scale-[1.005]"
          style="aspect-ratio: 210/297"
        >
          <!-- preview iframe or dummy -->
          <div v-if="previewHtml" class="absolute inset-0">
            <iframe :srcdoc="previewHtml" class="w-full h-full border-0" title="resume preview" />
          </div>
          <div v-else class="p-10 lg:p-12 flex flex-col gap-8 h-full pointer-events-none">
            <header class="border-b border-line pb-4">
              <h2 class="font-newsreader text-[28px] leading-none font-medium text-ink">
                Jane Doe
              </h2>
              <p class="font-mono text-[11px] tracking-[0.06em] uppercase text-graphite mt-2">
                Senior Product Designer · 8 yrs
              </p>
              <p class="text-sm text-[#464742] mt-3 leading-6">
                Senior product designer focused on design systems and user research. Led
                cross-functional teams to ship at scale.
              </p>
            </header>
            <section>
              <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-[#777871] mb-4">
                Experience
              </h3>
              <div class="mb-6">
                <div class="flex justify-between items-baseline">
                  <h4 class="font-inter text-sm font-medium text-ink">Tech Corp — Design Lead</h4>
                  <span class="font-mono text-[11px] text-graphite">2021 — Present</span>
                </div>
                <p class="text-[13px] leading-5 text-[#464742] mt-2">
                  Led design system overhaul, increasing component reusability by 40% and reducing
                  ship time by 30%.
                </p>
              </div>
              <div class="relative pl-4">
                <div class="absolute -left-1 top-0 w-0.5 h-full bg-vermilion rounded-full"></div>
                <div class="flex justify-between items-baseline">
                  <h4 class="font-inter text-sm font-medium text-ink">
                    Startup Inc — Product Designer
                  </h4>
                  <span class="font-mono text-[11px] text-graphite">2018 — 2021</span>
                </div>
                <p class="text-[13px] leading-5 text-[#464742] mt-2">
                  Designed user interfaces for mobile applications.
                </p>
                <span
                  class="inline-flex items-center gap-1 mt-2 font-mono text-[10px] tracking-widest uppercase text-vermilion"
                >
                  <span class="w-1 h-1 rounded-full bg-vermilion"></span> {{ t('ai.suggestions') }}
                </span>
              </div>
            </section>
            <section class="mt-auto">
              <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-[#777871] mb-2">
                Skills
              </h3>
              <p class="text-[13px] leading-5 text-[#464742]">
                Figma, Sketch, Prototyping, User Research, Design Systems
              </p>
              <p v-if="missingSkills.length" class="mt-2 text-xs text-vermilion">
                Missing: {{ missingSkills.join(', ') }}
              </p>
            </section>
          </div>

          <!-- floating score badge for preview context -->
          <div
            class="absolute top-4 right-4 bg-ink text-paper rounded-full px-3 py-1 flex items-center gap-2 shadow-xl"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-vermilion animate-pulse"></span>
            <span class="font-mono text-[11px] tracking-widest"
              >{{ t('ai.score') }} {{ overallScore }}</span
            >
          </div>
        </div>
      </div>

      <!-- Diagnostic Panel (Right) — fused c70e2 + 8105b9 -->
      <div
        class="w-full lg:w-[400px] xl:w-[480px] shrink-0 bg-[#fbf9f5] border-l border-line flex flex-col h-auto lg:h-[calc(100vh-56px)] lg:overflow-hidden"
      >
        <!-- Drawer Header -->
        <div
          class="h-14 border-b border-line flex justify-between items-center px-6 shrink-0 bg-paper/80 backdrop-blur"
        >
          <div class="flex items-center gap-2 text-ink">
            <span class="material-symbols-outlined text-[20px]">auto_awesome</span>
            <span class="font-newsreader text-[20px] font-semibold">{{ t('ai.title') }}</span>
            <span class="w-1.5 h-1.5 rounded-full bg-vermilion ml-1"></span>
          </div>
          <button
            class="w-8 h-8 rounded-full hover:bg-stone flex items-center justify-center text-graphite hover:text-ink transition-colors"
            @click="router.push(resumeId !== 'demo' ? `/editor/${resumeId}` : '/resumes')"
            aria-label="close"
          >
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <!-- Scrollable -->
        <div class="flex-1 overflow-y-auto">
          <!-- Diagnostic Report Header (8105b9) -->
          <div class="p-6 border-b border-line bg-white flex justify-between items-start">
            <div>
              <h2 class="font-newsreader text-[20px] font-semibold text-ink">{{ t('ai.scan') }}</h2>
              <p
                class="font-mono text-[11px] tracking-[0.06em] text-graphite mt-1 flex items-center gap-2"
              >
                <span class="w-1.5 h-1.5 bg-vermilion rounded-full"></span>
                {{ grammarRes || optimizeRes ? t('ai.subtitle') : t('ai.empty') }}
              </p>
              <p
                v-if="grammarRes?.checkedAt || optimizeRes?.createdAt"
                class="font-mono text-[10px] text-graphite mt-1"
              >
                {{
                  (grammarRes?.checkedAt || optimizeRes?.createdAt || '')
                    .slice(0, 19)
                    .replace('T', ' ')
                }}
              </p>
            </div>
            <div class="flex flex-col items-end">
              <div class="font-newsreader text-[40px] leading-none font-medium text-ink">
                {{ overallScore }}
              </div>
              <div class="font-mono text-[11px] tracking-widest uppercase text-[#777871] -mt-1">
                {{ t('ai.score') }}
              </div>
            </div>
          </div>

          <!-- Overall Score Section (c70e2) -->
          <div class="p-6 border-b border-line flex items-end justify-between bg-[#fbf9f5]">
            <div>
              <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-graphite mb-2">
                {{ t('ai.score') }}
              </h3>
              <div class="flex items-baseline gap-1">
                <span class="font-newsreader text-[72px] leading-none font-medium text-ink">{{
                  overallScore
                }}</span>
                <span class="font-mono text-[11px] text-graphite">/100</span>
              </div>
              <p class="font-mono text-[11px] text-graphite mt-1">
                {{
                  overallScore >= 85
                    ? 'Excellent — ready to deliver'
                    : overallScore >= 70
                      ? 'Good — polish for impact'
                      : 'Needs work'
                }}
              </p>
            </div>
            <div
              class="w-16 h-16 rounded-full border-2 border-line flex items-center justify-center relative bg-white shrink-0"
            >
              <svg class="absolute inset-0 w-full h-full -rotate-90" viewBox="0 0 64 64">
                <circle cx="32" cy="32" r="30" fill="none" stroke="#e3e2df" stroke-width="2" />
                <circle
                  cx="32"
                  cy="32"
                  r="30"
                  fill="none"
                  :stroke="overallScore >= 80 ? '#0f0f0e' : '#ff3b1f'"
                  stroke-width="2"
                  :stroke-dasharray="188"
                  :stroke-dashoffset="dashOffset"
                  stroke-linecap="round"
                  class="transition-all duration-700"
                />
              </svg>
              <span class="material-symbols-outlined text-ink relative">analytics</span>
            </div>
          </div>

          <!-- Action Buttons (8105b9) -->
          <div class="p-4 grid grid-cols-2 gap-2 border-b border-line bg-[#fbf9f5]">
            <button
              class="col-span-2 bg-ink text-paper rounded-full h-9 font-inter text-sm font-medium hover:bg-black transition-colors flex justify-center items-center gap-2 disabled:opacity-50"
              :disabled="optimizeLoading || polling"
              @click="runOptimize"
            >
              <span
                class="w-1.5 h-1.5 bg-vermilion rounded-full"
                :class="polling ? 'animate-ping' : ''"
              ></span>
              {{
                polling
                  ? t('common.loading')
                  : optimizeLoading
                    ? t('common.loading')
                    : t('ai.optimize')
              }}
            </button>
            <button
              class="bg-white text-ink rounded-full h-9 font-inter text-sm font-medium hover:bg-stone border border-line transition-colors"
            >
              {{ t('common.export') }}
            </button>
            <button
              class="bg-white text-ink rounded-full h-9 font-inter text-sm font-medium hover:bg-stone border border-line transition-colors"
            >
              {{ t('common.share') }}
            </button>
            <div v-if="optimizeError" class="col-span-2 text-xs text-red-600 font-mono">
              {{ optimizeError }}
            </div>
          </div>

          <!-- Dimensions (c70e2 cards) -->
          <div class="p-6 border-b border-line bg-[#fbf9f5]">
            <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-graphite mb-4">
              {{ t('ai.dimensions') }}
            </h3>
            <div class="grid grid-cols-3 gap-3">
              <div
                v-for="d in dimCards"
                :key="d.key"
                class="bg-[#f5f3f0] p-3 rounded-xl border border-line"
              >
                <div class="flex items-center justify-between mb-2">
                  <span class="font-inter text-[13px] text-ink">{{ d.label }}</span>
                  <span class="material-symbols-outlined text-[16px] text-[#3f0300]">{{
                    d.icon
                  }}</span>
                </div>
                <div class="font-newsreader text-[20px] font-semibold text-ink leading-none">
                  {{ d.score }}
                </div>
                <div class="w-full bg-line h-1 mt-2 rounded-full overflow-hidden">
                  <div
                    class="h-full transition-all duration-700"
                    :class="
                      d.score >= 85 ? 'bg-ink' : d.score >= 70 ? 'bg-[#5d5f5d]' : 'bg-vermilion'
                    "
                    :style="{ width: d.score + '%' }"
                  ></div>
                </div>
              </div>
            </div>
            <!-- Breakdown bars (8105b9) -->
            <div class="mt-6">
              <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-[#777871] mb-3">
                {{ t('ai.highlights') }}
              </h3>
              <div class="space-y-2.5">
                <div v-for="b in breakdown" :key="b.label" class="flex items-center gap-3">
                  <div class="w-2 h-2 rounded-full shrink-0" :class="b.dot"></div>
                  <span class="font-inter text-[13px] text-[#464742] w-20">{{ b.label }}</span>
                  <div class="flex-1 h-1 bg-[#e3e2df] rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all duration-700"
                      :class="b.dot"
                      :style="{ width: b.value + '%' }"
                    ></div>
                  </div>
                  <span class="font-mono text-[11px] w-10 text-right">{{ b.value }}%</span>
                </div>
              </div>
            </div>
            <!-- JD input -->
            <div class="mt-5 p-3 bg-white rounded-xl border border-line">
              <div class="flex items-center justify-between">
                <span
                  class="font-mono text-[11px] tracking-widest uppercase text-graphite flex items-center gap-1.5"
                >
                  <span class="w-1.5 h-1.5 rounded-full bg-vermilion"></span> {{ t('ai.optimize') }}
                </span>
                <button
                  class="font-mono text-[11px] text-ink underline"
                  @click="showJdInput = !showJdInput"
                >
                  {{ showJdInput ? t('common.cancel') : t('common.edit') }}
                </button>
              </div>
              <textarea
                v-if="showJdInput"
                v-model="jobDescription"
                :placeholder="t('ai.emptyDesc')"
                class="mt-3 w-full min-h-[88px] p-3 text-sm leading-5 border border-line rounded-lg focus:outline-none focus:border-ink bg-[#fbf9f5] resize-none"
              />
              <p v-else class="mt-2 text-xs leading-5 text-graphite line-clamp-2">
                {{ jobDescription || t('ai.emptyDesc') }}
              </p>
              <AtButton
                v-if="showJdInput"
                variant="ai"
                size="sm"
                class="mt-3 w-full justify-center"
                @click="runOptimize"
                >{{ t('ai.optimize') }}</AtButton
              >
            </div>
          </div>

          <!-- Highlights & Suggestions (c70e2) + Module Optimizations (8105b9) -->
          <div class="p-6 border-b border-line bg-[#fbf9f5]">
            <h3
              class="font-mono text-[11px] tracking-[0.08em] uppercase text-graphite mb-4 flex items-center gap-2"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-vermilion"></span> {{ t('ai.suggestions') }}
              <span
                v-if="optimizations.length"
                class="ml-auto bg-ink text-paper text-[10px] px-2 py-0.5 rounded-full font-mono tracking-widest"
                >{{ optimizations.length }}</span
              >
            </h3>

            <div class="space-y-3">
              <div
                v-for="(o, idx) in optimizations"
                :key="idx"
                class="p-4 bg-white border border-line rounded-xl hover:border-ink transition-colors group"
              >
                <div class="flex justify-between items-start mb-2">
                  <span
                    class="font-mono text-[11px] tracking-widest uppercase flex items-center gap-1.5 px-2 py-1 rounded bg-vermilion/10 text-vermilion"
                    :class="o.tag === 'Keywords' ? 'bg-[#f5f3f0] text-[#777871]' : ''"
                  >
                    <span class="material-symbols-outlined text-[14px]">{{
                      o.tag === 'Keywords' ? 'key' : 'trending_up'
                    }}</span>
                    {{ o.tag || 'Impact' }}
                  </span>
                  <span
                    class="material-symbols-outlined text-[#c7c7c0] group-hover:text-ink text-[18px]"
                    >chevron_right</span
                  >
                </div>
                <p class="font-inter text-[13px] font-medium text-ink leading-5">
                  “{{ o.originalSummary }}”
                </p>
                <p class="font-mono text-[11px] leading-5 text-graphite mt-1">{{ o.reasoning }}</p>

                <div
                  v-if="o.optimizedContent"
                  class="bg-[#f5f3f0] p-3 rounded-lg border border-line font-inter text-[13px] leading-5 text-graphite mt-3"
                >
                  <del class="text-[#ba1a1a] mr-1 decoration-red-300">{{ o.originalSummary }}</del
                  ><br />
                  <ins class="text-ink no-underline font-medium">{{ o.optimizedContent }}</ins>
                </div>

                <div
                  class="mt-3 pt-3 border-t border-dashed border-line flex items-center gap-2"
                  v-if="idx === 0"
                >
                  <span class="w-1.5 h-1.5 bg-vermilion rounded-full"></span>
                  <span class="font-mono text-[11px] tracking-widest uppercase text-ink">{{
                    t('ai.suggestions')
                  }}</span>
                </div>

                <div class="flex gap-2 mt-3">
                  <button
                    class="font-mono text-[11px] tracking-widest uppercase text-white bg-ink px-4 py-2 rounded-full flex items-center gap-1.5 hover:bg-black transition-colors group-hover:scale-[1.02]"
                    @click="applySuggestion(idx)"
                  >
                    <span class="material-symbols-outlined text-[14px]">done</span>
                    {{ t('common.confirm') }}
                  </button>
                  <button
                    class="font-mono text-[11px] tracking-widest uppercase text-graphite hover:text-ink px-4 py-2 rounded-full transition-colors border border-line hover:border-ink bg-white"
                    @click="ignoreSuggestion(idx)"
                  >
                    {{ t('common.cancel') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- missing skills -->
            <div
              v-if="missingSkills.length"
              class="mt-4 p-3 bg-white rounded-xl border border-line"
            >
              <h4
                class="font-mono text-[11px] tracking-widest uppercase text-graphite mb-2 flex items-center gap-1.5"
              >
                <span class="w-1 h-1 rounded-full bg-vermilion"></span> {{ t('ai.highlights') }}
              </h4>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="s in missingSkills"
                  :key="s"
                  class="px-3 py-1 bg-[#f5f3f0] border border-line rounded-full text-xs text-ink"
                  >{{ s }}</span
                >
              </div>
            </div>
            <div
              v-if="recommendations.length"
              class="mt-3 p-3 bg-vermilion/5 border border-vermilion/20 rounded-xl"
            >
              <p class="font-mono text-[11px] tracking-widest uppercase text-vermilion mb-1">
                {{ t('ai.suggestions') }}
              </p>
              <ul class="list-disc pl-4 text-xs leading-5 text-ink">
                <li v-for="r in recommendations" :key="r">{{ r }}</li>
              </ul>
            </div>
          </div>

          <!-- Grammar & Style (c70e2) -->
          <div class="p-6 bg-[#fbf9f5]">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-graphite">
                {{ t('ai.grammar') }}
              </h3>
              <span
                v-if="grammarRes?.model"
                class="font-mono text-[10px] px-2 py-1 bg-white border border-line rounded-full text-graphite"
                >{{ grammarRes.model }}</span
              >
            </div>

            <div
              v-if="grammarError"
              class="mb-3 text-xs text-red-600 bg-red-50 border border-red-200 rounded-lg p-3"
            >
              {{ grammarError }}
            </div>

            <div class="space-y-1">
              <div
                v-for="(iss, i) in grammarIssues"
                :key="i"
                class="flex items-start gap-3 p-3 hover:bg-white rounded-xl transition-colors cursor-pointer border border-transparent hover:border-line"
              >
                <span
                  class="material-symbols-outlined text-[20px] mt-0.5 shrink-0"
                  :class="iss.severity === 'Critical' ? 'text-[#ba1a1a]' : 'text-[#777871]'"
                >
                  {{ iss.severity === 'Critical' ? 'error' : 'edit' }}
                </span>
                <div class="min-w-0 flex-1">
                  <span
                    class="font-mono text-[11px] tracking-widest uppercase block mb-1"
                    :class="iss.severity === 'Critical' ? 'text-[#ba1a1a]' : 'text-[#777871]'"
                    >{{ iss.severity }}</span
                  >
                  <p class="font-inter text-[13px] leading-5 text-ink">{{ iss.explanation }}</p>
                  <p v-if="iss.originalText" class="font-mono text-[11px] text-graphite mt-1">
                    “{{ iss.originalText }}”
                  </p>
                  <p v-if="iss.suggestion" class="font-inter text-xs text-ink mt-1">
                    → {{ iss.suggestion }}
                  </p>
                  <span class="font-mono text-[10px] text-graphite"
                    >{{ iss.sectionType }}{{ iss.field ? ' · ' + iss.field : '' }}</span
                  >
                </div>
              </div>
            </div>

            <div
              v-if="!grammarRes"
              class="mt-4 p-3 bg-white rounded-xl border border-dashed border-line text-center"
            >
              <p class="font-mono text-[11px] text-graphite">{{ t('ai.emptyDesc') }}</p>
              <AtButton variant="ai" size="sm" class="mt-2" @click="runGrammarCheck">{{
                t('ai.run')
              }}</AtButton>
            </div>

            <!-- raw result for debugging -->
            <details v-if="grammarRes" class="mt-4">
              <summary
                class="font-mono text-[11px] tracking-widest uppercase text-graphite cursor-pointer"
              >
                Raw response
              </summary>
              <pre class="mt-2 bg-ink text-paper rounded-xl p-3 text-xs overflow-auto max-h-48">{{
                JSON.stringify(grammarRes, null, 2)
              }}</pre>
            </details>
            <details v-if="optimizeRes" class="mt-3">
              <summary
                class="font-mono text-[11px] tracking-widest uppercase text-graphite cursor-pointer"
              >
                Optimize raw
              </summary>
              <pre class="mt-2 bg-ink text-paper rounded-xl p-3 text-xs overflow-auto max-h-48">{{
                JSON.stringify(optimizeRes, null, 2)
              }}</pre>
            </details>
          </div>
        </div>

        <!-- Quick Actions Footer (c70e2) -->
        <div class="p-6 border-t border-line bg-[#f5f3f0] shrink-0">
          <h3 class="font-mono text-[11px] tracking-[0.08em] uppercase text-graphite mb-3">
            {{ t('ai.suggestions') }}
          </h3>
          <div class="flex flex-wrap gap-2">
            <button
              class="bg-ink text-paper font-mono text-[11px] tracking-widest uppercase px-4 py-2 rounded-full flex items-center gap-1.5 hover:bg-black transition-colors"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-vermilion"></span> SHORTEN
            </button>
            <button
              class="bg-ink text-paper font-mono text-[11px] tracking-widest uppercase px-4 py-2 rounded-full flex items-center gap-1.5 hover:bg-black transition-colors"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-vermilion"></span> EXPAND
            </button>
            <button
              class="bg-ink text-paper font-mono text-[11px] tracking-widest uppercase px-4 py-2 rounded-full flex items-center gap-1.5 hover:bg-black transition-colors"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-vermilion"></span> TRANSLATE
            </button>
            <button
              class="bg-ink text-paper font-mono text-[11px] tracking-widest uppercase px-4 py-2 rounded-full flex items-center gap-1.5 hover:bg-black transition-colors"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-vermilion"></span> POLISH
            </button>
          </div>
          <p class="font-mono text-[10px] text-graphite mt-3">
            All actions keep vermilion signal dot · Editorial 4pt grid
          </p>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
  .material-symbols-outlined {
    font-variation-settings:
      'FILL' 0,
      'wght' 300,
      'GRAD' 0,
      'opsz' 20;
  }
</style>
