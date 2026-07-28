<template>
  <MainLayout>
    <main class="max-w-[1440px] mx-auto px-margin-page py-stack-lg">
      <header class="flex justify-between items-end mb-stack-lg gap-gutter">
        <div class="flex flex-col">
          <h1 class="text-headline-md font-headline-md text-on-surface">
            通知中心
          </h1>
          <p class="text-body-md text-on-surface-variant mt-1">
            查看简历导出、AI 点评、头像优化等消息
          </p>
        </div>
        <div class="flex gap-2">
          <button
            class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
            @click="markAllRead"
          >
            全部已读
          </button>
          <button
            class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2"
            @click="clearAll"
          >
            清空
          </button>
        </div>
      </header>

      <div class="max-w-[1080px] mx-auto w-full flex flex-col gap-stack-lg">
        <div class="flex gap-2 bg-surface-container-lowest p-2 rounded-xl border border-outline-variant/30 w-fit">
          <button
            :class="[
              'flex items-center gap-2 px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
              filter === 'all'
                ? 'bg-primary text-on-primary'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            ]"
            @click="filter = 'all'"
          >
            全部
            <span
              :class="[
                'min-w-[18px] h-[18px] px-1.5 rounded-full text-[11px] font-bold inline-flex items-center justify-center',
                filter === 'all' ? 'bg-white/25 text-white' : 'bg-surface-container text-on-surface-variant'
              ]"
            >
              {{ notifications.length }}
            </span>
          </button>
          <button
            :class="[
              'flex items-center gap-2 px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
              filter === 'unread'
                ? 'bg-primary text-on-primary'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            ]"
            @click="filter = 'unread'"
          >
            未读
            <span
              :class="[
                'min-w-[18px] h-[18px] px-1.5 rounded-full text-[11px] font-bold inline-flex items-center justify-center',
                filter === 'unread' ? 'bg-white/25 text-white' : 'bg-surface-container text-on-surface-variant'
              ]"
            >
              {{ unreadCount }}
            </span>
          </button>
        </div>

        <div class="flex flex-col gap-stack-md">
          <div
            v-for="item in filteredNotifications"
            :key="item.id"
            :class="[
              'bg-surface-container-lowest rounded-xl border p-5 flex items-start gap-4 hover:shadow-sm transition-all cursor-pointer',
              item.read ? 'border-outline-variant' : 'border-primary/30 bg-gradient-to-r from-primary/5 to-surface-container-lowest'
            ]"
            @click="markRead(item.id)"
          >
            <div
              class="w-11 h-11 rounded-lg flex items-center justify-center flex-shrink-0"
              :class="iconBgClass(item.type)"
            >
              <el-icon size="20">
                <component :is="iconOf(item.type)" />
              </el-icon>
            </div>
            <div class="flex-1 min-w-0 flex flex-col gap-1.5">
              <div class="flex justify-between items-center gap-3">
                <span class="text-body-md font-semibold text-on-surface">
                  {{ typeLabel(item.type) }}
                </span>
                <span class="text-label-md text-on-surface-variant whitespace-nowrap">
                  {{ formatTime(item.time) }}
                </span>
              </div>
              <p class="text-body-md text-on-surface-variant leading-relaxed m-0">
                {{ item.content }}
              </p>
            </div>
            <div
              v-if="!item.read"
              class="w-2 h-2 rounded-full bg-primary flex-shrink-0 mt-1.5"
            />
          </div>

          <div
            v-if="filteredNotifications.length === 0"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 flex flex-col items-center gap-4 text-center text-on-surface-variant"
          >
            <el-icon size="48">
              <Bell />
            </el-icon>
            <p class="text-body-md">
              暂无通知
            </p>
          </div>
        </div>
      </div>
    </main>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Bell, Document, Picture, MagicStick, InfoFilled } from '@element-plus/icons-vue'
import MainLayout from '@/components/layout/MainLayout.vue'

type NotificationType = 'pdf' | 'avatar' | 'ai' | 'system'

interface NotificationItem {
  id: string
  type: NotificationType
  content: string
  time: string
  read: boolean
}

const STORAGE_KEY = 'resume_notifications'

const filter = ref<'all' | 'unread'>('all')
const notifications = ref<NotificationItem[]>([])

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const filteredNotifications = computed(() => {
  if (filter.value === 'unread') return notifications.value.filter(n => !n.read)
  return notifications.value
})

const typeLabels: Record<NotificationType, string> = {
  pdf: 'PDF 导出',
  avatar: '头像优化',
  ai: 'AI 点评',
  system: '系统通知'
}

const typeIcons: Record<NotificationType, any> = {
  pdf: Document,
  avatar: Picture,
  ai: MagicStick,
  system: InfoFilled
}

function typeLabel(type: NotificationType) {
  return typeLabels[type]
}

function iconOf(type: NotificationType) {
  return typeIcons[type]
}

function iconBgClass(type: NotificationType): string {
  switch (type) {
    case 'pdf':
      return 'bg-error-container text-error'
    case 'avatar':
      return 'bg-secondary-container text-secondary'
    case 'ai':
      return 'bg-primary/10 text-primary'
    case 'system':
      return 'bg-surface-container text-on-surface-variant'
    default:
      return 'bg-surface-container text-on-surface-variant'
  }
}

function formatTime(time: string) {
  return new Date(time).toLocaleString()
}

function loadNotifications() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      notifications.value = JSON.parse(raw)
      return
    } catch {
      /* ignore */
    }
  }
  notifications.value = sampleNotifications()
  saveNotifications()
}

function sampleNotifications(): NotificationItem[] {
  const now = new Date()
  return [
    {
      id: 'n1',
      type: 'pdf',
      content: '你的简历《Java 后端工程师》PDF 导出已完成，可前往下载中心下载。',
      time: new Date(now.getTime() - 1000 * 60 * 5).toISOString(),
      read: false
    },
    {
      id: 'n2',
      type: 'ai',
      content: 'AI 简历点评已生成，你的简历匹配度评分为 82 分。',
      time: new Date(now.getTime() - 1000 * 60 * 30).toISOString(),
      read: false
    },
    {
      id: 'n3',
      type: 'avatar',
      content: '头像一寸照优化已完成，可前往下载中心查看。',
      time: new Date(now.getTime() - 1000 * 60 * 60 * 2).toISOString(),
      read: true
    },
    {
      id: 'n4',
      type: 'system',
      content: '欢迎使用智能简历生成工具，开始创建你的第一份简历吧！',
      time: new Date(now.getTime() - 1000 * 60 * 60 * 24).toISOString(),
      read: true
    }
  ]
}

function saveNotifications() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(notifications.value))
}

function markRead(id: string) {
  const item = notifications.value.find(n => n.id === id)
  if (item && !item.read) {
    item.read = true
    saveNotifications()
  }
}

function markAllRead() {
  notifications.value.forEach(n => { n.read = true })
  saveNotifications()
}

function clearAll() {
  notifications.value = []
  saveNotifications()
}

onMounted(() => {
  loadNotifications()
})
</script>
