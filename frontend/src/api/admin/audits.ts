import adminRequest from '@/utils/adminRequest'

export interface AuditItem {
  id: string
  targetType: string
  targetId: string
  targetTitle?: string
  userId: string
  riskLevel: 'low' | 'medium' | 'high'
  status: 'pending' | 'approved' | 'rejected' | 'warning'
  reviewerId?: string
  reviewNote?: string
  reviewedAt?: string
  createdAt: string
  updatedAt: string
}

export interface AuditPage {
  records: AuditItem[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AuditStats {
  total: number
  pending: number
  approved: number
  rejected: number
  warning: number
  todayReviewed: number
}

export interface AuditReviewRequest {
  note?: string
  riskLevel?: string
}

export const auditApi = {
  list(params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    riskLevel?: string
  }): Promise<AuditPage> {
    return adminRequest.get('/admin/audits', { params })
  },
  stats(): Promise<AuditStats> {
    return adminRequest.get('/admin/audits/stats')
  },
  approve(id: string, note?: string): Promise<AuditItem> {
    return adminRequest.post(`/admin/audits/${id}/approve`, { note })
  },
  reject(id: string, note?: string): Promise<AuditItem> {
    return adminRequest.post(`/admin/audits/${id}/reject`, { note })
  },
  markWarning(id: string, riskLevel: string, note?: string): Promise<AuditItem> {
    return adminRequest.post(`/admin/audits/${id}/mark-warning`, { riskLevel, note })
  }
}

/** 管理端简历预览 HTML 地址（需 admin_token，浏览器直开）。 */
export function adminResumePreviewUrl(resumeId: string): string {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
  return `${base}/admin/resumes/${resumeId}/preview`
}