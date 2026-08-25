import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { ApiError } from '@/utils/apiError'

const ADMIN_TOKEN_KEY = 'admin_token'
const ADMIN_REFRESH_TOKEN_KEY = 'admin_refresh_token'

const adminRequest = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

let refreshPromise: Promise<string> | null = null

/**
 * 管理端刷新令牌（单飞：并发 401 只发起一次刷新）。
 * 与 request.ts 一致，使用独立 axios 实例避免与拦截器形成循环依赖；
 * 刷新成功后同步更新 admin_token / admin_refresh_token。
 */
function refreshAdminToken(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise
  }
  refreshPromise = (async () => {
    const refreshToken = localStorage.getItem(ADMIN_REFRESH_TOKEN_KEY)
    if (!refreshToken) {
      throw new Error('缺少刷新令牌')
    }
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    const response = await axios.post(`${base}/auth/refresh`, { refreshToken })
    const data = response.data?.data
    if (!data?.accessToken || !data?.refreshToken) {
      throw new Error('刷新令牌无效')
    }
    localStorage.setItem(ADMIN_TOKEN_KEY, data.accessToken)
    localStorage.setItem(ADMIN_REFRESH_TOKEN_KEY, data.refreshToken)
    return data.accessToken as string
  })().finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

/** 刷新失败：清理管理端登录态并跳转登录页（管理员未保存的表单会丢失，故仅在无法恢复时才走这里）。 */
function handleAdminAuthExpired() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_REFRESH_TOKEN_KEY)
  ElMessage.error('登录已过期，请重新登录')
  window.location.href = '/admin/login'
}

adminRequest.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

adminRequest.interceptors.response.use(
  (response: AxiosResponse) => {
    // 登录接口返回的 refreshToken 落地存储，供后续 401 静默刷新使用
    if (
      String(response.config.url || '').endsWith('/auth/login') &&
      response.data?.data?.refreshToken
    ) {
      localStorage.setItem(ADMIN_REFRESH_TOKEN_KEY, response.data.data.refreshToken)
    }
    if (response.data.code !== 200) {
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

    // 401 时先尝试静默刷新并重放原请求一次；/auth/** 本身不重试，避免登录失败被误判为会话过期
    if (status === 401 && config && !config._retried && !url.startsWith('/auth/')) {
      config._retried = true
      try {
        const token = await refreshAdminToken()
        config.headers.Authorization = `Bearer ${token}`
        return adminRequest(config)
      } catch {
        handleAdminAuthExpired()
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

export default adminRequest
