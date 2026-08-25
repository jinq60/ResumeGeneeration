import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { useAuthModalStore } from '@/stores/authModal'
import { useUserStore } from '@/stores/user'
import {
  getAccessToken,
  readStoredAuth,
  updateStoredTokens,
  clearStoredAuth
} from '@/utils/authStorage'
import { ApiError } from '@/utils/apiError'

export { ApiError }

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
  // 同步清理 Pinia 用户态与本地存储，避免侧边栏仍显示已登录、
  // 后续请求重复触发"刷新失败→弹窗"循环。
  try {
    useUserStore().clearUser()
  } catch {
    // Pinia 尚未初始化时兜底只清 localStorage
    clearStoredAuth()
  }
  const path = window.location.pathname
  // 管理端有自己的登录态处理（adminRequest + /admin/login），整个 /admin 前缀都不弹用户端登录框
  if (!path.startsWith('/admin')) {
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
      // 业务错误必须携带 R.code，供下游按业务码分支（如 6004 AI 并发限流）
      return Promise.reject(
        new ApiError(response.data.message || '请求失败', {
          code: response.data.code,
          httpStatus: response.status
        })
      )
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
        return Promise.reject(new ApiError('登录已过期，请重新登录', { httpStatus: 401 }))
      }
    }

    const message = error.response?.data?.message || '网络异常，请稍后重试'
    return Promise.reject(
      new ApiError(message, {
        code: error.response?.data?.code,
        httpStatus: status
      })
    )
  }
)

export default request
