type BottomNavProps = {
  active: 'analyze' | 'history' | 'results'
  onNavigate: (route: string) => void
  onNewAnalysis?: () => void
}

export function BottomNav({ active, onNavigate, onNewAnalysis }: BottomNavProps) {
  const isAnalyzer = active === 'analyze' || active === 'results'
  return (
    <nav className="bottom-nav" aria-label="Navegación inferior">
      <button aria-current={isAnalyzer ? 'page' : undefined} className={isAnalyzer ? 'active' : ''} type="button" onClick={() => onNavigate('/analizar')}>
        <span aria-hidden="true">⊕</span>Inicio
      </button>
      <button aria-current={active === 'history' ? 'page' : undefined} className={active === 'history' ? 'active' : ''} type="button" onClick={() => onNavigate('/historial')}>
        <span aria-hidden="true">▤</span>Historial
      </button>
      {onNewAnalysis && (
        <button type="button" onClick={onNewAnalysis}>
          <span aria-hidden="true">↻</span>Nuevo análisis
        </button>
      )}
    </nav>
  )
}
