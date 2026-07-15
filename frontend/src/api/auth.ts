import request from '@/utils/request'

export interface RegisterRequest {
  phone?: string
  email?: string
  verifyCode: string
  password: string
}

export interface LoginRequest {
  account: string
  password: string
  loginType: 'phone' | 'email'
}

export interface AuthResponse {
  userId: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  isGuest?: boolean
}

export const authApi = {
  register(data: RegisterRequest): Promise<AuthResponse> {
    return request.post('/auth/register', data) as Promise<AuthResponse>
  },
  login(data: LoginRequest): Promise<AuthResponse> {
    return request.post('/auth/login', data) as Promise<AuthResponse>
  },
  guest(): Promise<AuthResponse> {
    return request.post('/auth/guest') as Promise<AuthResponse>
  },
  refresh(refreshToken: string): Promise<AuthResponse> {
    return request.post('/auth/refresh', { refreshToken }) as Promise<AuthResponse>
  }
}
