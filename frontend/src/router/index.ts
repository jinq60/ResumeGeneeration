import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthModalStore } from '@/stores/authModal'
import { getAccessToken } from '@/utils/authStorage'
import adminRoutes from './admin'
import websiteRoutes from './website'
import workbenchRoutes from './workbench'

const routes: RouteRecordRaw[] = [
  // /login 仅作为 OAuth 回调入口，会立即打开弹窗并跳转
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  { path: '/share/:token', name: 'Share', component: () => import('@/views/ShareView.vue'), meta: { title: '简历分享' } },
  ...websiteRoutes,
  ...workbenchRoutes,
  ...adminRoutes,
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const publicRouteNames = ['Login', 'AdminLogin', 'Share', 'Home', 'Features', 'TemplatesShowcase', 'Pricing', 'About', 'Contact', 'HelpDocs']
const appTitle = import.meta.env.VITE_APP_TITLE || '智能简历生成工具'

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${appTitle}` : appTitle

  // Admin routes use separate token
  if (to.path.startsWith('/admin')) {
    const adminToken = localStorage.getItem('admin_token')
    if (to.meta.requiresAuth && !adminToken) {
      next('/admin/login')
    } else if (to.path === '/admin/login' && adminToken) {
      next('/admin/dashboard')
    } else {
      next()
    }
    return
  }

  const token = getAccessToken()

  // /login 用于 OAuth 回调；已登录用户访问则直接进工作台
  if (to.name === 'Login') {
    if (token && !to.query.oauth_code && !to.query.error) {
      next({ path: '/workbench/dashboard' })
    } else {
      next()
    }
    return
  }

  // Workbench routes: 未登录时打开登录弹窗并留在首页
  if (to.path.startsWith('/workbench')) {
    if (!token) {
      const authModalStore = useAuthModalStore()
      authModalStore.open()
      next({ path: '/' })
    } else {
      next()
    }
    return
  }

  // Public website routes
  if (!token && !publicRouteNames.includes(to.name as string)) {
    const authModalStore = useAuthModalStore()
    authModalStore.open()
    next({ path: '/' })
  } else {
    next()
  }
})

export default router
