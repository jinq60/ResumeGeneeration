<template>
  <main class="workbench-page py-stack-lg">
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
          :disabled="notifications.length === 0 || unreadCount === 0"
          @click="markAllRead"
        >
          全部已读
        </button>
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="notifications.length === 0 || clearing"
          @click="clearAll"
        >
          {{ clearing ? '清空中…' : '清空' }}
        </button>
      </div>
    </header>

    <div class="workbench-page w-full flex flex-col gap-stack-lg">
      <div class="flex gap-2 bg-surface-container-lowest p-2 rounded-xl border border-outline-variant/30 w-fit">
        <button
          :class="[
            'flex items-center gap-2 px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
            filter === 'all'
              ? 'bg-primary text-on-primary'
              : 'text-on-surface-variant hover:bg-surface-container-low'
          ]"
          @click="filter = 'all'; loadList()"
        >
          全部
          <span
            :class="[
              'min-w-[18px] h-[18px] px-1.5 rounded-full text-[11px] font-bold inline-flex items-center justify-center',
              filter === 'all' ? 'bg-white/25 text-white' : 'bg-surface-container text-on-surface-variant'
            ]"
          >
            {{ total }}
          </span>
        </button>
        <button
          :class="[
            'flex items-center gap-2 px-4 py-2 rounded-lg text-label-md font-label-md transition-all',
            filter === 'unread'
              ? 'bg-primary text-on-primary'
              : 'text-on-surface-variant hover:bg-surface-container-low'
          ]"
          @click="filter = 'unread'; loadList()"
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
          v-for="item in notifications"
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
                {{ formatTime(item.createdAt) }}
              </span>
            </div>
            <p class="text-body-md font-semibold text-on-surface leading-relaxed m-0">
              {{ item.title }}
            </p>
            <p class="text-body-md text-on-surface-variant leading-relaxed m-0">
              {{ item.content }}
            </p>
          </div>
          <button
            class="self-start p-1 text-on-surface-variant hover:text-error rounded transition-colors"
            title="删除"
            @click.stop="removeNotification(item.id)"
          >
            <el-icon size="16">
              <Close />
            </el-icon>
          </button>
          <div
            v-if="!item.read"
            class="w-2 h-2 rounded-full bg-primary flex-shrink-0 mt-1.5"
          />
        </div>

        <div
          v-if="loading"
          class="flex flex-col gap-stack-md"
        >
          <el-skeleton
            v-for="i in 3"
            :key="i"
            :rows="2"
            animated
          />
        </div>

        <div
          v-else-if="notifications.length === 0"
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Document, Picture, MagicStick, InfoFilled, Close } from '@element-plus/icons-vue'
import { notificationApi, type NotificationItem, type NotificationType } from '@/api/notification'

const filter = ref<'all' | 'unread'>('all')
const notifications = ref<NotificationItem[]>([])
const loading = ref(false)
const total = ref(0)

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

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
  return typeLabels[type] || type
}

function iconOf(type: NotificationType) {
  return typeIcons[type] || InfoFilled
}

function iconBgClass(type: NotificationType): string {
  switch (type) {
    case 'pdf':
      return 'bg-error-container text-error'
    case 'avatar':
      return 'bg-secondary-container text-secondary'
    case 'ai':
      return 'bg-primary/10 text-primary'
    default:
      return 'bg-surface-container text-on-surface-variant'
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

async function loadList() {
  loading.value = true
  try {
    const res = await notificationApi.list({
      page: 1,
      size: 100,
      unreadOnly: filter.value === 'unread'
    })
    notifications.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载通知失败')
  } finally {
    loading.value = false
  }
}

async function markRead(id: string) {
  const item = notifications.value.find(n => n.id === id)
  if (!item || item.read) return
  try {
    await notificationApi.markRead(id)
    item.read = true
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllRead()
    notifications.value.forEach(n => { n.read = true })
    ElMessage.success('已全部标记为已读')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function removeNotification(id: string) {
  try {
    await notificationApi.remove(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    if (filter.value === 'unread') total.value = notifications.value.length
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

const clearing = ref(false)

async function clearAll() {
  if (clearing.value) return
  try {
    await ElMessageBox.confirm(
      '确定清空所有通知吗？清空后不可恢复。',
      '清空通知',
      { confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  clearing.value = true
  let failed = 0
  try {
    for (const item of notifications.value) {
      try {
        await notificationApi.remove(item.id)
      } catch {
        failed++
      }
    }
    await loadList()
    if (failed === 0) {
      ElMessage.success('已清空通知')
    } else {
      ElMessage.warning(`部分通知删除失败（${failed} 条），列表已刷新`)
    }
  } finally {
    clearing.value = false
  }
}

onMounted(loadList)
</script>