import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useAutoSave } from '@/composables/useAutoSave'

describe('useAutoSave', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should debounce save by 2 seconds', () => {
    const { triggerSave, saveStatus } = useAutoSave()
    const saveFn = vi.fn().mockResolvedValue(undefined)

    triggerSave(saveFn)
    expect(saveFn).not.toHaveBeenCalled()
    expect(saveStatus.value).toBe('unsaved')

    vi.advanceTimersByTime(2000)
    expect(saveFn).toHaveBeenCalledTimes(1)
  })

  it('should reset debounce on rapid triggers', () => {
    const { triggerSave } = useAutoSave()
    const saveFn = vi.fn().mockResolvedValue(undefined)

    triggerSave(saveFn)
    vi.advanceTimersByTime(1500)
    triggerSave(saveFn)
    vi.advanceTimersByTime(1500)
    expect(saveFn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(500)
    expect(saveFn).toHaveBeenCalledTimes(1)
  })

  it('should flush pending save before unmount', async () => {
    const { triggerSave, flush } = useAutoSave()
    const saveFn = vi.fn().mockResolvedValue(undefined)

    triggerSave(saveFn)
    vi.advanceTimersByTime(1000)
    flush()
    await vi.runAllTimersAsync()
    expect(saveFn).toHaveBeenCalledTimes(1)
  })

  it('should mark status as saved after successful save', async () => {
    const { triggerSave, saveStatus } = useAutoSave()
    const saveFn = vi.fn().mockResolvedValue(undefined)

    triggerSave(saveFn)
    vi.advanceTimersByTime(2000)
    await vi.runAllTimersAsync()
    expect(saveStatus.value).toBe('saved')
  })

  it('should mark status as error when save fails', async () => {
    const { triggerSave, saveStatus } = useAutoSave()
    const saveFn = vi.fn().mockRejectedValue(new Error('boom'))

    triggerSave(saveFn)
    vi.advanceTimersByTime(2000)
    await vi.runAllTimersAsync()
    expect(saveStatus.value).toBe('error')
  })

  it('should restore saved status when a later queued save succeeds', async () => {
    const { triggerSave, saveStatus } = useAutoSave()
    const saveFn = vi.fn()
      .mockRejectedValueOnce(new Error('boom'))
      .mockResolvedValueOnce(undefined)

    triggerSave(saveFn)
    vi.advanceTimersByTime(2000)
    await vi.runAllTimersAsync()
    expect(saveStatus.value).toBe('error')

    // 后续保存成功后应从 error 恢复，而不是永久卡死在 error
    triggerSave(saveFn)
    vi.advanceTimersByTime(2000)
    await vi.runAllTimersAsync()
    expect(saveStatus.value).toBe('saved')
  })

  it('retry re-enqueues the last save function', async () => {
    const { triggerSave, retry, saveStatus } = useAutoSave()
    const saveFn = vi.fn().mockRejectedValue(new Error('boom'))

    triggerSave(saveFn)
    vi.advanceTimersByTime(2000)
    await vi.runAllTimersAsync()
    expect(saveFn).toHaveBeenCalledTimes(1)

    saveFn.mockResolvedValue(undefined)
    retry()
    await vi.runAllTimersAsync()
    expect(saveFn).toHaveBeenCalledTimes(2)
    expect(saveStatus.value).toBe('saved')
  })
})
