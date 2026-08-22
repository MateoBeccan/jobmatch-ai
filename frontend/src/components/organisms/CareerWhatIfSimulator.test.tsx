import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { CareerWhatIfSimulator } from './CareerWhatIfSimulator'
import type { CareerLearningPriorityItem, CareerPathMarket } from '../../lib/types/types'

describe('CareerWhatIfSimulator', () => {
  it('renders missing market-backed skills as real checkboxes', () => {
    const markup = render()

    expect(markup).toContain('type="checkbox"')
    expect(markup).toContain('Docker')
    expect(markup).toContain('36% de la muestra')
    expect(markup).toContain('Aprende ahora')
  })

  it('does not render current skills as selectable options', () => {
    const markup = render()
    const optionsStart = markup.indexOf('career-sim-skill-grid')
    const comparisonStart = markup.indexOf('career-sim-comparison')
    const optionMarkup = markup.slice(optionsStart, comparisonStart)

    expect(optionMarkup).not.toContain('Java')
    expect(optionMarkup).not.toContain('Spring Boot')
    expect(optionMarkup).toContain('Docker')
  })

  it('renders baseline coverage and invitational state without selection', () => {
    const markup = render()

    expect(markup).toContain('Actual')
    expect(markup).toContain('71%')
    expect(markup).toContain('Simulado')
    expect(markup).toContain('Selecciona una habilidad para comenzar.')
    expect(markup).not.toContain('+0 puntos')
  })

  it('uses progressbars with accessible values', () => {
    const markup = render()

    expect(markup).toContain('role="progressbar"')
    expect(markup).toContain('aria-valuemin="0"')
    expect(markup).toContain('aria-valuemax="100"')
    expect(markup).toContain('aria-valuenow="71"')
  })

  it('does not show ambiguous zero percent delta without selection', () => {
    const markup = render()

    expect(markup).not.toContain('+0 puntos')
    expect(markup).not.toContain('probabilidad aumentaria')
  })

  it('shows visible disclaimer and sample context', () => {
    const markup = render()

    expect(markup).toContain('No representa una probabilidad de contratacion.')
    expect(markup).toContain('Simulacion basada en 22 ofertas relacionadas.')
    expect(markup).toContain('Fuente de la muestra: JOBICY.')
  })

  it('shows LOW confidence warning but keeps the simulator visible', () => {
    const markup = render({ market: marketFixture({ confidence: 'LOW', sampleSize: 2, coveragePercentage: 71 }) })

    expect(markup).toContain('Muestra pequena')
    expect(markup).toContain('type="checkbox"')
  })

  it('hides simulator options for INSUFFICIENT confidence', () => {
    const markup = render({ market: marketFixture({ confidence: 'INSUFFICIENT', sampleSize: 0, coveragePercentage: 0 }) })

    expect(markup).toContain('Necesitamos una muestra mas amplia')
    expect(markup).not.toContain('type="checkbox"')
  })

  it('shows complete coverage state when coverage is 100', () => {
    const markup = render({
      market: marketFixture({
        coveragePercentage: 100,
        currentSkillsDetected: ['Java', 'Spring Boot', 'Docker', 'Testing', 'AWS', 'Kubernetes'],
      }),
    })

    expect(markup).toContain('Cobertura completa dentro de las habilidades observadas en esta muestra.')
    expect(markup).not.toContain('type="checkbox"')
  })

  it('shows no missing skills state when all demanded skills are covered', () => {
    const markup = render({
      market: marketFixture({
        coveragePercentage: 0,
        currentSkillsDetected: [],
        skillDemand: [],
      }),
    })

    expect(markup).toContain('Tu perfil ya cubre las principales habilidades detectadas en esta muestra.')
    expect(markup).not.toContain('type="checkbox"')
  })

  it('shows learning priority badges only when provided by backend', () => {
    const markup = render({
      priorities: [
        { skill: 'Docker', jobsMentioning: 14, frequencyPercentage: 64, priority: 'NOW' },
      ],
    })

    expect(markup).toContain('Aprende ahora')
    expect(markup).not.toContain('Puede esperar')
  })

  it('limits visible options to six skills', () => {
    const markup = render({ market: marketWithEightSimulableSkills() })

    expect((markup.match(/type="checkbox"/g) ?? []).length).toBe(6)
    expect(markup).toContain('Docker')
    expect(markup).not.toContain('GraphQL')
  })

  it('shows a limit note when eight simulable skills are reduced to six visible options', () => {
    const markup = render({ market: marketWithEightSimulableSkills() })

    expect((markup.match(/type="checkbox"/g) ?? []).length).toBe(6)
    expect(markup).toContain('Mostramos las 6 habilidades con mayor presencia en la muestra.')
    expect(markup).toContain('La cobertura tambien considera otras habilidades detectadas.')
  })

  it('does not show a limit note when exactly six skills are simulable', () => {
    const markup = render({ market: marketWithSimulableSkillCount(6) })

    expect((markup.match(/type="checkbox"/g) ?? []).length).toBe(6)
    expect(markup).not.toContain('Mostramos las 6 habilidades')
  })

  it('does not show a limit note when four skills are simulable', () => {
    const markup = render({ market: marketWithSimulableSkillCount(4) })

    expect((markup.match(/type="checkbox"/g) ?? []).length).toBe(4)
    expect(markup).not.toContain('Mostramos las 6 habilidades')
  })

  it('keeps baseline simulated coverage unchanged when the limit note is visible', () => {
    const markup = render({ market: marketWithEightSimulableSkills() })

    expect(markup).toContain('Actual')
    expect(markup).toContain('21%')
    expect(markup).toContain('Simulado')
    expect(markup).toContain('Selecciona una habilidad para comenzar.')
    expect(markup).not.toContain('+0 puntos')
  })

  it('orders simulable skills by jobsMentioning desc then skill asc', () => {
    const markup = render({
      market: marketFixture({
        coveragePercentage: 30,
        currentSkillsDetected: ['Java'],
        skillDemand: [
          { skill: 'Java', jobsMentioning: 16, frequencyPercentage: 73 },
          { skill: 'AWS', jobsMentioning: 9, frequencyPercentage: 41 },
          { skill: 'Docker', jobsMentioning: 14, frequencyPercentage: 64 },
          { skill: 'Testing', jobsMentioning: 14, frequencyPercentage: 64 },
        ],
      }),
    })

    expect(markup.indexOf('Docker')).toBeLessThan(markup.indexOf('Testing'))
    expect(markup.indexOf('Testing')).toBeLessThan(markup.indexOf('AWS'))
  })

  it('does not render misleading simulator if public data cannot reproduce backend coverage', () => {
    const markup = render({ market: marketFixture({ coveragePercentage: 12 }) })

    expect(markup).toContain('no permiten reproducir coverage de forma fiable')
    expect(markup).not.toContain('type="checkbox"')
  })

  it('does not call fetch while rendering the simulator', () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    render()

    expect(fetchMock).not.toHaveBeenCalled()
    vi.unstubAllGlobals()
  })

  it('does not touch localStorage while rendering hidden-skill guidance', () => {
    const storageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    }
    vi.stubGlobal('localStorage', storageMock)

    render({ market: marketWithEightSimulableSkills() })

    expect(storageMock.getItem).not.toHaveBeenCalled()
    expect(storageMock.setItem).not.toHaveBeenCalled()
    expect(storageMock.removeItem).not.toHaveBeenCalled()
    expect(storageMock.clear).not.toHaveBeenCalled()
    vi.unstubAllGlobals()
  })
})

function render(overrides: { market?: CareerPathMarket; priorities?: CareerLearningPriorityItem[]; provider?: string } = {}) {
  return renderToStaticMarkup(
    <CareerWhatIfSimulator
      market={overrides.market ?? marketFixture()}
      learningPriorities={overrides.priorities ?? prioritiesFixture()}
      provider={overrides.provider ?? 'JOBICY'}
    />,
  )
}

function marketFixture(overrides: Partial<CareerPathMarket> = {}): CareerPathMarket {
  const market: CareerPathMarket = {
    sampleSize: 22,
    confidence: 'HIGH',
    coveragePercentage: 71,
    currentSkillsDetected: ['Java', 'Spring Boot'],
    missingSkills: [
      { skill: 'Docker', jobsMentioning: 14, frequencyPercentage: 64 },
    ],
    skillDemand: [
      { skill: 'Java', jobsMentioning: 16, frequencyPercentage: 73 },
      { skill: 'Spring Boot', jobsMentioning: 14, frequencyPercentage: 64 },
      { skill: 'Docker', jobsMentioning: 8, frequencyPercentage: 36 },
      { skill: 'Testing', jobsMentioning: 4, frequencyPercentage: 18 },
    ],
  }
  return { ...market, ...overrides }
}

function marketWithEightSimulableSkills(): CareerPathMarket {
  return marketFixture({
    coveragePercentage: 21,
    currentSkillsDetected: ['Java'],
    skillDemand: [
      { skill: 'Java', jobsMentioning: 16, frequencyPercentage: 73 },
      { skill: 'Docker', jobsMentioning: 14, frequencyPercentage: 64 },
      { skill: 'Testing', jobsMentioning: 12, frequencyPercentage: 55 },
      { skill: 'AWS', jobsMentioning: 10, frequencyPercentage: 45 },
      { skill: 'Kubernetes', jobsMentioning: 8, frequencyPercentage: 36 },
      { skill: 'Redis', jobsMentioning: 7, frequencyPercentage: 32 },
      { skill: 'Kafka', jobsMentioning: 6, frequencyPercentage: 27 },
      { skill: 'GraphQL', jobsMentioning: 5, frequencyPercentage: 23 },
      { skill: 'Linux', jobsMentioning: 4, frequencyPercentage: 18 },
    ],
  })
}

function marketWithSimulableSkillCount(count: number): CareerPathMarket {
  return marketFixture({
    coveragePercentage: count === 4 ? 24 : 19,
    currentSkillsDetected: ['Java'],
    skillDemand: [
      { skill: 'Java', jobsMentioning: 12, frequencyPercentage: 75 },
      ...[
        { skill: 'Docker', jobsMentioning: 11, frequencyPercentage: 69 },
        { skill: 'Testing', jobsMentioning: 10, frequencyPercentage: 63 },
        { skill: 'AWS', jobsMentioning: 9, frequencyPercentage: 56 },
        { skill: 'Kubernetes', jobsMentioning: 8, frequencyPercentage: 50 },
        { skill: 'Redis', jobsMentioning: 7, frequencyPercentage: 44 },
        { skill: 'Kafka', jobsMentioning: 6, frequencyPercentage: 38 },
      ].slice(0, count),
    ],
  })
}

function prioritiesFixture(): CareerLearningPriorityItem[] {
  return [
    { skill: 'Docker', jobsMentioning: 14, frequencyPercentage: 64, priority: 'NOW' },
    { skill: 'Testing', jobsMentioning: 12, frequencyPercentage: 55, priority: 'NOW' },
    { skill: 'AWS', jobsMentioning: 10, frequencyPercentage: 45, priority: 'NEXT' },
    { skill: 'Kubernetes', jobsMentioning: 8, frequencyPercentage: 36, priority: 'LATER' },
  ]
}
