import axios from 'axios'

const previewRequest = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

previewRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function fetchResumePreview(resumeId: string, templateId?: string): Promise<string> {
  const response = await previewRequest.get<string>(`/resumes/${resumeId}/preview`, {
    params: templateId ? { templateId } : undefined,
    responseType: 'text',
    transformResponse: []
  })
  return response.data
}

export async function fetchLivePreview(resume: unknown, templateId?: string): Promise<string> {
  const response = await previewRequest.post<string>(
    '/resumes/preview',
    { resume, templateId },
    { responseType: 'text', transformResponse: [] }
  )
  return response.data
}
