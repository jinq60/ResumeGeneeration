import request from '@/utils/request'

export interface TemplateConfig {
  page?: Record<string, string>
  font?: Record<string, string>
  color?: Record<string, string>
  layout?: Record<string, unknown>
  sectionTitle?: Record<string, string>
  skill?: { displayStyle?: 'tag' | 'category' | 'level' }
}

// 与后端 TemplateDTO（backend/src/main/java/com/resume/template/dto/TemplateDTO.java）逐字段对齐
export interface Template {
  id: string; name: string; category: string; thumbnailUrl?: string
  description?: string; config: TemplateConfig; htmlTemplate?: string
  renderEngine?: 'server' | 'client' | 'hybrid'
  sortOrder?: number
  /** 模板编码 */
  code?: string
  isRecommended?: boolean
  /**
   * @deprecated 后端 TemplateDTO 无此字段，运行时恒为 undefined；
   * 因 workbench 模板视图仍引用该属性（不在本修复线可修改范围内）暂保留为可选，
   * 后续由视图侧清理。
   */
  isPremium?: boolean
  status?: string
  createdAt?: string
  updatedAt?: string
}

export const templateApi = {
  list(): Promise<Template[]> {
    return request.get('/templates') as Promise<Template[]>
  },
  get(id: string): Promise<Template> {
    return request.get(`/templates/${id}`) as Promise<Template>
  }
}
