import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const isMobile = ref(false)
  const activeTab = ref('edit')

  function setMobile(value: boolean) {
    isMobile.value = value
  }

  function setActiveTab(tab: string) {
    activeTab.value = tab
  }

  return {
    isMobile,
    activeTab,
    setMobile,
    setActiveTab
  }
})
