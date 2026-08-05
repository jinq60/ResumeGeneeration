import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/components/website/WebsiteLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/website/HomeView.vue'), meta: { title: '首页' } },
      { path: 'features', name: 'Features', component: () => import('@/views/website/FeaturesView.vue'), meta: { title: '功能特性' } },
      { path: 'templates', name: 'TemplatesShowcase', component: () => import('@/views/website/TemplatesShowcaseView.vue'), meta: { title: '模板中心' } },
      { path: 'pricing', name: 'Pricing', component: () => import('@/views/website/PricingView.vue'), meta: { title: '定价方案' } },
      { path: 'about', name: 'About', component: () => import('@/views/website/AboutView.vue'), meta: { title: '关于我们' } },
      { path: 'contact', name: 'Contact', component: () => import('@/views/website/ContactView.vue'), meta: { title: '联系我们' } },
      { path: 'help', name: 'HelpDocs', component: () => import('@/views/website/HelpDocsView.vue'), meta: { title: '帮助文档' } }
    ]
  }
]

export default routes
