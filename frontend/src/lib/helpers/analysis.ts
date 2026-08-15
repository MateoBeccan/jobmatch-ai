import type {
  AnalysisComparison,
  AnalysisResponse,
  AnalysisSummary,
  CvSuggestion,
  HistoryRecord,
  HistoryStats,
  Recommendation,
  RequirementMatch,
  RequirementStatus,
  ScoreExplanation,
  ScoreFactor,
  ScoreRange,
} from '../types/types'

const STATUS_LABELS: Record<RequirementStatus, string> = {
  match: 'Coincide',
  partial: 'Coincidencia parcial',
  missing: 'No encontrado',
}

export function getRequirementLabel(status: RequirementStatus) {
  return STATUS_LABELS[status]
}

export function buildScoreExplanation(result: AnalysisResponse): ScoreExplanation {
  const requirements = result.requirements ?? []
  const factors: ScoreFactor[] = []

  const matched = requirements.filter((requirement) => requirement.status === 'match')
  const partial = requirements.filter((requirement) => requirement.status === 'partial')
  const missing = requirements.filter((requirement) => requirement.status === 'missing')

  if (matched.length > 0) {
    factors.push({
      type: 'positive',
      text: `Tenés experiencia con ${listNames(matched)}.`,
    })
  }
  if (requirements.length > 0) {
    factors.push({
      type: 'positive',
      text: `Cumplís ${matched.length} de ${requirements.length} requisitos principales de la oferta.`,
    })
  }
  partial.slice(0, 3).forEach((requirement) => {
    factors.push({
      type: 'partial',
      text: `${requirement.name} no aparece de forma clara en tu CV.`,
    })
  })
  missing.slice(0, 3).forEach((requirement) => {
    factors.push({
      type: 'missing',
      text: `No encontramos experiencia con ${requirement.name}.`,
    })
  })

  return {
    summary:
      `Tu compatibilidad es ${result.matchPercentage}% principalmente por los requisitos que la oferta considera ` +
      `prioritarios y cómo se reflejan en tu CV.`,
    factors,
  }
}

export function toStructuredRecommendations(items: string[]): Recommendation[] {
  return items.map((item) => ({ problem: item }))
}

export function buildCvSuggestions(result: AnalysisResponse): CvSuggestion[] {
  const suggestions: CvSuggestion[] = []
  const requirements = result.requirements ?? []

  requirements.forEach((requirement, index) => {
    if (requirement.status === 'missing') {
      suggestions.push({
        id: `missing-${index}`,
        type: 'skill',
        title: `${requirement.name} no aparece en tu CV`,
        detail: requirement.evidence
          ? `La oferta menciona ${requirement.name} y no encontramos esa tecnología en tu CV.`
          : `La oferta menciona ${requirement.name} y no encontramos evidencia de esa tecnología.`,
        action: `Si tenés experiencia real con ${requirement.name}, considerá destacarla en la sección de proyectos o experiencia.`,
      })
    } else if (requirement.status === 'partial') {
      suggestions.push({
        id: `partial-${index}`,
        type: 'wording',
        title: `Experiencia con ${requirement.name} poco visible`,
        detail: `Aparece ${requirement.name}, pero no se entiende con claridad qué nivel de dominio tenés.`,
        action: `Reformulá la mención de ${requirement.name} para que se vea tu experiencia concreta (proyecto, tiempo, resultado).`,
      })
    }
  })

  return suggestions
}

export function computeHistoryStats(records: AnalysisSummary[]): HistoryStats {
  if (records.length === 0) {
    return { total: 0, averageScore: 0, bestScore: 0 }
  }

  const scores = records.map((record) => record.score)
  const averageScore = Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
  const bestScore = Math.max(...scores)

  const sorted = [...records].sort((a, b) => a.createdAt - b.createdAt)
  const firstHalf = sorted.slice(0, Math.floor(sorted.length / 2))
  const secondHalf = sorted.slice(Math.floor(sorted.length / 2))
  const firstAverage = firstHalf.length > 0
    ? firstHalf.reduce((sum, record) => sum + record.score, 0) / firstHalf.length
    : 0
  const secondAverage = secondHalf.length > 0
    ? secondHalf.reduce((sum, record) => sum + record.score, 0) / secondHalf.length
    : 0
  const trendDelta = firstAverage > 0 ? Math.round(secondAverage - firstAverage) : undefined

  return { total: records.length, averageScore, bestScore, trendDelta }
}

export function filterByScoreRange(records: AnalysisSummary[], range: ScoreRange) {
  if (range === 'all') return records
  if (range === 'top') return records.filter((record) => record.score >= 80)
  if (range === 'mid') return records.filter((record) => record.score >= 60 && record.score < 80)
  return records.filter((record) => record.score < 60)
}

export function buildComparison(previous: HistoryRecord, current: HistoryRecord): AnalysisComparison {
  const previousRequirements = previous.result.requirements ?? []
  const currentRequirements = current.result.requirements ?? []

  const previousMatched = new Set(
    previousRequirements.filter((requirement) => requirement.status !== 'missing').map((requirement) => requirement.name.toLowerCase()),
  )
  const newMatches = currentRequirements
    .filter((requirement) => requirement.status !== 'missing' && !previousMatched.has(requirement.name.toLowerCase()))
    .map((requirement) => requirement.name)
  const stillMissing = currentRequirements
    .filter((requirement) => requirement.status === 'missing')
    .map((requirement) => requirement.name)

  const difference = current.score - previous.score
  const notes: string[] = []
  if (newMatches.length > 0) {
    notes.push(`Agregaste ${newMatches.length === 1 ? 'un requisito' : `${newMatches.length} requisitos`} nuevo${newMatches.length === 1 ? '' : 's'}: ${listNames(currentRequirements.filter((requirement) => newMatches.some((name) => name === requirement.name)))}.`)
  }
  if (stillMissing.length > 0 && difference <= 0) {
    notes.push('Todavía faltan requisitos clave para la posición.')
  }

  return {
    role: current.role,
    company: current.company,
    previousCvVersion: previous.cvVersion,
    currentCvVersion: current.cvVersion,
    previousScore: previous.score,
    currentScore: current.score,
    difference,
    newMatches,
    stillMissing,
    notes,
  }
}

function listNames(requirements: Array<Pick<RequirementMatch, 'name'>>) {
  const names = requirements.map((requirement) => requirement.name)
  if (names.length === 0) return ''
  if (names.length === 1) return names[0]
  return `${names.slice(0, -1).join(', ')} y ${names[names.length - 1]}`
}
