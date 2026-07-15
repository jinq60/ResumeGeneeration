import { onUnmounted, ref } from 'vue'

export function useAutoSave() {
  const saveStatus = ref<'saved' | 'saving' | 'unsaved'>('saved')
  let timer: ReturnType<typeof setTimeout> | null = null

  function triggerSave(saveFn: () => Promise<void>) {
    saveStatus.value = 'unsaved'
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(async () => {
      saveStatus.value = 'saving'
      await saveFn()
      saveStatus.value = 'saved'
    }, 2000)
  }

  onUnmounted(() => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  })

  return {
    saveStatus,
    triggerSave
  }
}
