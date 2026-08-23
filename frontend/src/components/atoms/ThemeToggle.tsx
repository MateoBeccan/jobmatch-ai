import darkThemeIcon from '../../assets/career-multiverse/paths/dark-theme.png'
import lightThemeIcon from '../../assets/career-multiverse/paths/light-theme.png'
import type { Theme } from '../../lib/types/types'

type ThemeToggleProps = {
  theme: Theme
  onToggle: () => void
}

export function ThemeToggle({ theme, onToggle }: ThemeToggleProps) {
  const isDark = theme === 'dark'
  const label = isDark ? 'Activar modo claro' : 'Activar modo oscuro'
  const icon = isDark ? lightThemeIcon : darkThemeIcon

  return (
    <button className="theme-toggle" type="button" aria-pressed={isDark} aria-label={label} title={label} onClick={onToggle}>
      <span className="theme-toggle-icon" aria-hidden="true">
        <img src={icon} alt="" />
      </span>
    </button>
  )
}
