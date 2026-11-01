import { describe, expect, it } from 'vitest'
import { contrast, generateTheme } from './theme-generator'
import { erpPalettes, type ERPColorPalette } from './palettes'

const palettes: ERPColorPalette[] = [
  { id: 'pastel', name: 'Pastel', colors: ['#F1DFD2', '#FBC9D8', '#D6EEAB', '#C9E9F0'] },
  { id: 'deep', name: 'Deep', colors: ['#21142F', '#623A94', '#2F6B61', '#DBA348'] },
]

describe('theme generation', () => {
  it('registers exactly 32 ordered reference palettes with four valid swatches each', () => {
    expect(erpPalettes).toHaveLength(32)
    erpPalettes.forEach((palette, index) => {
      expect(palette.id).toBe(`palette-${String(index + 1).padStart(2, '0')}`)
      expect(palette.colors).toHaveLength(4)
      palette.colors.forEach(color => expect(color).toMatch(/^#[0-9A-F]{6}$/))
    })
  })

  it('keeps the main text, sidebar text, active navigation, and buttons readable across all 64 combinations', () => {
    for (const palette of erpPalettes) for (const mode of ['light', 'dark'] as const) {
      const theme = generateTheme(palette, mode)
      for (const [foreground, background] of [
        ['--erp-card-text', '--erp-card-background'],
        ['--erp-sidebar-text', '--erp-sidebar'],
        ['--erp-sidebar-muted', '--erp-sidebar'],
        ['--erp-sidebar-active-foreground', '--erp-sidebar-active'],
        ['--erp-primary-foreground', '--erp-primary'],
        ['--erp-status-info-text', '--erp-status-info-bg'],
        ['--erp-navbar-text', '--erp-navbar-bg-start'],
        ['--erp-navbar-muted', '--erp-navbar-bg-start'],
        ['--erp-navbar-icon', '--erp-navbar-bg-start'],
        ['--erp-navbar-text', '--erp-navbar-control-bg'],
        ['--erp-navbar-active-icon', '--erp-navbar-active-bg'],
      ]) expect(contrast(theme[foreground], theme[background]), `${palette.id} ${mode} ${foreground}`).toBeGreaterThanOrEqual(4.5)
    }
  })

  it('keeps chart series distinct even when the source swatches are similar', () => {
    for (const palette of erpPalettes) for (const mode of ['light', 'dark'] as const) {
      const theme = generateTheme(palette, mode)
      const colors = ['--erp-chart-blue', '--erp-chart-cyan', '--erp-chart-green', '--erp-chart-yellow'].map(token => theme[token])
      for (let first = 0; first < colors.length; first++) for (let second = first + 1; second < colors.length; second++) {
        const a = [1, 3, 5].map(start => parseInt(colors[first].slice(start, start + 2), 16))
        const b = [1, 3, 5].map(start => parseInt(colors[second].slice(start, start + 2), 16))
        expect(Math.hypot(...a.map((value, index) => value - b[index])), `${palette.id} ${mode} chart pair ${first}/${second}`).toBeGreaterThanOrEqual(55)
      }
    }
  })

  for (const palette of palettes) for (const mode of ['light', 'dark'] as const) {
    it(`keeps text and controls readable for ${palette.id} in ${mode} mode`, () => {
      const theme = generateTheme(palette, mode)
      expect(contrast(theme['--erp-card-text'], theme['--erp-card-background'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-sidebar-text'], theme['--erp-sidebar'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-sidebar-muted'], theme['--erp-sidebar'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-sidebar-active-foreground'], theme['--erp-sidebar-active'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-primary-foreground'], theme['--erp-primary'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-primary-foreground'], theme['--erp-primary-hover'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-primary-foreground'], theme['--erp-primary-active'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-status-info-text'], theme['--erp-status-info-bg'])).toBeGreaterThanOrEqual(4.5)
    })
  }

  it('changes the sidebar, primary button, chart, and workspace between different palettes', () => {
    const first = generateTheme(palettes[0], 'light')
    const second = generateTheme(palettes[1], 'light')
    for (const token of ['--erp-sidebar', '--erp-sidebar-active', '--erp-primary', '--erp-chart-blue', '--erp-background', '--erp-navbar-bg-start', '--erp-navbar-border', '--erp-navbar-icon', '--erp-navbar-control-border']) {
      expect(first[token]).not.toEqual(second[token])
    }
  })

  it('keeps key text readable across very light and very dark source swatches', () => {
    const edgeColors = ['#FFFFFF', '#000000', '#FFFF00', '#00FF00', '#FF0000', '#0000FF', '#808080']
    for (const color of edgeColors) for (const mode of ['light', 'dark'] as const) {
      const theme = generateTheme({ id: color, name: color, colors: [color, color, color, color] }, mode)
      expect(contrast(theme['--erp-primary-foreground'], theme['--erp-primary'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-sidebar-active-foreground'], theme['--erp-sidebar-active'])).toBeGreaterThanOrEqual(4.5)
      expect(contrast(theme['--erp-sidebar-muted'], theme['--erp-sidebar'])).toBeGreaterThanOrEqual(4.5)
    }
  })
})
