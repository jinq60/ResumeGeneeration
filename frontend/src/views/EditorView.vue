<script setup lang="ts">
  import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
  import { useRoute } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import client from '@/api/client'
  import { useResumeStore } from '@/stores/resume'
  import type { SectionDTO, RenderSettings, ShareResponse } from '@/api/types'
  import AtModal from '@/components/atelier/AtModal.vue'
  import SectionEditor from '@/components/editor/SectionEditor.vue'
  import { pushToast, toastState } from '@/composables/useToast'

  const { t } = useI18n()

  const route = useRoute()
  const id = route.params.id as string
  const store = useResumeStore()

  const html = ref('')
  const saving = ref(false)
  let timer: any = null
  let rsTimer: any = null

  const zoom = ref(100)
  const viewMode = ref<'preview' | 'split' | 'focus'>('split')
  const activeId = ref<string | null>(null)
  const mobileSheetOpen = ref(false)

  const isPreview = computed(() => viewMode.value === 'preview')
  const isFocus = computed(() => viewMode.value === 'focus')

  // ---- Share state ----
  const shareOpen = ref(false)
  const shareData = ref<ShareResponse | null>(null)
  const shareHideContact = ref(false)
  const shareExpiresAt = ref('') // YYYY-MM-DD
  const shareLoading = ref(false)
  const shareUrl = computed(() => {
    if (!shareData.value) return ''
    const token = shareData.value.token
    const url = (shareData.value as any).url
    if (url && typeof url === 'string' && url.startsWith('http')) return url
    return `${window.location.origin}/share/${token}`
  })

  async function fetchShare() {
    try {
      const { data } = await client.get(`/resumes/${id}/share`)
      const resp = data?.data as ShareResponse | null
      if (resp && resp.token) {
        shareData.value = resp
        shareHideContact.value = !!resp.hideContact
        shareExpiresAt.value = resp.expiresAt ? String(resp.expiresAt).split('T')[0] : ''
      } else {
        shareData.value = null
      }
    } catch {
      shareData.value = null
    }
  }

  async function createShare() {
    shareLoading.value = true
    const hadShare = !!shareData.value
    try {
      const payload: Record<string, unknown> = { hideContact: shareHideContact.value }
      if (shareExpiresAt.value) payload.expiresAt = `${shareExpiresAt.value}T00:00:00`
      const { data } = await client.post(`/resumes/${id}/share`, payload)
      if (data.code !== 200) throw new Error(data.message || '创建失败')
      const resp = data.data as ShareResponse
      shareData.value = resp
      shareHideContact.value = !!resp.hideContact
      shareExpiresAt.value = resp.expiresAt
        ? String(resp.expiresAt).split('T')[0]
        : shareExpiresAt.value
          ? shareExpiresAt.value
          : ''
      if (!resp.expiresAt) shareExpiresAt.value = shareExpiresAt.value ? shareExpiresAt.value : ''
      pushToast(hadShare ? '分享已更新' : '分享已创建')
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '创建失败'
      pushToast(msg)
    } finally {
      shareLoading.value = false
    }
  }

  async function closeShare() {
    shareLoading.value = true
    try {
      const { data } = await client.delete(`/resumes/${id}/share`)
      if (data.code !== undefined && data.code !== 200) throw new Error(data.message || '关闭失败')
      shareData.value = null
      pushToast('分享已关闭')
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '关闭失败'
      pushToast(msg)
    } finally {
      shareLoading.value = false
    }
  }

  async function copyShareLink() {
    if (!shareUrl.value) return
    try {
      await navigator.clipboard.writeText(shareUrl.value)
      pushToast('已复制链接')
    } catch {
      // fallback
      const ta = document.createElement('textarea')
      ta.value = shareUrl.value
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      pushToast('已复制链接')
    }
  }

  const templates = ref<{ id: string; name: string }[]>([])
  async function fetchTemplates() {
    try {
      const { data } = await client.get('/templates')
      const raw: any = (data as any)?.data ?? data ?? []
      if (Array.isArray(raw)) templates.value = raw.map((t: any) => ({ id: t.id, name: t.name }))
    } catch {}
  }
  async function switchTemplate(tplId: string) {
    if (!store.current || tplId === store.current.templateId) return
    try {
      const res = await store.update(id, { templateId: tplId, version: store.current.version } as any)
      if (store.current) store.current.templateId = tplId
      if (store.current) store.current.version = (res as any).version ?? store.current.version + 1
      preview()
      pushToast('模板已切换')
    } catch (e: any) {
      if (e?.response?.data?.code === 2012) { await store.fetchOne(id); preview(); pushToast('版本冲突，已刷新') }
      else pushToast(e?.response?.data?.message || '切换失败')
    }
  }

  onMounted(async () => {
    await store.fetchOne(id)
    if (store.current?.sections?.length) activeId.value = store.current.sections[0].id
    preview()
    fetchShare()
    fetchTemplates()
  })

  async function preview() {
    try {
      const { data } = await client.get(`/resumes/${id}/preview`, { params: {}, responseType: 'text' as any })
      const htmlStr = typeof data === 'string' ? data : (data?.data ?? '')
      html.value = typeof htmlStr === 'string' ? htmlStr : ''
    } catch {
      html.value = ''
    }
  }

  // autosave with single debounced queue to avoid 429/409
  let pendingSave = false
  let pendingPayload: any = null
  function scheduleSave(payload: any, delay = 1800) {
    pendingPayload = { ... (pendingPayload || {}), ...payload }
    clearTimeout(timer)
    clearTimeout(rsTimer)
    timer = setTimeout(async () => {
      if (!store.current || !pendingPayload) return
      if (saving.value) { pendingSave = true; return }
      const toSend = pendingPayload
      pendingPayload = null
      saving.value = true
      try {
        if (toSend.sections) {
          const sanitized = toSend.sections.map((s: any) => {
            const listTypes = ['education', 'work', 'project', 'skill']
            if (listTypes.includes(s.type) && !Array.isArray(s.data)) s.data = []
            if (!listTypes.includes(s.type) && Array.isArray(s.data)) s.data = {}
            return s
          })
          toSend.sections = sanitized
        }
        const res = await store.update(id, { ...toSend, version: store.current.version })
        if (store.current) store.current.version = res.version ?? store.current.version + 1
        preview()
      } catch (e: any) {
        if (e.response?.data?.code === 2012) {
          try { await store.fetchOne(id); preview() } catch {}
          pushToast('版本冲突，已刷新最新')
        } else if (e.response?.status === 429) {
          pushToast('保存过于频繁，请稍后')
          pendingPayload = { ...pendingPayload, ...toSend }
          pendingSave = true
        } else if (e.response?.data?.code === 400) {
          pushToast(e.response?.data?.message || '保存失败')
        }
      } finally {
        saving.value = false
        if (pendingSave) { pendingSave = false; if (pendingPayload) scheduleSave(pendingPayload, 500) }
      }
    }, delay)
  }
  watch(() => store.current?.sections, (v) => { if (!v) return; scheduleSave({ sections: v }, 1800) }, { deep: true })
  watch(() => store.current?.renderSettings, (v) => { if (!v) return; scheduleSave({ renderSettings: v }, 1800) }, { deep: true })

  let pdfTimer: ReturnType<typeof setInterval> | null = null
  async function exportPdf() {
    const { data } = await client.post('/pdf/export', { resumeId: id })
    const taskId = data.data.taskId
    if (pdfTimer) clearInterval(pdfTimer)
    pdfTimer = setInterval(async () => {
      const t = (await client.get(`/pdf/tasks/${taskId}`)).data.data
      if (t.status === 'success') {
        if (pdfTimer) clearInterval(pdfTimer)
        pdfTimer = null
        const base = (import.meta as any).env?.VITE_API_BASE || '/api'
        window.open(`${base}/pdf/download/${taskId}`)
      }
      if (t.status === 'failed') {
        if (pdfTimer) clearInterval(pdfTimer)
        pdfTimer = null
        pushToast('导出失败: ' + (t.errorMsg || 'unknown'))
      }
    }, 1000)
  }
  onUnmounted(() => {
    if (pdfTimer) clearInterval(pdfTimer)
  })

  function setZoom(d: number) {
    zoom.value = Math.min(150, Math.max(50, zoom.value + d))
  }

  const activeSection = computed(
    () => store.current?.sections.find((s) => s.id === activeId.value) || null
  )

  function toggleVisible(s: SectionDTO) {
    s.visible = !s.visible
    // trigger watch via assignment
    if (store.current) store.current.sections = [...store.current.sections]
  }

  function removeSection(s: SectionDTO) {
    if (!store.current) return
    store.current.sections = store.current.sections.filter((x) => x.id !== s.id)
    if (activeId.value === s.id) activeId.value = store.current.sections[0]?.id || null
  }

  function addSection(type: SectionDTO['type'] = 'education') {
    if (!store.current) return
    const idNew = 'sec_' + Date.now()
    const titles: Record<string, string> = {
      profile: t('editor.profile'),
      education: t('editor.education'),
      work: t('editor.work'),
      project: t('editor.project'),
      skill: t('editor.skill'),
      introduction: t('editor.introduction'),
      custom: 'Custom',
    }
    store.current.sections.push({
      id: idNew,
      type,
      title: titles[type] || 'Custom',
      order: store.current.sections.length,
      visible: true,
      data: (['education','work','project','skill'].includes(type) ? [] : {}),
    })
    activeId.value = idNew
  }

  function updateField(sectionId: string, key: string, val: any) {
    const sec = store.current?.sections.find((s) => s.id === sectionId)
    if (!sec) return
    if (!sec.data) sec.data = {}
    sec.data[key] = val
    // trigger deep watch — reassign array to ensure reactivity
    if (store.current) store.current.sections = [...store.current.sections]
  }

  function updateRender<K extends keyof RenderSettings>(key: K, val: RenderSettings[K]) {
    if (!store.current) return
    if (!store.current.renderSettings) store.current.renderSettings = {}
    ;(store.current.renderSettings as any)[key] = val
  }

  function handleJsonInput(e: Event) {
    try {
      const v = (e.target as HTMLTextAreaElement).value
      if (!activeSection.value) return
      activeSection.value.data = JSON.parse(v)
      if (store.current) store.current.sections = [...store.current.sections]
    } catch {}
  }

  const sectionIcon: Record<string, string> = {
    profile: 'person',
    education: 'school',
    work: 'work',
    project: 'folder',
    skill: 'psychology',
    introduction: 'article',
    custom: 'extension',
  }
</script>

<template>
  <div
    class="h-screen flex flex-col overflow-hidden bg-[#fbf9f5] text-[#1b1c1a] antialiased"
    style="font-family: Inter, sans-serif"
  >
    <!-- TopAppBar — Stitch hairline + JetBrains Mono -->
    <header
      class="h-14 shrink-0 bg-[#fbf9f5]/80 backdrop-blur-sm border-b border-[#c7c7c0] flex items-center justify-between px-4 lg:px-6 sticky top-0 z-40"
    >
      <!-- Left -->
      <div class="flex items-center gap-4 lg:gap-6 shrink-0">
        <router-link
          to="/"
          class="font-[Newsreader] text-[20px] font-semibold tracking-tight text-black leading-none"
          >Resume Atelier</router-link
        >
        <nav class="hidden md:flex ml-2 lg:ml-8 gap-6 h-14 items-center">
          <router-link
            to="/workbench"
            class="text-[13px] font-medium text-[#464742] hover:text-black transition-colors"
            >Workbench</router-link
          >
          <a
            class="text-[13px] font-semibold text-black border-b-2 border-black h-14 flex items-center"
            >Editor</a
          >
        </nav>
      </div>

      <!-- Center — Split control (detail screen) -->
      <div class="hidden lg:flex flex-1 justify-center h-full items-center">
        <div class="flex items-center gap-1 bg-[#f5f3f0] rounded-full p-1 border border-[#eae8e3]">
          <button
            @click="viewMode = 'preview'"
            :class="[
              'px-4 py-1.5 rounded-full text-[13px] font-medium transition-colors',
              viewMode === 'preview'
                ? 'bg-white text-black shadow-sm'
                : 'text-[#464742] hover:text-black',
            ]"
          >
            {{ t('editor.preview') }}
          </button>
          <button
            @click="viewMode = 'split'"
            :class="[
              'px-4 py-1.5 rounded-full text-[13px] font-medium transition-colors',
              viewMode === 'split'
                ? 'bg-white text-black shadow-sm'
                : 'text-[#464742] hover:text-black',
            ]"
          >
            {{ t('editor.split') }}
          </button>
          <button
            @click="viewMode = 'focus'"
            :class="[
              'px-4 py-1.5 rounded-full text-[13px] font-medium transition-colors',
              viewMode === 'focus'
                ? 'bg-white text-black shadow-sm'
                : 'text-[#464742] hover:text-black',
            ]"
          >
            {{ t('editor.focus') }}
          </button>
        </div>
      </div>

      <!-- Right -->
      <div class="flex items-center gap-2 lg:gap-4 shrink-0">
        <!-- Zoom + page — JetBrains Mono caption -->
        <div class="hidden sm:flex items-center gap-1 border-r border-[#c7c7c0] pr-3 lg:pr-4 mr-1">
          <button
            @click="setZoom(-10)"
            class="w-7 h-7 flex items-center justify-center text-[#464742] hover:text-black transition-colors rounded hover:bg-[#efeeea]"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">remove</span>
          </button>
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium w-12 text-center tabular-nums"
            >{{ zoom }}%</span
          >
          <button
            @click="setZoom(10)"
            class="w-7 h-7 flex items-center justify-center text-[#464742] hover:text-black transition-colors rounded hover:bg-[#efeeea]"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">add</span>
          </button>
          <div class="hidden lg:block h-4 w-px bg-[#c7c7c0] mx-2"></div>
          <span
            class="hidden lg:inline font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#464742] whitespace-nowrap"
            >{{ t('editor.page') }}</span
          >
        </div>

        <span
          class="hidden lg:inline font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium"
          :class="saving ? 'text-[#FF3B1F]' : 'text-[#8a8a87]'"
        >
          {{ saving ? t('editor.saving') : t('editor.saved') }}
        </span>

        <button
          @click="preview"
          class="hidden lg:inline-flex font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#464742] hover:text-black px-3 py-2"
        >
          Refresh
        </button>
        <button
          @click="shareOpen = true"
          class="inline-flex items-center gap-1.5 h-8 lg:h-9 px-4 lg:px-5 rounded-full bg-white border border-[#c7c7c0] font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-black hover:border-black transition-colors shrink-0"
        >
          <span class="material-symbols-outlined text-[16px] leading-none">share</span>
          <span>分享</span>
        </button>
        <button
          @click="exportPdf"
          class="bg-black text-white px-4 lg:px-5 h-8 lg:h-9 rounded-full font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium hover:opacity-90 transition-opacity flex items-center gap-2 shrink-0"
        >
          <span>{{ t('editor.export') }}</span>
          <span class="material-symbols-outlined text-[16px] leading-none">auto_awesome</span>
        </button>
      </div>
    </header>

    <!-- Main 4-col layout -->
    <main class="flex-1 flex overflow-hidden min-h-0">
      <!-- Marginalia 64px — JetBrains Mono hairline numbering, hidden <1280 -->
      <aside
        class="hidden xl:flex w-16 shrink-0 bg-[#fbf9f5] border-r border-[#eae8e3] flex-col items-center py-8 gap-6 overflow-hidden"
        aria-hidden="true"
      >
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#8a8a87] [writing-mode:vertical-lr] rotate-180"
          >MARGINALIA — 01</span
        >
        <div class="flex flex-col items-center gap-3 w-full px-2">
          <template v-for="(s, i) in store.current?.sections || []" :key="s.id">
            <button
              @click="activeId = s.id"
              :class="[
                'w-full flex flex-col items-center gap-1 py-2 rounded-md transition-colors',
                activeId === s.id
                  ? 'bg-[#efeeea] text-black'
                  : 'text-[#8a8a87] hover:text-black hover:bg-[#f5f3f0]',
              ]"
              :title="s.title"
            >
              <span
                class="font-[JetBrains_Mono] text-[11px] font-medium tabular-nums leading-none"
                >{{ String(i + 1).padStart(2, '0') }}</span
              >
              <span class="material-symbols-outlined text-[16px] leading-none opacity-60">{{
                sectionIcon[s.type] || 'article'
              }}</span>
            </button>
            <div
              v-if="i < (store.current?.sections.length || 0) - 1"
              class="w-px h-3 bg-[#eae8e3]"
            ></div>
          </template>
        </div>
        <div class="w-px flex-1 bg-[#eae8e3] min-h-[24px]"></div>
        <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.08em] text-[#c7c7c0]"
          >— ED —</span
        >
      </aside>

      <!-- Left Section Layers + Inspector — 360px (Stitch 380 rounded to 360 per spec, scroll) -->
      <aside
        v-show="!isPreview"
        :class="[
          'shrink-0 bg-[#fbf9f5] border-r border-[#c7c7c0] flex flex-col overflow-hidden',
          isFocus ? 'w-[380px] lg:w-[420px]' : 'w-[360px]',
          'hidden md:flex',
        ]"
      >
        <!-- Section Layers -->
        <div class="px-5 pt-5 pb-3 border-b border-[#eae8e3] shrink-0">
          <div class="flex items-center justify-between mb-3">
            <h3
              class="font-[JetBrains_Mono] text-[11px] tracking-[0.14em] font-medium uppercase text-[#464742]"
            >
              Section Layers
            </h3>
            <span
              class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87] tabular-nums"
              >{{ store.current?.sections.length || 0 }} SECTIONS</span
            >
          </div>
          <ul class="space-y-1 max-h-[28vh] overflow-y-auto pr-1 -mr-1 custom-scroll">
            <li
              v-for="s in store.current?.sections || []"
              :key="s.id"
              @click="activeId = s.id"
              :class="[
                'group flex items-center justify-between px-2.5 py-2 rounded-md cursor-pointer border-l-2 transition-colors',
                activeId === s.id
                  ? 'bg-[#f5f3f0] border-black text-black'
                  : 'border-transparent hover:bg-[#efeeea] text-[#464742] hover:text-black',
              ]"
            >
              <div class="flex items-center gap-2 min-w-0">
                <span
                  class="material-symbols-outlined text-[16px] text-[#8a8a87] cursor-grab shrink-0"
                  >drag_indicator</span
                >
                <span
                  class="text-[13px] font-medium truncate"
                  :class="activeId === s.id ? 'font-semibold' : ''"
                  >{{ s.title }}</span
                >
                <span
                  v-if="!s.visible"
                  class="font-[JetBrains_Mono] text-[10px] tracking-wide text-[#8a8a87] ml-1"
                  >· Hidden</span
                >
              </div>
              <div
                :class="[
                  'flex items-center gap-0.5 shrink-0',
                  activeId === s.id ? 'opacity-100' : 'opacity-0 group-hover:opacity-100',
                ]"
              >
                <button
                  @click.stop="toggleVisible(s)"
                  class="w-7 h-7 flex items-center justify-center rounded hover:bg-white transition-colors"
                >
                  <span
                    class="material-symbols-outlined text-[16px]"
                    :class="s.visible ? 'text-[#1b1c1a]' : 'text-[#8a8a87]'"
                    >{{ s.visible ? 'visibility' : 'visibility_off' }}</span
                  >
                </button>
                <button
                  @click.stop="removeSection(s)"
                  class="w-7 h-7 flex items-center justify-center rounded hover:bg-white text-[#8a8a87] hover:text-[#ba1a1a] transition-colors"
                >
                  <span class="material-symbols-outlined text-[16px]">delete</span>
                </button>
              </div>
            </li>
          </ul>
          <div class="flex gap-2 mt-3">
            <button
              @click="addSection('education')"
              class="flex-1 h-7 rounded-full border border-[#c7c7c0] bg-white text-[11px] font-[JetBrains_Mono] tracking-[0.06em] font-medium hover:border-black transition-colors flex items-center justify-center gap-1"
            >
              <span class="material-symbols-outlined text-[14px]">add</span>
              {{ t('editor.education') }}
            </button>
            <button
              @click="addSection('work')"
              class="flex-1 h-7 rounded-full border border-[#c7c7c0] bg-white text-[11px] font-[JetBrains_Mono] tracking-[0.06em] font-medium hover:border-black transition-colors flex items-center justify-center gap-1"
            >
              <span class="material-symbols-outlined text-[14px]">add</span> {{ t('editor.work') }}
            </button>
          </div>
        </div>

        <!-- Inspector — scrollable form -->
        <div class="flex-1 overflow-y-auto custom-scroll">
          <div v-if="activeSection" class="p-5 space-y-6">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F] shrink-0"></span>
                <h2 class="font-[Newsreader] text-[20px] font-semibold leading-none text-black">
                  {{ activeSection.title }}
                </h2>
              </div>
              <p class="font-[Inter] text-[13px] leading-[20px] text-[#464742]">
                Manage your {{ activeSection.title.toLowerCase() }} — hairline inputs with JetBrains
                Mono labels.
              </p>
            </div>

            <!-- Stitch form groups — Institution / Degree / Major / Dates / Description -->
            <!-- Profile -->
            <div v-if="activeSection.type === 'profile'" class="space-y-4 relative">
              <div class="space-y-1.5">
                <label
                  class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  >Full Name</label
                >
                <input
                  :value="activeSection.data?.name || ''"
                  @input="
                    updateField(activeSection.id, 'name', ($event.target as HTMLInputElement).value)
                  "
                  class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black placeholder-[#8a8a87] focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                  placeholder="Jane Doe"
                />
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Email</label
                  >
                  <input
                    :value="activeSection.data?.email || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'email',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="jane@atelier.io"
                  />
                </div>
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Phone</label
                  >
                  <input
                    :value="activeSection.data?.phone || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'phone',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="+1 415 …"
                  />
                </div>
              </div>
              <div class="space-y-1.5">
                <label
                  class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  >Location</label
                >
                <input
                  :value="activeSection.data?.location || ''"
                  @input="
                    updateField(
                      activeSection.id,
                      'location',
                      ($event.target as HTMLInputElement).value
                    )
                  "
                  class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                  placeholder="San Francisco, CA"
                />
              </div>
              <div class="space-y-1.5">
                <label
                  class="flex justify-between font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >
                  <span>Summary</span
                  ><span class="text-[#8a8a87] font-normal normal-case tracking-normal"
                    >Optional</span
                  >
                </label>
                <textarea
                  :value="activeSection.data?.summary || activeSection.data?.description || ''"
                  @input="
                    updateField(
                      activeSection.id,
                      'summary',
                      ($event.target as HTMLTextAreaElement).value
                    )
                  "
                  rows="4"
                  class="w-full p-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[13px] leading-[20px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors resize-none"
                  placeholder="Multi-disciplinary designer…"
                ></textarea>
              </div>
              <!-- AI Polish floating -->
              <button
                class="absolute -right-3 top-[42%] hidden lg:flex bg-white border border-[#c7c7c0] text-black font-[JetBrains_Mono] text-[10px] tracking-[0.06em] font-medium px-3 py-1.5 rounded-full items-center gap-1.5 shadow-sm hover:bg-[#f5f3f0] transition-colors"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span> AI Polish
              </button>
            </div>

            <!-- Education -->
            <div v-else-if="activeSection.type === 'education'" class="space-y-4 relative">
              <div class="space-y-1.5">
                <label
                  class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  >Institution</label
                >
                <input
                  :value="activeSection.data?.institution || activeSection.data?.school || ''"
                  @input="
                    updateField(
                      activeSection.id,
                      'institution',
                      ($event.target as HTMLInputElement).value
                    )
                  "
                  class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                  placeholder="University of Design"
                />
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Degree</label
                  >
                  <input
                    :value="activeSection.data?.degree || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'degree',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="MFA"
                  />
                </div>
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Major</label
                  >
                  <input
                    :value="activeSection.data?.major || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'major',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="Interaction Design"
                  />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Start Date</label
                  >
                  <input
                    :value="activeSection.data?.startDate || activeSection.data?.start || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'startDate',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="Sep 2018"
                  />
                </div>
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >End Date</label
                  >
                  <input
                    :value="activeSection.data?.endDate || activeSection.data?.end || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'endDate',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="May 2020"
                  />
                </div>
              </div>
              <div class="space-y-1.5">
                <label
                  class="flex justify-between font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >
                  <span>Description</span
                  ><span class="text-[#8a8a87] font-normal normal-case tracking-normal"
                    >Optional</span
                  >
                </label>
                <textarea
                  :value="activeSection.data?.description || ''"
                  @input="
                    updateField(
                      activeSection.id,
                      'description',
                      ($event.target as HTMLTextAreaElement).value
                    )
                  "
                  rows="4"
                  class="w-full p-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[13px] leading-[20px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors resize-none"
                  placeholder="Focused on human-computer interaction…"
                >
Focused on human-computer interaction, tangible interfaces, and generative typography.</textarea>
              </div>
              <button
                class="absolute -right-3 top-[85%] hidden lg:flex bg-white border border-[#c7c7c0] text-black font-[JetBrains_Mono] text-[10px] tracking-[0.06em] font-medium px-3 py-1.5 rounded-full items-center gap-1.5 shadow-sm hover:bg-[#f5f3f0] transition-colors"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span> AI Polish
              </button>
            </div>

            <!-- Work -->
            <div v-else-if="activeSection.type === 'work'" class="space-y-4">
              <div class="space-y-1.5">
                <label
                  class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  >Company</label
                >
                <input
                  :value="activeSection.data?.company || ''"
                  @input="
                    updateField(
                      activeSection.id,
                      'company',
                      ($event.target as HTMLInputElement).value
                    )
                  "
                  class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                  placeholder="Atelier Studio"
                />
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Position</label
                  >
                  <input
                    :value="activeSection.data?.position || activeSection.data?.title || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'position',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="Senior Designer"
                  />
                </div>
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Location</label
                  >
                  <input
                    :value="activeSection.data?.location || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'location',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="Remote"
                  />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >Start</label
                  >
                  <input
                    :value="activeSection.data?.startDate || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'startDate',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="Jun 2020"
                  />
                </div>
                <div class="space-y-1.5">
                  <label
                    class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                    >End</label
                  >
                  <input
                    :value="activeSection.data?.endDate || ''"
                    @input="
                      updateField(
                        activeSection.id,
                        'endDate',
                        ($event.target as HTMLInputElement).value
                      )
                    "
                    class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                    placeholder="Present"
                  />
                </div>
              </div>
              <div class="space-y-1.5">
                <label
                  class="flex justify-between font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  ><span>Achievements</span
                  ><span class="text-[#8a8a87] font-normal normal-case tracking-normal">
                    bullets separated by ;</span
                  ></label
                >
                <textarea
                  :value="
                    (activeSection.data?.highlights || []).join('; ') ||
                    activeSection.data?.description ||
                    ''
                  "
                  @input="
                    updateField(
                      activeSection.id,
                      'description',
                      ($event.target as HTMLTextAreaElement).value
                    )
                  "
                  rows="4"
                  class="w-full p-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[13px] leading-[20px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors resize-none"
                  placeholder="Led design system…"
                ></textarea>
              </div>
            </div>

            <!-- Project / Skill / Other generic -->
            <div v-else class="space-y-4">
              <div class="space-y-1.5">
                <label
                  class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  >Title</label
                >
                <input
                  :value="activeSection.title"
                  @input="activeSection.title = ($event.target as HTMLInputElement).value"
                  class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
                />
              </div>
              <div class="space-y-1.5">
                <label
                  class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                  >Content (JSON)</label
                >
                <textarea
                  :value="JSON.stringify(activeSection.data, null, 2)"
                  @input="handleJsonInput"
                  rows="8"
                  class="w-full p-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[JetBrains_Mono] text-[12px] leading-[18px] text-black focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors resize-none"
                ></textarea>
              </div>
              <p class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87]">
                可编辑: {{ activeSection.type }} — Stitch hairline card
              </p>
            </div>

            <button
              @click="addSection(activeSection.type)"
              class="w-full h-9 mt-2 border border-[#c7c7c0] rounded-full font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-black hover:bg-white hover:border-black transition-colors flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-[16px]">add</span>
              {{ t('editor.addSection') }} {{ activeSection.title }}
            </button>

            <!-- Collapsible overview of other sections -->
            <div class="pt-4 border-t border-[#eae8e3] space-y-2">
              <p
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.12em] uppercase text-[#8a8a87]"
              >
                All Sections
              </p>
              <div class="space-y-2">
                <details
                  v-for="s in store.current?.sections || []"
                  :key="s.id + '_sum'"
                  class="group bg-white border border-[#eae8e3] rounded-[10px] open:border-[#c7c7c0] transition-colors"
                >
                  <summary
                    class="list-none flex items-center justify-between px-3 py-2.5 cursor-pointer"
                  >
                    <span class="text-[13px] font-medium text-black flex items-center gap-2"
                      ><span class="material-symbols-outlined text-[16px] text-[#8a8a87]">{{
                        sectionIcon[s.type] || 'article'
                      }}</span
                      >{{ s.title }}</span
                    >
                    <span
                      class="material-symbols-outlined text-[18px] text-[#8a8a87] group-open:rotate-180 transition-transform"
                      >expand_more</span
                    >
                  </summary>
                  <div
                    class="px-3 pb-3 pt-1 border-t border-[#eae8e3] text-[12px] text-[#464742] font-[Inter] leading-relaxed"
                  >
                    <pre class="whitespace-pre-wrap break-words font-[Inter] text-[12px]">{{
                      JSON.stringify(s.data, null, 2).slice(0, 300)
                    }}</pre>
                  </div>
                </details>
              </div>
            </div>
          </div>
          <div v-else class="p-8 text-center">
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] text-[#8a8a87]">
              No section selected
            </p>
          </div>
        </div>

        <!-- Bottom settings hint -->
        <div class="p-4 border-t border-[#eae8e3] shrink-0 bg-[#fbf9f5]">
          <router-link
            to="/templates"
            class="flex items-center gap-2 text-[13px] text-[#464742] hover:text-black"
          >
            <span class="material-symbols-outlined text-[18px]">tune</span> Switch Template
          </router-link>
        </div>
        <!-- Atelier AI: SectionEditor with resumeId passthrough for rich-text capsules (hairline Pill) -->
        <div class="hidden">
          <SectionEditor
            v-if="store.current?.sections"
            :sections="store.current.sections"
            :resumeId="id"
            @update:sections="(v) => store.current && (store.current.sections = v)"
          />
        </div>
      </aside>

      <!-- Center Canvas — Warm Desk #EDE9E3 + A4 210x297 shadow-paper -->
      <section class="flex-1 bg-[#EDE9E3] relative flex flex-col overflow-hidden min-w-0">
        <!-- Floating polishing pill (when saving) -->
        <div
          v-if="saving"
          class="absolute top-4 right-4 lg:right-6 z-20 bg-black text-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium px-4 py-2 rounded-full flex items-center gap-2 shadow-xl"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F] animate-pulse"></span> POLISHING…
        </div>

        <!-- Zoom / focus helpers -->
        <div class="absolute top-4 left-4 z-10 hidden lg:flex items-center gap-2">
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#8a8a87] bg-white/80 backdrop-blur px-2.5 py-1 rounded-full border border-[#eae8e3]"
            >A4 · 210×297</span
          >
          <span
            v-if="isFocus"
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#8a8a87] bg-[#FF3B1F]/10 text-[#93000a] px-2.5 py-1 rounded-full border border-[#ffb4a6]"
            >Focus Mode</span
          >
        </div>

        <!-- Scrollable desk -->
        <div
          class="flex-1 overflow-auto p-4 lg:p-8 xl:p-12 flex justify-center items-start custom-scroll"
          :class="isFocus ? 'bg-[#f5f3f0]' : ''"
        >
          <div
            class="bg-[#FCFCF9] shrink-0 relative flex overflow-hidden shadow-paper"
            :style="{
              width: '210mm',
              minHeight: '297mm',
              transform: `scale(${zoom / 100})`,
              transformOrigin: 'top center',
              marginBottom: zoom !== 100 ? `calc((1 - ${zoom / 100}) * 297mm * -1)` : undefined,
            }"
          >
            <!-- Marginalia rail inside A4 — w-16 hairline -->
            <div
              class="w-16 shrink-0 border-r border-[#eae8e3]/60 flex flex-col items-center py-12 gap-4 hidden sm:flex"
            >
              <span class="font-[Newsreader] text-[12px] font-medium text-[#c7c7c0] tracking-widest"
                >01</span
              >
              <div class="w-px flex-1 bg-[#eae8e3]/60"></div>
              <span
                class="font-[JetBrains_Mono] text-[10px] tracking-[0.14em] text-[#c7c7c0] [writing-mode:vertical-lr]"
                >ATELIER</span
              >
            </div>

            <!-- Content area — py-16 px-12 space-y-12 -->
            <div
              class="flex-1 py-10 lg:py-16 px-8 lg:px-12 space-y-8 lg:space-y-10 text-[#0F0F0E] min-w-0"
            >
              <!-- Rendered html from backend -->
              <div
                v-if="html"
                v-html="html"
                class="prose max-w-none prose-p:leading-relaxed prose-headings:font-[Newsreader]"
              ></div>

              <!-- Fallback mock (when no html) — Jane Doe Stitch -->
              <template v-else>
                <!-- Header -->
                <div class="border-b border-black pb-6 lg:pb-8">
                  <h1
                    class="font-[Newsreader] text-[40px] font-medium tracking-tight leading-none text-black"
                  >
                    {{
                      activeSection?.type === 'profile'
                        ? store.current?.sections.find((s) => s.type === 'profile')?.data?.name ||
                          'Jane Doe'
                        : 'Jane Doe'
                    }}
                  </h1>
                  <p
                    class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#464742] mt-3 flex flex-wrap gap-2 lg:gap-4"
                  >
                    <span>{{
                      store.current?.sections.find((s) => s.type === 'profile')?.data?.email ||
                      'jane.doe@atelier.io'
                    }}</span>
                    <span class="text-[#c7c7c0]">·</span>
                    <span>{{
                      store.current?.sections.find((s) => s.type === 'profile')?.data?.location ||
                      'San Francisco, CA'
                    }}</span>
                    <span class="text-[#c7c7c0]">·</span>
                    <span>portfolio.design</span>
                  </p>
                </div>

                <!-- Profile dimmed -->
                <div class="opacity-60">
                  <h2
                    class="font-[Newsreader] text-[12px] font-semibold tracking-[0.16em] uppercase text-black mb-3"
                  >
                    {{ t('editor.profile') }}
                  </h2>
                  <p class="font-[Inter] text-[14px] leading-[22px] text-black">
                    {{
                      store.current?.sections.find((s) => s.type === 'profile')?.data?.summary ||
                      'A multi-disciplinary designer focusing on digital craft and systematic approaches to complex interfaces. Over 8 years of experience building tools for creatives.'
                    }}
                  </p>
                </div>

                <!-- Education active highlight with left accent -->
                <div class="relative">
                  <div
                    class="absolute -left-8 lg:-left-12 top-0 h-full w-[3px] bg-black hidden sm:block"
                  ></div>
                  <h2
                    class="font-[Newsreader] text-[12px] font-semibold tracking-[0.16em] uppercase text-black mb-4"
                  >
                    {{ t('editor.education') }}
                  </h2>
                  <div
                    v-for="s in store.current?.sections.filter((s) => s.type === 'education') || [
                      {
                        data: {
                          institution: 'University of Design',
                          degree: 'MFA',
                          major: 'Interaction Design',
                          startDate: 'Sep 2018',
                          endDate: 'May 2020',
                          description:
                            'Focused on human-computer interaction, tangible interfaces, and generative typography.',
                        },
                      } as any,
                    ]"
                    :key="s.id"
                    class="mb-6"
                  >
                    <div class="flex justify-between items-baseline gap-4 mb-1">
                      <h3 class="font-[Inter] text-[15px] font-semibold text-black">
                        {{ s.data?.institution || s.data?.school || 'University of Design' }}
                      </h3>
                      <span
                        class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#464742] shrink-0"
                        >{{
                          (s.data?.startDate || 'Sep 2018') +
                          ' — ' +
                          (s.data?.endDate || 'May 2020')
                        }}</span
                      >
                    </div>
                    <p class="font-[Inter] text-[13px] italic text-[#464742] mb-1.5">
                      {{
                        (s.data?.degree || 'MFA') + ' ' + (s.data?.major || 'Interaction Design')
                      }}
                    </p>
                    <p class="font-[Inter] text-[13px] leading-[20px] text-black">
                      {{
                        s.data?.description ||
                        'Focused on human-computer interaction, tangible interfaces, and generative typography.'
                      }}
                    </p>
                  </div>
                </div>

                <!-- Work skeleton -->
                <div class="opacity-60">
                  <h2
                    class="font-[Newsreader] text-[12px] font-semibold tracking-[0.16em] uppercase text-black mb-4"
                  >
                    {{ t('editor.work') }}
                  </h2>
                  <div
                    v-if="
                      (store.current?.sections.filter((s) => s.type === 'work').length || 0) > 0
                    "
                    class="space-y-4"
                  >
                    <div
                      v-for="w in store.current?.sections.filter((s) => s.type === 'work')"
                      :key="w.id"
                    >
                      <div class="h-4 bg-[#eae8e3]/60 rounded w-1/3 mb-2">
                        {{ w.data?.company }}
                      </div>
                      <div class="text-[13px] text-black">
                        {{ w.data?.position }} · {{ w.data?.startDate }} — {{ w.data?.endDate }}
                      </div>
                      <div class="text-[13px] text-[#464742] mt-1">{{ w.data?.description }}</div>
                    </div>
                  </div>
                  <div v-else class="space-y-3">
                    <div class="h-4 bg-[#eae8e3]/60 rounded w-1/3"></div>
                    <div class="h-3 bg-[#eae8e3]/60 rounded w-1/4"></div>
                    <div class="h-3 bg-[#f5f3f0] rounded w-full"></div>
                    <div class="h-3 bg-[#f5f3f0] rounded w-5/6"></div>
                  </div>
                </div>

                <div
                  v-if="!store.current"
                  class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] text-[#8a8a87] py-12 text-center border border-dashed border-[#c7c7c0] rounded-[10px]"
                >
                  预览加载中…
                </div>
              </template>

              <!-- Accent line preview for right panel -->
              <div
                v-if="store.current?.renderSettings?.accentColor"
                class="pt-6 border-t border-[#eae8e3] flex items-center gap-3"
              >
                <span
                  class="font-[JetBrains_Mono] text-[10px] tracking-[0.12em] uppercase text-[#8a8a87]"
                  >{{ t('editor.accent') }}</span
                >
                <span
                  class="w-6 h-6 rounded-full border border-black/10"
                  :style="{ background: store.current.renderSettings.accentColor }"
                ></span>
                <span class="font-[JetBrains_Mono] text-[11px]">{{
                  store.current.renderSettings.accentColor
                }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bottom zoom bar mobile -->
        <div
          class="lg:hidden h-10 shrink-0 bg-white border-t border-[#eae8e3] flex items-center justify-between px-4"
        >
          <div class="flex items-center gap-2">
            <button
              @click="setZoom(-10)"
              class="w-8 h-8 rounded-full border border-[#eae8e3] flex items-center justify-center"
            >
              <span class="material-symbols-outlined text-[18px]">remove</span>
            </button>
            <span class="font-[JetBrains_Mono] text-[11px] w-12 text-center">{{ zoom }}%</span>
            <button
              @click="setZoom(10)"
              class="w-8 h-8 rounded-full border border-[#eae8e3] flex items-center justify-center"
            >
              <span class="material-symbols-outlined text-[18px]">add</span>
            </button>
          </div>
          <button
            @click="mobileSheetOpen = !mobileSheetOpen"
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium px-3 py-1.5 rounded-full border border-black bg-black text-white"
          >
            {{ t('editor.renderSettings') }}
          </button>
        </div>
      </section>

      <!-- Right Typography Panel — 280px -->
      <aside
        :class="[
          'w-[280px] shrink-0 bg-[#fbf9f5] border-l border-[#c7c7c0] flex-col overflow-y-auto hidden lg:flex',
          isFocus ? 'opacity-40 pointer-events-none' : '',
        ]"
      >
        <div class="p-5 space-y-6">
          <div>
            <h3 class="font-[Newsreader] text-[16px] font-semibold text-black">
              {{ t('editor.renderSettings') }}
            </h3>
            <p
              class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#8a8a87] mt-1"
            >
              {{ t('editor.renderSettings') }} · hairline controls
            </p>
          </div>

          <div class="space-y-2">
            <label
              class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black block"
              >{{ t('editor.fontFamily') }}</label
            >
            <select
              :value="store.current?.renderSettings?.fontFamily || 'Inter'"
              @change="updateRender('fontFamily', ($event.target as HTMLSelectElement).value)"
              class="w-full h-9 px-3 bg-white border border-[#c7c7c0] rounded-[10px] font-[Inter] text-[13px] focus:outline-none focus:border-black"
            >
              <option value="Inter">Inter — Sans</option>
              <option value="Newsreader">Newsreader — Serif</option>
              <option value="JetBrains Mono">JetBrains Mono</option>
              <option value="Source Serif 4">Source Serif</option>
            </select>
            <p class="font-[JetBrains_Mono] text-[10px] text-[#8a8a87]">
              Stitch uses Newsreader / Inter / JetBrains Mono
            </p>
          </div>

          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >{{ t('editor.baseSize') }}</label
              >
              <span
                class="font-[JetBrains_Mono] text-[11px] tabular-nums text-black bg-white border border-[#eae8e3] px-2 py-0.5 rounded-full"
                >{{ store.current?.renderSettings?.baseFontSize || 14 }}px</span
              >
            </div>
            <input
              type="range"
              min="12"
              max="16"
              step="0.5"
              :value="store.current?.renderSettings?.baseFontSize || 14"
              @input="
                updateRender('baseFontSize', Number(($event.target as HTMLInputElement).value))
              "
              class="w-full accent-black"
            />
            <div class="flex justify-between font-[JetBrains_Mono] text-[10px] text-[#8a8a87]">
              <span>12</span><span>16</span>
            </div>
          </div>

          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >{{ t('editor.lineHeight') }}</label
              >
              <span
                class="font-[JetBrains_Mono] text-[11px] tabular-nums text-black bg-white border border-[#eae8e3] px-2 py-0.5 rounded-full"
                >{{ store.current?.renderSettings?.lineHeight || 1.5 }}</span
              >
            </div>
            <input
              type="range"
              min="1.2"
              max="2"
              step="0.1"
              :value="store.current?.renderSettings?.lineHeight || 1.5"
              @input="updateRender('lineHeight', Number(($event.target as HTMLInputElement).value))"
              class="w-full accent-black"
            />
          </div>

          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >{{ t('editor.sectionSpacing') }}</label
              >
              <span
                class="font-[JetBrains_Mono] text-[11px] tabular-nums text-black bg-white border border-[#eae8e3] px-2 py-0.5 rounded-full"
                >{{ store.current?.renderSettings?.sectionSpacing || 24 }}px</span
              >
            </div>
            <input
              type="range"
              min="12"
              max="48"
              step="4"
              :value="store.current?.renderSettings?.sectionSpacing || 24"
              @input="
                updateRender('sectionSpacing', Number(($event.target as HTMLInputElement).value))
              "
              class="w-full accent-black"
            />
          </div>

          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >{{ t('editor.pagePadding') }}</label
              >
              <span
                class="font-[JetBrains_Mono] text-[11px] tabular-nums text-black bg-white border border-[#eae8e3] px-2 py-0.5 rounded-full"
                >{{ store.current?.renderSettings?.pagePadding || 32 }}px</span
              >
            </div>
            <input
              type="range"
              min="16"
              max="64"
              step="4"
              :value="store.current?.renderSettings?.pagePadding || 32"
              @input="
                updateRender('pagePadding', Number(($event.target as HTMLInputElement).value))
              "
              class="w-full accent-black"
            />
          </div>

          <div class="space-y-2">
            <label
              class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black block"
              >{{ t('editor.accent') }}</label
            >
            <div class="flex items-center gap-3">
              <input
                type="color"
                :value="store.current?.renderSettings?.accentColor || '#0f0f0e'"
                @input="updateRender('accentColor', ($event.target as HTMLInputElement).value)"
                class="w-10 h-10 rounded-full overflow-hidden border border-[#c7c7c0] p-1 bg-white shrink-0"
              />
              <input
                :value="store.current?.renderSettings?.accentColor || '#0f0f0e'"
                @input="updateRender('accentColor', ($event.target as HTMLInputElement).value)"
                class="flex-1 h-9 px-3 bg-white border border-[#c7c7c0] rounded-[10px] font-[JetBrains_Mono] text-[13px] focus:outline-none focus:border-black"
                placeholder="#0f0f0e"
              />
            </div>
            <div class="flex gap-2">
              <button
                v-for="c in ['#0f0f0e', '#1a56db', '#057a55', '#c81e1e', '#ff3b1f']"
                :key="c"
                @click="updateRender('accentColor', c)"
                class="w-7 h-7 rounded-full border border-black/10"
                :style="{ background: c }"
              ></button>
            </div>
          </div>

          <!-- Auto one page -->
          <label
            class="flex items-center justify-between gap-3 py-3 border-t border-[#eae8e3] cursor-pointer"
          >
            <span
              class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
              >Auto One Page</span
            >
            <input
              type="checkbox"
              :checked="!!store.current?.renderSettings?.autoOnePage"
              @change="updateRender('autoOnePage', ($event.target as HTMLInputElement).checked)"
              class="w-4 h-4 accent-black"
            />
          </label>

          <div class="space-y-2 pt-4 border-t border-[#eae8e3]">
            <label class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black block">Template</label>
            <div class="relative">
              <select
                :value="store.current?.templateId || ''"
                @change="switchTemplate(($event.target as HTMLSelectElement).value)"
                class="w-full h-9 pl-3 pr-8 bg-white border border-[#c7c7c0] rounded-[10px] font-[Inter] text-[13px] focus:outline-none focus:border-black appearance-none cursor-pointer"
              >
                <option value="" disabled>选择模板</option>
                <option v-for="tpl in templates" :key="tpl.id" :value="tpl.id">{{ tpl.name }}</option>
              </select>
              <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-[#777871] text-[16px] pointer-events-none">expand_more</span>
            </div>
            <router-link to="/templates" class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87] hover:text-black underline">浏览模板中心 →</router-link>
          </div>

          <div class="pt-2">
            <button
              @click="preview"
              class="w-full h-9 rounded-full bg-black text-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium hover:opacity-90"
            >
              Apply & Preview
            </button>
            <p class="font-[JetBrains_Mono] text-[10px] leading-4 text-[#8a8a87] mt-2 text-center">
              Autosave 900ms · hairline 1px · JetBrains Mono 11px
            </p>
          </div>
        </div>
      </aside>
    </main>

    <!-- Mobile: Left drawer as bottom sheet for sections -->
    <div v-if="!isPreview" class="md:hidden fixed bottom-0 inset-x-0 z-30 pointer-events-none">
      <div
        v-if="mobileSheetOpen"
        @click="mobileSheetOpen = false"
        class="absolute inset-0 bg-black/20 pointer-events-auto"
      ></div>
      <div
        :class="[
          'bg-[#fbf9f5] border-t border-[#c7c7c0] rounded-t-[16px] max-h-[68vh] flex flex-col pointer-events-auto transition-transform duration-300',
          mobileSheetOpen ? 'translate-y-0' : 'translate-y-[calc(100%-44px)]',
        ]"
      >
        <button
          @click="mobileSheetOpen = !mobileSheetOpen"
          class="h-11 flex items-center justify-center gap-2 border-b border-[#eae8e3] shrink-0"
        >
          <span class="w-8 h-1 rounded-full bg-[#c7c7c0]"></span>
          <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium">{{
            activeSection?.title || 'Sections'
          }}</span>
          <span class="material-symbols-outlined text-[18px]">{{
            mobileSheetOpen ? 'expand_more' : 'expand_less'
          }}</span>
        </button>
        <div class="overflow-auto p-4 space-y-3 flex-1">
          <div class="flex gap-2 overflow-x-auto pb-2">
            <button
              v-for="s in store.current?.sections || []"
              :key="s.id + '_m'"
              @click="activeId = s.id; mobileSheetOpen = true"
              :class="[
                'px-3 py-1.5 rounded-full text-[13px] font-medium whitespace-nowrap border',
                activeId === s.id
                  ? 'bg-black text-white border-black'
                  : 'bg-white border-[#eae8e3] text-black',
              ]"
            >
              {{ s.title }}
            </button>
          </div>
          <template v-if="activeSection">
            <div class="space-y-3">
              <input
                :value="activeSection.data?.institution || activeSection.data?.name || ''"
                @input="
                  updateField(
                    activeSection.id,
                    activeSection.type === 'profile' ? 'name' : 'institution',
                    ($event.target as HTMLInputElement).value
                  )
                "
                class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] text-[14px]"
                :placeholder="activeSection.title"
              />
              <textarea
                :value="activeSection.data?.description || activeSection.data?.summary || ''"
                @input="
                  updateField(
                    activeSection.id,
                    'description',
                    ($event.target as HTMLTextAreaElement).value
                  )
                "
                rows="3"
                class="w-full p-3 bg-white rounded-[10px] border border-[#c7c7c0] text-[13px] resize-none"
                placeholder="Description"
              ></textarea>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- Mobile Typography BottomSheet (b51102 reserved) -->
    <div
      v-if="mobileSheetOpen"
      class="lg:hidden fixed inset-0 z-40 flex items-end justify-center p-4 pointer-events-none"
    >
      <!-- duplicate sheet for RenderSettings when not in section mode - we reuse same toggle, so hide -->
    </div>

    <!-- Share Atelier Modal -->
    <AtModal v-model="shareOpen">
      <div class="flex items-center justify-between mb-5">
        <div>
          <h3 class="font-[Newsreader] text-[18px] font-semibold leading-none text-black">
            分享设置
          </h3>
          <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] text-[#8a8a87] mt-1">
            Share — hairline · pill
          </p>
        </div>
        <button
          @click="shareOpen = false"
          class="w-8 h-8 rounded-full border border-[#eae8e3] flex items-center justify-center hover:border-black transition-colors"
        >
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
      </div>

      <div class="space-y-5">
        <!-- 隐藏联系方式 -->
        <label
          class="flex items-center justify-between gap-3 py-3 border-y border-[#eae8e3] cursor-pointer"
        >
          <span
            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
            >隐藏联系方式</span
          >
          <input type="checkbox" v-model="shareHideContact" class="w-4 h-4 accent-black" />
        </label>

        <!-- 过期时间 -->
        <div class="space-y-1.5">
          <label
            class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
            >过期时间</label
          >
          <input
            type="date"
            v-model="shareExpiresAt"
            class="w-full h-[38px] px-3 bg-white rounded-[10px] border border-[#c7c7c0] font-[Inter] text-[14px] text-black placeholder-[#8a8a87] focus:outline-none focus:border-black focus:[border-width:1.5px] transition-colors"
          />
          <p class="font-[JetBrains_Mono] text-[10px] leading-4 text-[#8a8a87]">
            留空为永久有效 · 格式 YYYY-MM-DD
          </p>
        </div>

        <!-- 分享链接 -->
        <div v-if="shareData" class="space-y-2">
          <label
            class="block font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
            >分享链接</label
          >
          <div class="flex items-center gap-2">
            <div
              class="flex-1 h-9 px-3 bg-[#f5f3f0] border border-[#eae8e3] rounded-[10px] font-[JetBrains_Mono] text-[12px] text-black flex items-center truncate overflow-hidden select-all"
            >
              {{ shareUrl }}
            </div>
            <button
              @click="copyShareLink"
              class="h-9 px-4 rounded-full bg-white border border-[#c7c7c0] font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-black hover:border-black hover:bg-[#f5f3f0] transition-colors shrink-0"
            >
              复制
            </button>
          </div>
          <p class="font-[JetBrains_Mono] text-[10px] text-[#8a8a87]">
            任何人可通过此链接只读访问（公开 HTML）
          </p>
        </div>
        <div
          v-else
          class="py-4 text-center font-[JetBrains_Mono] text-[11px] tracking-[0.06em] text-[#8a8a87] border border-dashed border-[#c7c7c0] rounded-[10px]"
        >
          尚未创建分享
        </div>

        <!-- 操作 -->
        <div class="flex gap-2 pt-2">
          <button
            @click="createShare"
            :disabled="shareLoading"
            class="flex-1 h-9 rounded-full bg-black text-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium hover:opacity-90 disabled:opacity-50 transition-opacity flex items-center justify-center gap-2"
          >
            <span
              v-if="shareLoading"
              class="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"
            ></span>
            <span>{{ shareData ? '轮换链接' : '创建分享' }}</span>
          </button>
          <button
            v-if="shareData"
            @click="closeShare"
            :disabled="shareLoading"
            class="h-9 px-5 rounded-full border border-[#c7c7c0] bg-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#464742] hover:border-black hover:text-black disabled:opacity-50 transition-colors"
          >
            关闭分享
          </button>
        </div>
        <p class="font-[JetBrains_Mono] text-[10px] leading-4 text-[#8a8a87] text-center">
          创建/轮换 调用 POST /resumes/{id}/share · 关闭调用 DELETE
        </p>
      </div>
    </AtModal>

    <!-- Local toast for Editor (outside AtelierLayout) -->
    <Transition name="toast">
      <div
        v-if="toastState.text"
        class="fixed bottom-6 left-1/2 -translate-x-1/2 bg-black text-white text-[13px] px-4 py-2 rounded-full shadow-xl z-50"
      >
        {{ toastState.text }}
      </div>
    </Transition>
  </div>
</template>

<style scoped>
  .custom-scroll::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  .custom-scroll::-webkit-scrollbar-thumb {
    background: #c7c7c0;
    border-radius: 9999px;
  }
  .shadow-paper {
    box-shadow:
      0px 4px 24px rgba(0, 0, 0, 0.04),
      0px 2px 8px rgba(0, 0, 0, 0.02);
  }
  .material-symbols-outlined {
    font-variation-settings:
      'FILL' 0,
      'wght' 300,
      'GRAD' 0,
      'opsz' 20;
  }
  .toast-enter-active {
    transition: all 200ms cubic-bezier(0.16, 1, 0.3, 1);
  }
  .toast-leave-active {
    transition: all 180ms ease;
  }
  .toast-enter-from {
    opacity: 0;
    transform: translate(-50%, 8px);
  }
  .toast-leave-to {
    opacity: 0;
    transform: translate(-50%, 4px);
  }
</style>
