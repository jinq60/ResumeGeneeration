import { ref } from 'vue'

export type ThemePreference = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'resume_theme'

const preference = ref<ThemePreference>(loadPreference())

function loadPreference(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch {
    // Fall through to system default.
  }
  return 'system'
}

function systemPrefersDark(): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-color-scheme: dark)').matches
    : false
}

function resolveDark(pref: ThemePreference): boolean {
  if (pref === 'dark') return true
  if (pref === 'light') return false
  return systemPrefersDark()
}

function apply(pref: ThemePreference) {
  document.documentElement.classList.toggle('dark', resolveDark(pref))
}

export function initTheme() {
  preference.value = loadPreference()
  apply(preference.value)
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (preference.value === 'system') {
        apply(preference.value)
      }
    })
  }
}

export function useTheme() {
  function setTheme(pref: ThemePreference) {
    preference.value = pref
    try {
      localStorage.setItem(STORAGE_KEY, pref)
    } catch {
      // Ignore storage failures.
    }
    apply(pref)
  }

  function toggleTheme() {
    setTheme(resolveDark(preference.value) ? 'light' : 'dark')
  }

  return {
    preference,
    setTheme,
    toggleTheme
  }
}
