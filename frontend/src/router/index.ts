import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import adminRoutes from './admin'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  { path: '/landing', name: 'Landing', component: () => import('@/views/LandingView.vue') },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
  { path: '/resumes', name: 'ResumeList', component: () => import('@/views/ResumeListView.vue') },
  { path: '/resumes/create', name: 'TemplateSelect', component: () => import('@/views/TemplateSelectView.vue') },
  { path: '/templates', name: 'TemplateCenter', component: () => import('@/views/TemplateCenterView.vue') },
  { path: '/templates/:id', name: 'TemplateDetail', component: () => import('@/views/TemplateDetailView.vue') },
  { path: '/editor/:id', name: 'Editor', component: () => import('@/views/EditorView.vue') },
  { path: '/resumes/:id/edit', name: 'ResumeEdit', component: () => import('@/views/EditorView.vue') },
  { path: '/resumes/:id/export', name: 'Export', component: () => import('@/views/ExportView.vue') },
  { path: '/resumes/:id/preview', name: 'Preview', component: () => import('@/views/ExportView.vue') },
  { path: '/resumes/:id/review', name: 'AIReview', component: () => import('@/views/AIReviewView.vue') },
  { path: '/resumes/:id', name: 'ResumeDetail', component: () => import('@/views/ResumeDetailView.vue') },
  { path: '/avatar/upload', name: 'AvatarUpload', component: () => import('@/views/AvatarUploadView.vue') },
  { path: '/ai-review', name: 'AIReviewCenter', component: () => import('@/views/AIReviewCenterView.vue') },
  { path: '/delivery', name: 'DeliveryManagement', component: () => import('@/views/DeliveryManagementView.vue') },
  { path: '/settings', name: 'Settings', component: () => import('@/views/SettingsView.vue') },
  { path: '/downloads', name: 'DownloadCenter', component: () => import('@/views/DownloadCenterView.vue') },
  { path: '/notifications', name: 'NotificationCenter', component: () => import('@/views/NotificationCenterView.vue') },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue') },
  ...adminRoutes
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const publicRoutes = ['Login', 'AdminLogin', 'Landing']

router.beforeEach((to, _from, next) => {
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

  // User routes
  const token = localStorage.getItem('access_token')
  if (!token && !publicRoutes.includes(to.name as string)) {
    next({ name: 'Login' })
  } else if (token && to.name === 'Login') {
    next({ path: '/' })
  } else {
    next()
  }
})

export default router
