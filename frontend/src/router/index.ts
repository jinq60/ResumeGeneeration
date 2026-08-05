import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import adminRoutes from './admin'
import websiteRoutes from './website'
import workbenchRoutes from './workbench'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  ...websiteRoutes,
  ...workbenchRoutes,
  ...adminRoutes,
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const publicRouteNames = ['Login', 'AdminLogin', 'Home', 'Features', 'TemplatesShowcase', 'Pricing', 'About', 'Contact', 'HelpDocs']

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 智能简历` : '智能简历'

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

  // Workbench routes
  const token = localStorage.getItem('access_token')
  if (to.path.startsWith('/workbench')) {
    if (!token) {
      next({ name: 'Login' })
    } else {
      next()
    }
    return
  }

  // Public website routes
  if (!token && !publicRouteNames.includes(to.name as string)) {
    next({ name: 'Login' })
  } else if (token && to.name === 'Login') {
    next({ path: '/workbench/dashboard' })
  } else {
    next()
  }
})

export default router
