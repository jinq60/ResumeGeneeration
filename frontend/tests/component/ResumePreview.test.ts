import { describe, it, expect, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ResumePreview from '@/components/preview/ResumePreview.vue'

vi.mock('@/api/preview', () => ({
  fetchResumePreview: vi.fn(() => Promise.resolve('<html>preview</html>')),
  fetchLivePreview: vi.fn(() => Promise.resolve('<html>preview</html>'))
}))

describe('ResumePreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders iframe preview after loading', async () => {
    const wrapper = mount(ResumePreview, {
      props: { resumeId: 'resume_1' }
    })
    await flushPromises()
    expect(wrapper.find('.resume-preview').exists()).toBe(true)
  })
})
