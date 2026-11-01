import type { ThemeTokens } from './theme-generator'

type VariableStyle = Pick<CSSStyleDeclaration, 'setProperty' | 'removeProperty'>

export function applyThemeVariables(style: VariableStyle, tokens: ThemeTokens | null): () => void {
  if (!tokens) return () => undefined
  for (const [name, value] of Object.entries(tokens)) style.setProperty(name, value)
  return () => { for (const name of Object.keys(tokens)) style.removeProperty(name) }
}
