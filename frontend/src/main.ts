import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import { initTheme } from './composables/useTheme'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/styles/tailwind.css'
import './assets/styles/design-system.scss'

// 渲染前应用主题，避免暗色模式首屏闪烁
initTheme()

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 刷新页面后从 localStorage 恢复登录态，保证 store 与路由守卫一致
useUserStore(pinia).restoreFromStorage()
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
