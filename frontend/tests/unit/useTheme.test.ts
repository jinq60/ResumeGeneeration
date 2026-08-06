import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useTheme, initTheme } from '@/composables/useTheme'

function mockMatchMedia(matches: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: vi.fn().mockReturnValue({
      matches,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })
  })
}

describe('useTheme', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
    mockMatchMedia(false)
  })

  it('defaults to system preference and applies it', () => {
    initTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('setTheme persists and applies dark mode', () => {
    const { setTheme } = useTheme()
    setTheme('dark')
    expect(localStorage.getItem('resume_theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('setTheme persists and applies light mode', () => {
    const { setTheme } = useTheme()
    setTheme('dark')
    setTheme('light')
    expect(localStorage.getItem('resume_theme')).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('toggleTheme flips between light and dark', () => {
    const { setTheme, toggleTheme } = useTheme()
    setTheme('light')
    toggleTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    toggleTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('restores stored preference on init', () => {
    localStorage.setItem('resume_theme', 'dark')
    initTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('ignores invalid stored values', () => {
    localStorage.setItem('resume_theme', 'blue')
    initTheme()
    expect(localStorage.getItem('resume_theme')).toBe('blue')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('follows dark system preference by default', () => {
    mockMatchMedia(true)
    initTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })
})
