import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import LoginView from '@/views/LoginView.vue'

const pushMock = vi.fn()
const replaceMock = vi.fn()
let mockQuery: Record<string, string | string[]> = {}

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
    replace: replaceMock
  }),
  useRoute: () => ({
    query: mockQuery
  })
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    guest: vi.fn(),
    emailCodeSend: vi.fn(),
    emailCodeLogin: vi.fn()
  },
  oauthAuthorizeUrl: vi.fn((provider: string) => `/api/auth/oauth/${provider}/authorize`)
}))

const authResponse = {
  userId: 'user_1',
  accessToken: 'token_1',
  refreshToken: 'refresh_1',
  expiresIn: 3600,
  isGuest: false
}

function mountView() {
  return mount(LoginView, {
    global: {
      plugins: [ElementPlus]
    }
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    pushMock.mockClear()
    replaceMock.mockClear()
    mockQuery = {}
    ;(authApi.guest as any).mockReset()
    ;(authApi.login as any).mockReset()
    ;(authApi.register as any).mockReset()
    ;(authApi.emailCodeSend as any).mockReset()
    ;(authApi.emailCodeLogin as any).mockReset()
  })

  it('guest mode should call authApi.guest, update store and redirect', async () => {
    (authApi.guest as any).mockResolvedValue({ ...authResponse, userId: 'guest_1', isGuest: true })

    const wrapper = mountView()
    await wrapper.find('button.login-ghost').trigger('click')
    await flushPromises()

    expect(authApi.guest).toHaveBeenCalled()
    const store = useUserStore()
    expect(store.userId).toBe('guest_1')
    expect(store.isGuest).toBe(true)
    expect(store.accessToken).toBe('token_1')
    expect(pushMock).toHaveBeenCalledWith('/workbench/dashboard')
  })

  it('password login should call authApi.login and redirect', async () => {
    (authApi.login as any).mockResolvedValue(authResponse)

    const wrapper = mountView()
    await wrapper.find('input[placeholder="请输入手机号或邮箱"]').setValue('demo@example.com')
    await wrapper.find('input[placeholder="请输入 8-32 位密码"]').setValue('Passw0rd123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith({
      account: 'demo@example.com',
      password: 'Passw0rd123',
      loginType: 'email'
    })
    expect(pushMock).toHaveBeenCalledWith('/workbench/dashboard')
  })

  it('email code login should send code with countdown and login', async () => {
    (authApi.emailCodeSend as any).mockResolvedValue(undefined)
    ;(authApi.emailCodeLogin as any).mockResolvedValue(authResponse)

    const wrapper = mountView()
    // 切到邮箱验证码方式
    await wrapper.findAll('button').find(b => b.text() === '邮箱验证码')!.trigger('click')
    await wrapper.find('input[placeholder="请输入邮箱"]').setValue('demo@example.com')
    await wrapper.findAll('button').find(b => b.text() === '获取验证码')!.trigger('click')
    await flushPromises()

    expect(authApi.emailCodeSend).toHaveBeenCalledWith('demo@example.com')
    expect(wrapper.text()).toContain('s 后重发')

    await wrapper.find('input[placeholder="6 位验证码"]').setValue('123456')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.emailCodeLogin).toHaveBeenCalledWith({ email: 'demo@example.com', code: '123456' })
    expect(pushMock).toHaveBeenCalledWith('/workbench/dashboard')
  })

  it('oauth callback with token should authenticate and redirect', async () => {
    mockQuery = { token: 'oauth_token', refresh: 'oauth_refresh', guest: 'false' }

    mountView()
    await flushPromises()

    const store = useUserStore()
    expect(store.accessToken).toBe('oauth_token')
    expect(store.refreshToken).toBe('oauth_refresh')
    expect(replaceMock).toHaveBeenCalledWith('/workbench/dashboard')
  })

  it('oauth callback with error should show message', async () => {
    mockQuery = { error: '第三方登录失败' }

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('第三方登录失败')
  })
})
