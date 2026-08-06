import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import WorkbenchLayout from '@/components/workbench/WorkbenchLayout.vue'

const SidebarStub = defineComponent({
  name: 'WorkbenchSidebar',
  props: {
    mobileOpen: Boolean
  },
  emits: ['closeMobile'],
  template: '<aside data-sidebar :data-open="mobileOpen" @click="$emit(\'closeMobile\')" />'
})

describe('WorkbenchLayout', () => {
  it('opens and closes the mobile sidebar drawer', async () => {
    const wrapper = mount(WorkbenchLayout, {
      global: {
        stubs: {
          WorkbenchSidebar: SidebarStub,
          RouterView: { template: '<div data-router-view />' },
          'el-icon': { template: '<span><slot /></span>' }
        }
      }
    })

    const sidebar = wrapper.findComponent(SidebarStub)
    expect(sidebar.props('mobileOpen')).toBe(false)

    await wrapper.find('.mobile-menu-trigger').trigger('click')
    expect(sidebar.props('mobileOpen')).toBe(true)

    sidebar.vm.$emit('closeMobile')
    await nextTick()
    expect(sidebar.props('mobileOpen')).toBe(false)
  })
})
