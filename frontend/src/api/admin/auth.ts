import axios from 'axios'
import type { AxiosResponse } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token过期，跳转到登录页
      localStorage.removeItem('admin_token')
      window.location.href = '/admin/login'
    }
    return Promise.reject(error)
  }
)

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  userId: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  isGuest: boolean
}

export const authApi = {
  // 管理员登录
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return apiClient.post('/admin/auth/login', data)
  },

  // 刷新Token
  refreshToken: (refreshToken: string): Promise<LoginResponse> => {
    return apiClient.post('/auth/refresh', { refreshToken })
  },

  // 登出
  logout: (): Promise<void> => {
    return apiClient.post('/auth/logout')
  }
}
