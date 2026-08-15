import type { HistoryRecord, RequirementMatch } from '../../lib/types/types'
import { buildComparison } from '../../lib/helpers/analysis'
import { ScoreRing } from '../atoms/ScoreRing'

type ComparisonViewProps = {
  previous: HistoryRecord | null
  current: HistoryRecord | null
  loading: boolean
  error: string
  onBack: () => void
  onReanalyze?: () => void
}

export function ComparisonView({ previous, current, loading, error, onBack, onReanalyze }: ComparisonViewProps) {
  if (loading) {
    return (
      <main className="comparison-shell">
        <button type="button" className="back-button" onClick={onBack} aria-label="Volver al historial">←</button>
        <div className="history-status" role="status"><span className="spinner history-spinner" /> Cargando comparación...</div>
      </main>
    )
  }

  if (error) {
    return (
      <main className="comparison-shell">
        <button type="button" className="back-button" onClick={onBack} aria-label="Volver al historial">←</button>
        <div className="history-status error" role="alert"><p>{error}</p></div>
      </main>
    )
  }

  if (!previous || !current) return null

  const comparison = buildComparison(previous, current)

  return (
    <main className="comparison-shell">
      <header className="comparison-header">
        <button type="button" className="back-button" onClick={onBack} aria-label="Volver al historial">←</button>
        <div>
          <span className="intro-kicker">COMPARACIÓN DE CVs</span>
          <h1>{comparison.role}</h1>
          <p>{comparison.company}</p>
        </div>
      </header>

      <section className="comparison-grid">
        <ComparisonColumn version={comparison.previousCvVersion} score={comparison.previousScore} requirements={previous.result.requirements ?? []} />
        <div className="comparison-delta" aria-label={`Diferencia de ${comparison.difference >= 0 ? '+' : ''}${comparison.difference} puntos`}>
          <strong className={comparison.difference >= 0 ? 'up' : 'down'}>
            {comparison.difference >= 0 ? '+' : ''}{comparison.difference}
          </strong>
          <span>puntos</span>
        </div>
        <ComparisonColumn version={comparison.currentCvVersion} score={comparison.currentScore} requirements={current.result.requirements ?? []} current />
      </section>

      <section className="comparison-details">
        {comparison.newMatches.length > 0 && (
          <div className="comparison-block positive">
            <h2>Requisitos nuevos cumplidos</h2>
            <ul>{comparison.newMatches.map((name) => <li key={name}>✓ {name}</li>)}</ul>
          </div>
        )}
        {comparison.stillMissing.length > 0 && (
          <div className="comparison-block negative">
            <h2>Siguen faltando</h2>
            <ul>{comparison.stillMissing.map((name) => <li key={name}>✕ {name}</li>)}</ul>
          </div>
        )}
        {comparison.notes.map((note, index) => <p key={index} className="comparison-note">{note}</p>)}
      </section>

      {onReanalyze && <div className="result-actions"><button type="button" className="primary-action" onClick={onReanalyze}>Volver a analizar</button></div>}
    </main>
  )
}

function ComparisonColumn({ version, score, requirements, current = false }: { version: string; score: number; requirements: RequirementMatch[]; current?: boolean }) {
  return (
    <div className={`comparison-column ${current ? 'current' : ''}`}>
      <h2>{version}</h2>
      <ScoreRing value={score} size={132} label={current ? 'Versión actual' : 'Versión anterior'} />
      <ul className="comparison-requirements">
        {requirements.map((requirement) => (
          <li key={`${requirement.name}-${requirement.status}`} className={requirement.status}>
            <span aria-hidden="true">{requirement.status === 'match' ? '✓' : requirement.status === 'partial' ? '⚠' : '✕'}</span>
            {requirement.name}
          </li>
        ))}
      </ul>
    </div>
  )
}
