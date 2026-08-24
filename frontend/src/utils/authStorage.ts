/**
 * 用户端令牌与登录态的唯一持久化入口。
 *
 * 令牌只存于 `resume_user_info` 一个键内（单一事实来源），
 * 禁止再向其他键写入副本，避免多副本不同步与清理遗漏。
 */

const STORAGE_KEY = 'resume_user_info'

// 历史版本曾单独维护 access_token 键：读取时兜底兼容，写入时顺带清理
const LEGACY_ACCESS_TOKEN_KEY = 'access_token'

export interface StoredAuth {
  userId: string
  nickname?: string
  avatar?: string
  isGuest?: boolean
  accessToken: string
  refreshToken?: string
}

export function readStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredAuth
  } catch {
    return null
  }
}

export function writeStoredAuth(info: StoredAuth): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(info))
  localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY)
}

export function getAccessToken(): string {
  const stored = readStoredAuth()
  if (stored?.accessToken) return stored.accessToken
  // 兼容尚未迁移的历史数据
  return localStorage.getItem(LEGACY_ACCESS_TOKEN_KEY) || ''
}

export function updateStoredTokens(accessToken: string, refreshToken: string): void {
  const current = readStoredAuth()
  if (!current) return
  writeStoredAuth({ ...current, accessToken, refreshToken })
}

export function clearStoredAuth(): void {
  localStorage.removeItem(STORAGE_KEY)
  localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY)
}
