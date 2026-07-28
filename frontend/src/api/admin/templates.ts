import adminRequest from '@/utils/adminRequest'

export interface Template {
  id: string
  code: string
  name: string
  category: string
  thumbnailUrl: string | null
  description: string | null
  status: string
  isBuiltin: boolean
  isRecommended: boolean
  sortOrder: number
  createdBy: string | null
  createdAt: string
  updatedAt: string
  htmlTemplate?: string
  config?: any
}

export interface TemplatePage {
  records: Template[]
  total: number
  size: number
  current: number
  pages: number
}

export interface TemplateRequest {
  code: string
  name: string
  category: string
  thumbnailUrl?: string
  description?: string
  htmlTemplate: string
  renderEngine?: string
  config: any
  sortOrder?: number
  isRecommended?: boolean
}

export interface TemplateStats {
  totalTemplates: number
  activeTemplates: number
  inactiveTemplates: number
  builtinTemplates: number
}

export const templateApi = {
  // 获取模板列表
  getTemplates: (params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    category?: string
  }): Promise<TemplatePage> => {
    return adminRequest.get('/admin/templates', { params })
  },

  // 获取模板详情
  getTemplate: (id: string): Promise<Template> => {
    return adminRequest.get(`/admin/templates/${id}`)
  },

  // 获取模板统计
  getTemplateStats: (): Promise<TemplateStats> => {
    return adminRequest.get('/admin/templates/stats')
  },

  // 创建模板
  createTemplate: (data: TemplateRequest): Promise<Template> => {
    return adminRequest.post('/admin/templates', data)
  },

  // 更新模板
  updateTemplate: (id: string, data: TemplateRequest): Promise<Template> => {
    return adminRequest.put(`/admin/templates/${id}`, data)
  },

  // 删除模板
  deleteTemplate: (id: string): Promise<void> => {
    return adminRequest.delete(`/admin/templates/${id}`)
  },

  // 更新模板状态
  updateTemplateStatus: (id: string, status: string): Promise<void> => {
    return adminRequest.patch(`/admin/templates/${id}/status`, { status })
  },

  // 上传模板缩略图
  uploadThumbnail: (file: File): Promise<{ url: string }> => {
    const formData = new FormData()
    formData.append('file', file)
    return adminRequest.post('/admin/templates/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
