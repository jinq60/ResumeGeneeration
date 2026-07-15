import { describe, it, expect, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ResumePreview from '@/components/preview/ResumePreview.vue'

vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: '<html>preview</html>' })),
    post: vi.fn(() => Promise.resolve({ data: '<html>preview</html>' }))
  },
  __esModule: true
}))

describe('ResumePreview', () => {
  it('renders iframe preview after loading', async () => {
    const wrapper = mount(ResumePreview, {
      props: { resumeId: 'resume_1' }
    })
    await flushPromises()
    expect(wrapper.find('.resume-preview').exists()).toBe(true)
  })
})
