import { watch, type Ref } from 'vue'
import type { Section } from '@/types/resume'

/**
 * 编辑表单与父组件 sections 的同步守卫。
 *
 * 表单本地状态变化 -> emit 给父组件 -> 父组件回传新的 sections 引用。
 * 若每次都重新提取，会与自身 emit 的回传形成无限级联。
 * 因此仅当回传内容与表单上次 emit 的内容不一致（外部变更：切换模板、
 * 重新加载、其他模块保存）时才重新提取本地状态。
 *
 * 性能优化：调用方可以提供 getOwnedSlice()，仅对当前表单负责的 section（按 type）做序列化对比，
 * 避免长简历下（8 段工作项 + 富文本）每个 keystroke 序列化全量 sections。
 * 默认仍回退到全量 JSON.stringify 保持向后兼容。
 */
export function useSectionSync(
  localState: Ref<unknown>,
  getSections: () => Section[] | undefined,
  extract: () => void,
  emitUpdate: () => void,
  getOwnedSlice?: () => unknown
) {
  let lastEmitted = ''

  watch(localState, () => {
    // 必须在 emit 前抓取本次写入的内容，否则回传的 sections 引用与 emit 后状态一致会误判为「无变化」
    lastEmitted = JSON.stringify(getOwnedSlice ? getOwnedSlice() : getSections() ?? null)
    emitUpdate()
  }, { deep: true })

  watch(() => getSections(), (incoming) => {
    const incomingKey = JSON.stringify(getOwnedSlice ? getOwnedSlice() : incoming ?? null)
    if (incomingKey === lastEmitted) return
    extract()
  })
}
