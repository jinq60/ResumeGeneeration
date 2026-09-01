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

  async function fetchList(page = 1, size = 20) {
    const { data } = await client.get('/resumes', { params: { page, size } })
    const p: Page<ResumeListItemResponse> = data.data
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
