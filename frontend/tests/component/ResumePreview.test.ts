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

  it('emits the rendered content height for page navigation', async () => {
    const wrapper = mount(ResumePreview, {
      props: { resumeId: 'resume_1' }
    })
    await flushPromises()

    Object.defineProperty(wrapper.find('iframe').element, 'contentDocument', {
      configurable: true,
      value: {
        documentElement: { scrollHeight: 2200 },
        body: { scrollHeight: 2100 }
      }
    })
    await wrapper.find('iframe').trigger('load')

    expect(wrapper.emitted('loaded')).toEqual([[2200]])
  })

  it('fits an auto-one-page document to A4 height', async () => {
    const wrapper = mount(ResumePreview, {
      props: { resumeId: 'resume_1' }
    })
    await flushPromises()

    const innerDocument = document.implementation.createHTMLDocument()
    const page = innerDocument.createElement('div')
    page.className = 'resume-page'
    page.dataset.autoOnePage = 'true'
    Object.defineProperty(page, 'scrollHeight', { configurable: true, value: 1600 })
    innerDocument.body.appendChild(page)

    Object.defineProperty(wrapper.find('iframe').element, 'contentDocument', {
      configurable: true,
      value: innerDocument
    })
    await wrapper.find('iframe').trigger('load')

    const scale = Number(page.style.getPropertyValue('--resume-fit-scale'))
    expect(scale).toBeLessThan(1)
    const loadedEvents = wrapper.emitted('loaded') || []
    expect(loadedEvents[loadedEvents.length - 1]?.[0]).toBeGreaterThan(1100)
  })
})
