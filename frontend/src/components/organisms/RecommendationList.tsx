import { useState } from 'react'
import type { Recommendation } from '../../lib/types/types'

type RecommendationListProps = {
  recommendations: Recommendation[]
}

const INITIAL_RECOMMENDATION_COUNT = 3

export function RecommendationList({ recommendations }: RecommendationListProps) {
  const [openIndex, setOpenIndex] = useState<number | null>(null)
  const [showAllRecommendations, setShowAllRecommendations] = useState(false)
  const visibleRecommendations = showAllRecommendations
    ? recommendations
    : recommendations.slice(0, INITIAL_RECOMMENDATION_COUNT)
  const hasHiddenRecommendations = recommendations.length > INITIAL_RECOMMENDATION_COUNT

  if (recommendations.length === 0) {
    return (
      <section className="results-panel recommendations-section" aria-label="Recomendaciones">
        <div className="panel-title-row">
          <div>
            <span className="panel-eyebrow">Acciones sugeridas</span>
            <h2>Recomendaciones</h2>
          </div>
        </div>
        <p className="empty-list">No hay recomendaciones para mostrar.</p>
      </section>
    )
  }

  return (
    <section className="results-panel recommendations-section" aria-label="Recomendaciones para tu postulación">
      <div className="panel-title-row">
        <div>
          <span className="panel-eyebrow">Acciones sugeridas</span>
          <h2>Qué deberías priorizar ahora</h2>
        </div>
        <span className="panel-count">{recommendations.length} recomendaciones</span>
      </div>
      <ol className="recommendation-list">
        {visibleRecommendations.map((recommendation, index) => {
          const expanded = openIndex === index
          return (
            <li key={`${recommendation.problem}-${index}`} className="recommendation-item">
              <span className="recommendation-index" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
              <span className="recommendation-copy">
                <strong>{recommendation.problem}</strong>
                {recommendation.explanation && <p>{recommendation.explanation}</p>}
                {recommendation.action && (
                  <>
                    <button
                      type="button"
                      className="recommendation-toggle"
                      aria-expanded={expanded}
                      onClick={() => setOpenIndex(expanded ? null : index)}
                    >
                      {expanded ? 'Ocultar sugerencia' : 'Ver sugerencia'}
                    </button>
                    {expanded && <p className="recommendation-action"><b>Acción:</b> {recommendation.action}</p>}
                  </>
                )}
              </span>
            </li>
          )
        })}
      </ol>
      {hasHiddenRecommendations && (
        <button
          type="button"
          className="list-expand-button"
          aria-expanded={showAllRecommendations}
          onClick={() => setShowAllRecommendations((current) => !current)}
        >
          {showAllRecommendations ? 'Ver menos' : `Ver más recomendaciones (${recommendations.length - INITIAL_RECOMMENDATION_COUNT})`}
        </button>
      )}
    </section>
  )
}
