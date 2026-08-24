import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/auth/LoginModal.vue'
import { authApi } from '@/api/auth'

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
    emailCodeSend: vi.fn(),
    emailCodeLogin: vi.fn(),
    smsCodeSend: vi.fn(),
    smsCodeLogin: vi.fn(),
    loginMethods: vi.fn()
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

function mountModal(modelValue = true) {
  return mount(LoginModal, {
    props: { modelValue },
    attachTo: document.body,
    global: {
      plugins: [ElementPlus]
    }
  })
}

function queryByPlaceholder(placeholder: string): HTMLInputElement | null {
  return document.body.querySelector(`input[placeholder="${placeholder}"]`)
}

describe('LoginModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    pushMock.mockClear()
    replaceMock.mockClear()
    mockQuery = {}
    ;(authApi.login as any).mockReset()
    ;(authApi.emailCodeSend as any).mockReset()
    ;(authApi.emailCodeLogin as any).mockReset()
    ;(authApi.loginMethods as any).mockReset()
    ;(authApi.loginMethods as any).mockResolvedValue({
      loginMethods: [
        { method: 'password', configured: true },
        { method: 'email_code', configured: true },
        { method: 'sms_code', configured: false }
      ],
      oauthProviders: [
        { provider: 'github', configured: true },
        { provider: 'google', configured: false },
        { provider: 'qq', configured: false }
      ]
    })
  })

  it('renders login form when open', async () => {
    const wrapper = mountModal(true)
    await flushPromises()

    expect(document.body.textContent).toContain('欢迎回来')
    expect(document.body.textContent).toContain('账号密码')
    wrapper.unmount()
  })

  it('does not render when closed', () => {
    const wrapper = mountModal(false)
    expect(document.body.textContent).not.toContain('欢迎回来')
    wrapper.unmount()
  })

  it('password login should call authApi.login and redirect', async () => {
    ;(authApi.login as any).mockResolvedValue(authResponse)

    const wrapper = mountModal(true)
    await flushPromises()

    const accountInput = queryByPlaceholder('请输入手机号或邮箱')!
    const passwordInput = queryByPlaceholder('请输入 8-32 位密码')!

    accountInput.value = 'demo@example.com'
    accountInput.dispatchEvent(new Event('input'))
    passwordInput.value = 'Passw0rd123'
    passwordInput.dispatchEvent(new Event('input'))

    const form = document.body.querySelector('form')!
    form.dispatchEvent(new Event('submit'))
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith({
      account: 'demo@example.com',
      password: 'Passw0rd123',
      loginType: 'email'
    })
    expect(pushMock).toHaveBeenCalledWith('/workbench/dashboard')
    wrapper.unmount()
  })

  it('emits close when clicking close button', async () => {
    const wrapper = mountModal(true)
    await flushPromises()

    const closeButton = document.body.querySelector('button[type="button"]')!
    closeButton.dispatchEvent(new Event('click'))
    await flushPromises()

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false])
    wrapper.unmount()
  })

  it('hides login methods and OAuth providers that are not configured', async () => {
    const wrapper = mountModal(true)
    await flushPromises()

    const buttons = Array.from(document.body.querySelectorAll('button[type="button"]'))

    const smsButton = buttons.find(b => b.textContent?.includes('手机验证码'))
    expect(smsButton).toBeFalsy()

    const qqImg = document.body.querySelector('img[alt="QQ"]')
    expect(qqImg).toBeFalsy()

    wrapper.unmount()
  })

  it('enables sms_code login and configured OAuth providers', async () => {
    ;(authApi.loginMethods as any).mockResolvedValue({
      loginMethods: [
        { method: 'sms_code', configured: true },
        { method: 'password', configured: true },
        { method: 'email_code', configured: true }
      ],
      oauthProviders: [
        { provider: 'github', configured: true },
        { provider: 'qq', configured: true }
      ]
    })
    ;(authApi.smsCodeLogin as any).mockResolvedValue(authResponse)

    const wrapper = mountModal(true)
    await flushPromises()

    const buttons = Array.from(document.body.querySelectorAll('button[type="button"]'))
    const smsButton = buttons.find(b => b.textContent?.includes('手机验证码'))

    expect(smsButton).toBeTruthy()
    expect(smsButton?.hasAttribute('disabled')).toBe(false)

    // 切换到短信验证码登录
    smsButton?.dispatchEvent(new Event('click'))
    await flushPromises()
    expect(document.body.querySelector('input[placeholder="请输入手机号"]')).toBeTruthy()

    // 已配置的 QQ 可用
    const qqButton = document.body.querySelector('img[alt="QQ"]')?.closest('button')
    expect(qqButton).toBeTruthy()
    expect(qqButton?.hasAttribute('disabled')).toBe(false)

    // 提交短信验证码登录
    const phoneInput = document.body.querySelector('input[placeholder="请输入手机号"]') as HTMLInputElement
    phoneInput.value = '13800138000'
    phoneInput.dispatchEvent(new Event('input'))
    const codeInput = document.body.querySelector('input[placeholder="6 位验证码"]') as HTMLInputElement
    codeInput.value = '123456'
    codeInput.dispatchEvent(new Event('input'))
    const form = document.body.querySelector('form')!
    form.dispatchEvent(new Event('submit'))
    await flushPromises()

    expect(authApi.smsCodeLogin).toHaveBeenCalledWith({ phone: '13800138000', code: '123456' })
    expect(pushMock).toHaveBeenCalledWith('/workbench/dashboard')

    wrapper.unmount()
  })
})
