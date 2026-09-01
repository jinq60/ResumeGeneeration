<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import client from '@/api/client'
  import type { TemplateDTO } from '@/api/types'

  const { t } = useI18n()
  const router = useRouter()
  const list = ref<TemplateDTO[]>([])
  const loading = ref(true)
  const activeCategory = ref('all')
  const selectedId = ref<string>('template_classic_single')

  const categories = computed(
    () =>
      [
        { key: 'all', label: t('templates.all') },
        { key: 'classic', label: t('templates.classic') },
        { key: 'tech', label: t('templates.tech') },
        { key: 'fresh', label: t('templates.fresh') },
        { key: 'business', label: t('templates.business') },
        { key: 'postgraduate', label: t('templates.postgraduate') },
      ] as const
  )

  const MOCK_TEMPLATES: TemplateDTO[] = [
    {
      id: 'template_classic_single',
      code: 'classic-single',
      name: '经典单栏 · The Helvetica',
      category: 'classic',
      thumbnailUrl:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuBVxyT9Aye0TSaYUBsEj42Kx1RVCG-W9gQ9-3E_JD4AkNWC0Awos-1ZCdOtorRvXSkMCp0cl288eB3-hRTcWQZ1cCd-TIDtVRTTyud3IAhV8eLXMaTkleT3L8mgqTOWLQpfDNxJ7AlUXyE5-WDiyBub5OEYmiJ25LxU0l5TqmPvyDM8MG7sCVttUWbcA_SJbeT3S4PdCYB3spNzq13U1DFK6vOIytgbA4KFo4MMwOXfmeAqUVFxs4i1',
      description: '传统单栏布局，适合大多数岗位',
      config: {},
      htmlTemplate: 'classic-single',
      renderEngine: 'server',
      sortOrder: 10,
      isRecommended: true,
      status: 'active',
      createdAt: '',
      updatedAt: '',
    },
    {
      id: 'template_classic_double',
      code: 'classic-double',
      name: '经典双栏 · The Scholar',
      category: 'classic',
      thumbnailUrl:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuCvwk_kQwAU7LnD__JfS1yQAx1Sxoi9dZwSrqfD2bqXDbkUxGhouDkRU9FF38iQDtbiHtp4Vfd8hhPrF83l8lT-eGn56_9jfmSVys4ULoLmEtPrubTd_KPBR75LK0hZNvxqZK2eVdSC0x1y_qIM8dakPwbizRdkF-PX9bxAw5aozTxv3N4S__qqAyVFlwgJN5aNM8bpu_ExzpNF_tIbz57Z3QXZATNnvi8K2HKhVzQ6fgFTJPWmQ0s9',
      description: '左侧 sidebar 展示个人信息与技能，右侧展示经历',
      config: {},
      htmlTemplate: 'classic-double',
      renderEngine: 'server',
      sortOrder: 20,
      isRecommended: false,
      status: 'active',
      createdAt: '',
      updatedAt: '',
    },
    {
      id: 'template_tech',
      code: 'tech',
      name: '技术岗 · The Director',
      category: 'tech',
      thumbnailUrl:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuCb6kfWrO1l1DsHWtoT2mLkGthKFJXbS45JfY0Ze2q3gsA1IgAhGb5Wm8oeB2N6Q4EN18AiXb4OQVq243K8K8YztQcuyI78wpVPrDiOjldN8FTHKdnKp3exE1HySS0JY29LDp_YUpViPcf9zeAkOQKZMH7GiRTECxIKJUlTuFXQ3lMYwgA0Ix_LN_CbaKnqH7IN1Y64bUziMLDe--MW7oeuNm9Th6pdxRv5DxptnlzVsuLEJXdL7TPC',
      description: '突出技术栈与项目经历，适合研发岗位',
      config: {},
      htmlTemplate: 'tech',
      renderEngine: 'server',
      sortOrder: 30,
      isRecommended: true,
      status: 'active',
      createdAt: '',
      updatedAt: '',
    },
    {
      id: 'template_fresh',
      code: 'fresh',
      name: '应届生 · Fresh Graduate',
      category: 'fresh',
      thumbnailUrl:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuCvwk_kQwAU7LnD__JfS1yQAx1Sxoi9dZwSrqfD2bqXDbkUxGhouDkRU9FF38iQDtbiHtp4Vfd8hhPrF83l8lT-eGn56_9jfmSVys4ULoLmEtPrubTd_KPBR75LK0hZNvxqZK2eVdSC0x1y_qIM8dakPwbizRdkF-PX9bxAw5aozTxv3N4S__qqAyVFlwgJN5aNM8bpu_ExzpNF_tIbz57Z3QXZATNnvi8K2HKhVzQ6fgFTJPWmQ0s9',
      description: '清新简洁，突出教育背景与校园经历',
      config: {},
      htmlTemplate: 'fresh',
      renderEngine: 'server',
      sortOrder: 40,
      isRecommended: true,
      status: 'active',
      createdAt: '',
      updatedAt: '',
    },
    {
      id: 'template_business',
      code: 'business',
      name: '商务简洁 · The Executive',
      category: 'business',
      thumbnailUrl: '',
      description: '商务稳重风格，适合社招与管理层',
      config: {},
      htmlTemplate: 'business',
      renderEngine: 'server',
      sortOrder: 50,
      isRecommended: false,
      status: 'active',
      createdAt: '',
      updatedAt: '',
    },
    {
      id: 'template_postgraduate',
      code: 'postgraduate',
      name: '考研复试 · Academic',
      category: 'postgraduate',
      thumbnailUrl: '',
      description: '学术风格，突出科研项目与教育背景',
      config: {},
      htmlTemplate: 'postgraduate',
      renderEngine: 'server',
      sortOrder: 60,
      isRecommended: false,
      status: 'active',
      createdAt: '',
      updatedAt: '',
    },
  ]

  function resolveThumb(url?: string): string {
    if (!url) return ''
    if (url.startsWith('http')) return url
    const base = (import.meta as any).env?.VITE_API_BASE || '/api'
    if (url.startsWith('/templates/thumbs/')) return `${base}${url}`
    if (url.startsWith('/')) return `${base}${url}`
    return url
  }

  function categoryLabel(cat: string) {
    const normalized = cat.toLowerCase()
    const found = categories.value.find((c) => c.key === normalized)
    return found ? found.label : cat
  }

  const filtered = computed(() => {
    if (activeCategory.value === 'all') return list.value
    return list.value.filter((item) => item.category.toLowerCase() === activeCategory.value)
  })

  const fetchError = ref<string | null>(null)
  async function fetchTemplates() {
    loading.value = true
    fetchError.value = null
    try {
      const { data, status } = await client.get('/templates', { validateStatus: (s) => s >= 200 && s < 400 })
      // 304 Not Modified 时 axios 视为成功但 data 可能为空，保留现有 list
      if (status === 304) return
      const raw: TemplateDTO[] = (data as any)?.data ?? data ?? []
      if (Array.isArray(raw) && raw.length > 0) {
        const sorted = [...raw].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        list.value = sorted
        if (!sorted.find((it) => it.id === selectedId.value)) selectedId.value = sorted[0].id
      } else if (Array.isArray(raw) && raw.length === 0) {
        // 空列表保持空态，不再静默回落 MOCK 以暴露后端问题；开发环境可手动查看 MOCK
        list.value = []
      } else {
        list.value = []
      }
    } catch (e: any) {
      fetchError.value = e?.response?.data?.message || e?.message || '加载失败'
      // 仅开发环境无后端时可回落 MOCK 保证视觉预览，其余环境保持空态
      if ((import.meta as any).env?.DEV) {
        // eslint-disable-next-line no-console
        console.warn('[templates] fallback to MOCK in DEV', fetchError.value)
        list.value = MOCK_TEMPLATES
      } else {
        list.value = []
      }
    } finally {
      loading.value = false
    }
  }

  function selectTemplate(id: string) {
    selectedId.value = id
  }

  async function useTemplate(tpl: TemplateDTO) {
    selectTemplate(tpl.id)
    try {
      const { data } = await client.post('/resumes', {
        title: `基于 ${tpl.name} 的简历`,
        scene: 'social_recruitment',
        templateId: tpl.id,
      })
      const created = data?.data
      if (created?.id) {
        router.push(`/editor/${created.id}`)
        return
      }
    } catch {
      // fallback: go editor with query
    }
    router.push(`/editor/${tpl.id}`)
  }

  onMounted(fetchTemplates)
</script>

<template>
  <div class="p-10 lg:p-12 flex flex-col gap-8 min-w-0 bg-[#fbf9f5]">
    <!-- TopAppBar — Stitch hairline, paper -->
    <header
      class="flex justify-between items-center h-14 px-6 w-full bg-[#fbf9f5] border-b sticky top-0 z-20 shrink-0"
      style="border-color: #c7c7c0"
    >
      <h2
        class="font-[Newsreader] text-[20px] md:text-[22px] font-medium tracking-tight text-black hidden md:block"
        style="font-family: 'Newsreader', serif"
      >
        Resume Atelier
      </h2>
      <h2
        class="font-[Newsreader] text-[16px] font-medium tracking-tight text-black md:hidden"
        style="font-family: 'Newsreader', serif"
      >
        {{ t('templates.title') }}
      </h2>
      <div class="flex items-center gap-4">
        <span
          class="hidden md:inline font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
          style="color: #8a8a87"
          >{{ filtered.length }} {{ t('templates.title') }}</span
        >
        <button
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-3 py-1 rounded-full border hover:border-black hover:text-black transition-colors"
          style="border-color: #eae8e3; color: #464742"
        >
          {{ t('common.filter') }}
        </button>
      </div>
    </header>

    <!-- Gallery Container -->
    <div class="flex-1 w-full max-w-7xl mx-auto px-6 md:px-10 lg:px-12 py-8 md:py-12 flex flex-col">
      <!-- Title Row + Tabs -->
      <div class="mb-8 md:mb-12 flex flex-col lg:flex-row lg:justify-between lg:items-end gap-6">
        <div>
          <h1
            class="font-[Newsreader] text-[32px] md:text-[40px] leading-[1.1] tracking-[-0.03em] font-medium text-black"
            style="font-family: 'Newsreader', serif"
          >
            {{ t('templates.choose') }}
          </h1>
          <p class="font-[Inter] text-[14px] leading-[22px] mt-2 max-w-2xl" style="color: #5d5f5d">
            {{ t('templates.desc') }}
          </p>
        </div>

        <!-- Desktop Tabs — Stitch border-b + active ink underline -->
        <div
          class="hidden md:flex items-center gap-1 border-b shrink-0"
          style="border-color: #c7c7c0"
        >
          <button
            v-for="c in categories"
            :key="c.key"
            @click="activeCategory = c.key"
            class="font-[Inter] text-[13px] leading-[20px] px-3 pb-2 pt-1 border-b-2 -mb-px transition-colors"
            :class="
              activeCategory === c.key
                ? 'text-black font-medium border-black'
                : 'text-[#636563] border-transparent hover:text-black hover:border-black/20'
            "
          >
            {{ c.label }}
          </button>
        </div>

        <!-- Mobile Tabs — horizontal scroll, hairline pills -->
        <div class="flex md:hidden gap-2 overflow-x-auto pb-1 -mx-1 px-1 scrollbar-none">
          <button
            v-for="c in categories"
            :key="c.key"
            @click="activeCategory = c.key"
            class="shrink-0 h-7 px-3 rounded-full text-[12px] font-medium border transition-colors"
            :style="
              activeCategory === c.key
                ? 'background:#0f0f0e;color:white;border-color:#0f0f0e'
                : 'background:white;color:#464742;border-color:#eae8e3'
            "
          >
            {{ c.label }}
          </button>
        </div>
      </div>

      <!-- Loading Skeleton -->
      <div
        v-if="loading"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 md:gap-8"
      >
        <div v-for="i in 4" :key="i" class="flex flex-col gap-3">
          <div
            class="aspect-[210/297] bg-white animate-pulse flex items-center justify-center"
            style="border: 0.5px solid #eae8e3"
          >
            <span
              class="font-[JetBrains_Mono] text-[11px] tracking-widest"
              style="color: #c7c7c0"
              >{{ t('common.loading').toUpperCase() }}</span
            >
          </div>
          <div class="h-3 w-2/3 bg-[#e9e8e4] rounded"></div>
          <div class="h-3 w-1/3 bg-[#f5f3f0] rounded"></div>
        </div>
      </div>

      <!-- Fetch Error — Editorial hairline, 保留视觉 -->
      <div
        v-else-if="fetchError"
        class="bg-white border border-[#eae8e3] rounded-xl p-8 flex flex-col items-center text-center gap-3"
      >
        <span class="material-symbols-outlined text-[24px]" style="color: #ff3b1f">error</span>
        <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color: #464742">{{ fetchError }}</p>
        <button @click="fetchTemplates" class="h-8 px-4 rounded-full bg-black text-white text-[12px]">重试</button>
      </div>

      <!-- Templates Grid — Stitch 1/2/3/4 cols, gap-xl, stagger -->
      <div
        v-else
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 md:gap-8"
      >
        <div
          v-for="(tpl, idx) in filtered"
          :key="tpl.id"
          class="group flex flex-col gap-3 stagger-item"
          :style="{ animationDelay: `${idx * 40}ms` }"
        >
          <!-- Thumbnail Card — hairline 0.5px → ink 1.5px on select/hover -->
          <div
            @click="selectTemplate(tpl.id)"
            class="aspect-[210/297] bg-white relative overflow-hidden flex items-center justify-center cursor-pointer template-card"
            :style="
              selectedId === tpl.id
                ? 'border:1.5px solid #000000; background:#ffffff'
                : 'border:0.5px solid #EAE8E3; background:#ffffff'
            "
          >
            <!-- Image or placeholder -->
            <img
              v-if="tpl.thumbnailUrl"
              :src="resolveThumb(tpl.thumbnailUrl)"
              :alt="tpl.name"
              class="w-full h-full object-cover transition-all duration-300"
              :class="
                selectedId === tpl.id
                  ? 'opacity-100'
                  : 'opacity-90 group-hover:opacity-100 grayscale group-hover:grayscale-0'
              "
              loading="lazy"
              @error="
                (e: Event) => {
                  const t = e.target as HTMLImageElement
                  t.style.display = 'none'
                }
              "
            />
            <div
              v-if="!tpl.thumbnailUrl"
              class="w-full h-full bg-[#f5f3f0] flex flex-col items-center justify-center gap-2"
            >
              <!-- A4 paper mock for missing thumb (matches Stitch LOADING card but more editorial) -->
              <div
                class="w-[68%] aspect-[210/297] bg-white paper-shadow p-3 flex flex-col gap-2"
                style="border: 0.5px solid #eae8e3"
              >
                <div class="h-2 w-2/3" style="background: #0f0f0e"></div>
                <div class="h-px w-full" style="background: #eae8e3"></div>
                <div class="space-y-1 mt-1">
                  <div class="h-1 w-full" style="background: #e9e8e4"></div>
                  <div class="h-1 w-5/6" style="background: #e3e2df"></div>
                  <div class="h-1 w-full" style="background: #e3e2df"></div>
                </div>
                <div class="mt-auto flex items-center gap-1">
                  <span class="w-1 h-1 rounded-full" style="background: #ff3b1f"></span>
                  <span
                    class="font-[JetBrains_Mono] text-[7px] tracking-widest"
                    style="color: #8a8a87"
                    >{{ categoryLabel(tpl.category).toUpperCase() }}</span
                  >
                </div>
              </div>
              <span
                class="font-[JetBrains_Mono] text-[10px] tracking-widest mt-1"
                style="color: #8a8a87"
                >{{ tpl.code.toUpperCase() }}</span
              >
            </div>

            <!-- Hover overlay — paper/5 + PREVIEW pill -->
            <div
              class="absolute inset-0 bg-black/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center"
            >
              <button
                @click.stop="useTemplate(tpl)"
                class="bg-black text-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-4 py-2 rounded-full transform translate-y-3 group-hover:translate-y-0 transition-transform duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-[#1a1a18]"
              >
                {{ t('templates.preview').toUpperCase() }}
              </button>
            </div>

            <!-- Selected ink corner mark -->
            <div
              v-if="selectedId === tpl.id"
              class="absolute top-2 right-2 w-6 h-6 rounded-full bg-black text-white flex items-center justify-center"
            >
              <span
                class="material-symbols-outlined text-[16px] leading-none"
                style="font-variation-settings: 'FILL' 1"
                >check</span
              >
            </div>

            <!-- Recommended ribbon — vermilion dot + label -->
            <div
              v-if="tpl.isRecommended"
              class="absolute top-2 left-2 flex items-center gap-1.5 bg-white/95 backdrop-blur px-2 py-1 rounded-full"
              style="border: 0.5px solid #eae8e3"
            >
              <span class="w-1.5 h-1.5 rounded-full" style="background: #ff3b1f"></span>
              <span
                class="font-[JetBrains_Mono] text-[9px] tracking-widest font-medium"
                style="color: #1b1c1a"
                >{{ t('templates.recommended').toUpperCase() }}</span
              >
            </div>
          </div>

          <!-- Meta — name + category + check -->
          <div class="flex justify-between items-start pt-1">
            <div class="min-w-0 flex-1 pr-2">
              <h3
                class="font-[Inter] text-[13px] leading-[20px] font-medium truncate"
                :class="selectedId === tpl.id ? 'text-black' : 'text-[#1b1c1a]'"
              >
                {{ tpl.name }}
              </h3>
              <p
                class="font-[JetBrains_Mono] text-[11px] leading-[14px] tracking-[0.06em] uppercase mt-0.5"
                style="color: #8a8a87"
              >
                {{ categoryLabel(tpl.category) }}
              </p>
            </div>
            <span
              v-if="selectedId === tpl.id"
              class="material-symbols-outlined text-black text-[18px] shrink-0"
              style="font-variation-settings: 'FILL' 1"
              >check_circle</span
            >
            <button
              v-else
              @click="useTemplate(tpl)"
              class="shrink-0 font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-black hover:text-[#ff3b1f] transition-colors"
            >
              {{ t('templates.use') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div
        v-if="!loading && filtered.length === 0"
        class="py-16 flex flex-col items-center justify-center border border-dashed rounded-lg mt-2"
        style="border-color: #c7c7c0; background: #ffffff"
      >
        <span class="material-symbols-outlined text-[28px] mb-2" style="color: #c7c7c0"
          >search_off</span
        >
        <p class="font-[Inter] text-[14px]" style="color: #464742">
          No templates in “{{ categoryLabel(activeCategory) }}”.
        </p>
        <button
          @click="activeCategory = 'all'"
          class="mt-3 h-8 px-4 rounded-full bg-black text-white text-[12px] font-medium hover:bg-[#1a1a18]"
        >
          {{ t('templates.all') }}
        </button>
      </div>
    </div>

    <!-- Footer — Stitch bg-surface-container-low border-t -->
    <footer class="w-full py-8 mt-auto bg-[#f5f3f0] border-t" style="border-color: #c7c7c0">
      <div
        class="max-w-7xl mx-auto px-6 md:px-10 flex flex-col md:flex-row justify-between items-center gap-4"
      >
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.04em] uppercase"
          style="color: #777871"
          >© {{ new Date().getFullYear() }} Resume Atelier. Crafted for Precision.</span
        >
        <div class="flex gap-6">
          <a
            class="font-[Inter] text-[13px] hover:text-black transition-colors"
            style="color: #5d5f5d"
            href="#"
            >Documentation</a
          >
          <a
            class="font-[Inter] text-[13px] hover:text-black transition-colors"
            style="color: #5d5f5d"
            href="#"
            >Privacy Policy</a
          >
          <a
            class="font-[Inter] text-[13px] hover:text-black transition-colors"
            style="color: #5d5f5d"
            href="#"
            >Terms of Service</a
          >
          <a
            class="font-[Inter] text-[13px] hover:text-black transition-colors hidden sm:inline"
            style="color: #5d5f5d"
            href="#"
            >Changelog</a
          >
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
  .template-card {
    transition:
      transform 380ms cubic-bezier(0.16, 1, 0.3, 1),
      box-shadow 380ms cubic-bezier(0.16, 1, 0.3, 1),
      border-color 200ms ease;
  }
  .template-card:hover {
    transform: translateY(-4px);
    box-shadow:
      0px 4px 24px rgba(0, 0, 0, 0.04),
      0px 2px 8px rgba(0, 0, 0, 0.02);
  }
  .paper-shadow {
    box-shadow:
      0px 4px 24px rgba(0, 0, 0, 0.04),
      0px 2px 8px rgba(0, 0, 0, 0.02);
  }
  @keyframes fadeUp {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  .stagger-item {
    opacity: 0;
    animation: fadeUp 380ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  .material-symbols-outlined {
    font-variation-settings:
      'FILL' 0,
      'wght' 300,
      'GRAD' 0,
      'opsz' 20;
  }
  .scrollbar-none::-webkit-scrollbar {
    display: none;
  }
  .scrollbar-none {
    scrollbar-width: none;
  }
</style>
