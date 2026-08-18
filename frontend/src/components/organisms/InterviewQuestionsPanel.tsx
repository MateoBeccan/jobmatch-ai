import { memo } from 'react'

type InterviewQuestionsPanelProps = {
  questions: string[]
}

export const InterviewQuestionsPanel = memo(function InterviewQuestionsPanel({ questions }: InterviewQuestionsPanelProps) {
  if (questions.length === 0) return null

  return (
    <section className="results-panel interview-panel" aria-label="Posibles preguntas de entrevista">
      <div className="panel-title-row">
        <div>
          <span className="panel-eyebrow">Preparacion</span>
          <h2>Preguntas de entrevista</h2>
        </div>
        <span className="panel-count">{questions.length} preguntas</span>
      </div>
      <ol className="interview-list">
        {questions.map((question, index) => (
          <li key={`${question}-${index}`}>
            <span aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
            <p>{question}</p>
          </li>
        ))}
      </ol>
    </section>
  )
})
