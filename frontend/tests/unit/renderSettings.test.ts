import { describe, expect, it } from 'vitest'
import {
  DEFAULT_RENDER_SETTINGS,
  normalizeRenderSettings,
  type RenderSettings
} from '@/utils/renderSettings'

describe('render settings', () => {
  it('fills missing values with stable editor defaults', () => {
    expect(normalizeRenderSettings({ autoOnePage: true })).toEqual({
      ...DEFAULT_RENDER_SETTINGS,
      autoOnePage: true
    })
  })

  it('does not mutate the server settings object', () => {
    const serverSettings: RenderSettings = { autoOnePage: false, accentColor: '#123456' }

    const normalized = normalizeRenderSettings(serverSettings)

    expect(serverSettings).toEqual({ autoOnePage: false, accentColor: '#123456' })
    expect(normalized.accentColor).toBe('#123456')
  })
})
