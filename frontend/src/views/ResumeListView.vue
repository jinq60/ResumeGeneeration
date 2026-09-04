<script setup lang="ts">
  import { ref, computed, onMounted, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import { useResumeStore } from '@/stores/resume'
  import client from '@/api/client'
  import AtButton from '@/components/atelier/AtButton.vue'
  import AtInput from '@/components/atelier/AtInput.vue'
  import AtModal from '@/components/atelier/AtModal.vue'
  import { pushToast } from '@/composables/useToast'

  const store = useResumeStore()
  const router = useRouter()
  const { t } = useI18n()

  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(9)
  const searchQuery = ref('')
  const sceneFilter = ref('all')
  const activeTab = ref<'all' | 'recent' | 'targeted'>('all')
  const openMenuId = ref<string | null>(null)

  // import modal state — Atelier paper / hairline / JetBrains Mono
  const showImport = ref(false)
  const importing = ref(false)
  const importForm = ref<{
    format: 'json' | 'markdown'
    content: string
    title: string
    scene: string
    targetPosition: string
    templateId: string
  }>({
    format: 'json',
    content: '',
    title: '',
    scene: 'social_recruitment',
    targetPosition: '',
    templateId: 'template_classic_single',
  })
  const importTemplates = ref<{ id: string; name: string }[]>([])

  const tabs = computed(() => [
    { label: t('resumes.tabsAll'), value: 'all' as const },
    { label: t('resumes.tabsRecent'), value: 'recent' as const },
    { label: t('resumes.tabsTargeted'), value: 'targeted' as const },
  ])

  const sceneOptions = computed(() => [
    { value: 'all', label: t('resumes.allScenes') },
    { value: 'social_recruitment', label: '社招' },
    { value: 'campus_recruitment', label: '校招' },
    { value: 'internship', label: '实习' },
    { value: 'postgraduate_reexam', label: '考研复试' },
    { value: 'project_application', label: '项目申报' },
    { value: 'custom', label: '自定义' },
  ])

  const importSceneOptions = computed(() => sceneOptions.value.filter((o) => o.value !== 'all'))

  const isFiltering = computed(
    () => sceneFilter.value !== 'all' || !!searchQuery.value.trim() || activeTab.value !== 'all'
  )
  const filteredCount = computed(() => {
    // 仅统计当前已加载页内的过滤结果；若需全量过滤需后端支持 scene/keyword 查询
    let list = [...store.list]
    if (activeTab.value === 'recent') {
      list.sort(
        (a, b) =>
          new Date(b.updatedAt || b.createdAt).getTime() -
          new Date(a.updatedAt || a.createdAt).getTime()
      )
    } else if (activeTab.value === 'targeted') {
      list = list.filter((r) => !!r.targetPosition)
    }
    if (sceneFilter.value !== 'all') list = list.filter((r) => r.scene === sceneFilter.value)
    const kw = searchQuery.value.trim().toLowerCase()
    if (kw)
      list = list.filter(
        (r) =>
          (r.title || '').toLowerCase().includes(kw) ||
          (r.targetPosition || '').toLowerCase().includes(kw) ||
          (r.scene || '').toLowerCase().includes(kw)
      )
    return list.length
  })
  const totalPages = computed(() => {
    const total = isFiltering.value ? filteredCount.value : store.total || 0
    return Math.max(1, Math.ceil(total / pageSize.value))
  })

  const paginationRange = computed(() => {
    const total = totalPages.value
    const cur = currentPage.value
    const range: (number | string)[] = []
    const maxVisible = 5
    let start = Math.max(1, cur - Math.floor(maxVisible / 2))
    let end = Math.min(total, start + maxVisible - 1)
    if (end - start + 1 < maxVisible) start = Math.max(1, end - maxVisible + 1)
    for (let i = start; i <= end; i++) range.push(i)
    if (start > 1) {
      range.unshift('…')
      range.unshift(1)
    }
    if (end < total) {
      range.push('…')
      range.push(total)
    }
    return range
  })

  const filteredList = computed(() => {
    let list = [...store.list]
    if (activeTab.value === 'recent') {
      list.sort(
        (a, b) =>
          new Date(b.updatedAt || b.createdAt).getTime() -
          new Date(a.updatedAt || a.createdAt).getTime()
      )
    } else if (activeTab.value === 'targeted') {
      list = list.filter((r) => !!r.targetPosition)
    }
    if (sceneFilter.value !== 'all') {
      list = list.filter((r) => r.scene === sceneFilter.value)
    }
    const kw = searchQuery.value.trim().toLowerCase()
    if (kw) {
      list = list.filter(
        (r) =>
          (r.title || '').toLowerCase().includes(kw) ||
          (r.targetPosition || '').toLowerCase().includes(kw) ||
          (r.scene || '').toLowerCase().includes(kw)
      )
    }
    if (isFiltering.value) {
      const start = (currentPage.value - 1) * pageSize.value
      return list.slice(start, start + pageSize.value)
    }
    return list
  })

  function sceneLabel(scene: string) {
    const map: Record<string, string> = {
      social: '社招',
      social_recruitment: '社招',
      campus_recruitment: '校招',
      campus: '校招',
      internship: '实习',
      custom: '自定义',
      postgraduate_reexam: '考研',
      project_application: '项目',
    }
    return map[scene] || scene || t('common.empty')
  }

  function formatDate(iso: string) {
    if (!iso) return ''
    const d = new Date(iso)
    const now = Date.now()
    const diff = now - d.getTime()
    const days = Math.floor(diff / 86400000)
    if (days === 0)
      return 'Today ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    if (days === 1) return 'Yesterday'
    if (days < 7) return `${days} days ago`
    return d.toLocaleDateString('zh-CN')
  }

  async function fetchPage() {
    loading.value = true
    try {
      await store.fetchList(currentPage.value, pageSize.value, {
        keyword: searchQuery.value || undefined,
        scene: sceneFilter.value,
      })
    } catch (e) {
      console.warn('[resumes] fetch failed', e)
    } finally {
      loading.value = false
    }
  }

  async function fetchImportTemplates() {
    try {
      const { data } = await client.get('/templates')
      const raw: any = data?.data ?? data ?? []
      if (Array.isArray(raw) && raw.length > 0) {
        importTemplates.value = raw.map((tpl: any) => ({ id: tpl.id, name: tpl.name }))
        if (!importTemplates.value.find((t) => t.id === importForm.value.templateId)) {
          importForm.value.templateId = importTemplates.value[0].id
        }
        if (!importForm.value.scene) importForm.value.scene = 'social_recruitment'
      } else {
        importTemplates.value = []
      }
    } catch {
      importTemplates.value = []
    }
  }

  onMounted(() => {
    fetchPage()
    fetchImportTemplates()
  })
  watch(currentPage, fetchPage)

  watch([activeTab, sceneFilter, searchQuery], () => {
    if (isFiltering.value) currentPage.value = 1
  })

  function clearFilters() {
    searchQuery.value = ''
    sceneFilter.value = 'all'
    activeTab.value = 'all'
  }

  async function createBlank() {
    try {
      const r = await store.create({
        scene: 'social_recruitment',
        templateId: 'template_classic_single',
        title: t('common.empty'),
      })
      router.push(`/editor/${r.id}`)
    } catch (e) {
      console.warn(e)
    }
  }

  function openEditor(id: string) {
    router.push(`/editor/${id}`)
  }

  async function handleImport() {
    const f = importForm.value
    if (!f.content.trim()) {
      pushToast('请粘贴简历内容')
      return
    }
    importing.value = true
    try {
      const payload: Record<string, string> = {
        format: f.format,
        content: f.content,
        templateId: f.templateId || 'template_classic_single',
        scene: f.scene || 'social_recruitment',
      }
      if (f.title.trim()) payload.title = f.title.trim()
      if (f.targetPosition.trim()) payload.targetPosition = f.targetPosition.trim()
      const { data } = await client.post('/resumes/import', payload)
      // R<ResumeDetailResponse> → data is R, data.data is detail
      if (data && typeof data.code === 'number' && data.code !== 200) {
        throw new Error(data.message || '导入失败')
      }
      const detail: any = data?.data ?? data
      const newId: string | undefined = detail?.id || detail?.resumeId || detail?.data?.id
      pushToast('导入成功')
      showImport.value = false
      // keep format, clear content but retain other fields for next import
      importForm.value.content = ''
      if (newId) {
        router.push(`/editor/${newId}`)
      } else {
        await fetchPage()
      }
    } catch (e: any) {
      const msg =
        e?.response?.data?.message || e?.message || '导入失败，请检查 JSON / Markdown 格式'
      pushToast(msg)
    } finally {
      importing.value = false
    }
  }

  async function handleDuplicate(id: string) {
    try {
      const { data } = await client.post(`/resumes/${id}/duplicate`)
      // data is R<DuplicateResumeResponse>
      const obj: any = data?.data ?? data
      const newId: string | undefined = obj?.id || obj?.resumeId || obj?.data?.id
      pushToast('已复制')
      if (newId) router.push(`/editor/${newId}`)
      else await fetchPage()
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message
      // fallback: create copy via store only if duplicate unsupported
      if (e?.response?.status === 404 || e?.response?.status === 501) {
        const src = store.list.find((r) => r.id === id)
        if (src) {
          try {
            const r = await store.create({
              scene: src.scene,
              templateId: src.templateId,
              title: src.title + ' Copy',
            })
            pushToast('已复制')
            router.push(`/editor/${r.id}`)
            return
          } catch {}
        }
      }
      pushToast(msg || '复制失败')
    } finally {
      openMenuId.value = null
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('确认删除该简历？删除后不可恢复')) return
    // 二次确认 — 满足 DELETE + 二次确认契约
    if (!confirm('再次确认删除？')) return
    try {
      await client.delete(`/resumes/${id}`)
      pushToast('已删除')
      if (store.list.length === 0 && currentPage.value > 1) currentPage.value--
      else await fetchPage()
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '删除失败'
      pushToast(msg)
    } finally {
      openMenuId.value = null
    }
  }

  async function handleRename(item: { id: string; title: string }) {
    const next = prompt('重命名简历', item.title)
    if (next === null) {
      openMenuId.value = null
      return
    }
    const trimmed = next.trim()
    if (!trimmed || trimmed === item.title) {
      openMenuId.value = null
      return
    }
    try {
      await client.put(`/resumes/${item.id}/title`, { title: trimmed })
      pushToast('重命名成功')
      await fetchPage()
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '重命名失败'
      // fallback to versioned PUT if title endpoint not supported
      if (e?.response?.status === 404 || e?.response?.status === 405) {
        try {
          const detail = await store.fetchOne(item.id)
          await client.put(`/resumes/${item.id}`, { title: trimmed, version: detail.version })
          pushToast('重命名成功')
          await fetchPage()
          return
        } catch (err: any) {
          pushToast(err?.response?.data?.message || err?.message || msg)
        }
      } else {
        pushToast(msg)
      }
    } finally {
      openMenuId.value = null
    }
  }

  function toggleMenu(id: string) {
    openMenuId.value = openMenuId.value === id ? null : id
  }

  // mock thumbnails for empty state — Stitch 结构复刻
  const mockResumes = [
    {
      id: 'mock-1',
      title: 'Senior Product Designer',
      scene: 'social_recruitment',
      updatedAt: new Date(Date.now() - 86400000).toISOString(),
    },
    {
      id: 'mock-2',
      title: 'UX Lead — Tech Corp',
      scene: 'campus_recruitment',
      updatedAt: new Date(Date.now() - 3 * 86400000).toISOString(),
    },
    {
      id: 'mock-3',
      title: 'Frontend Engineer — Atelier',
      scene: 'internship',
      updatedAt: new Date(Date.now() - 7 * 86400000).toISOString(),
    },
  ]
</script>

<template>
  <div class="p-10 lg:p-12 flex flex-col gap-8 min-w-0 bg-[#fbf9f5]">
    <!-- Header — Editorial -->
    <header class="flex flex-col lg:flex-row justify-between gap-6 border-b border-[#eae8e3] pb-6">
      <div>
        <h2 class="font-[Newsreader] text-[28px] font-medium text-black">
          {{ t('resumes.title') }}
        </h2>
        <p
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.08em] uppercase text-[#464742] mt-2"
        >
          {{ store.total }} {{ t('resumes.documents') }} · {{ t('resumes.manage') }}
        </p>
      </div>
      <div class="flex items-center gap-3 shrink-0">
        <div
          class="hidden sm:flex items-center gap-2 text-[11px] font-[JetBrains_Mono] uppercase tracking-widest text-[#464742]"
        >
          <span class="w-2 h-2 rounded-full bg-[#FF3B1F]"></span> Atelier Editorial
        </div>
        <button
          @click="showImport = true"
          class="px-5 h-9 bg-white border border-[#eae8e3] text-black rounded-full text-[13px] hover:border-black hover:bg-[#f5f3f0] flex items-center gap-2 shrink-0 transition-colors"
        >
          <span class="material-symbols-outlined text-[18px]">upload</span> {{ t('common.import') }}
        </button>
        <button
          @click="createBlank"
          class="px-6 h-9 bg-black text-white rounded-full text-[13px] hover:opacity-90 flex items-center gap-2 shrink-0"
        >
          <span class="material-symbols-outlined text-[18px]">add</span> {{ t('resumes.new') }}
        </button>
      </div>
    </header>

    <!-- Filter Bar — Stitch 1:1 复刻: search + scene select + tabs -->
    <div
      class="bg-white border border-[#eae8e3] rounded-xl p-2 flex flex-col xl:flex-row justify-between gap-3"
    >
      <!-- Tabs -->
      <div class="flex items-center gap-1 bg-[#f5f3f0] p-1 rounded-full self-start">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          @click="activeTab = tab.value"
          :class="[
            'px-4 h-7 rounded-full text-[13px] transition-colors',
            activeTab === tab.value
              ? 'bg-black text-white font-medium'
              : 'text-[#464742] hover:text-black',
          ]"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Search + Scene + Sort -->
      <div class="flex flex-wrap items-center gap-2">
        <div class="relative">
          <span
            class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#777871] text-[18px]"
            >search</span
          >
          <input
            v-model="searchQuery"
            :placeholder="t('resumes.searchPlaceholder')"
            class="pl-9 pr-4 h-9 bg-[#fbf9f5] border border-[#eae8e3] rounded-full text-[13px] w-[220px] lg:w-[260px] placeholder:text-[#8a8a87] focus:bg-white focus:border-black focus:outline-none transition-colors"
          />
        </div>

        <div class="relative flex items-center">
          <span
            class="material-symbols-outlined absolute left-3 text-[#777871] text-[18px] pointer-events-none"
            >filter_list</span
          >
          <select
            v-model="sceneFilter"
            class="pl-9 pr-8 h-9 bg-white border border-[#eae8e3] rounded-full text-[13px] text-[#1b1c1a] focus:border-black focus:outline-none appearance-none cursor-pointer hover:border-[#c7c7c0]"
          >
            <option v-for="opt in sceneOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
          <span
            class="material-symbols-outlined absolute right-3 text-[#777871] text-[16px] pointer-events-none"
            >expand_more</span
          >
        </div>

        <button
          class="w-9 h-9 bg-white border border-[#eae8e3] rounded-full flex items-center justify-center hover:border-black transition-colors"
          :title="t('common.filter')"
          @click="activeTab = activeTab === 'recent' ? 'all' : 'recent'"
        >
          <span class="material-symbols-outlined text-[18px] text-[#464742]">swap_vert</span>
        </button>

        <button
          v-if="searchQuery || sceneFilter !== 'all' || activeTab !== 'all'"
          @click="clearFilters"
          class="px-4 h-9 border border-[#eae8e3] rounded-full text-[13px] text-[#464742] hover:border-black hover:text-black bg-white transition-colors"
        >
          {{ t('resumes.clearFilter') }}
        </button>
      </div>
    </div>

    <!-- Meta / Caption -->
    <div class="flex justify-between items-center">
      <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-[#464742]">
        <span v-if="!loading"
          >{{ isFiltering ? filteredCount : filteredList.length }} / {{ store.total }} {{ t('resumes.documents') }}</span
        >
        <span v-else>{{ t('common.loading') }}</span>
        <span v-if="sceneFilter !== 'all'" class="ml-2 text-black"
          >· {{ sceneLabel(sceneFilter) }}</span
        >
      </p>
      <p class="hidden sm:block font-[JetBrains_Mono] text-[11px] text-[#777871]">
        {{ t('common.total') }} {{ currentPage }} · {{ pageSize }}
      </p>
    </div>

    <!-- Loading Skeletons -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      <div
        v-for="i in 6"
        :key="i"
        class="bg-white border border-[#eae8e3] rounded-xl overflow-hidden animate-pulse"
      >
        <div class="aspect-[210/297] bg-[#f5f3f0] p-4 flex flex-col gap-3">
          <div class="h-3 bg-[#e9e8e4] rounded w-3/5"></div>
          <div class="h-2 bg-[#e9e8e4] rounded w-2/5 mb-4"></div>
          <div class="space-y-2">
            <div class="h-1.5 bg-white rounded w-full"></div>
            <div class="h-1.5 bg-white rounded w-full"></div>
            <div class="h-1.5 bg-white rounded w-5/6"></div>
          </div>
        </div>
        <div class="p-4 border-t border-[#eae8e3] flex flex-col gap-2">
          <div class="h-3 bg-[#e9e8e4] rounded w-2/3"></div>
          <div class="h-2 bg-[#f5f3f0] rounded w-1/3"></div>
        </div>
      </div>
    </div>

    <!-- Empty: No data at all — show mock thumbnail structure as per spec -->
    <div
      v-else-if="!loading && store.list.length === 0 && !searchQuery && sceneFilter === 'all'"
      class="flex flex-col gap-8"
    >
      <div
        class="bg-white border border-dashed border-[#c7c7c0] rounded-xl p-12 flex flex-col items-center text-center"
      >
        <div
          class="w-14 h-14 rounded-full bg-[#f5f3f0] border border-[#eae8e3] flex items-center justify-center mb-4"
        >
          <span class="material-symbols-outlined text-[28px] text-[#777871]">description</span>
        </div>
        <h3 class="font-[Newsreader] text-[20px] font-semibold text-black">
          {{ t('resumes.empty') }}
        </h3>
        <p
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-[#464742] mt-2 max-w-[360px]"
        >
          {{ t('resumes.emptyDesc') }}
        </p>
        <div class="flex items-center gap-3 mt-6">
          <button
            @click="createBlank"
            class="px-6 h-9 bg-black text-white rounded-full text-[13px] hover:opacity-90"
          >
            {{ t('resumes.createFirst') }}
          </button>
          <button
            @click="showImport = true"
            class="px-6 h-9 bg-white border border-[#eae8e3] rounded-full text-[13px] text-black hover:border-black flex items-center gap-2"
          >
            <span class="material-symbols-outlined text-[18px]">upload</span>
            {{ t('common.import') }}
          </button>
          <router-link
            to="/templates"
            class="px-6 h-9 border border-[#eae8e3] rounded-full text-[13px] text-black hover:border-black bg-white flex items-center"
            >{{ t('common.viewAll') }}</router-link
          >
        </div>
      </div>

      <!-- Mock thumbnails — Stitch 1:1 纸张结构 -->
      <div>
        <p
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.08em] uppercase text-[#777871] mb-4"
        >
          {{ t('common.preview') }} — {{ t('common.preview') }}
        </p>
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 opacity-70">
          <div
            v-for="m in mockResumes"
            :key="m.id"
            class="group bg-white border border-[#eae8e3] rounded-xl overflow-hidden"
          >
            <div class="aspect-[210/297] bg-white p-4 flex flex-col relative overflow-hidden">
              <div class="w-full h-2 bg-[#e9e8e4] mb-2"></div>
              <div class="w-3/4 h-2 bg-[#e9e8e4] mb-6"></div>
              <div class="space-y-2">
                <div class="h-1 bg-[#f5f3f0] w-full"></div>
                <div class="h-1 bg-[#f5f3f0] w-full"></div>
                <div class="h-1 bg-[#f5f3f0] w-5/6"></div>
                <div class="h-1 bg-[#f5f3f0] w-full mt-4"></div>
                <div class="h-1 bg-[#f5f3f0] w-full"></div>
              </div>
              <span class="absolute bottom-3 right-3 w-2 h-2 rounded-full bg-[#FF3B1F]"></span>
            </div>
            <div class="p-4 border-t border-[#eae8e3]">
              <p class="text-[13px] font-medium text-black truncate">{{ m.title }}</p>
              <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1">
                {{ sceneLabel(m.scene) }} · {{ t('resumes.edited') }} {{ formatDate(m.updatedAt) }}
              </p>
            </div>
          </div>
          <!-- New Draft dashed -->
          <div
            @click="createBlank"
            class="flex flex-col justify-center items-center border border-dashed border-[#c7c7c0] aspect-[210/297] rounded-xl hover:border-black hover:bg-[#f5f3f0] cursor-pointer transition-colors bg-[#fbf9f5]/50"
          >
            <span class="material-symbols-outlined text-[#777871] mb-2">add</span>
            <p class="text-[13px] text-[#464742]">{{ t('common.newDraft') }}</p>
            <p class="font-[JetBrains_Mono] text-[11px] text-[#777871] mt-1">
              {{ t('common.newBlank') }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty: Filtered no results -->
    <div
      v-else-if="!loading && filteredList.length === 0"
      class="bg-white border border-[#eae8e3] rounded-xl p-12 flex flex-col items-center text-center"
    >
      <div class="w-12 h-12 rounded-full bg-[#f5f3f0] flex items-center justify-center mb-4">
        <span class="material-symbols-outlined text-[#777871]">search_off</span>
      </div>
      <h3 class="font-[Newsreader] text-[18px] font-semibold text-black">
        {{ t('resumes.noResult') }}
      </h3>
      <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-2">{{ t('common.filter') }}</p>
      <button
        @click="clearFilters"
        class="mt-6 px-5 h-8 border border-[#eae8e3] rounded-full text-[13px] hover:border-black"
      >
        {{ t('resumes.clearFilter') }}
      </button>
    </div>

    <!-- Card Grid — Stitch card 1:1 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      <div
        v-for="r in filteredList"
        :key="r.id"
        class="group bg-white border border-[#eae8e3] rounded-xl overflow-hidden hover:shadow-[0_8px_32px_rgba(0,0,0,0.06)] hover:border-[#d6d3cf] transition-all duration-300 flex flex-col cursor-pointer"
        @click="openEditor(r.id)"
      >
        <!-- Thumbnail — A4 paper mock (Stitch) -->
        <div
          class="aspect-[210/297] bg-white p-4 flex flex-col relative overflow-hidden border-b border-[#f5f3f0]"
        >
          <div class="w-3/5 h-2.5 bg-[#0f0f0e] mb-2 rounded-[1px]"></div>
          <div class="w-2/5 h-1.5 bg-[#e9e8e4] mb-5 rounded-[1px]"></div>
          <div class="space-y-1.5">
            <div class="h-1 bg-[#f5f3f0] w-full rounded-[1px]"></div>
            <div class="h-1 bg-[#f5f3f0] w-full rounded-[1px]"></div>
            <div class="h-1 bg-[#f5f3f0] w-5/6 rounded-[1px]"></div>
            <div class="h-1 bg-[#f5f3f0] w-full rounded-[1px] mt-3"></div>
            <div class="h-1 bg-[#f5f3f0] w-4/5 rounded-[1px]"></div>
          </div>
          <div class="mt-auto pt-6 space-y-1.5">
            <div class="h-1 bg-[#f5f3f0] w-full rounded-[1px]"></div>
            <div class="h-1 bg-[#f5f3f0] w-3/4 rounded-[1px]"></div>
          </div>
          <span class="absolute bottom-3 right-3 w-2 h-2 rounded-full bg-[#FF3B1F]"></span>
          <!-- Hover veil -->
          <div
            class="absolute inset-0 bg-black/0 group-hover:bg-black/[0.02] transition-colors"
          ></div>
          <!-- Quick actions on hover -->
          <div
            class="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <span
              class="w-7 h-7 rounded-full bg-white border border-[#eae8e3] shadow-sm flex items-center justify-center"
            >
              <span class="material-symbols-outlined text-[16px] text-[#464742]">open_in_new</span>
            </span>
          </div>
        </div>

        <!-- Meta -->
        <div class="p-4 flex justify-between gap-3">
          <div class="min-w-0 flex-1">
            <p class="text-[13px] font-medium text-black truncate leading-none">
              {{ r.title || t('common.empty') }}
            </p>
            <p
              class="font-[JetBrains_Mono] text-[11px] tracking-[0.02em] text-[#464742] mt-1.5 flex items-center gap-1.5 flex-wrap"
            >
              <span class="inline-flex items-center gap-1">
                <span class="w-1 h-1 rounded-full bg-[#c7c7c0]"></span>
                {{ sceneLabel(r.scene) }}
              </span>
              <span v-if="r.targetPosition" class="text-[#777871]">· {{ r.targetPosition }}</span>
            </p>
            <p class="font-[JetBrains_Mono] text-[11px] text-[#777871] mt-2">
              {{ t('resumes.edited') }} {{ formatDate(r.updatedAt || r.createdAt) }}
            </p>
          </div>

          <!-- More menu -->
          <div class="relative shrink-0 self-start" @click.stop>
            <button
              @click="toggleMenu(r.id)"
              class="w-7 h-7 rounded-full border border-transparent hover:border-[#eae8e3] hover:bg-[#f5f3f0] flex items-center justify-center transition-colors"
            >
              <span class="material-symbols-outlined text-[18px] text-[#777871]">more_horiz</span>
            </button>
            <div
              v-if="openMenuId === r.id"
              class="absolute right-0 top-8 w-44 bg-white border border-[#eae8e3] rounded-[12px] shadow-[0_8px_32px_rgba(0,0,0,0.08)] py-1.5 z-20 overflow-hidden"
            >
              <button
                @click="openEditor(r.id)"
                class="w-full text-left px-3 py-2 text-[13px] hover:bg-[#f5f3f0] flex items-center gap-2 text-black"
              >
                <span class="material-symbols-outlined text-[16px]">edit</span> 编辑
              </button>
              <button
                @click="handleRename(r)"
                class="w-full text-left px-3 py-2 text-[13px] hover:bg-[#f5f3f0] flex items-center gap-2 text-black"
              >
                <span class="material-symbols-outlined text-[16px]">drive_file_rename_outline</span>
                重命名
              </button>
              <button
                @click="handleDuplicate(r.id)"
                class="w-full text-left px-3 py-2 text-[13px] hover:bg-[#f5f3f0] flex items-center gap-2 text-black"
              >
                <span class="material-symbols-outlined text-[16px]">content_copy</span> 复制
              </button>
              <div class="h-px bg-[#eae8e3] my-1"></div>
              <button
                @click="handleDelete(r.id)"
                class="w-full text-left px-3 py-2 text-[13px] text-[#BA1A1A] hover:bg-[#ffdad6]/50 flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-[16px]">delete</span>
                {{ t('common.delete') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- New Draft dashed card — always last when list not full -->
      <div
        v-if="filteredList.length < pageSize && filteredList.length > 0"
        @click="createBlank"
        class="group flex flex-col justify-center items-center border border-dashed border-[#c7c7c0] rounded-xl aspect-[210/297] hover:border-black hover:bg-[#f5f3f0] cursor-pointer transition-colors bg-[#fbf9f5]/30"
      >
        <span
          class="material-symbols-outlined text-[#777871] mb-2 group-hover:text-black transition-colors"
          >add</span
        >
        <p class="text-[13px] text-[#464742] group-hover:text-black">{{ t('common.newDraft') }}</p>
        <p class="font-[JetBrains_Mono] text-[11px] text-[#777871] mt-1">
          {{ t('common.newBlank') }}
        </p>
      </div>
    </div>

    <!-- Pagination — Editorial hairline -->
    <div
      v-if="!loading && totalPages > 1"
      class="flex flex-col sm:flex-row justify-between items-center gap-4 border-t border-[#eae8e3] pt-6"
    >
      <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-[#464742]">
        {{ currentPage }} / {{ totalPages }} · {{ isFiltering ? filteredCount : store.total }} {{ t('resumes.documents') }}
      </p>
      <div class="flex items-center gap-1">
        <button
          @click="currentPage = Math.max(1, currentPage - 1)"
          :disabled="currentPage <= 1"
          class="w-8 h-8 rounded-full border border-[#eae8e3] bg-white flex items-center justify-center hover:border-black disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          <span class="material-symbols-outlined text-[18px]">chevron_left</span>
        </button>

        <template v-for="(p, idx) in paginationRange" :key="idx">
          <span v-if="p === '…'" class="px-1 text-[#777871]">…</span>
          <button
            v-else
            @click="currentPage = p as number"
            :class="[
              'min-w-8 h-8 px-2 rounded-full border text-[13px] transition-colors',
              currentPage === p
                ? 'bg-black text-white border-black'
                : 'bg-white border-[#eae8e3] hover:border-black text-[#464742] hover:text-black',
            ]"
          >
            {{ p }}
          </button>
        </template>

        <button
          @click="currentPage = Math.min(totalPages, currentPage + 1)"
          :disabled="currentPage >= totalPages"
          class="w-8 h-8 rounded-full border border-[#eae8e3] bg-white flex items-center justify-center hover:border-black disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          <span class="material-symbols-outlined text-[18px]">chevron_right</span>
        </button>
      </div>

      <div class="flex items-center gap-2">
        <span class="font-[JetBrains_Mono] text-[11px] text-[#777871] hidden sm:inline">{{
          t('common.total')
        }}</span>
        <select
          v-model="pageSize"
          @change="currentPage = 1; fetchPage()"
          class="h-8 pl-3 pr-6 bg-white border border-[#eae8e3] rounded-full text-[13px] focus:border-black focus:outline-none"
        >
          <option :value="9">9</option>
          <option :value="12">12</option>
          <option :value="18">18</option>
        </select>
      </div>
    </div>

    <!-- Click-away to close menu -->
    <div v-if="openMenuId" @click="openMenuId = null" class="fixed inset-0 z-10"></div>

    <!-- Import Modal — Atelier: 白卡 hairline 12px, JetBrains Mono 标签, Pill 按钮, paper/stone 色板 -->
    <AtModal
      v-model="showImport"
      title="导入简历"
      subtitle="Atelier · 粘贴 JSON 或 Markdown · 自动建档"
    >
      <div class="flex flex-col gap-5">
        <!-- Format -->
        <div>
          <p
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.08em] uppercase text-[#464742] mb-2"
          >
            格式 Format
          </p>
          <div class="inline-flex items-center gap-1 bg-[#f5f3f0] p-1 rounded-full">
            <button
              @click="importForm.format = 'json'"
              :class="[
                'px-4 h-7 rounded-full text-[13px] transition-colors',
                importForm.format === 'json'
                  ? 'bg-black text-white font-medium'
                  : 'text-[#464742] hover:text-black',
              ]"
            >
              JSON
            </button>
            <button
              @click="importForm.format = 'markdown'"
              :class="[
                'px-4 h-7 rounded-full text-[13px] transition-colors',
                importForm.format === 'markdown'
                  ? 'bg-black text-white font-medium'
                  : 'text-[#464742] hover:text-black',
              ]"
            >
              Markdown
            </button>
          </div>
          <p class="font-[JetBrains_Mono] text-[11px] text-[#777871] mt-2">
            {{
              importForm.format === 'json'
                ? '粘贴 ResumeDetail JSON / sections 数组'
                : '粘贴 Markdown 简历全文，服务端自动解析'
            }}
          </p>
        </div>

        <!-- Content -->
        <div>
          <label
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.08em] uppercase text-[#464742] mb-2 block"
            >内容 Content *</label
          >
          <textarea
            v-model="importForm.content"
            :placeholder="
              importForm.format === 'json'
                ? '{\n  &quot;title&quot;: &quot;...&quot;,\n  &quot;sections&quot;: [...] \n}'
                : '# 张三\n## 教育经历\n...'
            "
            rows="8"
            class="w-full min-h-[160px] p-3 bg-white border border-[#eae8e3] rounded-[12px] text-[13px] leading-5 placeholder:text-[#8a8a87] focus:border-black focus:outline-none resize-y font-[JetBrains_Mono]"
          />
          <p class="font-[JetBrains_Mono] text-[11px] text-[#777871] mt-1.5">
            {{ importForm.content.trim().length }} 字符
          </p>
        </div>

        <!-- Template -->
        <div>
          <label
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.08em] uppercase text-[#464742] mb-2 block"
            >模板 Template</label
          >
          <div class="relative">
            <select
              v-model="importForm.templateId"
              class="w-full h-[38px] pl-3 pr-8 bg-white border border-[#eae8e3] rounded-[10px] text-[13px] text-[#1b1c1a] focus:border-black focus:outline-none appearance-none cursor-pointer hover:border-[#c7c7c0] transition-colors"
            >
              <option value="">自动选择</option>
              <option v-for="tpl in importTemplates" :key="tpl.id" :value="tpl.id">
                {{ tpl.name }}
              </option>
            </select>
            <span
              class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-[#777871] text-[18px] pointer-events-none"
              >expand_more</span
            >
          </div>
        </div>

        <!-- Title / Scene grid -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <AtInput
            v-model="importForm.title"
            label="标题 Title"
            placeholder="如：前端工程师-张三"
          />
          <div class="block">
            <span class="text-sm text-ink font-inter mb-1 block">场景 Scene</span>
            <div class="relative">
              <select
                v-model="importForm.scene"
                class="w-full h-[38px] px-3 pr-8 rounded-[10px] border border-line bg-white text-sm focus:border-ink focus:outline-none transition appearance-none cursor-pointer"
              >
                <option value="">自动</option>
                <option v-for="opt in importSceneOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
              <span
                class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-[#777871] text-[18px] pointer-events-none"
                >expand_more</span
              >
            </div>
          </div>
        </div>

        <!-- Target Position -->
        <AtInput
          v-model="importForm.targetPosition"
          label="目标岗位 Target Position"
          placeholder="如：前端工程师"
        />

        <!-- Hint -->
        <div
          class="bg-[#fbf9f5] border border-[#eae8e3] rounded-[12px] px-3 py-2.5 flex gap-2 items-start"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F] mt-1.5 shrink-0"></span>
          <p class="font-[JetBrains_Mono] text-[11px] leading-4 text-[#464742]">
            导入将调用 <span class="text-black font-medium">POST /resumes/import</span>，后端契约
            <span class="text-black">ResumeImportRequest</span>，成功后自动进入编辑页。
          </p>
        </div>
      </div>

      <template #footer>
        <AtButton variant="ghost" @click="showImport = false">取消</AtButton>
        <AtButton
          :disabled="importing || !importForm.content.trim()"
          @click="handleImport"
          class="gap-2"
        >
          <span
            v-if="importing"
            class="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block"
          ></span>
          {{ importing ? '导入中…' : '导入' }}
        </AtButton>
      </template>
    </AtModal>
  </div>
</template>

<style>
  .material-symbols-outlined {
    font-variation-settings:
      'FILL' 0,
      'wght' 300,
      'GRAD' 0,
      'opsz' 20;
  }
</style>
