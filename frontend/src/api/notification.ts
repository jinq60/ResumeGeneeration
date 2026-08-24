import request from '@/utils/request'
import type { Page } from '@/api/resume'

export type NotificationType = 'pdf' | 'avatar' | 'ai' | 'system'

export interface NotificationItem {
  id: string
  type: NotificationType
  title: string
  content: string
  read: boolean
  createdAt: string
}

export const notificationApi = {
  list(params?: {
    page?: number
    size?: number
    unreadOnly?: boolean
  }): Promise<Page<NotificationItem>> {
    return request.get('/notifications', { params }) as Promise<Page<NotificationItem>>
  },
  unreadCount(): Promise<{ count: number }> {
    return request.get('/notifications/unread-count') as Promise<{ count: number }>
  },
  markRead(id: string): Promise<void> {
    return request.put(`/notifications/${id}/read`) as Promise<void>
  },
  markAllRead(): Promise<void> {
    return request.put('/notifications/read-all') as Promise<void>
  },
  remove(id: string): Promise<void> {
    return request.delete(`/notifications/${id}`) as Promise<void>
  }
}