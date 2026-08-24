import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('setUser should update state and persist to localStorage', () => {
    const store = useUserStore()

    store.setUser({
      userId: 'user_1',
      nickname: '张三',
      isGuest: false,
      accessToken: 'token_123'
    })

    expect(store.userId).toBe('user_1')
    expect(store.nickname).toBe('张三')
    expect(store.isGuest).toBe(false)
    expect(store.accessToken).toBe('token_123')
    expect(store.isLoggedIn).toBe(true)
    // 令牌只存单一键，禁止 access_token 副本
    expect(JSON.parse(localStorage.getItem('resume_user_info') || '{}').userId).toBe('user_1')
    expect(JSON.parse(localStorage.getItem('resume_user_info') || '{}').accessToken).toBe('token_123')
    expect(localStorage.getItem('access_token')).toBeNull()
  })

  it('clearUser should reset state and remove storage', () => {
    const store = useUserStore()
    store.setUser({ userId: 'user_1', accessToken: 'token_123' })

    store.clearUser()

    expect(store.userId).toBeNull()
    expect(store.accessToken).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('access_token')).toBeNull()
    expect(localStorage.getItem('resume_user_info')).toBeNull()
  })

  it('restoreFromStorage should load persisted user info', () => {
    localStorage.setItem('resume_user_info', JSON.stringify({
      userId: 'guest_1',
      isGuest: true,
      accessToken: 'guest_token'
    }))

    const store = useUserStore()
    store.restoreFromStorage()

    expect(store.userId).toBe('guest_1')
    expect(store.isGuest).toBe(true)
    expect(store.accessToken).toBe('guest_token')
    expect(localStorage.getItem('access_token')).toBeNull()
  })

  it('restoreFromStorage should migrate legacy duplicate key storage', () => {
    localStorage.setItem('resume_user_info', JSON.stringify({
      userId: 'user_legacy',
      isGuest: false,
      accessToken: 'legacy_token'
    }))
    localStorage.setItem('access_token', 'legacy_token')

    const store = useUserStore()
    store.restoreFromStorage()

    expect(store.accessToken).toBe('legacy_token')
    // 迁移后清理历史副本键
    expect(localStorage.getItem('access_token')).toBeNull()
  })
})
