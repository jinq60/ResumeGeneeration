import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import { setActivePinia, createPinia } from 'pinia'
import request, { ApiError } from '@/utils/request'
import { useUserStore } from '@/stores/user'

/**
 * 模拟 axios adapter：根据 handler 决定响应。
 * 非 2xx 时按 axios 约定 reject 带 response 的错误，触发拦截器错误分支。
 */
type Responder = (config: Record<string, unknown>) => Promise<{ status: number; data: unknown }>

function makeAdapter(responder: Responder) {
  return async (config: Record<string, any>) => {
    const { status, data } = await responder(config)
    if (status >= 200 && status < 300) {
      return { data, status, statusText: '', headers: {}, config }
    }
    const error = new Error('Request failed with status code ' + status) as any
    error.config = config
    error.response = { data, status, statusText: '', headers: {}, config }
    throw error
  }
}

function get(url: string, responder: Responder) {
  return request.get(url, { adapter: makeAdapter(responder) } as any)
}

describe('request 拦截器', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('业务错误（HTTP 200 + code!=200）应 reject ApiError 并携带 R.code', async () => {
    const promise = get('/things', async () => ({
      status: 200,
      data: { code: 6004, message: 'AI 并发超限', data: null }
    }))

    await expect(promise).rejects.toBeInstanceOf(ApiError)
    await expect(promise).rejects.toMatchObject({
      code: 6004,
      httpStatus: 200,
      message: 'AI 并发超限'
    })
  })

  it('HTTP 业务错误（如乐观锁 409/2012）应携带 code 与 httpStatus', async () => {
    const promise = get('/resumes/1', async () => ({
      status: 409,
      data: { code: 2012, message: '内容已被他人修改', data: null }
    }))

    await expect(promise).rejects.toMatchObject({ code: 2012, httpStatus: 409 })
  })

  it('401 时应单飞刷新并重放请求；并发请求只触发一次刷新', async () => {
    const userStore = useUserStore()
    userStore.setUser({
      userId: 'user_1',
      isGuest: false,
      accessToken: 'old-token',
      refreshToken: 'old-refresh'
    })

    const refreshSpy = vi.spyOn(axios, 'post').mockImplementation(async () => {
      // 人为延迟，确保第二个并发 401 到达时刷新仍在进行
      await new Promise((resolve) => setTimeout(resolve, 20))
      return {
        data: { code: 200, data: { accessToken: 'new-token', refreshToken: 'new-refresh' } }
      }
    })

    const authHeaders: string[] = []
    let callCount = 0
    const responder: Responder = async (config: any) => {
      callCount += 1
      authHeaders.push(String(config.headers?.Authorization || ''))
      if (config.headers?.Authorization === 'Bearer new-token') {
        return { status: 200, data: { code: 200, message: 'ok', data: 'fine' } }
      }
      return { status: 401, data: { code: 2001, message: '未认证', data: null } }
    }

    const [a, b] = await Promise.all([
      get('/resumes', responder),
      get('/resumes', responder)
    ])

    expect(a).toBe('fine')
    expect(b).toBe('fine')
    // 两次初始请求 + 两次重放
    expect(callCount).toBe(4)
    // 单飞：刷新接口只调用一次
    expect(refreshSpy).toHaveBeenCalledTimes(1)
    // 重放均携带新令牌
    expect(authHeaders.filter((h) => h === 'Bearer new-token')).toHaveLength(2)

    const stored = JSON.parse(localStorage.getItem('resume_user_info') || '{}')
    expect(stored.accessToken).toBe('new-token')
    expect(stored.refreshToken).toBe('new-refresh')
  })

  it('刷新失败时应清理 Pinia 用户态与本地存储并 reject 401 ApiError', async () => {
    const userStore = useUserStore()
    userStore.setUser({
      userId: 'user_1',
      isGuest: false,
      accessToken: 'expired-token',
      refreshToken: 'expired-refresh'
    })
    expect(userStore.isLoggedIn).toBe(true)

    vi.spyOn(axios, 'post').mockRejectedValue(
      Object.assign(new Error('invalid grant'), { response: { status: 401, data: {} } })
    )

    const promise = get('/resumes', async () => ({
      status: 401,
      data: { code: 2001, message: '未认证', data: null }
    }))

    await expect(promise).rejects.toMatchObject({
      httpStatus: 401,
      message: '登录已过期，请重新登录'
    })
    // 关键回归点：Pinia 用户态同步清空，避免侧边栏仍显示已登录
    expect(userStore.isLoggedIn).toBe(false)
    expect(userStore.accessToken).toBe('')
    expect(localStorage.getItem('resume_user_info')).toBeNull()
  })
})
