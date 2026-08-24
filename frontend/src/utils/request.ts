import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { useAuthModalStore } from '@/stores/authModal'
import {
  getAccessToken,
  readStoredAuth,
  updateStoredTokens,
  clearStoredAuth
} from '@/utils/authStorage'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let refreshPromise: Promise<string> | null = null

/**
 * 用 refresh token 换取新凭证（单飞：并发 401 只发起一次刷新）。
 * 使用独立 axios 实例，避免与拦截器形成循环依赖。
 */
function refreshAccessToken(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise
  }
  refreshPromise = (async () => {
    const user = readStoredAuth()
    if (!user?.refreshToken) {
      throw new Error('缺少刷新令牌')
    }
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    const response = await axios.post(`${base}/auth/refresh`, { refreshToken: user.refreshToken })
    const data = response.data?.data
    if (!data?.accessToken || !data?.refreshToken) {
      throw new Error('刷新令牌无效')
    }
    updateStoredTokens(data.accessToken, data.refreshToken)
    return data.accessToken as string
  })().finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

function handleAuthExpired() {
  clearStoredAuth()
  const path = window.location.pathname
  if (!path.startsWith('/admin/login')) {
    const authModalStore = useAuthModalStore()
    authModalStore.open()
  }
}

request.interceptors.response.use(
  (response: AxiosResponse) => {
    // Blob 等非 JSON 响应直接放行，交由调用方处理原始响应
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }
    if (response.data.code !== 200) {
      return Promise.reject(new Error(response.data.message || '请求失败'))
    }
    return response.data.data
  },
  async (error) => {
    const status = error.response?.status
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const url = config?.url || ''

    if (status === 401 && config && !config._retried && !url.startsWith('/auth/')) {
      config._retried = true
      try {
        const token = await refreshAccessToken()
        config.headers.Authorization = `Bearer ${token}`
        return request(config)
      } catch {
        handleAuthExpired()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
    }

    const message = error.response?.data?.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export default request
