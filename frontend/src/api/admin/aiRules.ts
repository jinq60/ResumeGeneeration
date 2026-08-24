import adminRequest from '@/utils/adminRequest'

export interface AiRule {
  id: string
  familyId: string
  name: string
  ruleType: string
  description?: string
  systemPrompt?: string
  userPrompt?: string
  params: Record<string, unknown>
  status: 'draft' | 'active' | 'disabled'
  version: number
  publishedAt?: string
  createdBy?: string
  createdAt: string
  updatedAt: string
}

export interface AiRulePage {
  records: AiRule[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AiRuleStats {
  total: number
  active: number
  disabled: number
  draft: number
  todayPublished: number
}

export interface AiRuleRequest {
  name: string
  ruleType: string
  description?: string
  systemPrompt?: string
  userPrompt?: string
  params?: Record<string, unknown>
}

export const RULE_TYPE_LABELS: Record<string, string> = {
  score: '评分规则',
  prompt: '提示词',
  security: '安全规则',
  dict: '词库',
  match: '匹配策略',
  risk: '风险规则'
}

export const aiRuleApi = {
  list(params?: {
    page?: number
    size?: number
    keyword?: string
    ruleType?: string
    status?: string
  }): Promise<AiRulePage> {
    return adminRequest.get('/admin/ai-rules', { params })
  },
  stats(): Promise<AiRuleStats> {
    return adminRequest.get('/admin/ai-rules/stats')
  },
  get(id: string): Promise<AiRule> {
    return adminRequest.get(`/admin/ai-rules/${id}`)
  },
  create(data: AiRuleRequest): Promise<AiRule> {
    return adminRequest.post('/admin/ai-rules', data)
  },
  saveDraft(id: string, data: AiRuleRequest): Promise<AiRule> {
    return adminRequest.put(`/admin/ai-rules/${id}/draft`, data)
  },
  publish(id: string, data?: AiRuleRequest): Promise<AiRule> {
    return adminRequest.post(`/admin/ai-rules/${id}/publish`, data || {})
  },
  toggle(id: string, status: string): Promise<AiRule> {
    return adminRequest.patch(`/admin/ai-rules/${id}/status`, { status })
  },
  versions(id: string): Promise<AiRule[]> {
    return adminRequest.get(`/admin/ai-rules/${id}/versions`)
  },
  rollback(id: string, version: number): Promise<AiRule> {
    return adminRequest.post(`/admin/ai-rules/${id}/rollback/${version}`)
  },
  testRun(id: string, sampleInput?: string): Promise<{ ok: boolean; output: string; model?: string }> {
    return adminRequest.post(`/admin/ai-rules/${id}/test`, { sampleInput })
  },
  remove(id: string): Promise<void> {
    return adminRequest.delete(`/admin/ai-rules/${id}`)
  }
}