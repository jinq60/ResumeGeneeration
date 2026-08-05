import request from '@/utils/request'

export interface ShareInfo {
  token: string
  url: string
  status: string
  expiresAt?: string
  createdAt?: string
}

export const shareApi = {
  create(resumeId: string): Promise<ShareInfo> {
    return request.post(`/resumes/${resumeId}/share`) as Promise<ShareInfo>
  },
  get(resumeId: string): Promise<ShareInfo | null> {
    return request.get(`/resumes/${resumeId}/share`) as Promise<ShareInfo | null>
  },
  revoke(resumeId: string): Promise<void> {
    return request.delete(`/resumes/${resumeId}/share`) as Promise<void>
  }
}
