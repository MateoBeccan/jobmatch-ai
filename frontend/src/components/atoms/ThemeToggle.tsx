import type { Theme } from '../../lib/types/types'

type ThemeToggleProps = {
  theme: Theme
  onToggle: () => void
}

export function ThemeToggle({ theme, onToggle }: ThemeToggleProps) {
  const isDark = theme === 'dark'
  const label = isDark ? 'Activar modo claro' : 'Activar modo oscuro'

  return (
    <button className="theme-toggle inline-flex size-[38px] items-center justify-center rounded-lg p-0" type="button" aria-pressed={isDark} aria-label={label} title={label} onClick={onToggle}>
      <span className="theme-toggle-icon" aria-hidden="true">{isDark ? '☀' : '☾'}</span>
    </button>
  )
}
