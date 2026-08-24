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
  },

  // 管理端预览 HTML 地址（需 admin_token）
  previewUrl(id: string): string {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    return `${base}/admin/resumes/${id}/preview`
  },

  // 管理端导出 Word / Markdown（文件流下载地址）
  exportWordUrl(id: string): string {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    return `${base}/admin/resumes/${id}/export/word`
  },
  exportMarkdownUrl(id: string): string {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    return `${base}/admin/resumes/${id}/export/markdown`
  },

  // 管理端创建 PDF 导出任务
  exportPdf(id: string, templateId?: string): Promise<{ taskId: string; status: string }> {
    return adminRequest.post(`/admin/resumes/${id}/export/pdf`, templateId ? { templateId } : {})
  },

  // 管理端查询 / 下载 PDF 任务
  getPdfTask(taskId: string): Promise<PdfTask> {
    return adminRequest.get(`/admin/pdf/tasks/${taskId}`)
  },
  pdfDownloadUrl(taskId: string): string {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    return `${base}/admin/pdf/download/${taskId}`
  }
}

export interface PdfTask {
  id: string
  resumeId: string
  templateId: string
  fileName: string
  fileSize: number
  status: string
  errorMsg?: string
  createdAt: string
  completedAt?: string
}
