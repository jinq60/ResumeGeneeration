import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import LoginView from '@/views/LoginView.vue'

const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    guest: vi.fn()
  }
}))

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    pushMock.mockClear()
    ;(authApi.guest as any).mockReset()
  })

  it('guest mode should call authApi.guest, update store and redirect', async () => {
    (authApi.guest as any).mockResolvedValue({
      userId: 'guest_1',
      accessToken: 'guest_token',
      refreshToken: 'refresh',
      expiresIn: 3600,
      isGuest: true
    })

    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus]
      }
    })

    // 切到登录标签（默认即登录）后点击游客按钮
    await wrapper.find('button.login-ghost').trigger('click')
    await flushPromises()

    expect(authApi.guest).toHaveBeenCalled()
    const store = useUserStore()
    expect(store.userId).toBe('guest_1')
    expect(store.isGuest).toBe(true)
    expect(store.accessToken).toBe('guest_token')
    expect(pushMock).toHaveBeenCalledWith('/dashboard')
  })
})
