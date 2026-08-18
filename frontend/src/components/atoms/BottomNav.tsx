import analysisIcon from '../../assets/navigation/analysis.png.png'
import historyIcon from '../../assets/navigation/history.png.png'
import homeIcon from '../../assets/navigation/home.png.png'

type BottomNavProps = {
  active: 'home' | 'analyze' | 'history' | 'results'
  onNavigate: (route: string) => void
}

export function BottomNav({ active, onNavigate }: BottomNavProps) {
  const isAnalyzer = active === 'analyze' || active === 'results'
  return (
    <nav className="bottom-nav" aria-label="Navegación inferior">
      <button aria-label="Inicio" aria-current={active === 'home' ? 'page' : undefined} className={active === 'home' ? 'active' : ''} type="button" onClick={() => onNavigate('/')}>
        <span className="bottom-nav-icon" aria-hidden="true">
          <img src={homeIcon} alt="" />
        </span>
        <span className="bottom-nav-label">Inicio</span>
      </button>
      <button aria-label="Análisis" aria-current={isAnalyzer ? 'page' : undefined} className={isAnalyzer ? 'active' : ''} type="button" onClick={() => onNavigate('/analizar')}>
        <span className="bottom-nav-icon" aria-hidden="true">
          <img src={analysisIcon} alt="" />
        </span>
        <span className="bottom-nav-label">Análisis</span>
      </button>
      <button aria-label="Historial" aria-current={active === 'history' ? 'page' : undefined} className={active === 'history' ? 'active' : ''} type="button" onClick={() => onNavigate('/historial')}>
        <span className="bottom-nav-icon" aria-hidden="true">
          <img src={historyIcon} alt="" />
        </span>
        <span className="bottom-nav-label">Historial</span>
      </button>
    </nav>
  )
}
