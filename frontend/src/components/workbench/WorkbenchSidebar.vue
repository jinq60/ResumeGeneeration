<template>
  <aside class="app-sidebar">
    <div class="sidebar-brand">
      <div class="sidebar-brand-mark">
        <el-icon
          color="#fff"
          size="18"
        >
          <Document />
        </el-icon>
      </div>
      <span class="sidebar-brand-name">智能简历</span>
    </div>

    <nav class="sidebar-nav">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="sidebar-item"
        :class="{ 'is-active': isActive(item.path) }"
      >
        <el-icon size="18">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
        <span
          v-if="item.badge"
          class="sidebar-badge"
        >{{ item.badge }}</span>
      </RouterLink>

      <div class="sidebar-divider" />

      <RouterLink
        v-for="item in toolItems"
        :key="item.path"
        :to="item.path"
        class="sidebar-item"
        :class="{ 'is-active': isActive(item.path) }"
      >
        <el-icon size="18">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </RouterLink>

      <div class="sidebar-divider" />

      <RouterLink
        v-for="item in bottomItems"
        :key="item.path"
        :to="item.path"
        class="sidebar-item"
        :class="{ 'is-active': isActive(item.path) }"
      >
        <el-icon size="18">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-user">
      <div class="sidebar-user-avatar">
        {{ userInitial }}
      </div>
      <div class="sidebar-user-meta">
        <div class="sidebar-user-name">
          {{ userStore.nickname || '用户' }}
        </div>
        <div class="sidebar-user-role">
          {{ userStore.isGuest ? '游客' : '会员' }}
        </div>
      </div>
      <button
        class="sidebar-user-logout"
        title="退出登录"
        @click="handleLogout"
      >
        <el-icon size="16">
          <SwitchButton />
        </el-icon>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Document, DataAnalysis, Files, Picture, MagicStick, Postcard,
  TrendCharts, Download, Setting, Bell
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface NavItem { path: string; label: string; icon: any; badge?: string }

const navItems: NavItem[] = [
  { path: '/workbench/dashboard', label: '工作台', icon: DataAnalysis },
  { path: '/workbench/resumes', label: '我的简历', icon: Files },
  { path: '/workbench/templates', label: '选择模板', icon: Postcard },
]

const toolItems: NavItem[] = [
  { path: '/workbench/avatar/upload', label: '头像管理', icon: Picture },
  { path: '/workbench/ai-review', label: 'AI 点评', icon: MagicStick, badge: 'Beta' },
  { path: '/workbench/delivery', label: '投递管理', icon: TrendCharts }
]

const bottomItems: NavItem[] = [
  { path: '/workbench/downloads', label: '下载中心', icon: Download },
  { path: '/workbench/notifications', label: '通知中心', icon: Bell },
  { path: '/workbench/settings', label: '账号设置', icon: Setting }
]

const userInitial = computed(() => {
  const name = userStore.nickname || '用户'
  return name.charAt(0).toUpperCase()
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

async function handleLogout() {
  try {
    if (userStore.refreshToken) {
      const { authApi } = await import('@/api/auth')
      await authApi.logout(userStore.refreshToken)
    }
  } catch {
    // 服务端吊销失败不阻塞本地登出
  }
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.app-sidebar {
  width: var(--st-sidebar-width, 220px);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background: #001a43;
  color: rgba(255, 255, 255, 0.88);
  display: flex;
  flex-direction: column;
  z-index: 40;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: var(--st-stack-md);
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-brand-mark {
  width: 36px;
  height: 36px;
  border-radius: var(--st-radius-lg);
  background: var(--st-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--st-shadow-md);
}

.sidebar-brand-name {
  font-weight: 600;
  font-size: 16px;
  letter-spacing: -0.01em;
  color: #fff;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: var(--st-stack-md);
  padding: 10px 12px;
  border-radius: var(--st-radius-lg);
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.62);
  transition: all 0.2s ease;
  text-decoration: none;

  &:hover {
    color: rgba(255, 255, 255, 0.92);
    background: rgba(255, 255, 255, 0.04);
  }

  &.is-active {
    color: #fff;
    background: var(--st-primary-container);
  }
}

.sidebar-badge {
  margin-left: auto;
  background: var(--st-primary-container);
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: var(--st-radius-full);
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.sidebar-item.is-active .sidebar-badge {
  background: rgba(255, 255, 255, 0.22);
}

.sidebar-divider {
  margin: 12px 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-user {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  gap: var(--st-stack-md);
}

.sidebar-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--st-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.sidebar-user-meta {
  flex: 1;
  min-width: 0;
}

.sidebar-user-name {
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.46);
  margin-top: 2px;
}

.sidebar-user-logout {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.46);
  cursor: pointer;
  padding: 6px;
  border-radius: var(--st-radius-md);
  display: flex;
  align-items: center;
  transition: all 0.2s ease;

  &:hover {
    color: var(--st-error);
    background: rgba(186, 26, 26, 0.12);
  }
}
</style>