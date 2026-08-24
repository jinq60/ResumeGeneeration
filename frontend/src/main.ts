import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ArcoVue from '@arco-design/web-vue'
import ArcoVueIcon from '@arco-design/web-vue/es/icon'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import '@arco-design/web-vue/dist/arco.css'
import './assets/styles/tailwind.css'
import './assets/styles/design-system.scss'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 刷新页面后从 localStorage 恢复登录态，保证 store 与路由守卫一致
useUserStore(pinia).restoreFromStorage()
app.use(router)
app.use(ArcoVue)
app.use(ArcoVueIcon)

app.mount('#app')
