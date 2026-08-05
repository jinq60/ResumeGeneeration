import { watch, type Ref } from 'vue'

/**
 * 编辑表单与父组件 sections 的同步守卫。
 *
 * 表单本地状态变化 -> emit 给父组件 -> 父组件回传新的 sections 引用。
 * 若每次都重新提取，会与自身 emit 的回传形成无限级联。
 * 因此仅当回传内容与表单上次 emit 的内容不一致（外部变更：切换模板、
 * 重新加载、其他模块保存）时才重新提取本地状态。
 */
export function useSectionSync(
  localState: Ref<unknown>,
  getSections: () => any[] | undefined,
  extract: () => void,
  emitUpdate: () => void
) {
  let lastEmitted = ''

  watch(localState, () => {
    lastEmitted = JSON.stringify(getSections() ?? null)
    emitUpdate()
  }, { deep: true })

  watch(() => getSections(), (incoming) => {
    if (JSON.stringify(incoming ?? null) === lastEmitted) return
    extract()
  })
}
