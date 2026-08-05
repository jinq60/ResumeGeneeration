import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'resume_user_info'

export interface UserInfo {
  userId: string
  nickname?: string
  isGuest?: boolean
  accessToken: string
  refreshToken?: string
  avatar?: string
}

export const useUserStore = defineStore('user', () => {
  const userId = ref<string | null>(null)
  const nickname = ref('')
  const avatar = ref('')
  const isGuest = ref(true)
  const accessToken = ref('')
  const refreshToken = ref('')

  const isLoggedIn = computed(() => !!accessToken.value)

  function setUser(info: UserInfo) {
    userId.value = info.userId
    nickname.value = info.nickname || ''
    avatar.value = info.avatar || ''
    isGuest.value = info.isGuest ?? true
    accessToken.value = info.accessToken
    refreshToken.value = info.refreshToken || ''
    localStorage.setItem(STORAGE_KEY, JSON.stringify(info))
    localStorage.setItem('access_token', info.accessToken)
  }

  function clearUser() {
    userId.value = null
    nickname.value = ''
    avatar.value = ''
    isGuest.value = true
    accessToken.value = ''
    refreshToken.value = ''
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
        avatar.value = info.avatar || ''
        isGuest.value = info.isGuest ?? true
        accessToken.value = info.accessToken
        refreshToken.value = info.refreshToken || ''
        localStorage.setItem('access_token', info.accessToken)
      } catch {
        clearUser()
      }
    }
  }

  return {
    userId,
    nickname,
    avatar,
    isGuest,
    accessToken,
    refreshToken,
    isLoggedIn,
    setUser,
    clearUser,
    restoreFromStorage
  }
})
