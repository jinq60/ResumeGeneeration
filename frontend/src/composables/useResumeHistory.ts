import { computed, shallowRef } from 'vue'

const DEFAULT_LIMIT = 50
const DEFAULT_COALESCE_WINDOW_MS = 800

function clone<T>(value: T): T {
  try {
    if (typeof structuredClone === 'function') {
      return structuredClone(value)
    }
  } catch {
    // Vue Proxy 场景下 structuredClone 抛 DataCloneError，回退 JSON
  }
  return JSON.parse(JSON.stringify(value)) as T
}

/**
 * Editor history keeps snapshots outside the resume model so undo/redo never
 * becomes part of the server payload.
 */
export function useResumeHistory<T>(
  limit = DEFAULT_LIMIT,
  coalesceWindowMs = DEFAULT_COALESCE_WINDOW_MS
) {
  const past = shallowRef<T[]>([])
  const future = shallowRef<T[]>([])
  let lastGroup = ''
  let lastRecordedAt = 0

  function reset() {
    past.value = []
    future.value = []
    lastGroup = ''
    lastRecordedAt = 0
  }

  function record(previous: T, group = 'default') {
    const now = Date.now()
    const shouldCoalesce =
      lastGroup === group && now - lastRecordedAt <= coalesceWindowMs

    if (!shouldCoalesce) {
      past.value = [...past.value, clone(previous)].slice(-limit)
    }

    lastGroup = group
    lastRecordedAt = now
    future.value = []
  }

  function undo(current: T): T | null {
    const previous = past.value[past.value.length - 1]
    if (!previous) return null

    past.value = past.value.slice(0, -1)
    future.value = [...future.value, clone(current)].slice(-limit)
    lastGroup = ''
    lastRecordedAt = 0
    return clone(previous)
  }

  function redo(current: T): T | null {
    const next = future.value[future.value.length - 1]
    if (!next) return null

    future.value = future.value.slice(0, -1)
    past.value = [...past.value, clone(current)].slice(-limit)
    lastGroup = ''
    lastRecordedAt = 0
    return clone(next)
  }

  return {
    canUndo: computed(() => past.value.length > 0),
    canRedo: computed(() => future.value.length > 0),
    reset,
    record,
    undo,
    redo
  }
}
