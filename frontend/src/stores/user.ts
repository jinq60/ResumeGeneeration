import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  readStoredAuth,
  writeStoredAuth,
  clearStoredAuth
} from '@/utils/authStorage'

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
    writeStoredAuth({
      userId: info.userId,
      nickname: info.nickname,
      avatar: info.avatar,
      isGuest: info.isGuest,
      accessToken: info.accessToken,
      refreshToken: info.refreshToken
    })
  }

  function clearUser() {
    userId.value = null
    nickname.value = ''
    avatar.value = ''
    isGuest.value = true
    accessToken.value = ''
    refreshToken.value = ''
    clearStoredAuth()
  }

  function restoreFromStorage() {
    const info = readStoredAuth()
    if (info) {
      userId.value = info.userId
      nickname.value = info.nickname || ''
      avatar.value = info.avatar || ''
      isGuest.value = info.isGuest ?? true
      accessToken.value = info.accessToken
      refreshToken.value = info.refreshToken || ''
      // 迁移历史双键数据：重写为单一键存储
      if (info.accessToken) {
        writeStoredAuth(info)
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
