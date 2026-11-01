import { describe, expect, it } from 'vitest'
import { erpPalettes } from './palettes'
import { applyThemeVariables } from './theme-dom'
import { generateTheme } from './theme-generator'

describe('CSS variable application', () => {
  it('updates the sidebar, active navigation, buttons, workspace and charts without replacing the page', () => {
    const values = new Map<string, string>()
    const style = {
      setProperty: (name: string, value: string) => { values.set(name, value) },
      removeProperty: (name: string) => { values.delete(name); return '' },
    }
    const first = generateTheme(erpPalettes[0], 'light')
    const cleanFirst = applyThemeVariables(style, first)
    expect(values.get('--erp-sidebar')).toBe(first['--erp-sidebar'])
    expect(values.get('--erp-sidebar-active')).toBe(first['--erp-sidebar-active'])
    expect(values.get('--erp-primary')).toBe(first['--erp-primary'])
    expect(values.get('--erp-background')).toBe(first['--erp-background'])
    expect(values.get('--erp-chart-blue')).toBe(first['--erp-chart-blue'])

    cleanFirst()
    const second = generateTheme(erpPalettes[2], 'dark')
    applyThemeVariables(style, second)
    expect(values.get('--erp-sidebar')).toBe(second['--erp-sidebar'])
    expect(values.get('--erp-sidebar')).not.toBe(first['--erp-sidebar'])
    expect(values.get('--erp-primary')).toBe(second['--erp-primary'])
  })
})
