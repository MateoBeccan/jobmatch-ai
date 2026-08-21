import { memo, useState } from 'react'
import type { CvSuggestion } from '../../lib/types/types'

type CvOptimizationPanelProps = {
  suggestions: CvSuggestion[]
}

const INITIAL_CV_ACTION_COUNT = 3

export const CvOptimizationPanel = memo(function CvOptimizationPanel({ suggestions }: CvOptimizationPanelProps) {
  const [showAllActions, setShowAllActions] = useState(false)

  if (suggestions.length === 0) return null

  const visibleSuggestions = showAllActions
    ? suggestions
    : suggestions.slice(0, INITIAL_CV_ACTION_COUNT)
  const hasHiddenActions = suggestions.length > INITIAL_CV_ACTION_COUNT

  return (
    <section className="results-panel optimization-panel" id="optimizar-cv" aria-label="Oportunidades para optimizar tu CV">
      <div className="panel-title-row">
        <div>
          <span className="panel-eyebrow">Optimización del CV</span>
          <h2>Cómo comunicar mejor tu perfil</h2>
        </div>
        <span className="panel-count">{suggestions.length} acciones</span>
      </div>
      <p className="optimization-intro">
        {suggestions.length === 1
          ? 'Priorizamos 1 ajuste de comunicación para que tu experiencia se entienda mejor.'
          : `Priorizamos ${Math.min(suggestions.length, INITIAL_CV_ACTION_COUNT)} ajustes de comunicación para que tu experiencia se entienda mejor.`}
      </p>
      <ol className="optimization-list">
        {visibleSuggestions.map((suggestion) => (
          <li key={suggestion.id} className="optimization-item">
            <span className="optimization-title" aria-hidden="true">!</span>
            <span className="optimization-copy">
              <strong>{suggestion.title}</strong>
              <p>{suggestion.detail}</p>
              <p className="optimization-action"><b>Acción:</b> {suggestion.action}</p>
            </span>
          </li>
        ))}
      </ol>
      {hasHiddenActions && (
        <button
          type="button"
          className="list-expand-button"
          aria-expanded={showAllActions}
          onClick={() => setShowAllActions((current) => !current)}
        >
          {showAllActions ? 'Ver menos' : `Ver más acciones (${suggestions.length - INITIAL_CV_ACTION_COUNT})`}
        </button>
      )}
    </section>
  )
})
