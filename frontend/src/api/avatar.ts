import request from '@/utils/request'

export interface AvatarTask {
  taskId: string
  status: 'pending' | 'processing' | 'success' | 'failed'
  sourceImageUrl?: string
  resultImageUrl?: string
  errorMsg?: string
  createdAt?: string
  completedAt?: string
}

export interface OptimizeAvatarRequest {
  sourceImageUrl: string
  resumeId?: string
  backgroundType: string
  style: string
  keepIdentity?: boolean
  enhanceQuality?: boolean
  removeBackground?: boolean
  brightenSkin?: boolean
}

export const avatarApi = {
  upload(file: File, resumeId?: string): Promise<{ id: string; sourceImageUrl: string; fileName: string }> {
    const formData = new FormData()
    formData.append('file', file)
    if (resumeId) {
      formData.append('resumeId', resumeId)
    }
    // 让浏览器自动设置 boundary，避免手动指定导致 multipart 解析失败
    return request.post('/avatars/upload', formData) as Promise<{ id: string; sourceImageUrl: string; fileName: string }>
  },
  optimize(data: OptimizeAvatarRequest): Promise<{ taskId: string; status: string }> {
    return request.post('/avatars/optimize', data) as Promise<{ taskId: string; status: string }>
  },
  getTask(taskId: string): Promise<AvatarTask> {
    return request.get(`/avatars/tasks/${taskId}`) as Promise<AvatarTask>
  },
  remove(id: string): Promise<void> {
    return request.delete(`/avatars/${id}`) as Promise<void>
  }
}
