import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Resume, Section } from '@/types/resume'

export const useResumeStore = defineStore('resume', () => {
  const currentResume = ref<Resume | null>(null)
  const saveStatus = ref<'saved' | 'saving' | 'unsaved'>('saved')

  const sections = computed(() => currentResume.value?.sections || [])

  function setResume(resume: Resume) {
    currentResume.value = resume
  }

  function updateSections(newSections: Section[]) {
    if (currentResume.value) {
      currentResume.value.sections = newSections
      saveStatus.value = 'unsaved'
    }
  }

  function setSaveStatus(status: 'saved' | 'saving' | 'unsaved') {
    saveStatus.value = status
  }

  return {
    currentResume,
    saveStatus,
    sections,
    setResume,
    updateSections,
    setSaveStatus
  }
})
