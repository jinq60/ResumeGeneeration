import request from '@/utils/request'
import type { AxiosResponse } from 'axios'

export interface PdfTask {
  taskId: string
  status: 'pending' | 'processing' | 'success' | 'failed'
  fileName?: string
  fileSize?: number
  errorMsg?: string
  createdAt?: string
  completedAt?: string
}

export const pdfApi = {
  export(resumeId: string, templateId?: string): Promise<{ taskId: string; status: string }> {
    const body: { resumeId: string; templateId?: string } = { resumeId }
    if (templateId) {
      body.templateId = templateId
    }
    return request.post('/pdf/export', body) as Promise<{ taskId: string; status: string }>
  },
  getTask(taskId: string): Promise<PdfTask> {
    return request.get(`/pdf/tasks/${taskId}`) as Promise<PdfTask>
  },
  download(taskId: string): Promise<AxiosResponse<Blob>> {
    return request.get(`/pdf/download/${taskId}`, { responseType: 'blob' }) as Promise<AxiosResponse<Blob>>
  }
}
