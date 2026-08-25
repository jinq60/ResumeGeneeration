import { describe, it, expect } from 'vitest'
import { getJwtRole, parseJwtPayload } from '@/utils/jwt'

function makeJwt(payload: object): string {
  const encode = (obj: object) =>
    btoa(unescape(encodeURIComponent(JSON.stringify(obj))))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  return `header.${encode(payload)}.signature`
}

describe('utils/jwt', () => {
  it('应解析 base64url payload 并读取 role claim', () => {
    const token = makeJwt({ sub: 'admin_1', role: 'ADMIN' })
    expect(getJwtRole(token)).toBe('ADMIN')
  })

  it('应正确处理 UTF-8 中文等多字节字符', () => {
    const token = makeJwt({ sub: 'user_1', nickname: '张三丰', role: 'USER' })
    expect(parseJwtPayload(token)?.nickname).toBe('张三丰')
    expect(getJwtRole(token)).toBe('USER')
  })

  it('非 ADMIN 角色应返回其原始角色', () => {
    const token = makeJwt({ sub: 'user_1', role: 'USER' })
    expect(getJwtRole(token)).toBe('USER')
  })

  it('缺少 role claim 时返回 null', () => {
    const token = makeJwt({ sub: 'user_1' })
    expect(getJwtRole(token)).toBeNull()
  })

  it('非法令牌返回 null 而不抛出异常', () => {
    expect(getJwtRole('')).toBeNull()
    expect(getJwtRole('not-a-jwt')).toBeNull()
    expect(getJwtRole('header.@@invalid@@.signature')).toBeNull()
  })
})
