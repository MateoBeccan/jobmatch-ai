import { useEffect, useState } from 'react'
import { AnalysisStepper, ANALYSIS_STEPS } from './AnalysisStepper'

const LOADING_STEPS = ['Leyendo CV', 'Extrayendo habilidades', 'Comparando requisitos', 'Generando recomendaciones']
const LOADING_MESSAGES = [
  'Leyendo tu CV...',
  'Extrayendo tus habilidades...',
  'Comparando tus habilidades con la oferta...',
  'Preparando recomendaciones...',
]
const LOADING_PROGRESS = [22, 46, 72, 92]

export function LoadingScreen() {
  const [activeStep, setActiveStep] = useState(0)
  const progress = LOADING_PROGRESS[activeStep]

  useEffect(() => {
    const timer = window.setInterval(() => {
      setActiveStep((currentStep) => Math.min(currentStep + 1, LOADING_STEPS.length - 1))
    }, 1900)

    return () => window.clearInterval(timer)
  }, [])

  return (
    <main className="loading-shell">
      <header className="loading-header">JobMatch <b>AI</b></header>
      <section className="loading-card" aria-live="polite">
        <div className="loading-visual" aria-hidden="true">
          <div className="loading-orbit loading-orbit-one" />
          <div className="loading-orbit loading-orbit-two" />
          <div className="document-preview">
            <span className="document-fold" />
            <span className="document-line document-line-long" />
            <span className="document-line" />
            <span className="document-line document-line-short" />
            <span className="document-line" />
            <span className="document-line document-line-medium" />
            <span className="scan-beam" />
          </div>
          <span className="loading-pulse pulse-one" />
          <span className="loading-pulse pulse-two" />
          <span className="loading-pulse pulse-three" />
        </div>
        <AnalysisStepper steps={ANALYSIS_STEPS} current="analysis" />
        <p className="loading-kicker">ANÁLISIS EN CURSO</p>
        <h1>{LOADING_MESSAGES[activeStep]}</h1>
        <p className="loading-copy">Nuestra IA está comparando tus habilidades con los requisitos de la oferta.</p>
        <div className="loading-progress" aria-label={`${progress}% completado`} role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress}>
          <span style={{ width: `${progress}%` }} />
        </div>
        <div className="loading-steps-list">
          {LOADING_STEPS.map((step, index) => (
            <div className={`loading-steps ${index < activeStep ? 'done' : index === activeStep ? 'current' : 'pending'}`} key={step}>
              <span className="step-status" aria-hidden="true">{index < activeStep ? '✓' : index === activeStep ? '•' : '○'}</span>
              {step}
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}
