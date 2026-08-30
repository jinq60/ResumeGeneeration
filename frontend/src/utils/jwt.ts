/**
 * JWT payload 解析工具。
 *
 * 仅做客户端解析（不校验签名），用于路由守卫等本地预判场景；
 * 真正的权限校验仍以后端为准。
 */

/** 解析 JWT payload（第二段），兼容 base64url 与 UTF-8 多字节字符；解析失败返回 null。 */
export function parseJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const segments = token.split('.')
    if (segments.length < 2 || !segments[1]) {
      return null
    }
    let base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    // 补 padding：atob 要求长度为 4 的倍数
    base64 += '='.repeat((4 - (base64.length % 4)) % 4)
    const binary = atob(base64)
    // atob 输出是 Latin-1 字节串，中文等多字节字符需按 UTF-8 还原
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
    const json = new TextDecoder('utf-8').decode(bytes)
    const payload = JSON.parse(json)
    return typeof payload === 'object' && payload !== null ? (payload as Record<string, unknown>) : null
  } catch {
    return null
  }
}

/** 读取指定 claim，不存在返回 undefined。 */
export function getJwtClaim(token: string, claim: string): unknown {
  return parseJwtPayload(token)?.[claim]
}

/** 读取 role claim，非字符串时返回 null。 */
export function getJwtRole(token: string): string | null {
  const role = getJwtClaim(token, 'role')
  return typeof role === 'string' ? role : null
}

/** 判断 token 是否已过期（exp 为秒级时间戳）；过期或无 exp 返回 true，带 60s 时钟容差。 */
export function isJwtExpired(token: string): boolean {
  const payload = parseJwtPayload(token)
  if (!payload) return true
  const exp = payload.exp
  if (typeof exp !== 'number') return true
  return exp * 1000 - 60_000 < Date.now()
}
