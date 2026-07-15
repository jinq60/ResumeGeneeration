import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import Login from '@/views/admin/Login.vue'
import Dashboard from '@/views/admin/Dashboard.vue'
import TemplateManagement from '@/views/admin/TemplateManagement.vue'
import UserManagement from '@/views/admin/UserManagement.vue'
import SystemSettings from '@/views/admin/SystemSettings.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: Login,
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: Dashboard,
        meta: { title: '仪表板', requiresAuth: true }
      },
      {
        path: 'templates',
        name: 'AdminTemplateManagement',
        component: TemplateManagement,
        meta: { title: '模板管理', requiresAuth: true }
      },
      {
        path: 'users',
        name: 'AdminUserManagement',
        component: UserManagement,
        meta: { title: '用户管理', requiresAuth: true }
      },
      {
        path: 'settings',
        name: 'AdminSystemSettings',
        component: SystemSettings,
        meta: { title: '系统设置', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  
  if (to.meta.requiresAuth && !token) {
    next('/admin/login')
  } else if (to.path === '/admin/login' && token) {
    next('/admin/dashboard')
  } else {
    next()
  }
})

export default router
