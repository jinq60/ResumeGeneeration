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

  it('passes the abort signal to fetch', async () => {
    localStorage.setItem('access_token', 'token')
    const controller = new AbortController()
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader: () => ({
          read: vi.fn(async () => ({ done: true, value: undefined }))
        })
      }
    })
    vi.stubGlobal('fetch', fetchMock)

    await resumeApi.aiWriteStream(
      'resume_1',
      { sectionType: 'introduction', field: 'content', action: 'polish' },
      () => {},
      controller.signal
    )

    expect(fetchMock).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ signal: controller.signal })
    )
  })

  it('cancels the reader and rethrows when the stream is aborted mid-read', async () => {
    localStorage.setItem('access_token', 'token')
    const cancelMock = vi.fn()
    const abortError = Object.assign(new Error('The operation was aborted'), { name: 'AbortError' })
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader: () => ({
          read: vi.fn(async () => { throw abortError }),
          cancel: cancelMock
        })
      }
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(resumeApi.aiWriteStream(
      'resume_1',
      { sectionType: 'work', field: 'description', action: 'expand' },
      () => {}
    )).rejects.toThrow(abortError)

    // 中断时必须释放底层连接
    expect(cancelMock).toHaveBeenCalledTimes(1)
  })
})
