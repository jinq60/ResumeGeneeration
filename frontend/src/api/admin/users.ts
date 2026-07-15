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

apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/admin/login'
    }
    return Promise.reject(error)
  }
)

export interface User {
  id: string
  nickname: string
  email: string
  phone: string
  avatarUrl: string
  isGuest: boolean
  status: string
  resumeCount: number
  createdAt: string
  lastLoginAt: string
}

export interface UserStats {
  totalUsers: number
  activeUsers: number
  guestUsers: number
  registeredUsers: number
  todayNewUsers: number
}

export const userApi = {
  // 获取用户列表
  getUsers: (params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    isGuest?: boolean
  }): Promise<{ data: User[]; total: number }> => {
    return apiClient.get('/admin/users', { params })
  },

  // 获取用户详情
  getUser: (id: string): Promise<User> => {
    return apiClient.get(`/admin/users/${id}`)
  },

  // 更新用户状态
  updateUserStatus: (id: string, status: string): Promise<void> => {
    return apiClient.patch(`/admin/users/${id}/status`, { status })
  },

  // 获取用户统计
  getUserStats: (): Promise<UserStats> => {
    return apiClient.get('/admin/users/stats')
  },

  // 删除用户
  deleteUser: (id: string): Promise<void> => {
    return apiClient.delete(`/admin/users/${id}`)
  }
}
