import axios from 'axios'
import type { ApiResp } from './types'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
  withCredentials: true,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  // 幂等键 IdempotencyFilter:52 仅对 POST/PUT/PATCH/DELETE，401 重试时复用原 key
  if (['post', 'put', 'patch', 'delete'].includes((config.method || '').toLowerCase())) {
    if (!config.headers['Idempotency-Key']) config.headers['Idempotency-Key'] = crypto.randomUUID()
  }
  if (!config.headers['X-Trace-Id']) config.headers['X-Trace-Id'] = Math.random().toString(36).slice(2, 10)
  return config
})

let refreshing = false
let queue: Array<{ resolve: () => void; reject: (e: any) => void }> = []

client.interceptors.response.use(
  (res) => {
    // 业务错误仍为 200 包 R.code !=200 的由 GlobalExceptionHandler 映射为对应 HTTP
    return res
  },
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      if (refreshing) {
        await new Promise<void>((resolve, reject) => queue.push({ resolve, reject }))
        original.headers.Authorization = `Bearer ${localStorage.getItem('accessToken')}`
        return client(original)
      }
      original._retry = true
      refreshing = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) throw error
        const { data } = await client.post<ApiResp<import('./types').AuthResponse>>(
          '/auth/refresh',
          { refreshToken }
        )
        if (data.code === 200 && data.data) {
          localStorage.setItem('accessToken', data.data.accessToken)
          localStorage.setItem('refreshToken', data.data.refreshToken)
          try {
            const { useAuthStore } = await import('@/stores/auth')
            const s = useAuthStore()
            if (s && typeof s.syncFromStorage === 'function') s.syncFromStorage()
          } catch {}
          queue.forEach((q) => q.resolve())
          queue = []
          original.headers.Authorization = `Bearer ${data.data.accessToken}`
          return client(original)
        }
        throw error
      } catch (e) {
        queue.forEach((q) => q.reject(e))
        queue = []
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(e)
      } finally {
        refreshing = false
      }
    }
    // 统一错误语义：403/429/425/409 按 ResultCode 映射为用户可读 toast，不阻塞业务方自定义 catch
    const status: number | undefined = error.response?.status
    const code: number | undefined = error.response?.data?.code
    const msg: string = error.response?.data?.message || error.message
    try {
      const { pushToast } = await import('@/composables/useToast')
      if (status === 403) pushToast(msg || '无权访问')
      else if (status === 429 || code === 6009) pushToast(msg || '请求过于频繁，请稍后再试')
      else if (status === 425) pushToast(msg || '操作进行中，请稍后重试')
      else if (status === 409 && code === 2012) pushToast('版本冲突，已刷新最新')
      else if (status && status >= 500) pushToast(msg || '服务异常，请稍后重试')
    } catch {}
    if (msg) console.warn('[API]', status, code, msg)
    return Promise.reject(error)
  }
)

export default client

export function unwrap<T>(resp: { data: ApiResp<T> }): T {
  if (resp.data.code !== 200) throw new Error(resp.data.message)
  return resp.data.data
}
