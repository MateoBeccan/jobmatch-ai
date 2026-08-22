import { useMemo, useState } from 'react'
import { ScoreRing } from '../atoms/ScoreRing'
import { BottomNav } from '../atoms/BottomNav'
import { AnalysisStepper, ANALYSIS_STEPS } from '../molecules/AnalysisStepper'
import { ScoreExplanationPanel } from './ScoreExplanationPanel'
import { RequirementsSection } from './RequirementsSection'
import { RecommendationList } from './RecommendationList'
import { CvOptimizationPanel } from './CvOptimizationPanel'
import { InterviewQuestionsPanel } from './InterviewQuestionsPanel'
import { JobSearchPanel } from './JobSearchPanel'
import type { AnalysisResponse, CareerMultiverseRequest, CareerRegion, CriticalRequirementGap, ExperienceGap, JobSearchProfile, ScoreBreakdown } from '../../lib/types/types'
import { buildCvSuggestions, buildScoreExplanation, toStructuredRecommendations } from '../../lib/helpers/analysis'
import { getScoreClass } from '../../lib/helpers/format'

type ResultsProps = {
  result: AnalysisResponse
  onReset: () => void
  onReanalyze?: () => void
  onNavigate: (route: string) => void
  onExploreCareer?: (request: CareerMultiverseRequest) => void
}

const BREAKDOWN_LABELS: Array<{ key: keyof ScoreBreakdown; label: string; caption: string }> = [
  { key: 'mandatoryTechnical', label: 'Técnicos', caption: 'Requisitos clave' },
  { key: 'experienceSeniority', label: 'Experiencia', caption: 'Nivel y trayectoria' },
  { key: 'desirable', label: 'Deseables', caption: 'Plus de la oferta' },
  { key: 'complementary', label: 'Complementarios', caption: 'Señales extra' },
]

const CATEGORY_LABELS: Record<string, string> = {
  mandatory_technical: 'Técnico obligatorio',
  experience_seniority: 'Experiencia',
  desirable: 'Deseable',
  complementary: 'Complementario',
}

const EXPERIENCE_STATUS_LABELS: Record<ExperienceGap['status'], string> = {
  missing: 'Faltante',
  partial: 'Parcial',
  match: 'Coincide',
}

const INITIAL_WARNING_COUNT = 3

const CAREER_REGION_OPTIONS: Array<{ value: CareerRegion; label: string }> = [
  { value: 'ARGENTINA', label: 'Argentina' },
  { value: 'LATAM', label: 'LATAM' },
  { value: 'GLOBAL', label: 'Global' },
]

export function Results({ result, onReset, onReanalyze, onNavigate, onExploreCareer }: ResultsProps) {
  const [careerRegion, setCareerRegion] = useState<CareerRegion>('LATAM')
  const scoreClass = getScoreClass(result.matchPercentage)
  const scoreTitle = result.matchPercentage >= 80
    ? 'Un encaje muy prometedor'
    : result.matchPercentage >= 60
      ? 'Un buen punto de partida'
      : 'Hay oportunidades para mejorar'
  const warnings = result.warnings ?? []
  const criticalMissingRequirements = result.criticalMissingRequirements ?? []
  const experienceGap = result.experienceGap ?? null
  const requirements = result.requirements ?? []
  const breakdownChips = BREAKDOWN_LABELS.filter(({ key }) => typeof result.breakdown?.[key] === 'number')
  const hasCriticalCapWarning = warnings.some((warning) => warning.toLowerCase().includes('limitado'))
  const suggestions = useMemo(() => buildCvSuggestions(result), [result])
  const explanation = useMemo(() => buildScoreExplanation(result), [result])
  const structuredRecommendations = useMemo(() => toStructuredRecommendations(result.recommendations), [result.recommendations])
  const careerProfile = useMemo(() => buildCareerRequest(result.jobSearchProfile, careerRegion), [careerRegion, result.jobSearchProfile])

  const scrollToOptimization = () => {
    document.getElementById('optimizar-cv')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <section className="results-card" aria-live="polite">
      <div className="results-heading">
        <div>
          <span className="intro-kicker">RESULTADO DE TU ANALISIS</span>
          <h1>Compatibilidad estimada</h1>
        </div>
        <button type="button" className="reset-button" onClick={onReset}>+ Nueva evaluación</button>
      </div>

      <AnalysisStepper steps={ANALYSIS_STEPS} current="result" complete />

      <div className="score-row">
        <div className="score-ring-column">
          <ScoreRing value={result.matchPercentage} />
        </div>
        <div className="score-summary">
          <span className="summary-label">LECTURA GENERAL</span>
          <h2 className={scoreClass}>{scoreTitle}</h2>
          <p>Este porcentaje es una estimación basada en la información de tu CV y los requisitos de la oferta.</p>
          <WarningsBlock warnings={warnings} />
          {breakdownChips.length > 0 && (
            <div className="breakdown-area">
              <div className="breakdown-chips" aria-label="Desglose por categoría">
                {breakdownChips.map(({ key, label, caption }) => (
                  <span key={key} className="breakdown-chip">
                    <b>{result.breakdown![key]}%</b>
                    <span>{label}</span>
                    <small>{caption}</small>
                  </span>
                ))}
              </div>
              {hasCriticalCapWarning && (
                <p className="breakdown-note">El porcentaje final está limitado por requisitos críticos.</p>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="results-stack">
        <CriticalMissingPanel gaps={criticalMissingRequirements} experienceGap={experienceGap} />
        <ExperienceGapPanel gap={experienceGap} duplicatedInCritical={isExperienceDuplicated(criticalMissingRequirements, experienceGap)} />
        <ScoreExplanationPanel explanation={explanation} />
        <RequirementsSection requirements={requirements} />
        <RecommendationList recommendations={structuredRecommendations} />
        <CvOptimizationPanel suggestions={suggestions} />
        <InterviewQuestionsPanel questions={result.interviewQuestions} />
        {result.jobSearchProfile && (
          <JobSearchPanel
            key={`${result.jobSearchProfile.role}-${result.jobSearchProfile.seniority}-${result.jobSearchProfile.keywords.join('|')}`}
            profile={result.jobSearchProfile}
          />
        )}
        {careerProfile && onExploreCareer && (
          <section className="results-panel career-cta-panel" aria-labelledby="career-cta-title">
            <div>
              <span className="panel-eyebrow">Career Multiverse</span>
              <h2 id="career-cta-title">Tu perfil puede llevarte por distintos caminos.</h2>
              <p>Explora rutas profesionales relacionadas con tus habilidades y contrastalas con senales del mercado laboral actual.</p>
            </div>
            <div className="career-cta-controls">
              <fieldset>
                <legend>Donde queres explorar oportunidades?</legend>
                <div className="career-region-options">
                  {CAREER_REGION_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      aria-pressed={careerRegion === option.value}
                      className={careerRegion === option.value ? 'active' : ''}
                      onClick={() => setCareerRegion(option.value)}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </fieldset>
              <button type="button" className="primary-action" onClick={() => onExploreCareer(careerProfile)}>Explorar mi futuro</button>
            </div>
          </section>
        )}
      </div>

      <div className="result-actions">
        {suggestions.length > 0 && <button type="button" className="primary-action" onClick={scrollToOptimization}>Optimizar mi CV</button>}
        <button type="button" className="secondary-action" onClick={onReanalyze ?? onReset}>Volver a analizar</button>
      </div>

      <BottomNav active="results" onNavigate={onNavigate} />
    </section>
  )
}

function buildCareerRequest(profile: JobSearchProfile | undefined, region: CareerRegion): CareerMultiverseRequest | null {
  if (!profile) return null
  const role = profile.role.trim()
  const skills = profile.keywords.map((keyword) => keyword.trim()).filter(Boolean)
  if (!role || !profile.seniority || skills.length === 0 || !region) return null

  return {
    role,
    seniority: profile.seniority,
    skills,
    region,
  }
}

function WarningsBlock({ warnings }: { warnings: string[] }) {
  const [showAllWarnings, setShowAllWarnings] = useState(false)
  if (warnings.length === 0) return null

  const visibleWarnings = showAllWarnings ? warnings : warnings.slice(0, INITIAL_WARNING_COUNT)
  const hasHiddenWarnings = warnings.length > INITIAL_WARNING_COUNT

  return (
    <aside className="result-warnings" aria-label="Avisos importantes del análisis">
      <span className="warning-icon" aria-hidden="true">!</span>
      <div>
        <strong>Importante</strong>
        <ul>
          {visibleWarnings.map((warning) => (
            <li key={warning}>{warning}</li>
          ))}
        </ul>
        {hasHiddenWarnings && (
          <button
            type="button"
            className="warning-expand-button"
            aria-expanded={showAllWarnings}
            onClick={() => setShowAllWarnings((current) => !current)}
          >
            {showAllWarnings ? 'Ver menos' : `Ver más avisos (${warnings.length - INITIAL_WARNING_COUNT})`}
          </button>
        )}
      </div>
    </aside>
  )
}

function CriticalMissingPanel({ gaps, experienceGap }: { gaps: CriticalRequirementGap[]; experienceGap: ExperienceGap | null }) {
  if (gaps.length === 0) return null

  return (
    <section className="results-panel critical-gap-panel" aria-label="Requisitos críticos faltantes">
      <div className="panel-title-row">
        <div>
          <span className="panel-eyebrow">Prioridad alta</span>
          <h2>Requisitos críticos faltantes</h2>
        </div>
        <span className="panel-count">{gaps.length} {gaps.length === 1 ? 'requisito' : 'requisitos'}</span>
      </div>
      <ul className="critical-gap-list">
        {gaps.map((gap) => {
          const duplicatedExperience = sameRequirement(gap.requirement, experienceGap?.requirement)
          return (
            <li key={`${gap.requirement}-${gap.category}`} className="critical-gap-item">
              <span className="critical-gap-marker" aria-hidden="true">!</span>
              <span className="critical-gap-copy">
                <strong>{gap.requirement}</strong>
                <span>{formatCategory(gap.category)}</span>
                {duplicatedExperience
                  ? <small>Ver detalle específico en la brecha de experiencia.</small>
                  : gap.evidence && <small>{gap.evidence}</small>}
              </span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function ExperienceGapPanel({ gap, duplicatedInCritical }: { gap: ExperienceGap | null; duplicatedInCritical: boolean }) {
  if (!gap) return null

  return (
    <section className="results-panel experience-gap-panel" aria-label="Brecha de experiencia">
      <div className="panel-title-row">
        <div>
          <span className="panel-eyebrow">Seniority</span>
          <h2>Brecha de experiencia</h2>
        </div>
        <span className="experience-badges">
          <span className={`experience-status ${gap.status}`}>{EXPERIENCE_STATUS_LABELS[gap.status]}</span>
          {gap.critical && <span className="experience-critical">Requisito crítico</span>}
        </span>
      </div>
      <div className="experience-gap-copy">
        <strong>{gap.requirement}</strong>
        <p>{gap.summary}</p>
        {duplicatedInCritical && <small>Este punto también figura como requisito crítico faltante.</small>}
      </div>
    </section>
  )
}

function isExperienceDuplicated(gaps: CriticalRequirementGap[], gap: ExperienceGap | null) {
  if (!gap) return false
  return gaps.some((criticalGap) => sameRequirement(criticalGap.requirement, gap.requirement))
}

function sameRequirement(left: string | undefined, right: string | undefined) {
  return Boolean(left && right && left.trim().toLowerCase() === right.trim().toLowerCase())
}

function formatCategory(category: string) {
  return CATEGORY_LABELS[category] ?? category.replaceAll('_', ' ')
}
