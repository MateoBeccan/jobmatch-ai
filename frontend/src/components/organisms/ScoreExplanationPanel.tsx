import type { ScoreExplanation } from '../../lib/types/types'

type ScoreExplanationPanelProps = {
  explanation: ScoreExplanation
}

const FACTOR_ICONS: Record<ScoreExplanation['factors'][number]['type'], string> = {
  positive: '✓',
  partial: '⚠',
  missing: '✕',
}

export function ScoreExplanationPanel({ explanation }: ScoreExplanationPanelProps) {
  return (
    <section className="results-panel score-explanation-panel" aria-label="Por qué obtuviste este resultado">
      <h2>¿Por qué obtuve este resultado?</h2>
      <p className="score-explanation-summary">{explanation.summary}</p>
      <ul className="score-factor-list">
        {explanation.factors.map((factor, index) => (
          <li key={`${factor.type}-${index}`} className={factor.type}>
            <span aria-hidden="true">{FACTOR_ICONS[factor.type]}</span>
            {factor.text}
          </li>
        ))}
      </ul>
    </section>
  )
}
