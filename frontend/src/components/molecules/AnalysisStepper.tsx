type StepperStep = {
  key: string
  label: string
}

type AnalysisStepperProps = {
  steps: StepperStep[]
  current: string
  complete?: boolean
}

export function AnalysisStepper({ steps, current, complete = false }: AnalysisStepperProps) {
  const currentIndex = complete ? steps.length : steps.findIndex((step) => step.key === current)

  return (
    <ol className="analysis-stepper" aria-label="Progreso del análisis">
      {steps.map((step, index) => {
        const status = complete || index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'pending'
        return (
          <li key={step.key} className={status} aria-current={status === 'current' ? 'step' : undefined}>
            <span className="stepper-status" aria-hidden="true">{status === 'done' ? '✓' : status === 'current' ? '●' : '○'}</span>
            <span className="stepper-index" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
            <span className="stepper-label">{step.label}</span>
          </li>
        )
      })}
    </ol>
  )
}

export const ANALYSIS_STEPS: StepperStep[] = [
  { key: 'cv', label: 'Tu CV' },
  { key: 'offer', label: 'Oferta' },
  { key: 'analysis', label: 'Análisis' },
  { key: 'result', label: 'Resultado' },
]
