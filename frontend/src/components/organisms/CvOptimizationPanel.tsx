import { memo } from 'react'
import type { CvSuggestion } from '../../lib/types/types'

type CvOptimizationPanelProps = {
  suggestions: CvSuggestion[]
}

export const CvOptimizationPanel = memo(function CvOptimizationPanel({ suggestions }: CvOptimizationPanelProps) {
  if (suggestions.length === 0) return null

  return (
    <section className="results-panel optimization-panel" id="optimizar-cv" aria-label="Oportunidades para optimizar tu CV">
      <h2>Optimizar mi CV</h2>
      <p className="optimization-intro">
        {suggestions.length === 1
          ? 'Detectamos 1 oportunidad concreta de mejora.'
          : `Detectamos ${suggestions.length} oportunidades concretas de mejora.`}
      </p>
      <ol className="optimization-list">
        {suggestions.map((suggestion) => (
          <li key={suggestion.id} className="optimization-item">
            <span className="optimization-title" aria-hidden="true">⚠</span>
            <span className="optimization-copy">
              <strong>{suggestion.title}</strong>
              <p>{suggestion.detail}</p>
              <p className="optimization-action"><b>Acción:</b> {suggestion.action}</p>
            </span>
          </li>
        ))}
      </ol>
    </section>
  )
})
