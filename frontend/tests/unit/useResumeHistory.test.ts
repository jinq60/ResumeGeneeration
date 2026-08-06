import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useResumeHistory } from '@/composables/useResumeHistory'

describe('useResumeHistory', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-08-05T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('undoes and redoes immutable snapshots', () => {
    const history = useResumeHistory<{ value: string }>()
    const initial = { value: 'initial' }

    history.record(initial, 'introduction')
    const changed = { value: 'changed' }
    expect(history.undo(changed)).toEqual(initial)
    expect(history.canUndo.value).toBe(false)
    expect(history.canRedo.value).toBe(true)
    expect(history.redo(initial)).toEqual(changed)
  })

  it('coalesces rapid changes from the same field', () => {
    const history = useResumeHistory<{ value: string }>()
    history.record({ value: 'a' }, 'introduction')

    vi.advanceTimersByTime(500)
    history.record({ value: 'ab' }, 'introduction')

    expect(history.undo({ value: 'abc' })).toEqual({ value: 'a' })
  })

  it('starts a new history entry after the coalesce window', () => {
    const history = useResumeHistory<{ value: string }>()
    history.record({ value: 'a' }, 'introduction')

    vi.advanceTimersByTime(1000)
    history.record({ value: 'ab' }, 'introduction')

    expect(history.undo({ value: 'abc' })).toEqual({ value: 'ab' })
    expect(history.undo({ value: 'ab' })).toEqual({ value: 'a' })
  })

  it('clears redo history after a new edit', () => {
    const history = useResumeHistory<{ value: string }>()
    history.record({ value: 'a' })
    const changed = { value: 'b' }
    history.undo(changed)
    history.record({ value: 'a' })

    expect(history.canRedo.value).toBe(false)
  })

  it('respects the history limit', () => {
    const history = useResumeHistory<{ value: number }>(2, 0)
    history.record({ value: 0 }, 'a')
    history.record({ value: 1 }, 'b')
    history.record({ value: 2 }, 'c')

    expect(history.undo({ value: 3 })).toEqual({ value: 2 })
    expect(history.undo({ value: 2 })).toEqual({ value: 1 })
    expect(history.undo({ value: 1 })).toBeNull()
  })
})
