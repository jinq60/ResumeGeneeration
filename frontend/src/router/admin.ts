import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: () => import('@/components/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '统计总览', requiresAuth: true } },
      { path: 'users', name: 'AdminUserManagement', component: () => import('@/views/admin/UserManagement.vue'), meta: { title: '用户管理', requiresAuth: true } },
      { path: 'resumes', name: 'AdminResumeManagement', component: () => import('@/views/admin/ResumeManagement.vue'), meta: { title: '简历管理', requiresAuth: true } },
      { path: 'templates', name: 'AdminTemplateManagement', component: () => import('@/views/admin/TemplateManagement.vue'), meta: { title: '模板管理', requiresAuth: true } },
      { path: 'ai-rules', name: 'AdminAIRules', component: () => import('@/views/admin/SystemSettings.vue'), meta: { title: 'AI 规则', requiresAuth: true } },
      { path: 'audit', name: 'AdminContentAudit', component: () => import('@/views/admin/ContentAudit.vue'), meta: { title: '内容审核', requiresAuth: true } },
      { path: 'delivery', name: 'AdminDeliveryData', component: () => import('@/views/admin/DeliveryData.vue'), meta: { title: '投递数据', requiresAuth: true } }
    ]
  }
]

export default routes
