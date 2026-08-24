import request from '@/utils/request'
import type { Page } from '@/api/resume'

export interface DeliveryRecord {
  id: string
  userId: string
  resumeId: string
  company: string
  position: string
  channel?: string
  status: string
  applyDate?: string
  jdContent?: string
  note?: string
  interviewTime?: string
  interviewLocation?: string
  createdAt: string
  updatedAt: string
}

export interface DeliveryRecordRequest {
  resumeId: string
  company: string
  position: string
  channel?: string
  status?: string
  applyDate?: string
  jdContent?: string
  note?: string
  interviewTime?: string
  interviewLocation?: string
}

export const DELIVERY_STATUS_LABELS: Record<string, string> = {
  delivered: '已投递',
  written: '笔试',
  interview1: '一面',
  interview2: '二面',
  hr: 'HR 面',
  offer: 'Offer',
  rejected: '已拒绝',
  withdrawn: '已放弃'
}

export const DELIVERY_STATUSES = Object.keys(DELIVERY_STATUS_LABELS)

export const deliveryApi = {
  list(params?: {
    page?: number
    size?: number
    keyword?: string
    company?: string
    position?: string
    status?: string
    startDate?: string
    endDate?: string
  }): Promise<Page<DeliveryRecord>> {
    return request.get('/deliveries', { params }) as Promise<Page<DeliveryRecord>>
  },
  get(id: string): Promise<DeliveryRecord> {
    return request.get(`/deliveries/${id}`) as Promise<DeliveryRecord>
  },
  create(data: DeliveryRecordRequest): Promise<DeliveryRecord> {
    return request.post('/deliveries', data) as Promise<DeliveryRecord>
  },
  update(id: string, data: DeliveryRecordRequest): Promise<DeliveryRecord> {
    return request.put(`/deliveries/${id}`, data) as Promise<DeliveryRecord>
  },
  remove(id: string): Promise<void> {
    return request.delete(`/deliveries/${id}`) as Promise<void>
  }
}