import { useState } from 'react'
import type { Recommendation } from '../../lib/types/types'

type RecommendationListProps = {
  recommendations: Recommendation[]
}

export function RecommendationList({ recommendations }: RecommendationListProps) {
  const [openIndex, setOpenIndex] = useState<number | null>(null)

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
          <h2>¿Qué deberías mejorar?</h2>
        </div>
        <span className="panel-count">{recommendations.length} recomendaciones</span>
      </div>
      <ol className="recommendation-list">
        {recommendations.map((recommendation, index) => {
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
    </section>
  )
}
