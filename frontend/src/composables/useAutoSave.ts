import { onUnmounted, ref } from 'vue'

/**
 * 自动保存组合式函数：2 秒防抖 + 串行化保存。
 * <p>
 * - 防抖期间离开组件时，flush 最后一次未保存的修改（避免丢失）；
 * - 同一时刻只允许一个保存请求在途，新触发的保存排队等待，
 *   防止并发请求乱序到达导致旧数据覆盖新数据。
 * </p>
 */
export function useAutoSave() {
  const saveStatus = ref<'saved' | 'saving' | 'unsaved' | 'error'>('saved')
  let timer: ReturnType<typeof setTimeout> | null = null
  let lastSaveFn: (() => Promise<void>) | null = null
  const queue: Array<() => Promise<void>> = []
  let saving = false

  function triggerSave(saveFn: () => Promise<void>) {
    lastSaveFn = saveFn
    saveStatus.value = 'unsaved'
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      timer = null
      if (lastSaveFn) {
        enqueue(lastSaveFn)
      }
    }, 2000)
  }

  function enqueue(fn: () => Promise<void>) {
    queue.push(fn)
    if (!saving) {
      runQueue()
    }
  }

  async function runQueue() {
    saving = true
    saveStatus.value = 'saving'
    while (queue.length > 0) {
      const fn = queue.shift()!
      try {
        await fn()
      } catch {
        saveStatus.value = 'error'
      }
    }
    saving = false
    if (saveStatus.value !== 'error') {
      saveStatus.value = 'saved'
    }
  }

  /**
   * 立即执行防抖中的保存（供卸载前调用，避免最后一次编辑丢失）。
   */
  function flush() {
    if (timer) {
      clearTimeout(timer)
      timer = null
      if (lastSaveFn) {
        enqueue(lastSaveFn)
      }
    }
  }

  onUnmounted(() => {
    // 先 flush 防抖中的最后一次编辑，避免离开页面时丢失修改
    flush()
    // 卸载后继续完成队列中已排队的保存请求
    if (queue.length > 0 && !saving) {
      runQueue()
    }
  })

  return {
    saveStatus,
    triggerSave,
    flush
  }
}
