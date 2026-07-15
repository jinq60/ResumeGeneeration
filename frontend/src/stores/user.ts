import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'resume_user_info'

export interface UserInfo {
  userId: string
  nickname?: string
  isGuest?: boolean
  accessToken: string
}

export const useUserStore = defineStore('user', () => {
  const userId = ref<string | null>(null)
  const nickname = ref('')
  const isGuest = ref(true)
  const accessToken = ref('')

  const isLoggedIn = computed(() => !!accessToken.value)

  function setUser(info: UserInfo) {
    userId.value = info.userId
    nickname.value = info.nickname || ''
    isGuest.value = info.isGuest ?? true
    accessToken.value = info.accessToken
    localStorage.setItem(STORAGE_KEY, JSON.stringify(info))
    localStorage.setItem('access_token', info.accessToken)
  }

  function clearUser() {
    userId.value = null
    nickname.value = ''
    isGuest.value = true
    accessToken.value = ''
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem('access_token')
  }

  function restoreFromStorage() {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      try {
        const info: UserInfo = JSON.parse(raw)
        userId.value = info.userId
        nickname.value = info.nickname || ''
        isGuest.value = info.isGuest ?? true
        accessToken.value = info.accessToken
        localStorage.setItem('access_token', info.accessToken)
      } catch {
        clearUser()
      }
    }
  }

  return {
    userId,
    nickname,
    isGuest,
    accessToken,
    isLoggedIn,
    setUser,
    clearUser,
    restoreFromStorage
  }
})
