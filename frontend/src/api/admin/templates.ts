import axios from 'axios'
import type { AxiosResponse } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/admin/login'
    }
    return Promise.reject(error)
  }
)

export interface Template {
  id: string
  code: string
  name: string
  category: string
  thumbnailUrl: string
  description: string
  htmlTemplate: string
  renderEngine: string
  config: any
  sortOrder: number
  status: string
  isBuiltIn: boolean
  createdAt: string
  updatedAt: string
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
}

export const templateApi = {
  // 获取模板列表
  getTemplates: (params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    category?: string
  }): Promise<{ data: Template[]; total: number }> => {
    return apiClient.get('/admin/templates', { params })
  },

  // 获取模板详情
  getTemplate: (id: string): Promise<Template> => {
    return apiClient.get(`/admin/templates/${id}`)
  },

  // 创建模板
  createTemplate: (data: TemplateRequest): Promise<Template> => {
    return apiClient.post('/admin/templates', data)
  },

  // 更新模板
  updateTemplate: (id: string, data: TemplateRequest): Promise<Template> => {
    return apiClient.put(`/admin/templates/${id}`, data)
  },

  // 删除模板
  deleteTemplate: (id: string): Promise<void> => {
    return apiClient.delete(`/admin/templates/${id}`)
  },

  // 更新模板状态
  updateTemplateStatus: (id: string, status: string): Promise<void> => {
    return apiClient.patch(`/admin/templates/${id}/status`, { status })
  },

  // 上传模板缩略图
  uploadThumbnail: (file: File): Promise<{ url: string }> => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/admin/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
