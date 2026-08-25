/**
 * 携带后端统一响应业务码与 HTTP 状态码的错误对象。
 *
 * 拦截器 reject 时统一使用本类型，下游可通过 `error.code === 6004`
 * （AI 并发限流）、`error.code === 2012`（乐观锁冲突）等业务码做分支判断。
 */
export class ApiError extends Error {
  /** 后端 R.code 业务错误码；无法获取时为 undefined */
  code?: number
  /** HTTP 状态码（401/409/429 等）；网络层错误时为 undefined */
  httpStatus?: number

  constructor(message: string, options?: { code?: number; httpStatus?: number }) {
    super(message)
    this.name = 'ApiError'
    this.code = options?.code
    this.httpStatus = options?.httpStatus
  }
}
