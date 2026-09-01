<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { toastState, pushToast } from '@/composables/useToast'
import LangSwitch from '@/components/atelier/LangSwitch.vue'
import client from '@/api/client'
import type { NotificationResponse } from '@/api/types'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isAdmin = ref(false)
const nav = computed(() => {
  const base = [
    { label: t('nav.workbench'), to: '/workbench', icon: 'edit_note', match: ['/workbench'] },
    {
      label: t('nav.resumes'),
      to: '/resumes',
      icon: 'description',
      match: ['/resumes', '/editor'],
    },
    { label: t('nav.templates'), to: '/templates', icon: 'style', match: ['/templates'] },
    { label: t('nav.ai'), to: '/ai/review/demo', icon: 'document_scanner', match: ['/ai'] },
    { label: t('nav.deliveries'), to: '/deliveries', icon: 'send', match: ['/deliveries'] },
    { label: t('nav.settings'), to: '/settings', icon: 'settings', match: ['/settings'] },
  ]
  if (isAdmin.value) base.push({ label: 'Admin', to: '/admin', icon: 'admin_panel_settings', match: ['/admin'] })
  return base
})
const active = computed(
  () => nav.value.find((n) => n.match.some((m) => route.path.startsWith(m)))?.label
)
function isActive(item: (typeof nav.value)[number]) {
  return active.value === item.label
}
function logout() {
  auth.logout()
  router.push('/login')
}

// ── notifications ──
const unreadCount = ref(0)
const notifications = ref<NotificationResponse[]>([])
const showNoti = ref(false)
const notiLoading = ref(false)
const notiDropdownRef = ref<HTMLElement | null>(null)
const notiMobileRef = ref<HTMLElement | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

function unwrapCount(raw: unknown): number {
  const anyRaw = raw as Record<string, unknown>
  if (!anyRaw || typeof anyRaw !== 'object') return 0
  // direct {count}
  if (typeof anyRaw.count === 'number') return anyRaw.count as number
  // ApiResp {code,data:{count}} or {data: number}
  const data = (anyRaw as { data?: unknown }).data
  if (typeof data === 'number') return data
  if (data && typeof data === 'object' && typeof (data as { count?: unknown }).count === 'number') {
    return (data as { count: number }).count
  }
  // nested ApiResp data is {data:{count}}? fallback
  if (data && typeof data === 'object' && (data as { data?: unknown }).data) {
    const inner = (data as { data: unknown }).data
    if (inner && typeof inner === 'object' && typeof (inner as { count?: unknown }).count === 'number') {
      return (inner as { count: number }).count
    }
  }
  return 0
}

function unwrapPage(raw: unknown): NotificationResponse[] {
  const anyRaw = raw as Record<string, unknown>
  if (!anyRaw || typeof anyRaw !== 'object') return []
  // cases: ApiResp<Page> => {data:{records:[]}}
  const tryExtract = (obj: unknown): NotificationResponse[] | null => {
    if (!obj || typeof obj !== 'object') return null
    const o = obj as { records?: unknown; data?: unknown }
    if (Array.isArray(o.records)) return o.records as NotificationResponse[]
    if (o.data && typeof o.data === 'object') {
      const inner = o.data as { records?: unknown }
      if (Array.isArray(inner.records)) return inner.records as NotificationResponse[]
    }
    return null
  }
  // direct Page?
  const direct = tryExtract(anyRaw)
  if (direct) return direct
  // ApiResp wrapper .data
  if ((anyRaw as { data?: unknown }).data) {
    const inner = tryExtract((anyRaw as { data: unknown }).data)
    if (inner) return inner
    // double wrap .data.data
    const d = (anyRaw as { data: unknown }).data
    if (d && typeof d === 'object' && (d as { data?: unknown }).data) {
      const dd = tryExtract((d as { data: unknown }).data)
      if (dd) return dd
    }
  }
  return []
}

async function fetchUnreadCount() {
  if (!auth.isLoggedIn && !localStorage.getItem('accessToken')) return
  try {
    const { data } = await client.get('/notifications/unread-count')
    unreadCount.value = unwrapCount(data as unknown)
  } catch {
    // silent, keep previous
  }
}

async function fetchLatest() {
  if (!auth.isLoggedIn && !localStorage.getItem('accessToken')) return
  notiLoading.value = true
  try {
    const { data } = await client.get('/notifications', {
      params: { page: 1, size: 5, unreadOnly: true },
    })
    notifications.value = unwrapPage(data as unknown).slice(0, 5)
  } catch {
    // fallback try without unreadOnly filter if backend differs
    try {
      const { data } = await client.get('/notifications', {
        params: { page: 1, size: 5, unreadOnly: 'true' },
      })
      notifications.value = unwrapPage(data as unknown).slice(0, 5)
    } catch {
      notifications.value = []
    }
  } finally {
    notiLoading.value = false
  }
}

function toggleNoti() {
  showNoti.value = !showNoti.value
  if (showNoti.value) fetchLatest()
}

function isUnread(n: NotificationResponse): boolean {
  if (typeof n.read === 'boolean') return !n.read
  return (n.readFlag ?? 1) === 0
}
async function markAllRead() {
  try {
    await client.put('/notifications/read-all')
    notifications.value = notifications.value.map((n) => ({ ...n, read: true, readFlag: 1 }))
    unreadCount.value = 0
    pushToast('已全部已读')
    await fetchUnreadCount()
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '操作失败'
    pushToast(msg)
  }
}

async function markRead(id: string) {
  try {
    await client.put(`/notifications/${id}/read`)
    notifications.value = notifications.value.map((n) => (n.id === id ? { ...n, read: true, readFlag: 1 } : n))
    await fetchUnreadCount()
    pushToast('已标为已读')
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '操作失败'
    pushToast(msg)
  }
}

async function deleteNoti(id: string) {
  try {
    await client.delete(`/notifications/${id}`)
    notifications.value = notifications.value.filter((n) => n.id !== id)
    await fetchUnreadCount()
    pushToast('已删除')
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '删除失败'
    pushToast(msg)
  }
}

function onDocClick(e: MouseEvent) {
  if (!showNoti.value) return
  const target = e.target as Node
  const inDesktop = notiDropdownRef.value?.contains(target)
  const inMobile = notiMobileRef.value?.contains(target)
  // also need to consider the bell button itself is inside wrapper, so if click inside wrapper don't close
  if (!inDesktop && !inMobile) {
    // check if click is on bell button (outside wrappers) -> handled by toggle; but our wrappers contain bell button, so above already includes it
    // if not inside any wrapper, close
    showNoti.value = false
  }
}

// need to include bell button inside wrapper for outside detection, so we wrap each bell+dropdown together

onMounted(async () => {
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
  document.addEventListener('click', onDocClick)
  // admin 探测：仅当已登录且非游客时试探，避免 401 噪音
  if (auth.isLoggedIn && !auth.isGuest) {
    try {
      const { data } = await client.get('/admin/users/stats')
      const code = (data as any)?.code
      if (code === undefined || code === 200) isAdmin.value = true
    } catch {
      isAdmin.value = false
    }
  }
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('click', onDocClick)
})
</script>
<template>
  <div class="min-h-screen bg-[#fbf9f5] flex">
    <!-- SideNav -->
    <aside
      class="w-64 shrink-0 h-screen sticky top-0 flex flex-col border-r border-[#c7c7c0] bg-[#fbf9f5] max-md:hidden"
    >
      <div class="p-6 border-b border-[#c7c7c0]">
        <router-link
          to="/"
          class="font-[Newsreader] text-[20px] font-semibold text-black tracking-tight"
          >Resume Atelier</router-link
        >
        <p
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-[#464742] mt-1"
        >
          AI PRECISION
        </p>
      </div>
      <nav class="flex-1 py-6 px-3 flex flex-col gap-1">
        <router-link
          v-for="item in nav"
          :key="item.label"
          :to="item.to"
          :class="[
            'flex items-center gap-3 px-4 py-3 rounded-lg text-[13px] transition-colors',
            isActive(item)
              ? 'bg-[#f5f3f0] text-black font-semibold border-r-2 border-black -mr-[1px]'
              : 'text-[#464742] hover:bg-[#e9e8e4] hover:text-black',
          ]"
        >
          <span class="material-symbols-outlined text-[20px]">{{ item.icon }}</span>
          {{ item.label }}
        </router-link>
      </nav>

      <!-- Notification bell — Atelier 白卡 hairline, sits above avatar -->
      <div ref="notiDropdownRef" class="px-3 pb-3 relative">
        <button
          @click.stop="toggleNoti"
          class="w-full h-10 bg-white rounded-xl flex items-center gap-3 px-3 hover:border-black transition-colors relative"
          style="border: 1px solid #eae8e3"
        >
          <span class="w-8 h-8 rounded-full bg-[#fbf9f5] flex items-center justify-center shrink-0" style="border: 1px solid #eae8e3">
            <span class="material-symbols-outlined text-[18px] text-[#1b1c1a]">notifications</span>
          </span>
          <span class="flex-1 text-left">
            <span class="font-[JetBrains_Mono] text-[12px] font-medium text-black tracking-[0.02em]">通知</span>
            <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase ml-2" style="color: #8a8a87">Notifications</span>
          </span>
          <span
            v-if="unreadCount > 0"
            class="min-w-5 h-5 px-1.5 rounded-full bg-[#FF3B1F] text-white font-[JetBrains_Mono] text-[11px] font-medium flex items-center justify-center shrink-0"
            >{{ unreadCount > 99 ? '99+' : unreadCount }}</span
          >
          <span v-else class="w-2 h-2 rounded-full bg-[#c7c7c0] shrink-0"></span>
        </button>

        <!-- Dropdown — latest 5 unreadOnly -->
        <transition name="fade">
          <div
            v-if="showNoti"
            class="absolute left-3 right-3 bottom-[52px] bg-white rounded-xl shadow-[0_8px_32px_rgba(0,0,0,0.12)] z-30 overflow-hidden max-h-[420px] flex flex-col"
            style="border: 1px solid #eae8e3"
            @click.stop
          >
            <!-- Header -->
            <div class="flex items-center justify-between px-4 py-3 border-b shrink-0" style="border-color: #eae8e3; background: #fbf9f5">
              <div class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span>
                <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium" style="color: #1b1c1a">最新通知</span>
                <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-1.5 py-0.5 rounded-sm bg-white hidden sm:inline" style="border: 1px solid #eae8e3; color: #8a8a87">{{ unreadCount }} 未读</span>
              </div>
              <button
                @click="markAllRead"
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2.5 py-1 rounded-full bg-white hover:border-black hover:text-black transition-colors"
                style="border: 1px solid #eae8e3; color: #464742"
              >
                全部已读
              </button>
            </div>

            <!-- List -->
            <div class="flex-1 overflow-y-auto">
              <div v-if="notiLoading" class="p-4 flex flex-col gap-3">
                <div v-for="i in 3" :key="i" class="h-14 bg-[#f5f3f0] rounded-lg animate-pulse"></div>
              </div>
              <div v-else-if="notifications.length" class="divide-y" style="border-color: #eae8e3">
                <div
                  v-for="n in notifications"
                  :key="n.id"
                  class="px-4 py-3 hover:bg-[#fbf9f5] transition-colors flex flex-col gap-2 group"
                >
                  <div class="flex items-start justify-between gap-2">
                    <p class="font-[Inter] text-[13px] font-medium leading-5 text-black line-clamp-1 flex-1">{{ n.title }}</p>
                    <span
                      v-if="isUnread(n)"
                      class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F] mt-2 shrink-0"
                    ></span>
                    <span
                      v-else
                      class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] uppercase px-1.5 py-0.5 rounded-sm bg-[#f5f3f0] shrink-0"
                      style="border: 1px solid #eae8e3; color: #8a8a87"
                      >已读</span
                    >
                  </div>
                  <p class="font-[Inter] text-[12px] leading-4 line-clamp-2" style="color: #464742">{{ n.content }}</p>
                  <div class="flex items-center justify-between">
                    <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.04em] uppercase" style="color: #8a8a87">{{ String(n.createdAt || '').slice(0, 16).replace('T', ' ') }}</span>
                    <div class="flex items-center gap-1">
                      <button
                        v-if="isUnread(n)"
                        @click="markRead(n.id)"
                        class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2 py-1 rounded-full bg-white hover:border-black hover:text-black transition-colors"
                        style="border: 1px solid #eae8e3; color: #464742"
                      >
                        标已读
                      </button>
                      <button
                        @click="deleteNoti(n.id)"
                        class="w-7 h-7 rounded-full bg-white flex items-center justify-center hover:border-black transition-colors"
                        style="border: 1px solid #eae8e3"
                        title="删除"
                      >
                        <span class="material-symbols-outlined text-[14px]" style="color: #8a8a87">delete</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="py-10 flex flex-col items-center gap-2 px-4 text-center">
                <span class="w-10 h-10 rounded-full bg-[#f5f3f0] flex items-center justify-center" style="border: 1px solid #eae8e3">
                  <span class="material-symbols-outlined text-[18px]" style="color: #8a8a87">notifications_off</span>
                </span>
                <p class="font-[Inter] text-[13px] font-medium text-black">暂无未读通知</p>
                <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color: #8a8a87">GET /notifications?unreadOnly=true</p>
              </div>
            </div>

            <!-- Footer hint -->
            <div class="px-4 py-2 border-t flex items-center justify-between shrink-0" style="border-color: #eae8e3; background: #fbf9f5">
              <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.04em] uppercase" style="color: #8a8a87">最新 5 条 · unreadOnly</span>
              <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.04em] uppercase" style="color: #8a8a87">轮询 30s</span>
            </div>
          </div>
        </transition>
      </div>

      <div class="p-4 border-t border-[#c7c7c0] space-y-3">
        <div class="flex items-center gap-3">
          <img
            v-if="auth.user?.avatarUrl"
            :src="auth.user.avatarUrl"
            class="w-8 h-8 rounded-full object-cover"
          />
          <div
            v-else
            class="w-8 h-8 rounded-full bg-black text-white flex items-center justify-center text-xs font-medium"
          >
            {{ (auth.user?.nickname || 'G')[0] }}
          </div>
          <div class="min-w-0">
            <p class="text-[13px] font-medium text-black truncate">
              {{ auth.user?.nickname || 'Guest' }}
            </p>
            <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] truncate">
              {{ auth.user?.isGuest ? '游客' : auth.user?.email || '' }}
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <LangSwitch />
          <button
            @click="logout"
            class="flex-1 h-8 rounded-full border border-[#c7c7c0] text-[13px] text-[#464742] hover:border-black hover:text-black transition-colors"
          >
            退出
          </button>
        </div>
      </div>
    </aside>

    <!-- Main -->
    <div class="flex-1 min-w-0 flex flex-col">
      <!-- Mobile top -->
      <header
        class="md:hidden h-14 bg-[#fbf9f5] border-b border-[#c7c7c0] flex items-center px-4 justify-between sticky top-0 z-10 gap-2"
      >
        <span class="font-[Newsreader] font-semibold">Resume Atelier</span>
        <div class="flex items-center gap-2">
          <div ref="notiMobileRef" class="relative">
            <button
              @click.stop="toggleNoti"
              class="w-8 h-8 rounded-full bg-white flex items-center justify-center relative"
              style="border: 1px solid #eae8e3"
            >
              <span class="material-symbols-outlined text-[18px] text-[#1b1c1a]">notifications</span>
              <span
                v-if="unreadCount > 0"
                class="absolute -top-1 -right-1 min-w-4 h-4 px-1 rounded-full bg-[#FF3B1F] text-white font-[JetBrains_Mono] text-[10px] font-medium flex items-center justify-center"
                >{{ unreadCount > 99 ? '99+' : unreadCount }}</span
              >
            </button>

            <transition name="fade">
              <div
                v-if="showNoti"
                class="absolute right-0 top-10 w-80 max-w-[86vw] bg-white rounded-xl shadow-[0_8px_32px_rgba(0,0,0,0.12)] z-30 overflow-hidden flex flex-col max-h-[70vh]"
                style="border: 1px solid #eae8e3"
                @click.stop
              >
                <div class="flex items-center justify-between px-4 py-3 border-b shrink-0" style="border-color: #eae8e3; background: #fbf9f5">
                  <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium" style="color: #1b1c1a">通知 · {{ unreadCount }} 未读</span>
                  <button
                    @click="markAllRead"
                    class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2.5 py-1 rounded-full bg-white"
                    style="border: 1px solid #eae8e3; color: #464742"
                  >
                    全部已读
                  </button>
                </div>
                <div class="flex-1 overflow-y-auto">
                  <div v-if="notiLoading" class="p-4 flex flex-col gap-3">
                    <div v-for="i in 3" :key="i" class="h-14 bg-[#f5f3f0] rounded-lg animate-pulse"></div>
                  </div>
                  <div v-else-if="notifications.length" class="divide-y" style="border-color: #eae8e3">
                    <div v-for="n in notifications" :key="n.id" class="px-4 py-3 flex flex-col gap-2">
                      <p class="font-[Inter] text-[13px] font-medium text-black">{{ n.title }}</p>
                      <p class="font-[Inter] text-[12px]" style="color: #464742">{{ n.content }}</p>
                      <div class="flex items-center justify-between">
                        <span class="font-[JetBrains_Mono] text-[10px]" style="color: #8a8a87">{{ String(n.createdAt || '').slice(0, 16).replace('T', ' ') }}</span>
                        <div class="flex gap-1">
                          <button
                            v-if="isUnread(n)"
                            @click="markRead(n.id)"
                            class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2 py-1 rounded-full bg-white"
                            style="border: 1px solid #eae8e3; color: #464742"
                          >
                            标已读
                          </button>
                          <button
                            @click="deleteNoti(n.id)"
                            class="w-7 h-7 rounded-full bg-white flex items-center justify-center"
                            style="border: 1px solid #eae8e3"
                          >
                            <span class="material-symbols-outlined text-[14px]" style="color: #8a8a87">delete</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-else class="py-8 text-center">
                    <p class="font-[JetBrains_Mono] text-[11px]" style="color: #8a8a87">暂无未读</p>
                  </div>
                </div>
              </div>
            </transition>
          </div>
          <button
            class="w-8 h-8 rounded-full bg-black text-white flex items-center justify-center"
            @click="logout"
          >
            <span class="material-symbols-outlined text-[18px]">logout</span>
          </button>
        </div>
      </header>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
    <!-- Toast -->
    <transition name="toast">
      <div
        v-if="toastState.text"
        class="fixed bottom-6 left-1/2 -translate-x-1/2 bg-black text-white text-[13px] px-4 py-2 rounded-full shadow-xl"
      >
        {{ toastState.text }}
      </div>
    </transition>
  </div>
</template>
<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 180ms cubic-bezier(0.16, 1, 0.3, 1);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
