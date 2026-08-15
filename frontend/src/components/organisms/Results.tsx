import { ScoreRing } from '../atoms/ScoreRing'
import { BottomNav } from '../atoms/BottomNav'
import { AnalysisStepper, ANALYSIS_STEPS } from '../molecules/AnalysisStepper'
import { ScoreExplanationPanel } from './ScoreExplanationPanel'
import { RequirementsSection } from './RequirementsSection'
import { RecommendationList } from './RecommendationList'
import { CvOptimizationPanel } from './CvOptimizationPanel'
import { InterviewQuestionsPanel } from './InterviewQuestionsPanel'
import type { AnalysisResponse, ScoreBreakdown } from '../../lib/types/types'
import { buildCvSuggestions, buildScoreExplanation, toStructuredRecommendations } from '../../lib/helpers/analysis'
import { getScoreClass } from '../../lib/helpers/format'

type ResultsProps = {
  result: AnalysisResponse
  onReset: () => void
  onReanalyze?: () => void
  onNavigate: (route: string) => void
}

const BREAKDOWN_LABELS: Array<{ key: keyof ScoreBreakdown; label: string }> = [
  { key: 'mandatoryTechnical', label: 'Técnicos' },
  { key: 'experienceSeniority', label: 'Experiencia' },
  { key: 'desirable', label: 'Deseables' },
  { key: 'complementary', label: 'Complementarios' },
]

export function Results({ result, onReset, onReanalyze, onNavigate }: ResultsProps) {
  const scoreClass = getScoreClass(result.matchPercentage)
  const scoreTitle = result.matchPercentage >= 80
    ? 'Un encaje muy prometedor'
    : result.matchPercentage >= 60
      ? 'Un buen punto de partida'
      : 'Hay oportunidades para mejorar'
  const requirements = result.requirements ?? []
  const breakdownChips = BREAKDOWN_LABELS.filter(({ key }) => typeof result.breakdown?.[key] === 'number')
  const suggestions = buildCvSuggestions(result)

  const scrollToOptimization = () => {
    document.getElementById('optimizar-cv')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <section className="results-card" aria-live="polite">
      <div className="results-heading">
        <div>
          <span className="intro-kicker">RESULTADO DE TU ANÁLISIS</span>
          <h1>Compatibilidad estimada</h1>
        </div>
        <button type="button" className="reset-button" onClick={onReset}>+ Nueva evaluación</button>
      </div>

      <AnalysisStepper steps={ANALYSIS_STEPS} current="result" complete />

      <div className="score-row">
        <ScoreRing value={result.matchPercentage} />
        <div className="score-summary">
          <span className="summary-label">LECTURA GENERAL</span>
          <h2 className={scoreClass}>{scoreTitle}</h2>
          <p>Este porcentaje es una estimación basada en la información de tu CV y los requisitos de la oferta.</p>
          {breakdownChips.length > 0 && (
            <div className="breakdown-chips" aria-label="Desglose por categoría">
              {breakdownChips.map(({ key, label }) => (
                <span key={key} className="breakdown-chip"><b>{result.breakdown![key]}%</b>{label}</span>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="results-stack">
        <ScoreExplanationPanel explanation={buildScoreExplanation(result)} />
        <RequirementsSection requirements={requirements} />
        <RecommendationList recommendations={toStructuredRecommendations(result.recommendations)} />
        <CvOptimizationPanel suggestions={suggestions} />
        <InterviewQuestionsPanel questions={result.interviewQuestions} />
      </div>

      <div className="result-actions">
        {suggestions.length > 0 && <button type="button" className="primary-action" onClick={scrollToOptimization}>Optimizar mi CV</button>}
        <button type="button" className="secondary-action" onClick={onReanalyze ?? onReset}>Volver a analizar</button>
      </div>

      <BottomNav active="results" onNavigate={onNavigate} onNewAnalysis={onReset} />
    </section>
  )
}
