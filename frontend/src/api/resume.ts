import request from '@/utils/request'
import type { Resume, Section } from '@/types/resume'

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

export const resumeApi = {
  create(data: CreateResumeRequest): Promise<Resume> {
    return request.post('/resumes', data) as Promise<Resume>
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
  }
}
