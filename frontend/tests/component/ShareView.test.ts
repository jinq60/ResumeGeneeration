import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ShareView from '@/views/ShareView.vue'

let mockToken: string | null = 'valid_token'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { token: mockToken } })
}))

const routerLinkStub = {
  name: 'RouterLink',
  template: '<a class="router-link-stub"><slot /></a>'
}

describe('ShareView', () => {
  it('renders iframe pointing at the backend share page when token exists', () => {
    mockToken = 'abc123'
    const wrapper = mount(ShareView, {
      global: { plugins: [ElementPlus], stubs: { RouterLink: routerLinkStub } }
    })
    const iframe = wrapper.find('iframe')
    expect(iframe.exists()).toBe(true)
    expect(iframe.attributes('src')).toBe('/api/share/abc123')
  })

  it('does not show error when token is present', () => {
    mockToken = 'abc123'
    const wrapper = mount(ShareView, {
      global: { plugins: [ElementPlus], stubs: { RouterLink: routerLinkStub } }
    })
    expect(wrapper.text()).not.toContain('分享链接无效')
  })

  it('shows invalid link message when token is missing', () => {
    mockToken = null
    const wrapper = mount(ShareView, {
      global: { plugins: [ElementPlus], stubs: { RouterLink: routerLinkStub } }
    })
    expect(wrapper.find('iframe').exists()).toBe(false)
    expect(wrapper.text()).toContain('分享链接无效')
    expect(wrapper.text()).toContain('返回首页')
  })

  it('renders read-only footer with homepage link', () => {
    mockToken = 'abc123'
    const wrapper = mount(ShareView, {
      global: { plugins: [ElementPlus], stubs: { RouterLink: routerLinkStub } }
    })
    expect(wrapper.text()).toContain('公开只读预览')
    expect(wrapper.find('.router-link-stub').exists()).toBe(true)
  })

  it('escapes token in iframe src', () => {
    mockToken = 'a/b+c='
    const wrapper = mount(ShareView, {
      global: { plugins: [ElementPlus], stubs: { RouterLink: routerLinkStub } }
    })
    expect(wrapper.find('iframe').attributes('src')).toBe('/api/share/a%2Fb%2Bc%3D')
  })
})
