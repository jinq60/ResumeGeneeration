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
      <span class="sidebar-brand-name">ResumePro</span>
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
import { Document, DataBoard, Files, Picture, MagicStick, Postcard } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface NavItem { path: string; label: string; icon: any; badge?: string }

const navItems: NavItem[] = [
  { path: '/dashboard', label: '工作台', icon: DataBoard },
  { path: '/resumes', label: '我的简历', icon: Files },
  { path: '/templates', label: '选择模板', icon: Postcard },
]

const toolItems: NavItem[] = [
  { path: '/avatar/upload', label: '头像管理', icon: Picture },
  { path: '/ai-review', label: 'AI 点评', icon: MagicStick, badge: 'Beta' },
]

const userInitial = computed(() => {
  const name = userStore.nickname || '用户'
  return name.charAt(0).toUpperCase()
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.app-sidebar {
  width: 240px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background: var(--color-ink-navy);
  color: rgba(255, 255, 255, 0.88);
  display: flex;
  flex-direction: column;
  z-index: 40;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-5) var(--space-5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-brand-mark {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #5584FF, #2962FF);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px var(--color-archive-blue-12);
}

.sidebar-brand-name {
  font-weight: 600;
  font-size: var(--font-size-md);
  letter-spacing: -0.01em;
}

.sidebar-nav {
  flex: 1;
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 10px var(--space-3);
  border-radius: var(--radius-lg);
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: rgba(255, 255, 255, 0.62);
  transition: var(--transition-fast);

  &:hover {
    color: rgba(255, 255, 255, 0.92);
    background: rgba(255, 255, 255, 0.04);
  }

  &.is-active {
    color: #fff;
    background: var(--color-archive-blue);
    box-shadow: 0 2px 8px var(--color-archive-blue-12);
  }
}

.sidebar-badge {
  margin-left: auto;
  background: var(--color-archive-blue);
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: var(--radius-pill);
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.sidebar-item.is-active .sidebar-badge {
  background: rgba(255, 255, 255, 0.22);
}

.sidebar-divider {
  margin: var(--space-3) 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-user {
  padding: var(--space-4);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.sidebar-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #F1C948, #D9892B);
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
  font-size: var(--font-size-sm);
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
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  transition: var(--transition-base);

  &:hover {
    color: var(--color-signal-coral);
    background: var(--color-signal-coral-12);
  }
}
</style>