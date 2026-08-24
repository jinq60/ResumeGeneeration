import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import type { Section } from '@/types/resume'
import EditorModuleRail from '@/components/editor/EditorModuleRail.vue'

const sections: Section[] = [
  {
    id: 'profile',
    type: 'profile',
    title: '基本信息',
    order: 0,
    visible: true,
    data: { name: '张三' }
  },
  {
    id: 'skill',
    type: 'skill',
    title: '专业技能',
    order: 1,
    visible: true,
    data: []
  },
  {
    id: 'work',
    type: 'work',
    title: '工作经验',
    order: 2,
    visible: true,
    data: []
  }
]

function mountRail() {
  return mount(EditorModuleRail, {
    props: {
      sections,
      activeSectionId: 'profile',
      themeColor: '#20201d'
    },
    global: {
      stubs: {
        'el-icon': { template: '<span><slot /></span>' }
      }
    }
  })
}

describe('EditorModuleRail', () => {
  it('selects a module card and exposes the active state', async () => {
    const wrapper = mountRail()

    expect(wrapper.get('[data-section-id="profile"]').classes()).toContain('is-active')

    await wrapper.get('[data-section-id="work"]').trigger('click')

    expect(wrapper.emitted('select-section')).toEqual([['work']])
  })

  it('emits module visibility and removal requests without allowing profile removal', async () => {
    const wrapper = mountRail()

    expect(wrapper.find('[aria-label="删除基本信息模块"]').exists()).toBe(false)

    await wrapper.get('[aria-label="隐藏专业技能模块"]').trigger('click')
    await wrapper.get('[aria-label="删除工作经验模块"]').trigger('click')

    expect(wrapper.emitted('toggle-visibility')).toEqual([['skill', false]])
    expect(wrapper.emitted('remove-section')).toEqual([['work']])
  })

  it('emits a stable source and target when a module card is dragged', async () => {
    const wrapper = mountRail()
    const setData = vi.fn()
    const dataTransfer = { effectAllowed: '', setData }

    await wrapper.get('[data-section-id="work"]').trigger('dragstart', { dataTransfer })
    await wrapper.get('[data-section-id="skill"]').trigger('drop', { dataTransfer })

    expect(setData).toHaveBeenCalledWith('text/plain', 'work')
    expect(wrapper.emitted('reorder-sections')).toEqual([[['work', 'skill']]])
  })

  it('emits theme, layout and new-module intents from the lower rail controls', async () => {
    const wrapper = mountRail()

    await wrapper.get('[aria-label="使用蓝色主题"]').trigger('click')
    await wrapper.get('[aria-label="打开排版设置"]').trigger('click')
    await wrapper.get('[aria-label="添加自定义模块"]').trigger('click')

    expect(wrapper.emitted('select-theme')).toEqual([['#1557b0']])
    expect(wrapper.emitted('open-settings')).toHaveLength(1)
    expect(wrapper.emitted('add-custom-section')).toHaveLength(1)
  })
})
