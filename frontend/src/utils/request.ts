import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let refreshPromise: Promise<string> | null = null

function getStoredUser(): { accessToken: string; refreshToken?: string } | null {
  const raw = localStorage.getItem('resume_user_info')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/**
 * 用 refresh token 换取新凭证（单飞：并发 401 只发起一次刷新）。
 * 使用独立 axios 实例，避免与拦截器形成循环依赖。
 */
function refreshAccessToken(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise
  }
  refreshPromise = (async () => {
    const user = getStoredUser()
    if (!user?.refreshToken) {
      throw new Error('缺少刷新令牌')
    }
    const response = await axios.post('/api/auth/refresh', { refreshToken: user.refreshToken })
    const data = response.data?.data
    if (!data?.accessToken || !data?.refreshToken) {
      throw new Error('刷新令牌无效')
    }
    const next = { ...user, accessToken: data.accessToken, refreshToken: data.refreshToken }
    localStorage.setItem('resume_user_info', JSON.stringify(next))
    localStorage.setItem('access_token', next.accessToken)
    return next.accessToken
  })().finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

function handleAuthExpired() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('resume_user_info')
  const path = window.location.pathname
  if (!path.startsWith('/login') && !path.startsWith('/admin/login')) {
    window.location.href = '/login'
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
