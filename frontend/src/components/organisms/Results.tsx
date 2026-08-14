import type { AnalysisResponse } from '../../lib/types/types'
import { getScoreClass } from '../../lib/helpers/format'

type ResultsProps = {
  result: AnalysisResponse
  onReset: () => void
}

export function Results({ result, onReset }: ResultsProps) {
  const scoreClass = getScoreClass(result.matchPercentage)
  const scoreTitle = result.matchPercentage >= 80 ? 'Un encaje muy prometedor' : result.matchPercentage >= 60 ? 'Un buen punto de partida' : 'Hay oportunidades para mejorar'

  return (
    <section className="results-card" aria-live="polite">
      <div className="results-heading"><div><span className="intro-kicker">RESULTADO DE TU ANÁLISIS</span><h1>Compatibilidad estimada</h1></div><button type="button" className="reset-button" onClick={onReset}>+ Nueva evaluación</button></div>
      <div className="score-row">
        <div className={`score ${scoreClass}`} role="img" aria-label={`${result.matchPercentage}% de compatibilidad`} style={{ '--score': result.matchPercentage } as React.CSSProperties}><div className="score-value"><strong>{result.matchPercentage}</strong><span>%</span></div><small>Compatibilidad</small></div>
        <div className="score-summary"><span className="summary-label">LECTURA GENERAL</span><h2>{scoreTitle}</h2><p>Este porcentaje es una estimación basada en la información de tu CV y los requisitos de la oferta.</p></div>
      </div>
      <div className="result-grid">
        <ResultList title="Habilidades que coinciden" items={result.matchingSkills} variant="positive" />
        <ResultList title="Habilidades o requisitos faltantes" items={result.missingSkills} variant="negative" />
        <ResultList title="Recomendaciones para tu postulación" items={result.recommendations} variant="neutral" numbered />
        <ResultList title="Posibles preguntas de entrevista" items={result.interviewQuestions} variant="neutral" questions />
      </div>
      <div className="result-actions"><button type="button" className="primary-action" onClick={onReset}>Analizar otra oferta</button></div>
      <BottomNav active="results" onAnalyze={onReset} />
    </section>
  )
}

type ResultListProps = {
  title: string
  items: string[]
  variant: 'positive' | 'negative' | 'neutral'
  numbered?: boolean
  questions?: boolean
}

function ResultList({ title, items, variant, numbered = false, questions = false }: ResultListProps) {
  return <div className={`result-panel ${variant} ${numbered ? 'numbered' : ''} ${questions ? 'questions' : ''}`}><h3><span className={`list-icon ${variant}`}>{variant === 'positive' ? '✓' : variant === 'negative' ? '!' : '✦'}</span>{title}<small>{items.length}</small></h3>{items.length > 0 ? <ul>{items.map((item, index) => <li key={`${item}-${index}`}>{numbered && <span className="item-number">{index + 1}.</span>}{item}</li>)}</ul> : <p className="empty-list">No se encontraron elementos.</p>}</div>
}

type BottomNavProps = {
  active: 'analyze' | 'results'
  onAnalyze: () => void
}

export function BottomNav({ active, onAnalyze }: BottomNavProps) {
  return <nav className="bottom-nav" aria-label="Navegación inferior"><button aria-current={active === 'analyze' ? 'page' : undefined} className={active === 'analyze' ? 'active' : ''} type="button" onClick={onAnalyze}><span aria-hidden="true">⊕</span>Inicio</button><button aria-current={active === 'results' ? 'page' : undefined} className={active === 'results' ? 'active' : ''} type="button" onClick={onAnalyze}><span aria-hidden="true">▥</span>Análisis</button></nav>
}
