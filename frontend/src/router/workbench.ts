import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/workbench',
    component: () => import('@/components/workbench/WorkbenchLayout.vue'),
    redirect: '/workbench/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/workbench/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'resumes', name: 'ResumeList', component: () => import('@/views/workbench/ResumeListView.vue'), meta: { title: '我的简历' } },
      { path: 'resumes/create', name: 'TemplateSelect', component: () => import('@/views/workbench/TemplateSelectView.vue'), meta: { title: '选择模板' } },
      { path: 'templates', name: 'TemplateCenter', component: () => import('@/views/workbench/TemplateCenterView.vue'), meta: { title: '模板中心' } },
      { path: 'templates/:id', name: 'TemplateDetail', component: () => import('@/views/workbench/TemplateDetailView.vue'), meta: { title: '模板详情' } },
      { path: 'editor/:id', name: 'Editor', component: () => import('@/views/workbench/EditorView.vue'), meta: { title: '简历编辑器' } },
      { path: 'resumes/:id/edit', name: 'ResumeEdit', component: () => import('@/views/workbench/EditorView.vue'), meta: { title: '编辑简历' } },
      { path: 'resumes/:id/export', name: 'Export', component: () => import('@/views/workbench/ExportView.vue'), meta: { title: '导出简历' } },
      { path: 'resumes/:id/preview', name: 'Preview', component: () => import('@/views/workbench/ExportView.vue'), meta: { title: '预览简历' } },
      { path: 'resumes/:id/review', name: 'AIReview', component: () => import('@/views/workbench/AIReviewView.vue'), meta: { title: 'AI 点评' } },
      { path: 'resumes/:id', name: 'ResumeDetail', component: () => import('@/views/workbench/ResumeDetailView.vue'), meta: { title: '简历详情' } },
      { path: 'avatar/upload', name: 'AvatarUpload', component: () => import('@/views/workbench/AvatarUploadView.vue'), meta: { title: '头像管理' } },
      { path: 'ai-review', name: 'AIReviewCenter', component: () => import('@/views/workbench/AIReviewCenterView.vue'), meta: { title: 'AI 优化中心' } },
      { path: 'delivery', name: 'DeliveryManagement', component: () => import('@/views/workbench/DeliveryManagementView.vue'), meta: { title: '投递管理' } },
      { path: 'settings', name: 'Settings', component: () => import('@/views/workbench/SettingsView.vue'), meta: { title: '账号设置' } },
      { path: 'downloads', name: 'DownloadCenter', component: () => import('@/views/workbench/DownloadCenterView.vue'), meta: { title: '下载中心' } },
      { path: 'notifications', name: 'NotificationCenter', component: () => import('@/views/workbench/NotificationCenterView.vue'), meta: { title: '通知中心' } }
    ]
  }
]

export default routes
