import { ref } from 'vue'
import client from '@/api/client'
import type { ApiResp } from '@/api/types'

export interface AvatarUploadResponse {
  id: string
  sourceImageUrl: string
  fileName: string
}

export interface AvatarTaskResponse {
  taskId: string
  status: 'pending' | 'processing' | 'success' | 'failed'
  sourceImageUrl: string
  resultImageUrl?: string
  errorMsg?: string
  createdAt: string
  completedAt?: string
}

export interface OptimizeAvatarRequest {
  sourceImageUrl: string
  resumeId?: string
  backgroundType: 'white' | 'blue' | 'red'
  style: 'formal' | 'natural' | 'professional'
}

export function useAvatar() {
  const uploading = ref(false)
  const optimizing = ref(false)

  async function uploadAvatar(file: File, resumeId?: string): Promise<AvatarUploadResponse> {
    uploading.value = true
    try {
      const { data } = await client.postForm<ApiResp<AvatarUploadResponse>>('/avatars/upload', {
        file,
        ...(resumeId ? { resumeId } : {}),
      })
      if (data.code !== 200) throw new Error(data.message || '上传失败')
      return data.data
    } finally {
      uploading.value = false
    }
  }

  async function createOptimizeTask(payload: OptimizeAvatarRequest): Promise<string> {
    const { data } = await client.post<ApiResp<{ taskId: string }>>('/avatars/optimize', payload)
    if (data.code !== 200) throw new Error(data.message || '创建优化任务失败')
    return (data.data as any).taskId ?? (data.data as any).task_id ?? ''
  }

  async function getTask(taskId: string): Promise<AvatarTaskResponse> {
    const { data } = await client.get<ApiResp<AvatarTaskResponse>>(`/avatars/tasks/${taskId}`)
    if (data.code !== 200) throw new Error(data.message || '查询任务失败')
    return data.data
  }

  async function pollTask(taskId: string, intervalMs = 1000): Promise<AvatarTaskResponse> {
    return new Promise((resolve, reject) => {
      const timer = setInterval(async () => {
        try {
          const task = await getTask(taskId)
          if (task.status === 'success' || task.status === 'failed') {
            clearInterval(timer)
            if (task.status === 'success') resolve(task)
            else reject(new Error(task.errorMsg || '优化失败'))
          }
        } catch (e: any) {
          clearInterval(timer)
          reject(e)
        }
      }, intervalMs)
    })
  }

  return { uploading, optimizing, uploadAvatar, createOptimizeTask, getTask, pollTask }
}
