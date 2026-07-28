import adminRequest from '@/utils/adminRequest'

export interface Resume {
  id: string
  userId: string
  title: string
  scene: string
  targetPosition: string | null
  templateId: string
  templateName: string
  exportCount: number
  status: string
  lastEditedAt: string
  createdAt: string
  updatedAt: string
}

export interface ResumePage {
  records: Resume[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ResumeStats {
  totalResumes: number
  activeResumes: number
  deletedResumes: number
  todayNewResumes: number
}

export const resumeApi = {
  getResumes: (params?: {
    page?: number
    size?: number
    keyword?: string
  }): Promise<ResumePage> => {
    return adminRequest.get('/admin/resumes', { params })
  },

  getResumeStats: (): Promise<ResumeStats> => {
    return adminRequest.get('/admin/resumes/stats')
  }
}
