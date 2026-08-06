import request from '@/utils/request'

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

export interface EmailCodeLoginRequest {
  email: string
  code: string
}

export const authApi = {
  login(data: LoginRequest): Promise<AuthResponse> {
    return request.post('/auth/login', data) as Promise<AuthResponse>
  },
  guest(): Promise<AuthResponse> {
    return request.post('/auth/guest') as Promise<AuthResponse>
  },
  refresh(refreshToken: string): Promise<AuthResponse> {
    return request.post('/auth/refresh', { refreshToken }) as Promise<AuthResponse>
  },
  logout(refreshToken: string): Promise<void> {
    return request.post('/auth/logout', { refreshToken }) as Promise<void>
  },
  changePassword(oldPassword: string, newPassword: string): Promise<void> {
    return request.put('/users/me/password', { oldPassword, newPassword }) as Promise<void>
  },
  emailCodeSend(email: string): Promise<void> {
    return request.post('/auth/email-code/send', { email }) as Promise<void>
  },
  emailCodeLogin(data: EmailCodeLoginRequest): Promise<AuthResponse> {
    return request.post('/auth/email-code/login', data) as Promise<AuthResponse>
  },
  loginMethods(): Promise<{ loginMethods: { method: string; configured: boolean }[]; oauthProviders: { provider: string; configured: boolean }[] }> {
    return request.get('/auth/methods') as Promise<{ loginMethods: { method: string; configured: boolean }[]; oauthProviders: { provider: string; configured: boolean }[] }>
  }
}

/** 第三方授权跳转地址（浏览器整页跳转，回跳后由 /login 处理 token） */
export function oauthAuthorizeUrl(provider: string): string {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
  return `${base}/auth/oauth/${provider}/authorize`
}
