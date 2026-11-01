import type { ERPColorPalette } from './palettes'

export type ResolvedMode = 'light' | 'dark'
export type ThemeTokens = Record<string, string>
type RGB = [number, number, number]

function rgb(hex: string): RGB {
  const value = hex.replace('#', '')
  if (!/^[0-9a-f]{6}$/i.test(value)) throw new Error(`Invalid palette color: ${hex}`)
  return [0, 2, 4].map(index => parseInt(value.slice(index, index + 2), 16)) as RGB
}

function hex(color: RGB): string {
  return `#${color.map(channel => Math.round(Math.max(0, Math.min(255, channel))).toString(16).padStart(2, '0')).join('').toUpperCase()}`
}

export function mix(first: string, second: string, secondWeight: number): string {
  const a = rgb(first), b = rgb(second)
  return hex(a.map((channel, index) => channel * (1 - secondWeight) + b[index] * secondWeight) as RGB)
}

export function luminance(color: string): number {
  const channels = rgb(color).map(channel => {
    const normalized = channel / 255
    return normalized <= .04045 ? normalized / 12.92 : ((normalized + .055) / 1.055) ** 2.4
  })
  return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722
}

export function contrast(first: string, second: string): number {
  const a = luminance(first), b = luminance(second)
  return (Math.max(a, b) + .05) / (Math.min(a, b) + .05)
}

export function readableText(background: string): string {
  return contrast(background, '#FFFFFF') >= contrast(background, '#000000') ? '#FFFFFF' : '#000000'
}

export function ensureContrast(foreground: string, background: string, minimum = 4.5): string {
  if (contrast(foreground, background) >= minimum) return foreground
  const toward = contrast('#FFFFFF', background) > contrast('#101827', background) ? '#FFFFFF' : '#101827'
  for (let step = 1; step <= 20; step++) {
    const candidate = mix(foreground, toward, step / 20)
    if (contrast(candidate, background) >= minimum) return candidate
  }
  return toward
}

function colorDistance(first: string, second: string): number {
  const a = rgb(first), b = rgb(second)
  return Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2])
}

function rotateHue(color: string, degrees: number, light: boolean): string {
  const [red, green, blue] = rgb(color).map(channel => channel / 255)
  const maximum = Math.max(red, green, blue), minimum = Math.min(red, green, blue)
  const delta = maximum - minimum
  let hue = 0
  if (delta) {
    if (maximum === red) hue = ((green - blue) / delta) % 6
    else if (maximum === green) hue = (blue - red) / delta + 2
    else hue = (red - green) / delta + 4
    hue *= 60
  }
  hue = (((hue + degrees) % 360) + 360) % 360
  const saturation = .65
  const lightness = light ? .42 : .68
  const chroma = (1 - Math.abs(2 * lightness - 1)) * saturation
  const x = chroma * (1 - Math.abs(((hue / 60) % 2) - 1))
  const m = lightness - chroma / 2
  const segments: RGB[] = [[chroma, x, 0], [x, chroma, 0], [0, chroma, x], [0, x, chroma], [x, 0, chroma], [chroma, 0, x]]
  return hex(segments[Math.floor(hue / 60)].map(channel => (channel + m) * 255) as RGB)
}

function chartColors(colors: ERPColorPalette['colors'], surface: string, light: boolean): string[] {
  const result: string[] = []
  colors.forEach((color, index) => {
    let candidate = ensureContrast(color, surface, 3)
    if (result.some(previous => colorDistance(previous, candidate) < 55)) {
      for (let attempt = 1; attempt <= 5; attempt++) {
        const rotated = rotateHue(color, index * 55 + attempt * 50, light)
        candidate = ensureContrast(rotated, surface, 3)
        if (result.every(previous => colorDistance(previous, candidate) >= 55)) break
      }
    }
    result.push(candidate)
  })
  return result
}

export function generateTheme(palette: ERPColorPalette, mode: ResolvedMode): ThemeTokens {
  const colors = palette.colors
  colors.forEach(rgb)
  const darkest = [...colors].sort((a, b) => luminance(a) - luminance(b))[0]
  const accent = colors[1]
  const light = mode === 'light'
  const background = light ? mix(accent, '#FFFFFF', .96) : mix(darkest, '#070D1A', .78)
  const surface = light ? mix(accent, '#FFFFFF', .985) : mix(darkest, '#101A2C', .72)
  const foreground = light ? '#17233D' : '#EDF3FF'
  const muted = light ? '#66758F' : '#A5B4D0'
  const primary = ensureContrast(accent, light ? '#FFFFFF' : surface, light ? 4.5 : 3)
  const primaryText = readableText(primary)
  const sidebar = mix(darkest, '#050914', luminance(darkest) > .25 ? .68 : .35)
  const sidebarText = '#F3F7FF'
  const sidebarMuted = ensureContrast(mix(sidebar, '#FFFFFF', .67), sidebar)
  const sidebarHeading = ensureContrast(mix(sidebar, '#FFFFFF', .73), sidebar)
  const sidebarActive = ensureContrast(accent, sidebar, 3)
  const sidebarActiveText = readableText(sidebarActive)
  const border = light ? mix(accent, '#E1E7F1', .84) : mix(darkest, '#60708C', .62)
  const secondary = light ? mix(accent, '#FFFFFF', .93) : mix(accent, surface, .78)
  const chart = chartColors(colors, surface, light)
  const chartBlue = chart[1]

  return {
    '--erp-sidebar': sidebar,
    '--erp-sidebar-deep': mix(sidebar, '#000000', .12),
    '--erp-sidebar-card': mix(sidebar, accent, .19),
    '--erp-sidebar-active': sidebarActive,
    '--erp-sidebar-active-foreground': sidebarActiveText,
    '--erp-sidebar-active-hover': ensureContrast(mix(sidebarActive, sidebarActiveText, .12), sidebarActiveText),
    '--erp-sidebar-text': sidebarText,
    '--erp-sidebar-muted': sidebarMuted,
    '--erp-sidebar-heading': sidebarHeading,
    '--erp-sidebar-border': mix(sidebar, '#FFFFFF', .22),
    '--erp-sidebar-icon': sidebarMuted,
    '--erp-background': background,
    '--erp-background-secondary': secondary,
    '--erp-background-elevated': surface,
    '--erp-workspace-gradient': `linear-gradient(135deg, ${mix(background, '#FFFFFF', light ? .55 : .02)} 0%, ${background} 55%, ${mix(background, accent, light ? .06 : .1)} 100%)`,
    '--erp-header-background': surface,
    '--erp-header-border': border,
    '--erp-header-text': foreground,
    '--erp-header-muted': muted,
    '--erp-header-icon': primary,
    '--erp-card-background': surface,
    '--erp-card-border': border,
    '--erp-card-text': foreground,
    '--erp-card-muted': muted,
    '--erp-primary': primary,
    '--erp-primary-hover': ensureContrast(mix(primary, primaryText, .1), primaryText),
    '--erp-primary-active': ensureContrast(mix(primary, primaryText, .17), primaryText),
    '--erp-primary-foreground': primaryText,
    '--erp-revenue': chartBlue,
    '--erp-revenue-light': secondary,
    '--erp-table-header': secondary,
    '--erp-table-row': surface,
    '--erp-table-row-hover': mix(accent, surface, light ? .94 : .84),
    '--erp-table-border': border,
    '--erp-table-text': foreground,
    '--erp-status-info-bg': secondary,
    '--erp-status-info-text': ensureContrast(primary, secondary),
    '--erp-chart-blue': chartBlue,
    '--erp-chart-cyan': chart[2],
    '--erp-chart-green': chart[3],
    '--erp-chart-yellow': chart[0],
    '--erp-chart-purple': mix(chart[0], chart[2], .5),
    '--erp-chart-red': mix(chart[1], chart[3], .5),
    '--erp-chart-grid': border,
    '--erp-input-background': surface,
    '--erp-input-border': border,
    '--erp-input-text': foreground,
    '--erp-input-placeholder': muted,
    '--erp-input-focus': primary,
    '--erp-input-focus-ring': `color-mix(in srgb, ${primary} 20%, transparent)`,
    '--erp-surface-divider': border,
    '--background': background,
    '--foreground': foreground,
    '--card': surface,
    '--card-foreground': foreground,
    '--popover': surface,
    '--popover-foreground': foreground,
    '--primary': primary,
    '--primary-foreground': primaryText,
    '--secondary': secondary,
    '--secondary-foreground': foreground,
    '--muted': secondary,
    '--muted-foreground': muted,
    '--accent': secondary,
    '--accent-foreground': foreground,
    '--border': border,
    '--input': border,
    '--ring': primary,
    '--sidebar': sidebar,
    '--sidebar-foreground': sidebarText,
    '--sidebar-primary': sidebarActive,
    '--sidebar-primary-foreground': sidebarActiveText,
    '--sidebar-accent': mix(sidebar, accent, .19),
    '--sidebar-accent-foreground': sidebarText,
    '--sidebar-border': mix(sidebar, '#FFFFFF', .22),
    '--sidebar-ring': sidebarActive,
  }
}
