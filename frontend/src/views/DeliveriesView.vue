<script setup lang="ts">
  import { ref, computed, onMounted, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import client from '@/api/client'
  import AtModal from '@/components/atelier/AtModal.vue'
  import type { ApiResp, Page, DeliveryRecordResponse } from '@/api/types'

  const { t } = useI18n()

  type DeliveryStats = {
    totalDeliveries: number
    statusCounts: Record<string, number>
    topJobs: { name: string; count: number; percent: number }[]
  }

  const viewMode = ref<'user' | 'admin'>('user')
  const adminAvailable = ref(true)

  // filters — spec: keyword / company / position
  const keyword = ref('')
  const company = ref('')
  const position = ref('')
  const showFilters = ref(true)

  // pagination — spec asks for pagination, keep hairline controls
  const page = ref(1)
  const size = ref(10)
  const total = ref(0)
  const pages = ref(1)

  const list = ref<DeliveryRecordResponse[]>([])
  const loading = ref(false)
  const errorMsg = ref('')
  const statsAdmin = ref<DeliveryStats | null>(null)
  const selected = ref<DeliveryRecordResponse | null>(null)
  const showDetail = ref(false)
  function openDetail(d: DeliveryRecordResponse) {
    selected.value = d
    showDetail.value = true
  }
  const toast = ref('')
  let toastTimer: any = null
  function showToast(m: string) {
    toast.value = m
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => (toast.value = ''), 2200)
  }

  function formatDate(v?: string | null) {
    if (!v) return '—'
    const s = String(v)
    // LocalDate or LocalDateTime → slice 0..10
    return s.slice(0, 10)
  }

  function statusLabel(s: string) {
    const m: Record<string, string> = {
      delivered: '已投递',
      written: '笔试',
      interview1: '一面',
      interview2: '二面',
      hr: 'HR 面',
      offer: 'Offer',
      rejected: '已拒绝',
      withdrawn: '已撤回',
    }
    return m[s] || s
  }

  function statusStyle(s: string): string {
    switch (s) {
      case 'offer':
        return 'background:#1b1c1a;color:#ffffff;border:0.5px solid #1b1c1a;'
      case 'rejected':
        return 'background:#ffffff;color:#777871;border:0.5px solid #c7c7c0;'
      case 'withdrawn':
        return 'background:#f5f3f0;color:#8a8a87;border:0.5px solid #eae8e3;'
      case 'interview1':
      case 'interview2':
      case 'hr':
      case 'written':
        return 'background:#ffffff;color:#1b1c1a;border:0.5px solid #1b1c1a;border-left:3px solid #1b1c1a;'
      case 'delivered':
      default:
        return 'background:#e9e8e4;color:#464742;border:0.5px solid #e3e2df;'
    }
  }

  // unwrap helper — handles R<Page> and bare Page
  function unwrapPage(raw: any): Page<DeliveryRecordResponse> {
    if (!raw)
      return {
        records: [],
        total: 0,
        current: 1,
        size: size.value,
        pages: 1,
      } as Page<DeliveryRecordResponse>
    // R wrapper: { code, data: Page }
    if (
      raw.code !== undefined &&
      raw.data &&
      typeof raw.data === 'object' &&
      'records' in raw.data
    ) {
      return raw.data as Page<DeliveryRecordResponse>
    }
    if (raw.data && typeof raw.data === 'object' && 'records' in raw.data) {
      return raw.data as Page<DeliveryRecordResponse>
    }
    if ('records' in raw && Array.isArray(raw.records)) return raw as Page<DeliveryRecordResponse>
    return {
      records: [],
      total: 0,
      current: 1,
      size: size.value,
      pages: 1,
    } as Page<DeliveryRecordResponse>
  }

  async function fetchStats() {
    // try admin stats; if succeeds mark available. For user mode we keep admin stats for top cards if available.
    try {
      const { data } = await client.get<ApiResp<DeliveryStats>>('/admin/deliveries/stats')
      const payload: any = (data as any)?.data ?? data
      if (payload && typeof payload.totalDeliveries === 'number') {
        statsAdmin.value = payload as DeliveryStats
        adminAvailable.value = true
        return
      }
      // fallback: payload wrapped differently
      if ((data as any)?.data?.totalDeliveries !== undefined) {
        statsAdmin.value = (data as any).data
        adminAvailable.value = true
      }
    } catch (e: any) {
      const st = e?.response?.status
      if (st === 403 || st === 401) adminAvailable.value = false
      else adminAvailable.value = false
    }
  }

  async function fetchList() {
    loading.value = true
    errorMsg.value = ''
    try {
      if (viewMode.value === 'admin') {
        const kwParts = [keyword.value.trim(), company.value.trim(), position.value.trim()]
          .filter(Boolean)
          .join(' ')
        const kw = kwParts || keyword.value.trim() || undefined
        // admin endpoint only supports keyword; we merge company/position into keyword for 1:1 filter UX
        const { data } = await client.get('/admin/deliveries', {
          params: { page: page.value, size: size.value, keyword: kw || undefined },
        })
        const pg = unwrapPage(data as any)
        list.value = pg.records ?? []
        total.value = pg.total ?? 0
        pages.value = pg.pages ?? 1
      } else {
        const params: Record<string, any> = {
          page: page.value,
          size: size.value,
          keyword: keyword.value.trim() || undefined,
          company: company.value.trim() || undefined,
          position: position.value.trim() || undefined,
        }
        const { data } = await client.get('/deliveries', { params })
        const pg = unwrapPage(data as any)
        list.value = pg.records ?? []
        total.value = pg.total ?? 0
        pages.value = pg.pages ?? 1
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '加载失败'
      errorMsg.value = msg
      // keep previous list empty on error for clarity
      if (e?.response?.status === 403 && viewMode.value === 'admin') {
        showToast('无管理权限，已切回我的投递')
        viewMode.value = 'user'
      } else {
        // for demo without backend, keep empty but not crash build
        list.value = []
      }
    } finally {
      loading.value = false
    }
  }

  function onSearch() {
    page.value = 1
    fetchList()
  }
  function onReset() {
    keyword.value = ''
    company.value = ''
    position.value = ''
    page.value = 1
    fetchList()
  }
  function prevPage() {
    if (page.value > 1) {
      page.value -= 1
      fetchList()
    }
  }
  function nextPage() {
    if (page.value < pages.value) {
      page.value += 1
      fetchList()
    }
  }

  const statsCards = computed(() => {
    if (statsAdmin.value) {
      const sc = statsAdmin.value.statusCounts || {}
      const interview =
        (sc['written'] || 0) + (sc['interview1'] || 0) + (sc['interview2'] || 0) + (sc['hr'] || 0)
      const offer = sc['offer'] || 0
      const rejected = sc['rejected'] || 0
      return [
        {
          label: t('deliveries.statsTotal'),
          value: String(statsAdmin.value.totalDeliveries),
          mono: 'TOTAL',
          sub: statsAdmin.value.topJobs?.[0]?.name
            ? `TOP · ${statsAdmin.value.topJobs[0].name}`
            : t('deliveries.pipeline'),
        },
        {
          label: t('deliveries.pipeline'),
          value: String(sc['delivered'] ?? 0),
          mono: 'DELIVERED',
          sub: t('deliveries.status'),
        },
        {
          label: t('deliveries.statsInterview'),
          value: String(interview),
          mono: 'INTERVIEW',
          sub: t('deliveries.channel'),
        },
        {
          label: `${t('deliveries.statsOffer')} / ${t('deliveries.statsRejected')}`,
          value: `${offer} / ${rejected}`,
          mono: 'OFFER / REJECTED',
          sub: t('deliveries.pipeline'),
        },
      ]
    }
    const counts: Record<string, number> = {}
    list.value.forEach((r) => {
      const k = r.status || 'delivered'
      counts[k] = (counts[k] || 0) + 1
    })
    const interview =
      (counts['written'] || 0) +
      (counts['interview1'] || 0) +
      (counts['interview2'] || 0) +
      (counts['hr'] || 0)
    return [
      {
        label: t('deliveries.statsTotal'),
        value: String(total.value),
        mono: 'TOTAL',
        sub: t('deliveries.pipeline'),
      },
      {
        label: t('deliveries.pipeline'),
        value: String(counts['delivered'] || 0),
        mono: 'DELIVERED',
        sub: t('deliveries.status'),
      },
      {
        label: t('deliveries.statsInterview'),
        value: String(interview),
        mono: 'INTERVIEW',
        sub: t('deliveries.channel'),
      },
      {
        label: t('deliveries.statsOffer'),
        value: String(counts['offer'] || 0),
        mono: 'OFFER',
        sub: t('deliveries.statsRejected'),
      },
    ]
  })

  watch(viewMode, () => {
    page.value = 1
    fetchList()
    // stats already fetched; no need to refetch but keep
  })

  onMounted(async () => {
    await fetchStats()
    await fetchList()
  })
</script>

<template>
  <div class="p-10 lg:p-12 flex flex-col gap-8 min-w-0 bg-[#fbf9f5]">
    <!-- Mobile viewMode (visible sm) -->
    <div
      class="flex md:hidden items-center p-1 rounded-full bg-[#f5f3f0] w-fit"
      style="border: 0.5px solid #eae8e3"
    >
      <button
        @click="viewMode = 'user'"
        class="h-7 px-4 rounded-full text-[12px] font-medium"
        :style="viewMode === 'user' ? 'background:#0f0f0e;color:#ffffff;' : 'color:#464742;'"
      >
        {{ t('deliveries.title') }}
      </button>
      <button
        @click="viewMode = 'admin'"
        class="h-7 px-4 rounded-full text-[12px] font-medium"
        :style="viewMode === 'admin' ? 'background:#0f0f0e;color:#ffffff;' : 'color:#464742;'"
      >
        {{ t('deliveries.pipeline') }}
      </button>
    </div>

    <!-- 顶部统计 — 4 cards, hairline 0.5px #eae8e3, mono label -->
    <section class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div
        v-for="c in statsCards"
        :key="c.label"
        class="bg-white rounded-lg p-5 flex flex-col gap-3"
        style="border: 0.5px solid #eae8e3"
      >
        <p
          class="font-[JetBrains_Mono] text-[11px] leading-[14px] tracking-[0.06em] uppercase"
          style="color: #464742"
        >
          {{ c.mono }}
        </p>
        <p
          class="font-[JetBrains_Mono] text-[28px] md:text-[32px] leading-none tracking-[-0.02em] text-black"
        >
          {{ c.value }}
        </p>
        <div class="flex items-center justify-between mt-1">
          <p class="font-[Inter] text-[12px] leading-[16px] font-medium text-black">
            {{ c.label }}
          </p>
          <p
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
            style="color: #8a8a87"
          >
            {{ c.sub }}
          </p>
        </div>
      </div>
    </section>

    <!-- 筛选 — keyword / company / position, hairline inputs 38px / 10px -->
    <section
      v-show="showFilters"
      class="bg-white rounded-xl p-4 md:p-5 flex flex-col gap-4"
      style="border: 0.5px solid #eae8e3"
    >
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span>
          <h3
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
            style="color: #1b1c1a"
          >
            {{ t('deliveries.pipeline') }} — {{ t('common.filter') }}
          </h3>
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase hidden sm:inline"
            style="color: #8a8a87"
          >
            client.get('{{ viewMode === 'admin' ? '/admin/deliveries' : '/deliveries' }}')
          </span>
        </div>
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
          style="color: #8a8a87"
          >{{ total }} records</span
        >
      </div>
      <div class="grid grid-cols-1 md:grid-cols-12 gap-3">
        <label class="md:col-span-5 block">
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block"
            style="color: #464742"
            >Keyword — {{ t('deliveries.company') }} / {{ t('deliveries.position') }}</span
          >
          <div class="relative">
            <span
              class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px]"
              style="color: #8a8a87"
              >search</span
            >
            <input
              v-model="keyword"
              @keyup.enter="onSearch"
              :placeholder="t('common.search')"
              class="w-full h-[38px] pl-10 pr-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
              style="border: 0.5px solid #eae8e3"
            />
          </div>
        </label>
        <label class="md:col-span-3 block">
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block"
            style="color: #464742"
            >{{ t('deliveries.company') }}</span
          >
          <input
            v-model="company"
            @keyup.enter="onSearch"
            :placeholder="t('deliveries.company')"
            class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
            style="border: 0.5px solid #eae8e3"
          />
        </label>
        <label class="md:col-span-4 block">
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block"
            style="color: #464742"
            >{{ t('deliveries.position') }}</span
          >
          <div class="flex gap-2">
            <input
              v-model="position"
              @keyup.enter="onSearch"
              :placeholder="t('deliveries.position')"
              class="flex-1 h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
              style="border: 0.5px solid #eae8e3"
            />
            <button
              @click="onSearch"
              class="h-[38px] px-5 rounded-full bg-black text-white text-[13px] font-medium hover:bg-[#1a1a18] shrink-0"
            >
              {{ t('common.search') }}
            </button>
            <button
              @click="onReset"
              class="h-[38px] px-4 rounded-full bg-white text-[13px] font-medium hover:border-black hover:text-black transition-colors shrink-0"
              style="border: 0.5px solid #eae8e3; color: #464742"
            >
              {{ t('common.reset') }}
            </button>
          </div>
        </label>
      </div>
      <p
        class="font-[JetBrains_Mono] text-[10px] leading-[14px] tracking-[0.04em] uppercase"
        style="color: #8a8a87"
      >
        现接：<span class="text-black">GET /deliveries?page&size&keyword&company&position</span> 与
        <span class="text-black">GET /admin/deliveries?keyword</span> · 视图「{{
          viewMode === 'admin' ? t('deliveries.pipeline') : t('deliveries.title')
        }}」自动切换 endpoint；分页 page {{ page }} / {{ pages }} · size {{ size }}
      </p>
    </section>

    <!-- 错误条 -->
    <div
      v-if="errorMsg"
      class="bg-[#ffdad6] rounded-lg px-4 py-3 flex items-center gap-2"
      style="border: 0.5px solid #ffb4a6"
    >
      <span class="material-symbols-outlined text-[#ba1a1a] text-[18px]">error</span>
      <span class="text-[13px] text-[#93000a]">{{ errorMsg }}</span>
      <button
        class="ml-auto font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase underline text-[#ba1a1a]"
        @click="fetchList"
      >
        重试
      </button>
    </div>

    <!-- 表格 — hairline 0.5px #eae8e3, header mono, body 13px Inter -->
    <section class="bg-white rounded-xl overflow-hidden" style="border: 0.5px solid #eae8e3">
      <!-- table header meta -->
      <div
        class="flex items-center justify-between px-4 md:px-6 py-4 border-b bg-[#fbf9f5]"
        style="border-color: #eae8e3"
      >
        <div class="flex items-center gap-3">
          <h3
            class="font-[Newsreader] text-[16px] font-medium text-black"
            style="font-family: 'Newsreader', serif"
          >
            {{ t('deliveries.title') }}
          </h3>
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2 py-1 rounded-sm bg-white hidden md:inline"
            style="border: 0.5px solid #eae8e3; color: #464742"
            >{{ t('deliveries.newTrack') }}</span
          >
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2 py-1 rounded-sm bg-white"
            style="border: 0.5px solid #eae8e3; color: #464742"
          >
            {{ total }} 条 ·
            {{ viewMode === 'admin' ? t('deliveries.pipeline') : t('deliveries.title') }}
          </span>
        </div>
        <div
          class="hidden sm:flex items-center gap-2 font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
          style="color: #8a8a87"
        >
          <span>{{ t('deliveries.company') }}</span
          ><span class="opacity-30">/</span><span>{{ t('deliveries.position') }}</span
          ><span class="opacity-30">/</span><span>{{ t('deliveries.date') }}</span
          ><span class="opacity-30">/</span><span>{{ t('deliveries.status') }}</span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full border-collapse min-w-[720px]">
          <thead>
            <tr class="bg-[#f5f3f0] border-b" style="border-color: #eae8e3">
              <th
                class="text-left font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium px-6 py-3"
                style="color: #464742"
              >
                {{ t('deliveries.company') }}
              </th>
              <th
                class="text-left font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium px-4 py-3"
                style="color: #464742"
              >
                {{ t('deliveries.position') }}
              </th>
              <th
                class="text-left font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium px-4 py-3"
                style="color: #464742"
              >
                {{ t('deliveries.date') }}
              </th>
              <th
                class="text-left font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium px-4 py-3"
                style="color: #464742"
              >
                {{ t('deliveries.status') }}
              </th>
              <th
                class="text-right font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium px-6 py-3"
                style="color: #464742"
              >
                {{ t('deliveries.action') }}
              </th>
            </tr>
          </thead>
          <tbody>
            <!-- loading skeleton -->
            <template v-if="loading">
              <tr v-for="i in 3" :key="i" class="border-b" style="border-color: #eae8e3">
                <td class="px-6 py-5">
                  <div class="h-3 w-28 bg-[#e9e8e4] rounded animate-pulse"></div>
                </td>
                <td class="px-4 py-5">
                  <div class="h-3 w-36 bg-[#f5f3f0] rounded animate-pulse"></div>
                </td>
                <td class="px-4 py-5">
                  <div class="h-3 w-20 bg-[#eae8e3] rounded animate-pulse"></div>
                </td>
                <td class="px-4 py-5">
                  <div class="h-5 w-16 bg-[#e3e2df] rounded-sm animate-pulse"></div>
                </td>
                <td class="px-6 py-5">
                  <div class="h-3 w-12 bg-[#eae8e3] rounded animate-pulse ml-auto"></div>
                </td>
              </tr>
            </template>
            <template v-else-if="list.length">
              <tr
                v-for="d in list"
                :key="d.id"
                class="border-b last:border-0 hover:bg-[#fbf9f5] transition-colors group"
                style="border-color: #eae8e3"
              >
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <span
                      class="w-8 h-8 rounded-full bg-[#f5f3f0] flex items-center justify-center shrink-0"
                      style="border: 0.5px solid #eae8e3"
                    >
                      <span
                        class="font-[JetBrains_Mono] text-[11px] font-medium"
                        style="color: #1b1c1a"
                        >{{ (d.company || '—').slice(0, 1).toUpperCase() }}</span
                      >
                    </span>
                    <span
                      class="font-[Inter] text-[13px] leading-[20px] font-medium text-black group-hover:text-black"
                      >{{ d.company }}</span
                    >
                  </div>
                </td>
                <td class="px-4 py-4">
                  <span class="font-[Inter] text-[13px] leading-[20px]" style="color: #464742">{{
                    d.position
                  }}</span>
                  <span
                    v-if="d.channel"
                    class="ml-2 font-[JetBrains_Mono] text-[10px] tracking-[0.06em] uppercase px-1.5 py-0.5 rounded-sm bg-[#f5f3f0]"
                    style="border: 0.5px solid #eae8e3; color: #8a8a87"
                    >{{ d.channel }}</span
                  >
                </td>
                <td
                  class="px-4 py-4 font-[JetBrains_Mono] text-[12px] leading-[16px]"
                  style="color: #464742"
                >
                  {{ formatDate((d as any).applyDate || d.createdAt) }}
                  <span
                    class="hidden xl:inline ml-2 font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
                    style="color: #8a8a87"
                    >{{ String(d.createdAt || '').slice(11, 16) }}</span
                  >
                </td>
                <td class="px-4 py-4">
                  <span
                    class="inline-flex items-center gap-1.5 font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2.5 py-1 rounded-sm font-medium"
                    :style="statusStyle(d.status)"
                  >
                    <span
                      v-if="['interview1', 'interview2', 'hr', 'written'].includes(d.status)"
                      class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"
                    ></span>
                    <span
                      v-else-if="d.status === 'offer'"
                      class="w-1.5 h-1.5 rounded-full bg-white"
                    ></span>
                    {{ statusLabel(d.status) }}
                  </span>
                </td>
                <td class="px-6 py-4 text-right">
                  <button
                    class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-[#464742] hover:text-black underline-offset-2 hover:underline"
                    @click="openDetail(d)"
                  >
                    {{ t('deliveries.detail') }}
                  </button>
                </td>
              </tr>
            </template>
            <tr v-else>
              <td colspan="5" class="px-6 py-16 text-center">
                <div class="flex flex-col items-center gap-3">
                  <span
                    class="w-12 h-12 rounded-full bg-[#f5f3f0] flex items-center justify-center"
                    style="border: 0.5px solid #eae8e3"
                  >
                    <span class="material-symbols-outlined text-[20px]" style="color: #8a8a87"
                      >inbox</span
                    >
                  </span>
                  <p class="font-[Inter] text-[14px] font-medium text-black">
                    {{ t('deliveries.empty') }}
                  </p>
                  <p
                    class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase max-w-sm"
                    style="color: #8a8a87"
                  >
                    {{ t('deliveries.emptyDesc') }}
                  </p>
                  <button
                    class="mt-2 h-8 px-4 rounded-full bg-black text-white text-[12px] font-medium hover:bg-[#1a1a18]"
                    @click="onReset"
                  >
                    {{ t('common.reset') }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 — hairline top, mono label -->
      <div
        class="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 md:px-6 py-4 bg-[#fbf9f5] border-t"
        style="border-color: #eae8e3"
      >
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase"
          style="color: #8a8a87"
        >
          共 <span class="text-black">{{ total }}</span> 条 · 第
          <span class="text-black">{{ page }}</span> / {{ pages }} 页 · 每页 {{ size }} 条
        </span>
        <div class="flex items-center gap-2">
          <button
            @click="prevPage"
            :disabled="page <= 1 || loading"
            class="h-8 px-4 rounded-full border bg-white text-[12px] font-medium flex items-center gap-1 transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:border-black hover:text-black"
            style="border-color: #c7c7c0; color: #1b1c1a"
          >
            <span class="material-symbols-outlined text-[16px]">chevron_left</span>
            {{ t('common.prev') }}
          </button>
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2"
            style="color: #464742"
            >{{ page }} / {{ pages }}</span
          >
          <button
            @click="nextPage"
            :disabled="page >= pages || loading"
            class="h-8 px-4 rounded-full border bg-white text-[12px] font-medium flex items-center gap-1 transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:border-black hover:text-black"
            style="border-color: #c7c7c0; color: #1b1c1a"
          >
            {{ t('common.next') }}
            <span class="material-symbols-outlined text-[16px]">chevron_right</span>
          </button>
          <select
            v-model.number="size"
            @change="page = 1; fetchList()"
            class="h-8 rounded-full bg-white text-[12px] px-2 focus:outline-none"
            style="border: 0.5px solid #eae8e3; color: #464742"
          >
            <option :value="10">10 / 页</option>
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
          </select>
        </div>
      </div>
    </section>

    <!-- Detail Modal — Editorial hairline 12px, 展示新增字段 -->
    <AtModal v-model="showDetail" title="投递详情" subtitle="Delivery Detail · Editorial hairline">
      <div v-if="selected" class="flex flex-col gap-4">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">公司</p>
            <p class="font-[Inter] text-[13px] text-black">{{ selected.company }}</p>
          </div>
          <div>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">岗位</p>
            <p class="font-[Inter] text-[13px] text-black">{{ selected.position }}<span v-if="selected.channel" class="ml-2 text-[11px] text-[#8a8a87]">· {{ selected.channel }}</span></p>
          </div>
          <div>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">状态</p>
            <span class="inline-flex px-2 py-1 rounded-sm font-[JetBrains_Mono] text-[11px] uppercase" :style="statusStyle(selected.status)">{{ statusLabel(selected.status) }}</span>
          </div>
          <div>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">投递日期</p>
            <p class="font-[Inter] text-[13px]">{{ formatDate(selected.applyDate || selected.createdAt) }}</p>
          </div>
          <div v-if="selected.interviewTime">
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">面试时间</p>
            <p class="font-[Inter] text-[13px]">{{ String(selected.interviewTime).slice(0,16).replace('T',' ') }}</p>
          </div>
          <div v-if="selected.interviewLocation">
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">面试地点</p>
            <p class="font-[Inter] text-[13px]">{{ selected.interviewLocation }}</p>
          </div>
        </div>
        <div v-if="selected.note">
          <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">备注 Note</p>
          <p class="font-[Inter] text-[13px] leading-5 whitespace-pre-wrap">{{ selected.note }}</p>
        </div>
        <div v-if="selected.jdContent">
          <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color:#8a8a87">JD 原文</p>
          <div class="max-h-[160px] overflow-auto p-3 bg-[#fbf9f5] rounded-lg text-[12px] leading-5 whitespace-pre-wrap" style="border:0.5px solid #eae8e3">{{ selected.jdContent }}</div>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <button @click="showDetail=false" class="h-8 px-4 rounded-full border text-[12px]" style="border-color:#eae8e3">关闭</button>
        </div>
      </div>
    </AtModal>

    <!-- Kanban hint — editorial homage to Stitch 01d035, kept as mono footnote -->
    <p
      class="font-[JetBrains_Mono] text-[10px] leading-[14px] tracking-[0.04em] uppercase text-center"
      style="color: #8a8a87"
    >
      Board view homage — Stitch 01d035 Kanban: Delivered · Interview · Offer · Rejected · hairline
      0.5px #eae8e3 · mono label JetBrains Mono 11px/0.06em
    </p>
  </div>
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
