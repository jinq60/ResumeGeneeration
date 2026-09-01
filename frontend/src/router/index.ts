import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Landing', component: () => import('@/views/LandingView.vue') },
    { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/',
      component: () => import('@/layouts/AtelierLayout.vue'),
      children: [
        {
          path: 'workbench',
          name: 'Workbench',
          component: () => import('@/views/WorkbenchView.vue'),
          meta: { auth: true },
        },
        {
          path: 'resumes',
          name: 'ResumeList',
          component: () => import('@/views/ResumeListView.vue'),
          meta: { auth: true },
        },
        {
          path: 'templates',
          name: 'Templates',
          component: () => import('@/views/TemplatesView.vue'),
          meta: { auth: true },
        },
        {
          path: 'ai/review/:id?',
          name: 'AiReview',
          component: () => import('@/views/AiReviewView.vue'),
          meta: { auth: true },
        },
        {
          path: 'deliveries',
          name: 'Deliveries',
          component: () => import('@/views/DeliveriesView.vue'),
          meta: { auth: true },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { auth: true },
        },
        {
          path: 'admin',
          name: 'Admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { admin: true },
        },
      ],
    },
    {
      path: '/editor/:id',
      name: 'Editor',
      component: () => import('@/views/EditorView.vue'),
      meta: { auth: true },
    },
    { path: '/share/:token', name: 'Share', component: () => import('@/views/ShareView.vue') },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.auth && !token) return '/login'
  if (to.meta.admin) {
    if (!token) return '/login'
    // 前端轻拦截：游客默认非 ADMIN，减少 403 闪烁；真实由 SecurityConfig hasRole ADMIN 兜底
    try {
      const auth = useAuthStore()
      if (auth.isGuest) return '/workbench'
    } catch {}
  }
})

export default router
