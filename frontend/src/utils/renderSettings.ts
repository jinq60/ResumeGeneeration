export interface RenderSettings {
  autoOnePage?: boolean
  fontFamily?: string | null
  baseFontSize?: number | null
  lineHeight?: number | null
  pagePadding?: number | null
  sectionSpacing?: number | null
  accentColor?: string | null
}

export interface EffectiveRenderSettings {
  autoOnePage: boolean
  fontFamily: string
  baseFontSize: number
  lineHeight: number
  pagePadding: number
  sectionSpacing: number
  accentColor: string
}

export const DEFAULT_RENDER_SETTINGS: EffectiveRenderSettings = {
  autoOnePage: false,
  fontFamily: '"Noto Sans SC", "Microsoft YaHei", sans-serif',
  baseFontSize: 10.5,
  lineHeight: 1.5,
  pagePadding: 20,
  sectionSpacing: 16,
  accentColor: '#1a5276'
}

export function normalizeRenderSettings(settings?: RenderSettings | null): EffectiveRenderSettings {
  const source = settings || {}
  return {
    autoOnePage: source.autoOnePage ?? DEFAULT_RENDER_SETTINGS.autoOnePage,
    fontFamily: source.fontFamily ?? DEFAULT_RENDER_SETTINGS.fontFamily,
    baseFontSize: source.baseFontSize ?? DEFAULT_RENDER_SETTINGS.baseFontSize,
    lineHeight: source.lineHeight ?? DEFAULT_RENDER_SETTINGS.lineHeight,
    pagePadding: source.pagePadding ?? DEFAULT_RENDER_SETTINGS.pagePadding,
    sectionSpacing: source.sectionSpacing ?? DEFAULT_RENDER_SETTINGS.sectionSpacing,
    accentColor: source.accentColor ?? DEFAULT_RENDER_SETTINGS.accentColor
  }
}
