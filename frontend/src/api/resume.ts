import request from '@/utils/request'
import { getAccessToken } from '@/utils/authStorage'
import type { Resume, Section } from '@/types/resume'
import type { RenderSettings } from '@/utils/renderSettings'

export interface CreateResumeRequest {
  title?: string
  scene: string
  targetPosition?: string
  templateId: string
}

export interface UpdateResumeRequest {
  title?: string
  targetPosition?: string
  templateId?: string
  sections?: Section[]
  renderSettings?: RenderSettings
  /** 期望版本号（乐观锁）；与服务器不一致时后端返回业务码 2012 / HTTP 409 */
  version?: number
}

export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UpdateResumeResponse {
  id: string
  updatedAt: string
}

export interface ImportResumeRequest {
  title?: string
  scene?: string
  targetPosition?: string
  templateId?: string
  format: 'json' | 'markdown'
  content: string
}

export interface GrammarIssue {
  sectionType: string
  field: string
  itemIndex?: number
  severity: 'high' | 'medium' | 'low' | string
  originalText: string
  suggestion: string
  explanation: string
}

export interface GrammarCheckResponse {
  status: 'success' | 'unavailable' | string
  model: string
  message?: string
  issues: GrammarIssue[]
  checkedAt?: string
}

export interface ResumeOptimizeRequest {
  jobDescription: string
  focusSections?: string[]
}

export interface SectionOptimization {
  sectionId: string
  sectionType: string
  originalSummary: string
  optimizedContent: string
  reasoning: string
}

export interface ResumeOptimizeResult {
  taskId: string
  resumeId: string
  matchScore?: number | null
  dimensionScores?: Record<string, number> | null
  optimizations?: SectionOptimization[] | null
  missingSkills?: string[] | null
  recommendations?: string[] | null
  status: 'pending' | 'processing' | 'success' | 'failed' | string
  errorMsg?: string | null
  createdAt?: string
}

export const resumeApi = {
  create(data: CreateResumeRequest): Promise<Resume> {
    return request.post('/resumes', data) as Promise<Resume>
  },
  importResume(data: ImportResumeRequest): Promise<Resume> {
    return request.post('/resumes/import', data) as Promise<Resume>
  },
  list(page = 1, size = 20): Promise<Page<Resume>> {
    return request.get('/resumes', { params: { page, size } }) as Promise<Page<Resume>>
  },
  get(id: string): Promise<Resume> {
    return request.get(`/resumes/${id}`) as Promise<Resume>
  },
  update(id: string, data: UpdateResumeRequest): Promise<UpdateResumeResponse> {
    return request.put(`/resumes/${id}`, data) as Promise<UpdateResumeResponse>
  },
  remove(id: string): Promise<void> {
    return request.delete(`/resumes/${id}`) as Promise<void>
  },
  duplicate(id: string): Promise<{ id: string; title: string }> {
    return request.post(`/resumes/${id}/duplicate`) as Promise<{ id: string; title: string }>
  },
  rename(id: string, title: string): Promise<Resume> {
    return request.put(`/resumes/${id}/title`, { title }) as Promise<Resume>
  },
  review(id: string, data: { jobDescription: string }): Promise<any> {
    return request.post(`/resumes/${id}/reviews`, data) as Promise<any>
  },
  getLatestReview(id: string): Promise<any> {
    return request.get(`/resumes/${id}/reviews/latest`) as Promise<any>
  },
  createOptimizeTask(id: string, data: ResumeOptimizeRequest): Promise<{ taskId: string; resumeId: string; status: string }> {
    return request.post(`/resumes/${id}/optimize`, data) as Promise<{ taskId: string; resumeId: string; status: string }>
  },
  getOptimizeTask(id: string, taskId: string): Promise<ResumeOptimizeResult> {
    return request.get(`/resumes/${id}/optimize/${taskId}`) as Promise<ResumeOptimizeResult>
  },
  getLatestOptimize(id: string): Promise<ResumeOptimizeResult | null> {
    return request.get(`/resumes/${id}/optimize/latest`) as Promise<ResumeOptimizeResult | null>
  },
  grammarCheck(id: string): Promise<GrammarCheckResponse> {
    return request.post(`/resumes/${id}/grammar-check`) as Promise<GrammarCheckResponse>
  },
  aiWrite(id: string, payload: AiWritePayload): Promise<{ content: string }> {
    return request.post(`/resumes/${id}/ai/write`, payload) as Promise<{ content: string }>
  },
  async aiWriteStream(
    id: string,
    payload: AiWritePayload,
    onDelta: (content: string) => void,
    signal?: AbortSignal
  ): Promise<void> {
    const baseURL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    const token = getAccessToken()
    const response = await fetch(`${baseURL}/resumes/${id}/ai/write/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(payload),
      signal
    })

    if (!response.ok) {
      throw new Error(`AI 写作请求失败（${response.status}）`)
    }
    if (!response.body) {
      throw new Error('AI 写作未返回流式内容')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let eventName = 'message'
    let dataLines: string[] = []
    let streamDone = false

    const dispatch = () => {
      const data = dataLines.join('\n')
      if (eventName === 'delta' && data) {
        onDelta(data)
      } else if (eventName === 'error') {
        throw new Error(data || 'AI 写作失败，请稍后重试')
      }
      eventName = 'message'
      dataLines = []
    }

    // 中断（abort）或读取出错时释放底层连接，避免连接泄漏
    try {
      await consumeStream()
    } catch (e) {
      try {
        await reader.cancel()
      } catch {
        // 连接可能已被对端关闭，忽略取消失败
      }
      throw e
    }

    async function consumeStream(): Promise<void> {
      while (!streamDone) {
        const { done, value } = await reader.read()
        streamDone = done
        buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
        const lines = buffer.split(/\r?\n/)
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (!line) {
            dispatch()
          } else if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).replace(/^ /, ''))
          }
        }

        if (done) break
      }

      if (buffer || dataLines.length > 0) {
        if (buffer.startsWith('data:')) {
          dataLines.push(buffer.slice(5).replace(/^ /, ''))
        }
        dispatch()
      }
    }
  }
}

export interface AiWritePayload {
  sectionType: string
  field: string
  action: 'generate' | 'polish' | 'shorten' | 'expand' | 'translate'
  originalText?: string
  targetLang?: string
}
