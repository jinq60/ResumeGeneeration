<template>
  <header class="app-topnav">
    <div class="topnav-inner">
      <div class="topnav-left">
        <RouterLink
          to="/dashboard"
          class="topnav-brand"
        >
          <div class="brand-mark">
            <el-icon
              color="#fff"
              size="18"
            >
              <Document />
            </el-icon>
          </div>
          <span class="brand-name">智能简历</span>
        </RouterLink>

        <nav class="topnav-nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="['topnav-link', { 'is-active': isActive(item.path) }]"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>

      <div class="topnav-right">
        <div class="topnav-search">
          <el-icon
            size="16"
            class="search-icon"
          >
            <Search />
          </el-icon>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索简历 / 职位 / 公司"
            @keyup.enter="handleSearch"
          >
        </div>

        <button
          class="icon-action"
          @click="toggleNotify"
        >
          <el-icon size="20">
            <Bell />
          </el-icon>
          <span class="notify-dot" />
        </button>

        <button
          class="icon-action"
          @click="goSettings"
        >
          <el-icon size="20">
            <Setting />
          </el-icon>
        </button>

        <el-dropdown
          class="user-dropdown"
          trigger="click"
          @command="handleUserCommand"
        >
          <div class="user-trigger">
            <img
              v-if="userStore.avatar"
              class="user-avatar"
              :src="userStore.avatar"
              alt="avatar"
            >
            <div
              v-else
              class="user-avatar fallback"
            >
              {{ userInitial }}
            </div>
            <span class="user-name hidden md:block">{{ userStore.nickname || '用户' }}</span>
            <el-icon size="14">
              <ArrowDown />
            </el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                个人资料
              </el-dropdown-item>
              <el-dropdown-item command="downloads">
                下载中心
              </el-dropdown-item>
              <el-dropdown-item command="notifications">
                通知中心
              </el-dropdown-item>
              <el-dropdown-item
                command="logout"
                divided
              >
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { Document, Search, Bell, Setting, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')

const navItems = [
  { path: '/dashboard', label: '工作台' },
  { path: '/resumes', label: '我的简历' },
  { path: '/templates', label: '模板中心' },
  { path: '/ai-review', label: 'AI 优化' },
  { path: '/delivery', label: '投递管理' }
]

const userInitial = computed(() => {
  const name = userStore.nickname || '用户'
  return name.charAt(0).toUpperCase()
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/resumes', query: { q: searchKeyword.value.trim() } })
  }
}

function toggleNotify() {
  router.push('/notifications')
}

function goSettings() {
  router.push('/settings')
}

function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/settings')
  } else if (command === 'downloads') {
    router.push('/downloads')
  } else if (command === 'notifications') {
    router.push('/notifications')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.clearUser()
      router.push('/login')
    })
  }
}
</script>

<style scoped lang="scss">
.app-topnav {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 64px;
  background: var(--st-surface-container-lowest);
  border-bottom: 1px solid var(--st-outline-variant);
  box-shadow: var(--st-shadow-xs);
}

.topnav-inner {
  max-width: 1440px;
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--st-margin-page);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--st-stack-md);
}

.topnav-left {
  display: flex;
  align-items: center;
  gap: var(--st-stack-lg);
}

.topnav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--st-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-family: var(--st-font-headline);
  font-size: 18px;
  font-weight: 700;
  color: var(--st-primary);
  letter-spacing: -0.02em;
}

.topnav-nav {
  display: none;
  align-items: center;
  gap: var(--st-stack-md);

  @media (min-width: 768px) {
    display: flex;
  }
}

.topnav-link {
  position: relative;
  padding: 6px 2px;
  font-size: 14px;
  font-weight: 500;
  color: var(--st-on-surface-variant);
  text-decoration: none;
  transition: color 0.2s ease;

  &:hover {
    color: var(--st-primary);
  }

  &.is-active {
    color: var(--st-primary);
    font-weight: 700;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -8px;
      height: 2px;
      background: var(--st-primary);
      border-radius: 1px;
    }
  }
}

.topnav-right {
  display: flex;
  align-items: center;
  gap: var(--st-stack-sm);
}

.topnav-search {
  display: none;
  align-items: center;
  gap: 8px;
  width: 240px;
  padding: 8px 12px;
  background: var(--st-surface-container-low);
  border: 1px solid transparent;
  border-radius: var(--st-radius-full);
  transition: all 0.2s ease;

  @media (min-width: 1024px) {
    display: flex;
  }

  &:focus-within {
    border-color: var(--st-primary-container);
    box-shadow: 0 0 0 3px rgba(0, 87, 194, 0.08);
  }

  .search-icon {
    color: var(--st-on-surface-variant);
  }

  input {
    flex: 1;
    border: none;
    background: transparent;
    outline: none;
    font-size: 14px;
    color: var(--st-on-surface);
    font-family: var(--st-font-sans);

    &::placeholder {
      color: var(--st-outline);
    }
  }
}

.icon-action {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--st-on-surface-variant);
  background: transparent;
  border: none;
  border-radius: var(--st-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: var(--st-surface-container-low);
    color: var(--st-primary);
  }
}

.notify-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: var(--st-error);
  border: 2px solid var(--st-surface-container-lowest);
  border-radius: 50%;
}

.user-dropdown {
  margin-left: 4px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: var(--st-radius-full);
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: var(--st-surface-container-low);
  }
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--st-outline-variant);

  &.fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--st-primary);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
  }
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--st-on-surface);
}
</style>
