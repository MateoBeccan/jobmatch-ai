type InterviewQuestionsPanelProps = {
  questions: string[]
}

export function InterviewQuestionsPanel({ questions }: InterviewQuestionsPanelProps) {
  if (questions.length === 0) return null

  return (
    <section className="results-panel interview-panel" aria-label="Posibles preguntas de entrevista">
      <h2>Preguntas de entrevista</h2>
      <ul className="interview-list">
        {questions.map((question, index) => (
          <li key={`${question}-${index}`}>{question}</li>
        ))}
      </ul>
    </section>
  )
}
