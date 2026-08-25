import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import adminRequest from '@/utils/adminRequest'

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

// jsdom 下 window.location 不可直接赋值，用可写替身捕获跳转目标
const originalLocation = window.location
let hrefTarget = ''

beforeEach(() => {
  localStorage.clear()
  hrefTarget = ''
  vi.restoreAllMocks()
  Object.defineProperty(window, 'location', {
    writable: true,
    value: {
      get href() {
        return hrefTarget
      },
      set href(value: string) {
        hrefTarget = value
      }
    }
  })
})

afterEach(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation
  })
})

describe('adminRequest 拦截器', () => {
  it('登录成功时落地 refreshToken，供后续静默刷新使用', async () => {
    await adminRequest.post(
      '/auth/login',
      { account: 'admin', password: 'secret' },
      {
        adapter: makeAdapter(async () => ({
          status: 200,
          data: {
            code: 200,
            message: 'ok',
            data: { userId: 'admin_1', accessToken: 'at', refreshToken: 'rt', expiresIn: 3600 }
          }
        }))
      } as any
    )

    expect(localStorage.getItem('admin_token')).toBeNull() // token 由调用方（登录页）写入
    expect(localStorage.getItem('admin_refresh_token')).toBe('rt')
  })

  it('401 时应单飞刷新并重放原请求一次，成功后不跳转登录页', async () => {
    localStorage.setItem('admin_token', 'expired-token')
    localStorage.setItem('admin_refresh_token', 'valid-rt')

    const refreshSpy = vi.spyOn(axios, 'post').mockImplementation(async () => {
      await new Promise((resolve) => setTimeout(resolve, 20))
      return {
        data: { code: 200, data: { accessToken: 'new-admin-token', refreshToken: 'new-admin-rt' } }
      }
    })

    let callCount = 0
    const responder: Responder = async (config: any) => {
      callCount += 1
      if (config.headers?.Authorization === 'Bearer new-admin-token') {
        return { status: 200, data: { code: 200, message: 'ok', data: [{ userId: 'u1' }] } }
      }
      return { status: 401, data: { code: 2001, message: '未认证', data: null } }
    }

    const [a, b] = await Promise.all([
      adminRequest.get('/admin/users', { adapter: makeAdapter(responder) } as any),
      adminRequest.get('/admin/users', { adapter: makeAdapter(responder) } as any)
    ])

    expect(a).toEqual([{ userId: 'u1' }])
    expect(b).toEqual([{ userId: 'u1' }])
    expect(refreshSpy).toHaveBeenCalledTimes(1)

    expect(localStorage.getItem('admin_token')).toBe('new-admin-token')
    expect(localStorage.getItem('admin_refresh_token')).toBe('new-admin-rt')
    expect(hrefTarget).toBe('')
  })

  it('刷新失败时应清理管理端令牌、提示并跳转 /admin/login', async () => {
    localStorage.setItem('admin_token', 'expired-token')
    localStorage.setItem('admin_refresh_token', 'invalid-rt')

    vi.spyOn(axios, 'post').mockRejectedValue(
      Object.assign(new Error('invalid grant'), { response: { status: 401, data: {} } })
    )
    const errorSpy = vi
      .spyOn(ElMessage, 'error')
      .mockImplementation((() => ({ close: () => undefined })) as any)

    const promise = adminRequest.get('/admin/users', {
      adapter: makeAdapter(async () => ({
        status: 401,
        data: { code: 2001, message: '未认证', data: null }
      }))
    } as any)

    await expect(promise).rejects.toMatchObject({
      httpStatus: 401,
      message: '登录已过期，请重新登录'
    })

    expect(errorSpy).toHaveBeenCalled()
    expect(localStorage.getItem('admin_token')).toBeNull()
    expect(localStorage.getItem('admin_refresh_token')).toBeNull()
    expect(hrefTarget).toBe('/admin/login')
  })

  it('业务错误应携带 R.code 与 httpStatus', async () => {
    const promise = adminRequest.get('/admin/users/stats', {
      adapter: makeAdapter(async () => ({
        status: 429,
        data: { code: 6004, message: 'AI 并发超限', data: null }
      }))
    } as any)

    await expect(promise).rejects.toMatchObject({ code: 6004, httpStatus: 429 })
  })
})
