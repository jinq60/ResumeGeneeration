import { describe, it, expect, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { useSectionSync } from '@/composables/useSectionSync'

function makeSection(type: string, title: string, order: number) {
  return { id: `sec_${type}`, type, title, order, visible: true, data: {} }
}

describe('useSectionSync', () => {
  it('should emit when local state changes', async () => {
    const local = ref<any[]>([{ name: 'a' }])
    const sections = ref<any[]>([makeSection('work', '工作经历', 0)])
    const extract = vi.fn()
    const emit = vi.fn()

    useSectionSync(local, () => sections.value, extract, emit)

    local.value = [{ name: 'b' }]
    await nextTick()
    expect(emit).toHaveBeenCalledTimes(1)
    expect(extract).not.toHaveBeenCalled()
  })

  it('should re-extract on external sections change (content differs)', async () => {
    const local = ref<any[]>([])
    const sections = ref<any[]>([makeSection('work', '工作经历', 0)])
    const extract = vi.fn()
    const emit = vi.fn()

    useSectionSync(local, () => sections.value, extract, emit)

    // 先模拟一次本地变更 -> emit，建立 lastEmitted 基线
    local.value = [{ name: 'x' }]
    await nextTick()
    expect(emit).toHaveBeenCalledTimes(1)
    expect(extract).not.toHaveBeenCalled()

    // 模拟自身 emit 后的回传：内容与上次 emit 时一致 -> 忽略
    sections.value = [makeSection('work', '工作经历', 0)]
    await nextTick()
    expect(extract).not.toHaveBeenCalled()

    // 模拟外部真实变更（如切换模板）：section 内容变化 -> 重新提取
    sections.value = [makeSection('work', '新标题', 0)]
    await nextTick()
    expect(extract).toHaveBeenCalledTimes(1)
  })

  it('should not loop when emit echoes back the same content', async () => {
    const local = ref<any[]>([{ name: 'a' }])
    const sections = ref<any[]>([makeSection('work', '工作经历', 0)])
    let emitCount = 0
    const extract = vi.fn()
    const emit = vi.fn(() => {
      emitCount++
    })

    useSectionSync(local, () => sections.value, extract, emit)

    // 模拟：父组件收到 emit 后原样回传 sections
    local.value = [{ name: 'a' }]
    await nextTick()
    sections.value = [...sections.value]
    await nextTick()
    const emitsAfterEcho = emitCount

    // 再次相同回传不应再触发 emit（内容比对命中）
    sections.value = [...sections.value]
    await nextTick()
    expect(emitCount).toBe(emitsAfterEcho)
  })
})
