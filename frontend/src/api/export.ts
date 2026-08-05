import request from '@/utils/request'
import type { AxiosResponse } from 'axios'

export type ExportFormat = 'pdf' | 'word' | 'markdown'

function parseFileName(disposition: string | null, fallback: string): string {
  if (!disposition) return fallback
  const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
  if (match) {
    try {
      return decodeURIComponent(match[1])
    } catch {
      return match[1]
    }
  }
  return fallback
}

function saveBlob(data: Blob, fileName: string) {
  const url = window.URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export const exportApi = {
  async downloadMarkdown(resumeId: string): Promise<string> {
    const response = await request.get<Blob>(`/resumes/${resumeId}/export/markdown`, {
      responseType: 'blob'
    }) as unknown as AxiosResponse<Blob>
    const fileName = parseFileName(response.headers?.['content-disposition'] || null, '简历.md')
    saveBlob(response.data, fileName)
    return fileName
  },

  async downloadWord(resumeId: string): Promise<string> {
    const response = await request.get<Blob>(`/resumes/${resumeId}/export/word`, {
      responseType: 'blob'
    }) as unknown as AxiosResponse<Blob>
    const fileName = parseFileName(response.headers?.['content-disposition'] || null, '简历.docx')
    saveBlob(response.data, fileName)
    return fileName
  }
}
