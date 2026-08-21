import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { Results } from './Results'
import type { AnalysisResponse } from '../../lib/types/types'

describe('Results', () => {
  it('renders a new result without warnings or gaps and hides empty explanatory sections', () => {
    const markup = render(analysisResponse())

    expect(markup).toContain('82')
    expect(markup).toContain('Un encaje muy prometedor')
    expect(markup).toContain('Compatibilidad estimada')
    expect(markup).toContain('Requisitos')
    expect(markup).toContain('Java')
    expect(markup).toContain('Docker')
    expect(markup).not.toContain('Importante')
    expect(markup).not.toContain('Requisitos críticos faltantes')
    expect(markup).not.toContain('Brecha de experiencia')
  })

  it('keeps critical warnings and gaps visible above the longer lists', () => {
    const markup = render(criticalExperienceResponse())

    expect(markup).toContain('Importante')
    expect(markup).toContain('Falta 1 requisito crítico de la oferta.')
    expect(markup).toContain('Requisitos críticos faltantes')
    expect(markup).toContain('5+ años de experiencia profesional en Java')
    expect(markup).toContain('Brecha de experiencia')
    expect(markup).toContain('Requisito crítico')

    expect(markup.indexOf('Importante')).toBeLessThan(markup.indexOf('Requisitos críticos faltantes'))
    expect(markup.indexOf('Requisitos críticos faltantes')).toBeLessThan(markup.indexOf('Brecha de experiencia'))
    expect(markup.indexOf('Brecha de experiencia')).toBeLessThan(markup.indexOf('Resumen interpretado'))
    expect(markup.indexOf('Resumen interpretado')).toBeLessThan(markup.indexOf('Skills y requisitos'))
  })

  it('does not duplicate a critical requirement in the interpreted summary', () => {
    const markup = render(criticalExperienceResponse())
    const summaryStart = markup.indexOf('Resumen interpretado')
    const requirementsStart = markup.indexOf('Skills y requisitos')
    const summaryMarkup = markup.slice(summaryStart, requirementsStart)

    expect(summaryMarkup).not.toContain('5+ años de experiencia profesional en Java')
    expect(summaryMarkup).not.toContain('No se encontró evidencia suficiente en el CV.')
    expect(summaryMarkup).toContain('Cumplís')
  })

  it('keeps shared critical and experience evidence compact', () => {
    const markup = render(criticalExperienceResponse())

    expect(markup).toContain('Ver detalle específico en la brecha de experiencia.')
    expect(markup).toContain('Tu CV muestra proyectos técnicos, pero no evidencia la experiencia profesional mínima requerida.')
    expect(markup).not.toContain('No se encontró evidencia suficiente en el CV.')
  })

  it('shows at most three warnings initially and exposes a real expansion button', () => {
    const markup = render(analysisResponse({
      warnings: [
        'Falta 1 requisito crítico de la oferta.',
        'Un requisito crítico se cumple parcialmente.',
        'El score está limitado por requisitos críticos no cumplidos.',
        'La experiencia profesional requerida no está completamente respaldada por el CV.',
      ],
    }))

    expect(markup).toContain('Falta 1 requisito crítico de la oferta.')
    expect(markup).toContain('Un requisito crítico se cumple parcialmente.')
    expect(markup).toContain('El score está limitado por requisitos críticos no cumplidos.')
    expect(markup).not.toContain('La experiencia profesional requerida no está completamente respaldada por el CV.')
    expect(markup).toContain('aria-expanded="false"')
    expect(markup).toContain('Ver más avisos (1)')
  })

  it('shows five recommendations as three priority items initially with a show more control', () => {
    const markup = render(analysisResponse({
      recommendations: ['Prioridad 1', 'Prioridad 2', 'Prioridad 3', 'Prioridad 4', 'Prioridad 5'],
    }))

    expect(markup).toContain('Prioridad 1')
    expect(markup).toContain('Prioridad 2')
    expect(markup).toContain('Prioridad 3')
    expect(markup).not.toContain('Prioridad 4')
    expect(markup).not.toContain('Prioridad 5')
    expect(markup).toContain('Ver más recomendaciones (2)')
  })

  it('shows five CV actions as three communication actions initially with a show more control', () => {
    const markup = render(analysisResponse({
      missingSkills: ['Docker', 'AWS', 'Kubernetes', 'Redis', 'Kafka'],
      requirements: [
        { name: 'Java', status: 'match' },
        { name: 'Docker', status: 'missing' },
        { name: 'AWS', status: 'missing' },
        { name: 'Kubernetes', status: 'missing' },
        { name: 'Redis', status: 'missing' },
        { name: 'Kafka', status: 'missing' },
      ],
    }))

    expect(markup).toContain('Hacer visible experiencia real con Docker')
    expect(markup).toContain('Hacer visible experiencia real con AWS')
    expect(markup).toContain('Hacer visible experiencia real con Kubernetes')
    expect(markup).not.toContain('Hacer visible experiencia real con Redis')
    expect(markup).not.toContain('Hacer visible experiencia real con Kafka')
    expect(markup).toContain('Ver más acciones (2)')
  })

  it('does not recalculate the backend score when warnings mention a cap', () => {
    const markup = render(analysisResponse({
      matchPercentage: 69,
      breakdown: {
        mandatoryTechnical: 95,
        experienceSeniority: 90,
        desirable: 100,
        complementary: 100,
      },
      warnings: ['El score está limitado por requisitos críticos no cumplidos.'],
    }))

    expect(markup).toContain('69')
    expect(markup).toContain('95%')
    expect(markup).toContain('El porcentaje final está limitado por requisitos críticos.')
    expect(markup).not.toContain('85%')
  })

  it('renders JobSearchPanel after interview questions when jobSearchProfile exists', () => {
    const markup = render(analysisResponse())

    const questionsIndex = markup.indexOf('Preguntas de entrevista')
    const jobSearchIndex = markup.indexOf('Ofertas remotas relacionadas con tu perfil')
    const actionsIndex = markup.indexOf('Volver a analizar')

    expect(questionsIndex).toBeGreaterThan(-1)
    expect(jobSearchIndex).toBeGreaterThan(questionsIndex)
    expect(actionsIndex).toBeGreaterThan(jobSearchIndex)
  })

  it('keeps historical analysis responses without the new fields working', () => {
    const response: AnalysisResponse = {
      matchPercentage: 82,
      matchingSkills: ['Java'],
      missingSkills: ['Docker'],
      recommendations: ['Practicar Docker'],
      interviewQuestions: ['Como diseñarias una API REST?'],
      requirements: [
        { name: 'Java', status: 'match' },
        { name: 'Docker', status: 'missing' },
      ],
    }

    const markup = render(response)

    expect(markup).toContain('Compatibilidad estimada')
    expect(markup).toContain('Java')
    expect(markup).not.toContain('Importante')
    expect(markup).not.toContain('Requisitos críticos faltantes')
  })

  it('keeps legacy analysis responses without jobSearchProfile working', () => {
    const response = analysisResponse()
    delete response.jobSearchProfile

    const markup = render(response)

    expect(markup).toContain('Compatibilidad estimada')
    expect(markup).not.toContain('Ofertas remotas relacionadas con tu perfil')
  })
})

function render(result: AnalysisResponse) {
  return renderToStaticMarkup(
    <Results result={result} onReset={vi.fn()} onNavigate={vi.fn()} />,
  )
}

function criticalExperienceResponse(): AnalysisResponse {
  return analysisResponse({
    matchPercentage: 69,
    warnings: ['Falta 1 requisito crítico de la oferta.', 'El score está limitado por requisitos críticos no cumplidos.'],
    criticalMissingRequirements: [
      {
        requirement: '5+ años de experiencia profesional en Java',
        category: 'experience_seniority',
        evidence: 'No se encontró evidencia suficiente en el CV.',
      },
    ],
    experienceGap: {
      requirement: '5+ años de experiencia profesional en Java',
      status: 'missing',
      critical: true,
      summary: 'Tu CV muestra proyectos técnicos, pero no evidencia la experiencia profesional mínima requerida.',
    },
    requirements: [
      { name: 'Java', status: 'match' },
      { name: 'Spring Boot', status: 'match' },
      { name: 'Docker', status: 'missing' },
      { name: '5+ años de experiencia profesional en Java', status: 'missing' },
    ],
  })
}

function analysisResponse(overrides: Partial<AnalysisResponse> = {}): AnalysisResponse {
  return {
    matchPercentage: 82,
    matchingSkills: ['Java'],
    missingSkills: ['Docker'],
    criticalMissingRequirements: [],
    experienceGap: null,
    warnings: [],
    recommendations: ['Practicar Docker'],
    interviewQuestions: ['Como diseñarias una API REST?'],
    requirements: [
      { name: 'Java', status: 'match' },
      { name: 'Docker', status: 'missing' },
    ],
    breakdown: {
      mandatoryTechnical: 82,
      experienceSeniority: 80,
    },
    jobSearchProfile: {
      role: 'Java Backend Developer',
      seniority: 'JUNIOR',
      keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
    },
    ...overrides,
  }
}
