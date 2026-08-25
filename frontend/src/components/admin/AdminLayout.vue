<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark">
          <el-icon
            color="#fff"
            size="18"
          >
            <Document />
          </el-icon>
        </div>
        <div>
          <h1 class="brand-title">
            智能简历后台
          </h1>
          <p class="brand-sub">
            管理控制台
          </p>
        </div>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="['sidebar-item', { 'is-active': isActive(item.path) }]"
        >
          <el-icon size="18">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <button
          class="sidebar-item"
          @click="toggleCollapse"
        >
          <el-icon size="18">
            <Fold v-if="!collapsed" /><Expand v-else />
          </el-icon>
          <span>{{ collapsed ? '展开菜单' : '收起菜单' }}</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main
      class="admin-main"
      :class="{ collapsed }"
    >
      <!-- Top App Bar -->
      <header class="admin-topbar">
        <div class="breadcrumb">
          <span>{{ currentPageParent }}</span>
          <el-icon size="14">
            <ArrowRight />
          </el-icon>
          <span class="current">{{ currentPageTitle }}</span>
        </div>

        <div class="topbar-right">
          <div class="topbar-search">
            <el-icon
              size="16"
              class="search-icon"
            >
              <Search />
            </el-icon>
            <input
              ref="searchInputRef"
              v-model="searchKeyword"
              type="text"
              placeholder="搜索用户、简历、模板、订单等..."
              @keyup.enter="handleSearch"
            >
            <span class="search-kbd">⌘ K</span>
          </div>

          <button
            class="icon-btn relative"
            title="内容审核"
            @click="router.push('/admin/audit')"
          >
            <el-icon size="20">
              <Bell />
            </el-icon>
            <span
              v-if="pendingAudits > 0"
              class="badge"
            >{{ pendingAudits > 99 ? '99+' : pendingAudits }}</span>
          </button>

          <button
            class="icon-btn"
            title="帮助文档"
            @click="openHelp"
          >
            <el-icon size="20">
              <QuestionFilled />
            </el-icon>
          </button>

          <div class="divider" />

          <el-dropdown
            trigger="click"
            @command="handleUserCommand"
          >
            <div class="user-trigger">
              <div class="text-right hidden sm:block">
                <div class="user-name">
                  {{ adminName }}
                </div>
                <div class="user-role">
                  超级管理员
                </div>
              </div>
              <div class="user-avatar">
                {{ adminName.slice(0, 1).toUpperCase() }}
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  系统设置
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
      </header>

      <!-- Page Content -->
      <div class="admin-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getJwtRole } from '@/utils/jwt'
import {
  Document,
  Fold,
  Expand,
  ArrowRight,
  Search,
  Bell,
  QuestionFilled,
  DataAnalysis,
  User,
  DocumentCopy,
  Grid,
  MagicStick,
  Check,
  Wallet,
  TrendCharts
} from '@element-plus/icons-vue'
import { auditApi } from '@/api/admin/audits'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref<HTMLInputElement>()
const pendingAudits = ref(0)
const adminName = computed(() => localStorage.getItem('admin_username') || 'admin')

const navItems = [
  { path: '/admin/dashboard', label: '总览', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/resumes', label: '简历管理', icon: DocumentCopy },
  { path: '/admin/templates', label: '模板管理', icon: Grid },
  { path: '/admin/ai-rules', label: 'AI 规则', icon: MagicStick },
  { path: '/admin/audit', label: '内容审核', icon: Check },
  { path: '/admin/orders', label: '订单会员', icon: Wallet },
  { path: '/admin/delivery', label: '投递数据', icon: TrendCharts }
]

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

async function loadPendingAudits() {
  try {
    const stats = await auditApi.stats()
    pendingAudits.value = stats?.pending || 0
  } catch {
    pendingAudits.value = 0
  }
}

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/admin/users', query: { keyword: kw } })
}

function openHelp() {
  window.open('/help-docs', '_blank')
}

function handleSearchShortcut(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  // 兜底校验：路由守卫之外再解析一次 JWT role，防止非管理员令牌渲染后台壳
  const adminToken = localStorage.getItem('admin_token')
  if (!adminToken || getJwtRole(adminToken) !== 'ADMIN') {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_refresh_token')
    ElMessage.error('登录已过期，请重新登录')
    router.replace('/admin/login')
    return
  }
  loadPendingAudits()
  window.addEventListener('keydown', handleSearchShortcut)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleSearchShortcut)
})

const currentPageParent = computed(() => {
  return '后台'
})

const currentPageTitle = computed(() => {
  const item = navItems.find(i => isActive(i.path))
  return item?.label || route.meta.title || '管理后台'
})

function handleUserCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/admin/settings')
      break
    case 'settings':
      router.push('/admin/settings')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_username')
        ElMessage.success('退出登录成功')
        router.push('/admin/login')
      })
      break
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--st-background);
}

.admin-sidebar {
  width: 220px;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  background: #001a43;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 0;
  transition: width 0.2s ease;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px 24px;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--st-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.brand-sub {
  font-size: 10px;
  color: var(--st-primary-fixed-dim);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 8px;
  overflow-y: auto;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
  background: transparent;
  border: none;
  width: calc(100% - 16px);
  margin: 0 8px;

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.08);
  }

  &.is-active {
    color: #fff;
    background: var(--st-primary-container);
  }
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-main {
  flex: 1;
  margin-left: 220px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.2s ease;

  &.collapsed {
    margin-left: 220px;
  }
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  height: 64px;
  background: var(--st-surface-container-lowest);
  border-bottom: 1px solid var(--st-outline-variant);
  box-shadow: var(--st-shadow-xs);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--st-margin-page);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--st-on-surface-variant);

  .current {
    color: var(--st-on-surface);
    font-weight: 500;
  }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: var(--st-stack-md);
}

.topbar-search {
  display: none;
  align-items: center;
  position: relative;
  width: 320px;
  padding: 8px 12px;
  background: var(--st-surface-container-low);
  border-radius: var(--st-radius-full);

  @media (min-width: 1024px) {
    display: flex;
  }

  input {
    flex: 1;
    border: none;
    background: transparent;
    outline: none;
    font-size: 14px;
    color: var(--st-on-surface);
    padding: 0 8px;

    &::placeholder {
      color: var(--st-outline);
    }
  }

  .search-icon {
    color: var(--st-outline);
  }

  .search-kbd {
    font-size: 10px;
    padding: 2px 6px;
    background: #fff;
    border: 1px solid var(--st-outline-variant);
    border-radius: 4px;
    color: var(--st-outline);
    font-family: var(--st-font-mono);
  }
}

.icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--st-on-surface-variant);
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: var(--st-surface-container-low);
  }

  .badge {
    position: absolute;
    top: 6px;
    right: 6px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    background: var(--st-error);
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    border-radius: 8px;
    border: 2px solid var(--st-surface-container-lowest);
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.divider {
  width: 1px;
  height: 32px;
  background: var(--st-outline-variant);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.user-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--st-on-surface);
}

.user-role {
  font-size: 10px;
  color: var(--st-on-surface-variant);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: var(--st-primary);
  border: 1px solid var(--st-outline-variant);
  flex-shrink: 0;
}

.admin-content {
  flex: 1;
  padding: var(--st-margin-page);
}
</style>
