import adminRequest from '@/utils/adminRequest'

export interface User {
  userId: string
  nickname: string | null
  email: string | null
  phone: string | null
  avatarUrl: string | null
  role: string
  isGuest: boolean
  status: string
  createdAt: string
  updatedAt: string
}

export interface UserPage {
  records: User[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UserStats {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  guestUsers: number
  registeredUsers: number
  adminUsers: number
  todayNewUsers: number
}

export interface CreateUserRequest {
  nickname?: string
  phone?: string
  email: string
  initialPassword?: string
}

export interface CreateUserResponse {
  userId: string
  nickname?: string
  email: string
  phone?: string
  temporaryPassword?: string
  message: string
}

export const userApi = {
  // 获取用户列表
  getUsers: (params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
  }): Promise<UserPage> => {
    return adminRequest.get('/admin/users', { params })
  },

  // 获取用户详情
  getUser: (id: string): Promise<User> => {
    return adminRequest.get(`/admin/users/${id}`)
  },

  // 更新用户状态
  updateUserStatus: (id: string, status: string): Promise<void> => {
    return adminRequest.patch(`/admin/users/${id}/status`, { status })
  },

  // 获取用户统计
  getUserStats: (): Promise<UserStats> => {
    return adminRequest.get('/admin/users/stats')
  },

  // 删除用户
  deleteUser: (id: string): Promise<void> => {
    return adminRequest.delete(`/admin/users/${id}`)
  },

  // 新增用户
  createUser: (data: CreateUserRequest): Promise<CreateUserResponse> => {
    return adminRequest.post('/admin/users', data)
  },

  // 导出用户 CSV
  exportUrl(params?: { status?: string; keyword?: string }): string {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    const query = new URLSearchParams()
    if (params?.status) query.set('status', params.status)
    if (params?.keyword) query.set('keyword', params.keyword)
    const qs = query.toString()
    return `${base}/admin/users/export${qs ? `?${qs}` : ''}`
  }
}
