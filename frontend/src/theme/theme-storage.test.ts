import { afterEach, describe, expect, it, vi } from 'vitest'
import { defaultPreference, parsePreference, preferenceKey, readPreference, resolveAppearance, savePreference } from './theme-storage'

afterEach(() => vi.unstubAllGlobals())

describe('theme preferences', () => {
  it('isolates keys for different ERP users', () => {
    expect(preferenceKey('user-a')).not.toBe(preferenceKey('user-b'))
    expect(preferenceKey(null)).not.toBe(preferenceKey('user-a'))
  })

  it('rejects unknown palettes and invalid appearance values', () => {
    expect(parsePreference(JSON.stringify({ version: 1, paletteId: 'palette-99', appearance: 'dark' }))).toEqual(defaultPreference)
    expect(parsePreference(JSON.stringify({ version: 1, paletteId: 'default', appearance: 'neon' }))).toEqual(defaultPreference)
    expect(parsePreference('broken')).toEqual(defaultPreference)
  })

  it('accepts valid light, dark, and system preferences', () => {
    for (const appearance of ['light', 'dark', 'system']) {
      expect(parsePreference(JSON.stringify({ version: 1, paletteId: 'default', appearance }))).toEqual({ version: 1, paletteId: 'default', appearance })
    }
  })

  it('follows system preference changes only in system mode', () => {
    expect(resolveAppearance('system', false)).toBe('light')
    expect(resolveAppearance('system', true)).toBe('dark')
    expect(resolveAppearance('light', true)).toBe('light')
    expect(resolveAppearance('dark', false)).toBe('dark')
  })

  it('restores each user’s saved preference independently', () => {
    const values = new Map<string, string>()
    vi.stubGlobal('window', { localStorage: {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => { values.set(key, value) },
    } })
    const chosen = { version: 1 as const, paletteId: 'default', appearance: 'dark' as const }
    savePreference('cashier', chosen)
    expect(readPreference('cashier')).toEqual(chosen)
    expect(readPreference('manager')).toEqual(defaultPreference)
  })
})
