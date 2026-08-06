import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useResumeDraft } from '@/composables/useResumeDraft'

describe('useResumeDraft', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.setSystemTime(new Date('2026-08-05T12:00:00Z'))
  })

  it('saves and loads a draft by resume id', () => {
    const draft = useResumeDraft<{ title: string }>()
    draft.save('resume-1', { title: '本地草稿' })

    expect(draft.load('resume-1')).toEqual({
      savedAt: new Date('2026-08-05T12:00:00Z').getTime(),
      data: { title: '本地草稿' }
    })
  })

  it('clears a draft without affecting another resume', () => {
    const draft = useResumeDraft<{ title: string }>()
    draft.save('resume-1', { title: '一' })
    draft.save('resume-2', { title: '二' })

    draft.clear('resume-1')

    expect(draft.load('resume-1')).toBeNull()
    expect(draft.load('resume-2')?.data.title).toBe('二')
  })

  it('ignores malformed storage data', () => {
    localStorage.setItem('resume_editor_draft:resume-1', '{bad-json')
    const draft = useResumeDraft<{ title: string }>()

    expect(draft.load('resume-1')).toBeNull()
  })
})
