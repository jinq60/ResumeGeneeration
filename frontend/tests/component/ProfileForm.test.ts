import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import type { Resume } from '@/types/resume'
import ProfileForm from '@/components/editor/ProfileForm.vue'

vi.mock('@/api/avatar', () => ({
  avatarApi: {
    upload: vi.fn()
  }
}))

const resume: Resume = {
  id: 'resume_1',
  userId: 'user_1',
  title: '前端工程师简历',
  scene: 'social_recruitment',
  templateId: 'template_1',
  createdAt: '2026-08-20T00:00:00Z',
  updatedAt: '2026-08-20T00:00:00Z',
  sections: [
    {
      id: 'profile',
      type: 'profile',
      title: '基本信息',
      order: 0,
      visible: true,
      data: { name: '张三', targetPosition: '前端工程师' }
    }
  ]
}

describe('ProfileForm', () => {
  it('renders a name field and syncs its value into the profile section', async () => {
    const wrapper = mount(ProfileForm, {
      props: { resume },
      global: { plugins: [ElementPlus] }
    })

    const nameInput = wrapper.get('input[placeholder="请输入姓名"]')
    await nameInput.setValue('李娜')
    await flushPromises()

    const emitted = wrapper.emitted('update') || []
    const latestPayload = emitted[emitted.length - 1][0] as { sections: Resume['sections'] }
    const profile = latestPayload.sections.find((section) => section.type === 'profile')

    expect(profile?.data.name).toBe('李娜')
  })
})
