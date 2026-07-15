import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/resumes'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/resumes',
    name: 'ResumeList',
    component: () => import('@/views/ResumeListView.vue')
  },
  {
    path: '/editor/:id',
    name: 'Editor',
    component: () => import('@/views/EditorView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
