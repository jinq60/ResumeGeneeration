import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useResumeConflict, isVersionConflictError } from '@/composables/useResumeConflict'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

import { ElMessage, ElMessageBox } from 'element-plus'

describe('isVersionConflictError', () => {
  it('detects business code 2012', () => {
    expect(isVersionConflictError({ code: 2012 })).toBe(true)
    expect(isVersionConflictError({ code: '2012' })).toBe(true)
  })

  it('detects http status 409', () => {
    expect(isVersionConflictError({ httpStatus: 409 })).toBe(true)
    expect(isVersionConflictError({ status: 409 })).toBe(true)
  })

  it('rejects unrelated errors', () => {
    expect(isVersionConflictError({ code: 6004, httpStatus: 429 })).toBe(false)
    expect(isVersionConflictError(new Error('boom'))).toBe(false)
    expect(isVersionConflictError(null)).toBe(false)
    expect(isVersionConflictError(undefined)).toBe(false)
  })
})

describe('useResumeConflict', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('backs up draft and fetches latest when user confirms', async () => {
    ;(ElMessageBox.confirm as any).mockResolvedValue(true)
    const backupDraft = vi.fn()
    const fetchLatest = vi.fn().mockResolvedValue(undefined)
    const { resolveConflict } = useResumeConflict()

    const applied = await resolveConflict({ backupDraft, fetchLatest })

    expect(applied).toBe(true)
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      expect.stringContaining('其他窗口'),
      '简历版本冲突',
      expect.objectContaining({ type: 'warning' })
    )
    // 先备份本地草稿，再拉取服务器最新版本
    expect(backupDraft).toHaveBeenCalledTimes(1)
    expect(fetchLatest).toHaveBeenCalledTimes(1)
    expect(vi.mocked(backupDraft).mock.invocationCallOrder[0])
      .toBeLessThan(vi.mocked(fetchLatest).mock.invocationCallOrder[0])
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('keeps local state untouched when user cancels', async () => {
    ;(ElMessageBox.confirm as any).mockRejectedValue(new Error('cancel'))
    const backupDraft = vi.fn()
    const fetchLatest = vi.fn()
    const { resolveConflict } = useResumeConflict()

    const applied = await resolveConflict({ backupDraft, fetchLatest })

    expect(applied).toBe(false)
    expect(backupDraft).not.toHaveBeenCalled()
    expect(fetchLatest).not.toHaveBeenCalled()
  })

  it('shows error and skips reload when fetching latest fails', async () => {
    ;(ElMessageBox.confirm as any).mockResolvedValue(true)
    const fetchLatest = vi.fn().mockRejectedValue(new Error('网络异常'))
    const { resolveConflict } = useResumeConflict()

    const applied = await resolveConflict({ backupDraft: vi.fn(), fetchLatest })

    expect(applied).toBe(false)
    expect(ElMessage.error).toHaveBeenCalledWith('网络异常')
  })

  it('ignores re-entrant calls while a conflict is being resolved', async () => {
    let releaseConfirm!: (value: unknown) => void
    ;(ElMessageBox.confirm as any).mockImplementation(
      () => new Promise((resolve) => { releaseConfirm = resolve })
    )
    const { resolving, resolveConflict } = useResumeConflict()

    const first = resolveConflict({ backupDraft: vi.fn(), fetchLatest: vi.fn() })
    const second = await resolveConflict({ backupDraft: vi.fn(), fetchLatest: vi.fn() })

    expect(second).toBe(false)
    releaseConfirm(undefined)
    await first
    expect(resolving.value).toBe(false)
  })
})
