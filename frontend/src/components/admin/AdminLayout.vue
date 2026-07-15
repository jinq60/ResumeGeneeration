<template>
  <div class="admin-layout">
    <el-container>
      <el-aside
        width="240px"
        class="sidebar"
      >
        <div class="logo">
          <h2>管理后台</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表板</span>
          </el-menu-item>
          <el-menu-item index="/admin/templates">
            <el-icon><Grid /></el-icon>
            <span>模板管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-button
              text
              @click="toggleSidebar"
            >
              <el-icon><Fold /></el-icon>
            </el-button>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/admin' }">
                首页
              </el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-dropdown">
                <el-avatar
                  :size="32"
                  :src="userInfo.avatar"
                >
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ userInfo.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    个人资料
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
        </el-header>
        
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Grid,
  User,
  Setting,
  Fold,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => {
  const titleMap: Record<string, string> = {
    '/admin/dashboard': '仪表板',
    '/admin/templates': '模板管理',
    '/admin/users': '用户管理',
    '/admin/settings': '系统设置'
  }
  return titleMap[route.path] || '首页'
})

const userInfo = ref({
  nickname: '管理员',
  avatar: ''
})

const toggleSidebar = () => {
  // TODO: 实现侧边栏折叠功能
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      // TODO: 跳转到个人资料页面
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 清除登录状态
    ElMessage.success('退出登录成功')
    router.push('/admin/login')
  })
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.el-container {
  height: 100%;
}

.sidebar {
  background-color: #304156;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
  color: white;
}

.logo h2 {
  font-size: 18px;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
  height: calc(100vh - 60px);
}

.header {
  background: white;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-dropdown:hover {
  background-color: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
