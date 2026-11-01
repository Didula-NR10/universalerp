import { findPalette } from './palettes'

export type Appearance = 'light' | 'dark' | 'system'
export interface ThemePreference { version: 1; paletteId: string; appearance: Appearance }

export const defaultPreference: ThemePreference = { version: 1, paletteId: 'default', appearance: 'light' }

export function resolveAppearance(appearance: Appearance, systemDark: boolean): 'light' | 'dark' {
  return appearance === 'system' ? (systemDark ? 'dark' : 'light') : appearance
}

export function preferenceKey(userId: string | null): string {
  return `erp-theme:v1:${userId ? `user:${userId}` : 'guest'}`
}

export function parsePreference(raw: string | null): ThemePreference {
  if (!raw) return defaultPreference
  try {
    const value = JSON.parse(raw) as Partial<ThemePreference>
    if (value.version !== 1 || typeof value.paletteId !== 'string' || !findPalette(value.paletteId)) return defaultPreference
    if (value.appearance !== 'light' && value.appearance !== 'dark' && value.appearance !== 'system') return defaultPreference
    return { version: 1, paletteId: value.paletteId, appearance: value.appearance }
  } catch { return defaultPreference }
}

export function readPreference(userId: string | null): ThemePreference {
  try { return parsePreference(window.localStorage.getItem(preferenceKey(userId))) }
  catch { return defaultPreference }
}

export function savePreference(userId: string | null, preference: ThemePreference): void {
  try { window.localStorage.setItem(preferenceKey(userId), JSON.stringify(preference)) }
  catch { /* Storage can be disabled; the in-memory selection still works. */ }
}
