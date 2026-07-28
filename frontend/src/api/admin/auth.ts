import adminRequest from '@/utils/adminRequest'

export interface LoginRequest {
  account: string
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
  // 管理员登录（复用统一认证接口，登录后校验 JWT 中的 role 是否为 ADMIN）
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return adminRequest.post('/auth/login', data)
  },

  // 刷新Token
  refreshToken: (refreshToken: string): Promise<LoginResponse> => {
    return adminRequest.post('/auth/refresh', { refreshToken })
  }
}
