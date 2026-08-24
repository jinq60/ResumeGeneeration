import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局登录弹窗状态。
 * 任何页面都可以通过 open() 唤起登录弹窗，无需跳转到 /login。
 */
export const useAuthModalStore = defineStore('authModal', () => {
  const isOpen = ref(false)

  function open() {
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
    open,
    close,
    toggle
  }
})
