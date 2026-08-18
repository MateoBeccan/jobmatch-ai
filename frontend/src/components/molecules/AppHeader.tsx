import type { Theme } from '../../lib/types/types'
import { BRAND_LOGO_PATH, BRAND_NAME } from '../../lib/constants/brand'
import { ThemeToggle } from '../atoms/ThemeToggle'

type HeaderRoute = 'home' | 'analyze' | 'history'

type AppHeaderProps = {
  active: HeaderRoute
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
}

const NAV_ITEMS: Array<{ route: string; label: string; value: HeaderRoute }> = [
  { route: '/', label: 'Inicio', value: 'home' },
  { route: '/analizar', label: 'Analizar CV', value: 'analyze' },
  { route: '/historial', label: 'Historial', value: 'history' },
]

export function AppHeader({ active, theme, onToggleTheme, onNavigate }: AppHeaderProps) {
  return (
    <header className="app-header">
      <button className="brand-lockup" type="button" onClick={() => onNavigate('/')} aria-label="Ir al inicio">
        <img src={BRAND_LOGO_PATH} alt={BRAND_NAME} />
        <span>{BRAND_NAME}</span>
      </button>
      <nav className="top-links" aria-label="Navegacion principal">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.value}
            className={active === item.value ? 'active' : ''}
            type="button"
            aria-current={active === item.value ? 'page' : undefined}
            onClick={() => onNavigate(item.route)}
          >
            {item.label}
          </button>
        ))}
      </nav>
      <ThemeToggle theme={theme} onToggle={onToggleTheme} />
    </header>
  )
}
