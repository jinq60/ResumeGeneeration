import { beforeEach, describe, expect, it, vi } from 'vitest'
import { resumeApi } from '@/api/resume'

describe('resumeApi.aiWriteStream', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('parses delta events and ignores the done event', async () => {
    localStorage.setItem('access_token', 'token')
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: delta\ndata: first\n\n'),
      encoder.encode('event: delta\ndata: second\n\nevent: done\ndata:\n\n')
    ]
    let index = 0
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader: () => ({
          read: vi.fn(async () => {
            if (index >= chunks.length) return { done: true, value: undefined }
            return { done: false, value: chunks[index++] }
          })
        })
      }
    })
    vi.stubGlobal('fetch', fetchMock)

    const deltas: string[] = []
    await resumeApi.aiWriteStream(
      'resume_1',
      { sectionType: 'introduction', field: 'content', action: 'polish', originalText: 'old' },
      (content) => deltas.push(content)
    )

    expect(deltas).toEqual(['first', 'second'])
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/resumes/resume_1/ai/write/stream',
      expect.objectContaining({ method: 'POST' })
    )
  })
})
