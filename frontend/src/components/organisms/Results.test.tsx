import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { Results } from './Results'
import type { AnalysisResponse } from '../../lib/types/types'

describe('Results', () => {
  it('renders JobSearchPanel after interview questions when jobSearchProfile exists', () => {
    const markup = renderToStaticMarkup(
      <Results result={analysisResponse()} onReset={vi.fn()} onNavigate={vi.fn()} />,
    )

    const questionsIndex = markup.indexOf('Preguntas de entrevista')
    const jobSearchIndex = markup.indexOf('Ofertas remotas relacionadas con tu perfil')
    const actionsIndex = markup.indexOf('Volver a analizar')

    expect(questionsIndex).toBeGreaterThan(-1)
    expect(jobSearchIndex).toBeGreaterThan(questionsIndex)
    expect(actionsIndex).toBeGreaterThan(jobSearchIndex)
  })

  it('renders the main analysis sections without changing response contracts', () => {
    const markup = renderToStaticMarkup(
      <Results result={analysisResponse()} onReset={vi.fn()} onNavigate={vi.fn()} />,
    )

    expect(markup).toContain('82')
    expect(markup).toContain('Un encaje muy prometedor')
    expect(markup).toContain('Compatibilidad estimada')
    expect(markup).toContain('Requisitos')
    expect(markup).toContain('Java')
    expect(markup).toContain('Docker')
    expect(markup).toContain('¿Qué deberías mejorar?')
    expect(markup).toContain('Practicar Docker')
    expect(markup).toContain('Preguntas de entrevista')
    expect(markup).toContain('Ofertas remotas relacionadas con tu perfil')
  })

  it('keeps legacy analysis responses without jobSearchProfile working', () => {
    const response = analysisResponse()
    delete response.jobSearchProfile

    const markup = renderToStaticMarkup(
      <Results result={response} onReset={vi.fn()} onNavigate={vi.fn()} />,
    )

    expect(markup).toContain('Compatibilidad estimada')
    expect(markup).not.toContain('Ofertas remotas relacionadas con tu perfil')
  })
})

function analysisResponse(): AnalysisResponse {
  return {
    matchPercentage: 82,
    matchingSkills: ['Java'],
    missingSkills: ['Docker'],
    recommendations: ['Practicar Docker'],
    interviewQuestions: ['Como diseñarias una API REST?'],
    requirements: [
      { name: 'Java', status: 'match' },
      { name: 'Docker', status: 'missing' },
    ],
    jobSearchProfile: {
      role: 'Java Backend Developer',
      seniority: 'JUNIOR',
      keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
    },
  }
}
