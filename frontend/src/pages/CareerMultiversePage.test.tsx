import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { CareerMultiversePage } from './CareerMultiversePage'
import type { CareerMultiverseRequest, CareerMultiverseResponse, CareerPath } from '../lib/types/types'

describe('CareerMultiversePage', () => {
  it('renders a direct-access empty state without a request', () => {
    const markup = render({ request: null, response: null })

    expect(markup).toContain('Primero necesitamos conocer tu perfil.')
    expect(markup).toContain('Analizar mi CV')
    expect(markup).not.toContain('/api/career/multiverse')
  })

  it('renders a Career-specific loading state', () => {
    const markup = render({ isLoading: true })

    expect(markup).toContain('Explorando caminos')
    expect(markup).toContain('Interpretando tu perfil')
    expect(markup).toContain('Contrastando senales del mercado')
    expect(markup).not.toContain('%')
  })

  it('renders retryable errors with a manual retry button', () => {
    const markup = render({
      error: {
        title: 'No pudimos explorar tus caminos',
        message: 'No pudimos explorar tus caminos profesionales en este momento.',
        retryable: true,
      },
    })

    expect(markup).toContain('No pudimos explorar tus caminos')
    expect(markup).toContain('Intentar nuevamente')
    expect(markup).toContain('Volver al analisis')
  })

  it('renders three paths ordered NATURAL, EXPANSION, ALTERNATIVE', () => {
    const markup = renderSuccess()

    const natural = markup.indexOf('Java Backend Developer')
    const expansion = markup.indexOf('Cloud Backend Developer')
    const alternative = markup.indexOf('QA Automation Engineer')

    expect(natural).toBeGreaterThan(-1)
    expect(expansion).toBeGreaterThan(natural)
    expect(alternative).toBeGreaterThan(expansion)
  })

  it('renders coverage, confidence translation, sample and hiring disclaimer', () => {
    const markup = renderSuccess()

    expect(markup).toContain('Cobertura actual de habilidades')
    expect(markup).toContain('71%')
    expect(markup).toContain('22 ofertas relacionadas analizadas')
    expect(markup).toContain('Alta')
    expect(markup).toContain('No representa probabilidad de contratacion.')
  })

  it('renders accessible selectable path cards with selected state', () => {
    const markup = renderSuccess()

    expect(markup).toContain('role="tablist"')
    expect(markup).toContain('role="tab"')
    expect(markup).toContain('aria-selected="true"')
    expect(markup).toContain('aria-current="true"')
    expect(markup).toContain('Explorar camino')
  })

  it('renders priorities NOW, NEXT, and LATER without hiding frequency percentages', () => {
    const markup = renderSuccess()

    expect(markup).toContain('Aprende ahora')
    expect(markup).toContain('Proximo')
    expect(markup).toContain('Puede esperar')
    expect(markup).toContain('Detectado en 64% de las ofertas relacionadas de esta muestra.')
    expect(markup).toContain('Podrias priorizar primero las anteriores.')
  })

  it('renders roadmap and project challenge when present', () => {
    const markup = renderSuccess()

    expect(markup.indexOf('Que aprender primero')).toBeLessThan(markup.indexOf('Que pasaria si incorporaras estas habilidades?'))
    expect(markup.indexOf('Que pasaria si incorporaras estas habilidades?')).toBeLessThan(markup.indexOf('Tu proximo movimiento'))
    expect(markup).toContain('Tu proximo movimiento')
    expect(markup).toContain('01')
    expect(markup).toContain('Aprende fundamentos de Docker')
    expect(markup).toContain('Reto de portfolio')
    expect(markup).toContain('Converti aprendizaje en evidencia concreta para tu proximo CV.')
  })

  it('handles INSUFFICIENT confidence without hiding the path', () => {
    const response = responseFixture({
      paths: [
        pathFixture('NATURAL', 'Java Backend Developer', { confidence: 'INSUFFICIENT', sampleSize: 0, learningPriorities: [], projectChallenge: null }),
        pathFixture('EXPANSION', 'Cloud Backend Developer'),
        pathFixture('ALTERNATIVE', 'QA Automation Engineer'),
      ],
    })
    const markup = render({ response })

    expect(markup).toContain('Java Backend Developer')
    expect(markup).toContain('Insuficiente')
    expect(markup).toContain('No encontramos suficientes ofertas relacionadas para extraer una tendencia confiable.')
  })

  it('does not render a project challenge when backend returns null', () => {
    const response = responseFixture({
      paths: [
        pathFixture('NATURAL', 'Java Backend Developer', { projectChallenge: null }),
        pathFixture('EXPANSION', 'Cloud Backend Developer'),
        pathFixture('ALTERNATIVE', 'QA Automation Engineer'),
      ],
    })
    const markup = render({ response })

    expect(markup).not.toContain('Reto de portfolio')
  })

  it('renders methodology copy and no salaries when backend does not return salaries', () => {
    const markup = renderSuccess()

    expect(markup).toContain('De donde salen estas recomendaciones')
    expect(markup).toContain('Las frecuencias representan unicamente las ofertas analizadas')
    expect(markup).toContain('Calcula como cambiaria la cobertura')
    expect(markup).toContain('Ofertas de la muestra provistas por JOBICY.')
    expect(markup.toLowerCase()).not.toContain('salario')
    expect(markup.toLowerCase()).not.toContain('sueldo')
  })
})

function renderSuccess() {
  return render({ response: responseFixture() })
}

function render(overrides: Partial<Parameters<typeof CareerMultiversePage>[0]> = {}) {
  const request = overrides.request === undefined ? requestFixture() : overrides.request
  const response = overrides.response === undefined ? null : overrides.response
  return renderToStaticMarkup(
    <CareerMultiversePage
      theme="light"
      onToggleTheme={vi.fn()}
      onNavigate={vi.fn()}
      request={request}
      response={response}
      error={overrides.error ?? null}
      isLoading={overrides.isLoading ?? false}
      selectedPathType={overrides.selectedPathType ?? 'NATURAL'}
      onSelectPath={vi.fn()}
      onRetry={vi.fn()}
      hasAnalysisResult
    />,
  )
}

function requestFixture(): CareerMultiverseRequest {
  return {
    role: 'Java Backend Developer',
    seniority: 'JUNIOR',
    skills: ['Java', 'Spring Boot', 'SQL'],
    region: 'LATAM',
  }
}

function responseFixture(overrides: Partial<CareerMultiverseResponse> = {}): CareerMultiverseResponse {
  return {
    provider: 'JOBICY',
    region: 'LATAM',
    profile: {
      role: 'Java Backend Developer',
      seniority: 'JUNIOR',
      skills: ['Java', 'Spring Boot', 'SQL'],
    },
    paths: [
      pathFixture('ALTERNATIVE', 'QA Automation Engineer'),
      pathFixture('NATURAL', 'Java Backend Developer'),
      pathFixture('EXPANSION', 'Cloud Backend Developer'),
    ],
    ...overrides,
  }
}

function pathFixture(
  type: CareerPath['type'],
  role: string,
  overrides: Partial<CareerPath> & { sampleSize?: number; confidence?: CareerPath['market']['confidence'] } = {},
): CareerPath {
  return {
    type,
    role,
    summary: `${role} summary`,
    rationale: `${role} rationale`,
    market: {
      sampleSize: overrides.sampleSize ?? 22,
      confidence: overrides.confidence ?? 'HIGH',
      coveragePercentage: 71,
      currentSkillsDetected: ['Java', 'SQL'],
      missingSkills: [
        { skill: 'Docker', jobsMentioning: 10, frequencyPercentage: 45 },
      ],
      skillDemand: [
        { skill: 'Java', jobsMentioning: 18, frequencyPercentage: 82 },
        { skill: 'SQL', jobsMentioning: 7, frequencyPercentage: 32 },
        { skill: 'Docker', jobsMentioning: 10, frequencyPercentage: 45 },
      ],
    },
    learningPriorities: [
      { skill: 'Docker', jobsMentioning: 14, frequencyPercentage: 64, priority: 'NOW' },
      { skill: 'AWS', jobsMentioning: 9, frequencyPercentage: 41, priority: 'NEXT' },
      { skill: 'Kubernetes', jobsMentioning: 4, frequencyPercentage: 18, priority: 'LATER' },
    ],
    roadmap: [
      { step: 1, title: 'Aprende fundamentos de Docker', description: 'Practica Docker con ejercicios.' },
    ],
    projectChallenge: {
      title: 'Prepara un proyecto de portfolio',
      description: 'Crea una aplicacion relacionada con el rol.',
      skills: ['Docker'],
    },
    ...overrides,
  }
}
