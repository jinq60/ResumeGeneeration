import { defineStore } from 'pinia'
import { ref } from 'vue'
import client from '@/api/client'
import type {
  ResumeDetailResponse,
  CreateResumeRequest,
  UpdateResumeRequest,
  Page,
  ResumeListItemResponse,
} from '@/api/types'

export const useResumeStore = defineStore('resume', () => {
  const list = ref<ResumeListItemResponse[]>([])
  const total = ref(0)
  const current = ref<ResumeDetailResponse | null>(null)

  async function fetchList(page = 1, size = 20, opts?: { keyword?: string; scene?: string; targetPosition?: string }) {
    const params: Record<string, any> = { page, size }
    if (opts?.keyword?.trim()) params.keyword = opts.keyword.trim()
    if (opts?.scene && opts.scene !== 'all') params.scene = opts.scene
    if (opts?.targetPosition?.trim()) params.targetPosition = opts.targetPosition.trim()
    const { data } = await client.get('/resumes', { params })
    const p = data?.data as Page<ResumeListItemResponse> | undefined
    if (!p || !Array.isArray(p.records)) throw new Error('简历列表契约破裂')
    list.value = p.records
    total.value = p.total
    return p
  }

  async function fetchOne(id: string) {
    const { data } = await client.get(`/resumes/${id}`)
    current.value = data.data
    return data.data as ResumeDetailResponse
  }

  async function create(payload: CreateResumeRequest) {
    const { data } = await client.post('/resumes', payload)
    return data.data as ResumeDetailResponse
  }

  async function update(id: string, payload: UpdateResumeRequest) {
    const { data } = await client.put(`/resumes/${id}`, payload)
    return data.data
  }

  async function remove(id: string) {
    await client.delete(`/resumes/${id}`)
  }

  return { list, total, current, fetchList, fetchOne, create, update, remove }
})
