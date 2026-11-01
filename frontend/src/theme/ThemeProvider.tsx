import { createContext, useCallback, useContext, useEffect, useLayoutEffect, useMemo, useState, type ReactNode } from 'react'
import { useAuth } from '../features/auth/AuthProvider'
import { findPalette } from './palettes'
import { generateTheme, type ResolvedMode } from './theme-generator'
import { applyThemeVariables } from './theme-dom'
import { defaultPreference, readPreference, resolveAppearance, savePreference, type Appearance, type ThemePreference } from './theme-storage'

interface ThemeContextValue {
  preference: ThemePreference
  resolvedMode: ResolvedMode
  selectPalette: (id: string) => void
  setAppearance: (appearance: Appearance) => void
  reset: () => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)
const initialSystemDark = () => window.matchMedia('(prefers-color-scheme: dark)').matches

export function ThemeProvider({ children }: { children: ReactNode }) {
  const userId = useAuth().session?.user.id ?? null
  const [preference, setPreference] = useState<ThemePreference>(() => readPreference(userId))
  const [systemDark, setSystemDark] = useState(initialSystemDark)
  const resolvedMode: ResolvedMode = resolveAppearance(preference.appearance, systemDark)

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = (event: MediaQueryListEvent) => setSystemDark(event.matches)
    setSystemDark(media.matches)
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  // Layout effect keeps account switches from painting the previous user's theme.
  useLayoutEffect(() => { setPreference(readPreference(userId)) }, [userId])

  const tokens = useMemo(() => {
    const palette = findPalette(preference.paletteId)
    return palette && palette.id !== 'default' ? generateTheme(palette, resolvedMode) : null
  }, [preference.paletteId, resolvedMode])

  useLayoutEffect(() => {
    const root = document.documentElement
    root.classList.toggle('dark', resolvedMode === 'dark')
    root.dataset.themePalette = preference.paletteId
    return applyThemeVariables(root.style, tokens)
  }, [tokens, resolvedMode, preference.paletteId])

  const update = useCallback((next: ThemePreference) => {
    setPreference(next)
    savePreference(userId, next)
  }, [userId])

  const value = useMemo<ThemeContextValue>(() => ({
    preference, resolvedMode,
    selectPalette: id => { if (findPalette(id)) update({ ...preference, paletteId: id }) },
    setAppearance: appearance => update({ ...preference, appearance }),
    reset: () => update(defaultPreference),
  }), [preference, resolvedMode, update])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('ThemeProvider is missing')
  return context
}
