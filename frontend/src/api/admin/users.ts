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
  }
}
