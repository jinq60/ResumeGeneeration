import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ResumePreview from '@/components/preview/ResumePreview.vue'

describe('ResumePreview', () => {
  it('renders correctly', () => {
    const wrapper = mount(ResumePreview)
    expect(wrapper.find('.resume-preview').exists()).toBe(true)
  })
})
