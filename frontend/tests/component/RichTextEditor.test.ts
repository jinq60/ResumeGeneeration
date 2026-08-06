import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'

describe('RichTextEditor', () => {
  it('emits sanitized content from the contenteditable surface', async () => {
    const wrapper = mount(RichTextEditor, {
      props: { modelValue: '' }
    })
    const content = wrapper.find('.rich-text-content')

    content.element.innerHTML = '<p><strong>重点</strong></p><script>alert(1)</script>'
    await content.trigger('input')

    const emitted = wrapper.emitted('update:modelValue') || []
    expect(emitted[emitted.length - 1]?.[0]).toBe('<p><strong>重点</strong></p>')
  })

  it('runs formatting commands from the toolbar', async () => {
    Object.defineProperty(document, 'execCommand', {
      configurable: true,
      value: vi.fn(() => true)
    })
    const execCommand = vi.mocked(document.execCommand)
    const wrapper = mount(RichTextEditor, {
      props: { modelValue: '<p>内容</p>' }
    })

    await wrapper.find('button[aria-label="加粗"]').trigger('mousedown')
    expect(execCommand).toHaveBeenCalledWith('bold', false)
  })
})
