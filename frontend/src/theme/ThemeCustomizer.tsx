import { useEffect, useRef, useState } from 'react'
import { Check, Palette, X } from 'lucide-react'
import { Button } from '../components/ui/button'
import { erpPalettes, type ERPColorPalette } from './palettes'
import { useTheme } from './ThemeProvider'
import type { Appearance } from './theme-storage'

const appearances: readonly { id: Appearance; label: string }[] = [
  { id: 'light', label: 'Light' },
  { id: 'dark', label: 'Dark' },
  { id: 'system', label: 'System' },
]

function PaletteCard({ palette }: { palette: ERPColorPalette }) {
  const { preference, selectPalette } = useTheme()
  const selected = preference.paletteId === palette.id
  return <button type="button" className={`theme-palette-card${selected ? ' selected' : ''}`}
    aria-label={`${palette.name} palette`} aria-pressed={selected} onClick={() => selectPalette(palette.id)}>
    <span className="theme-palette-swatches" aria-hidden="true">{palette.colors.map((color, index) => <span key={index} style={{ backgroundColor: color }}/>)}</span>
    <span className="theme-palette-label">{palette.name}{selected && <Check size={15} aria-hidden="true"/>}</span>
  </button>
}

export function ThemeCustomizer() {
  const [open, setOpen] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)
  const { preference, setAppearance, reset } = useTheme()

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return <>
    <Button variant="ghost" aria-label="Customize Theme" title="Customize Theme" aria-haspopup="dialog" aria-expanded={open}
      onClick={() => setOpen(true)}><Palette size={19}/></Button>
    <dialog ref={dialogRef} className="theme-dialog" aria-labelledby="theme-dialog-title" onClose={() => setOpen(false)}>
      <div className="theme-dialog-header"><div><h2 id="theme-dialog-title">Appearance &amp; Themes</h2><p>Personalize your ERP workspace.</p></div>
        <Button variant="ghost" aria-label="Close theme customization" onClick={() => setOpen(false)}><X size={20}/></Button></div>
      <div className="theme-dialog-body">
        <section aria-labelledby="appearance-heading"><h3 id="appearance-heading">Appearance</h3>
          <div className="theme-mode-group" role="group" aria-label="Appearance mode">{appearances.map(mode =>
            <button type="button" key={mode.id} className={preference.appearance === mode.id ? 'selected' : ''}
              aria-pressed={preference.appearance === mode.id} onClick={() => setAppearance(mode.id)}>{mode.label}</button>
          )}</div></section>
        <section aria-labelledby="palette-heading"><h3 id="palette-heading">Color palettes</h3>
          <div className="theme-palette-grid">{erpPalettes.map(palette => <PaletteCard key={palette.id} palette={palette}/>)}</div>
        </section>
      </div>
      <div className="theme-dialog-actions"><Button variant="outline" onClick={reset}>Reset to Default</Button><Button onClick={() => setOpen(false)}>Close</Button></div>
    </dialog>
  </>
}
