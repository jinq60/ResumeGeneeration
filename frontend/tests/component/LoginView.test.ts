import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAuthModalStore } from '@/stores/authModal'
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
    oauthExchange: vi.fn()
  }
}))

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
    vi.mocked(authApi.oauthExchange).mockReset()
  })

  it('oauth callback with one-time code should exchange tokens and redirect to dashboard', async () => {
    vi.mocked(authApi.oauthExchange).mockResolvedValue({
      userId: 'user_1',
      accessToken: 'oauth_token',
      refreshToken: 'oauth_refresh',
      expiresIn: 3600,
      isGuest: false
    })
    mockQuery = { oauth_code: 'one-time-code' }

    mountView()
    await flushPromises()

    expect(authApi.oauthExchange).toHaveBeenCalledWith('one-time-code')
    const store = useUserStore()
    expect(store.accessToken).toBe('oauth_token')
    expect(store.refreshToken).toBe('oauth_refresh')
    // JWT 不经 URL 传递：URL 中只应有授权码
    expect(mockQuery.token).toBeUndefined()
    expect(replaceMock).toHaveBeenCalledWith('/workbench/dashboard')
  })

  it('oauth callback with failed exchange should open auth modal and stay home', async () => {
    vi.mocked(authApi.oauthExchange).mockRejectedValue(new Error('授权码无效或已过期'))
    mockQuery = { oauth_code: 'expired-code' }

    mountView()
    await flushPromises()

    const store = useUserStore()
    expect(store.accessToken).toBe('')
    const modalStore = useAuthModalStore()
    expect(modalStore.isOpen).toBe(true)
    expect(replaceMock).toHaveBeenCalledWith('/')
  })

  it('oauth callback with error should open auth modal and redirect to home', async () => {
    mockQuery = { error: '第三方登录失败' }

    mountView()
    await flushPromises()

    const modalStore = useAuthModalStore()
    expect(modalStore.isOpen).toBe(true)
    // 错误文本在清除 URL query 前转存到 authModalStore，供 LoginModal 打开时消费
    expect(modalStore.oauthError).toBe('第三方登录失败')
    expect(replaceMock).toHaveBeenCalledWith('/')
  })

  it('normal /login visit should open auth modal and redirect to home', async () => {
    mountView()
    await flushPromises()

    const modalStore = useAuthModalStore()
    expect(modalStore.isOpen).toBe(true)
    expect(replaceMock).toHaveBeenCalledWith('/')
  })
})
