import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import AiWriterButton from '@/components/editor/AiWriterButton.vue'

vi.mock('@/api/resume', () => ({
  resumeApi: {
    aiWrite: vi.fn()
  }
}))

import { resumeApi } from '@/api/resume'

describe('AiWriterButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should call aiWrite and open dialog on action', async () => {
    (resumeApi.aiWrite as any).mockResolvedValue({ content: 'AI 生成的内容' })
    const wrapper = mount(AiWriterButton, {
      props: {
        resumeId: 'resume_1',
        sectionType: 'introduction',
        field: 'content',
        getOriginalText: () => '原文内容'
      }
    })

    await (wrapper.vm as any).runAction('generate')
    expect(resumeApi.aiWrite).toHaveBeenCalledWith('resume_1', expect.objectContaining({
      sectionType: 'introduction',
      field: 'content',
      action: 'generate'
    }))
    expect((wrapper.vm as any).dialogVisible).toBe(true)
  })

  it('should emit apply when applying content', async () => {
    (resumeApi.aiWrite as any).mockResolvedValue({ content: '结果' })
    const wrapper = mount(AiWriterButton, {
      props: {
        resumeId: 'resume_1',
        sectionType: 'introduction',
        field: 'content'
      }
    })

    // 直接调用内部方法：先跑一次 action 得到内容，再应用
    await (wrapper.vm as any).runAction('polish')
    expect(resumeApi.aiWrite).toHaveBeenCalledWith('resume_1', expect.objectContaining({
      sectionType: 'introduction',
      field: 'content',
      action: 'polish'
    }))

    ;(wrapper.vm as any).applyContent()
    expect(wrapper.emitted('apply')).toBeTruthy()
    expect((wrapper.emitted('apply')![0] as any[])[0]).toBe('结果')
  })

  it('should show error message when api fails', async () => {
    (resumeApi.aiWrite as any).mockRejectedValue(new Error('AI 服务不可用'))
    const wrapper = mount(AiWriterButton, {
      props: {
        resumeId: 'resume_1',
        sectionType: 'work',
        field: 'description'
      }
    })

    await (wrapper.vm as any).runAction('polish')
    expect((wrapper.vm as any).errorMsg).toContain('AI 服务不可用')
  })

  it('should include targetLang when translating', async () => {
    (resumeApi.aiWrite as any).mockResolvedValue({ content: 'Hello' })
    const wrapper = mount(AiWriterButton, {
      props: {
        resumeId: 'resume_1',
        sectionType: 'introduction',
        field: 'content',
        getOriginalText: () => '你好'
      }
    })

    await (wrapper.vm as any).runAction('translate')
    expect(resumeApi.aiWrite).toHaveBeenCalledWith('resume_1', expect.objectContaining({
      action: 'translate',
      targetLang: 'en'
    }))
  })
})
