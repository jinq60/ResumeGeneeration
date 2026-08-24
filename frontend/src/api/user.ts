import request from '@/utils/request'

export interface UserInfo {
  userId: string
  nickname?: string
  phone?: string
  email?: string
  avatarUrl?: string
  isGuest?: boolean
}

export interface UpdateProfileRequest {
  nickname?: string
  phone?: string
  email?: string
  avatarUrl?: string
}

export interface UserPreferences {
  emailNotify?: boolean
  autoSaveNotify?: boolean
  keepGuestData?: boolean
}

export const userApi = {
  me(): Promise<UserInfo> {
    return request.get('/users/me') as Promise<UserInfo>
  },
  updateProfile(data: UpdateProfileRequest): Promise<UserInfo> {
    return request.put('/users/me', data) as Promise<UserInfo>
  },
  getPreferences(): Promise<Record<string, unknown>> {
    return request.get('/users/me/preferences') as Promise<Record<string, unknown>>
  },
  savePreferences(preferences: Record<string, unknown>): Promise<Record<string, unknown>> {
    return request.put('/users/me/preferences', preferences) as Promise<Record<string, unknown>>
  }
}