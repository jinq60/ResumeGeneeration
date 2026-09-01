import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import client from '@/api/client'
import type { AuthResponse, UserInfoResponse } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref<UserInfoResponse | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isGuest = computed(() => user.value?.isGuest ?? true)

  async function login(account: string, password: string) {
    const { data } = await client.post('/auth/login', { account, password })
    setAuth(data.data)
    await fetchMe()
    return data.data as AuthResponse
  }

  async function loginByMethod(method: string, params: Record<string, string>) {
    const { data } = await client.post(`/auth/login/${method}`, params)
    setAuth(data.data)
    await fetchMe()
    return data.data
  }

  async function guest() {
    const { data } = await client.post('/auth/guest')
    setAuth(data.data)
    await fetchMe()
    return data.data
  }

  async function fetchMe() {
    if (!accessToken.value) return null
    const { data } = await client.get('/users/me')
    user.value = data.data
    return user.value
  }

  function setAuth(auth: AuthResponse) {
    accessToken.value = auth.accessToken
    refreshToken.value = auth.refreshToken
    localStorage.setItem('accessToken', auth.accessToken)
    localStorage.setItem('refreshToken', auth.refreshToken)
  }

  function syncFromStorage() {
    accessToken.value = localStorage.getItem('accessToken') || ''
    refreshToken.value = localStorage.getItem('refreshToken') || ''
  }

  function logout() {
    const rt = localStorage.getItem('refreshToken')
    if (rt) client.post('/auth/logout', { refreshToken: rt }).catch(() => {})
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    isGuest,
    login,
    loginByMethod,
    guest,
    fetchMe,
    logout,
    setAuth,
    syncFromStorage,
  }
})
