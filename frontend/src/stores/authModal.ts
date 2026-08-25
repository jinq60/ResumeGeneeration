import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局登录弹窗状态。
 * 任何页面都可以通过 open() 唤起登录弹窗，无需跳转到 /login。
 */
export const useAuthModalStore = defineStore('authModal', () => {
  const isOpen = ref(false)
  // OAuth 回调错误文本：/login 回调页会在清除 URL query 前转存到这里，
  // LoginModal 打开时读取并消费，避免与 router.replace 清 query 的时序竞争
  const oauthError = ref('')

  function open(options?: { oauthError?: string }) {
    if (options?.oauthError) {
      oauthError.value = options.oauthError
    }
    isOpen.value = true
  }

  function close() {
    isOpen.value = false
  }

  function toggle() {
    isOpen.value = !isOpen.value
  }

  return {
    isOpen,
    oauthError,
    open,
    close,
    toggle
  }
})
