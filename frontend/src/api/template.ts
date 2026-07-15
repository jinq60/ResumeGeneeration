import request from '@/utils/request'

export interface TemplateConfig {
  page?: Record<string, string>
  font?: Record<string, string>
  color?: Record<string, string>
  layout?: Record<string, unknown>
  sectionTitle?: Record<string, string>
  skill?: { displayStyle?: 'tag' | 'category' | 'level' }
}

export interface Template {
  id: string
  name: string
  category: string
  thumbnailUrl?: string
  description?: string
  config: TemplateConfig
  htmlTemplate?: string
}

export const templateApi = {
  list(): Promise<Template[]> {
    return request.get('/templates') as Promise<Template[]>
  },
  get(id: string): Promise<Template> {
    return request.get(`/templates/${id}`) as Promise<Template>
  }
}
