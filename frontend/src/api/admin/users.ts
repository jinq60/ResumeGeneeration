import adminRequest from '@/utils/adminRequest'

// 与后端 AdminUserListItemResponse / AdminUserDetailResponse 对齐：
// avatarUrl 仅详情接口（AdminUserDetailResponse）返回，列表接口
// （AdminUserListItemResponse）不含该字段，故标注为可选。
// 因后台用户管理视图以单一 User 类型同时承接列表与详情数据，
// 不做接口级类型拆分，使用时注意列表项的 avatarUrl 可能为 undefined。
export interface User {
  userId: string
  nickname: string | null
  email: string | null
  phone: string | null
  /** 仅详情接口 GET /admin/users/{id} 返回；列表接口无此字段 */
  avatarUrl?: string | null
  role: string
  status: string
  isGuest: boolean
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
  email?: string
  phone?: string
  temporaryPassword?: string
  message?: string
}

export interface ResetPasswordResponse {
  userId: string
  temporaryPassword: string
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

  // 调整用户角色（USER / ADMIN）
  updateUserRole: (id: string, role: 'USER' | 'ADMIN'): Promise<void> => {
    return adminRequest.patch(`/admin/users/${id}/role`, { role })
  },

  // 重置用户密码，返回一次性临时密码
  resetUserPassword: (id: string): Promise<ResetPasswordResponse> => {
    return adminRequest.post(`/admin/users/${id}/reset-password`)
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
