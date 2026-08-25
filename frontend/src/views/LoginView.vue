<template>
  <div class="min-h-screen flex items-center justify-center bg-background">
    <el-icon class="animate-spin text-muted-foreground" size="32"><Loading /></el-icon>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuthModalStore } from '@/stores/authModal'
import { authApi } from '@/api/auth'
import { Loading } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const authModalStore = useAuthModalStore()

onMounted(async () => {
  const query = route.query

  // OAuth 回调：用一次性授权码换取令牌对（JWT 不经 URL 传递），完成后清理地址栏
  if (query.oauth_code) {
    const code = String(query.oauth_code)
    router.replace({ path: '/login' })
    try {
      const auth = await authApi.oauthExchange(code)
      userStore.setUser({
        userId: auth.userId || '',
        accessToken: auth.accessToken,
        refreshToken: auth.refreshToken,
        isGuest: auth.isGuest ?? false
      })
      router.replace('/workbench/dashboard')
    } catch {
      authModalStore.open()
      router.replace('/')
    }
    return
  }

  // 有错误信息时打开弹窗并展示错误
  if (query.error) {
    // 先把错误文本转存到 authModalStore，再清理地址栏；
    // LoginModal 打开后从 store 读取，避免与 router.replace 清 query 的时序竞争
    authModalStore.open({ oauthError: String(query.error) })
    router.replace('/')
    return
  }

  // 普通 /login 入口：打开登录弹窗并回到首页
  authModalStore.open()
  router.replace('/')
})
</script>
