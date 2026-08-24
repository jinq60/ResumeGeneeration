import adminRequest from '@/utils/adminRequest'
import type { DeliveryRecord } from '@/api/delivery'

export interface DeliveryStats {
  totalDeliveries: number
  statusCounts: Record<string, number>
  topJobs: Array<{ name: string; count: number; percent: number }>
}

export interface AdminDeliveryPage {
  records: DeliveryRecord[]
  total: number
  size: number
  current: number
  pages: number
}

export const adminDeliveryApi = {
  list(params?: {
    page?: number
    size?: number
    keyword?: string
  }): Promise<AdminDeliveryPage> {
    return adminRequest.get('/admin/deliveries', { params })
  },
  stats(): Promise<DeliveryStats> {
    return adminRequest.get('/admin/deliveries/stats')
  },
  exportUrl(): string {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
    return `${base}/admin/deliveries/export`
  }
}