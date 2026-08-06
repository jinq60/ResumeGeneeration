const DRAFT_PREFIX = 'resume_editor_draft:'

export interface ResumeDraft<T> {
  savedAt: number
  data: T
}

function draftKey(resumeId: string) {
  return `${DRAFT_PREFIX}${resumeId}`
}

export function useResumeDraft<T>() {
  function save(resumeId: string, data: T) {
    try {
      const draft: ResumeDraft<T> = {
        savedAt: Date.now(),
        data
      }
      localStorage.setItem(draftKey(resumeId), JSON.stringify(draft))
    } catch {
      // Draft recovery is best-effort and must never block editing.
    }
  }

  function load(resumeId: string): ResumeDraft<T> | null {
    try {
      const raw = localStorage.getItem(draftKey(resumeId))
      if (!raw) return null

      const draft = JSON.parse(raw) as ResumeDraft<T>
      if (!draft || typeof draft.savedAt !== 'number' || !draft.data) {
        return null
      }
      return draft
    } catch {
      return null
    }
  }

  function clear(resumeId: string) {
    try {
      localStorage.removeItem(draftKey(resumeId))
    } catch {
      // Ignore storage failures.
    }
  }

  return { save, load, clear }
}
