import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  DEMO_SCENARIO_DURATION_MS,
  DEMO_SCENARIOS,
  HOME_STEP_ENTER_THRESHOLD,
  HOME_STEP_EXIT_THRESHOLD,
  HomePage,
  getNextDemoScenarioIndex,
  resolveHomeStepVisibility,
  scheduleDemoScenarioRotation,
  shouldRotateDemoScenarios,
} from './HomePage'
import { BRAND_LOGO_PATH } from '../lib/constants/brand'

describe('HomePage', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('renders the product, main CTA, how it works section and logo', () => {
    const markup = renderToStaticMarkup(
      <HomePage theme="light" onToggleTheme={vi.fn()} onNavigate={vi.fn()} />,
    )

    expect(markup).toContain('JobMatch AI')
    expect(markup).toContain('ANÁLISIS DE COMPATIBILIDAD CON IA')
    expect(markup).toContain('Comenzar análisis')
    expect(markup).toContain('CÓMO FUNCIONA')
    expect(markup).toContain('De tu CV a tu próximo paso profesional')
    expect(markup).toContain('mejorar tu candidatura y explorar nuevas oportunidades')
    expect(markup).toContain('Subí tu CV')
    expect(markup).toContain('Explorá tu futuro')
    expect(markup).toContain('caminos profesionales')
    expect(markup).toContain(`src="${BRAND_LOGO_PATH}"`)
    expect(markup).toContain('alt="JobMatch AI"')
  })

  it('renders a coherent initial demo scenario in Hero and benefits', () => {
    const markup = renderToStaticMarkup(
      <HomePage theme="light" onToggleTheme={vi.fn()} onNavigate={vi.fn()} />,
    )
    const scenario = DEMO_SCENARIOS[0]

    expect(markup).toContain(scenario.role)
    scenario.matchingSkills.forEach((skill) => expect(markup).toContain(skill))
    expect(markup).toContain(scenario.missingSkill)
    expect(markup).toContain(scenario.strength)
    expect(markup).toContain(scenario.recommendation)
    expect(markup).toContain(scenario.recommendationImpact)
    expect(markup).toContain('Buen nivel de coincidencias')
    expect(markup).toContain('Natural')
    expect(markup).toContain('Expansión')
    expect(markup).toContain('Alternativo')
    expect(markup).toContain('Continuidad con tu perfil')
    expect(markup).toContain('Amplía tu especialización')
    expect(markup).toContain('Explora otra dirección')
    expect(markup).toContain('preguntas de entrevista')
    expect(markup).toContain('oportunidades relacionadas')
    expect(markup).toContain('Tres formas de evolucionar tu perfil')
    expect(markup).toContain('Tu perfil')
    expect(markup).toContain('future-route-lines')
    expect(markup).toContain('future-destination-natural')
  })

  it('renders CTA controls that target the analyzer flow', () => {
    const markup = renderToStaticMarkup(
      <HomePage theme="dark" onToggleTheme={vi.fn()} onNavigate={vi.fn()} />,
    )

    expect(markup).toContain('Comenzar análisis')
    expect(markup).toContain('Analizar mi CV')
    expect(markup).toContain('Análisis asistido por Google Gemini')
    expect(markup).toContain('¿Listo para conocer tu compatibilidad y explorar tu próximo paso?')
  })

  it('keeps home step animations stable between enter and exit thresholds', () => {
    expect(resolveHomeStepVisibility(false, HOME_STEP_ENTER_THRESHOLD)).toBe(true)
    expect(resolveHomeStepVisibility(true, HOME_STEP_ENTER_THRESHOLD - 0.01)).toBe(true)
    expect(resolveHomeStepVisibility(true, HOME_STEP_EXIT_THRESHOLD + 0.01)).toBe(true)
    expect(resolveHomeStepVisibility(true, HOME_STEP_EXIT_THRESHOLD)).toBe(false)
    expect(resolveHomeStepVisibility(false, HOME_STEP_ENTER_THRESHOLD)).toBe(true)
  })

  it('advances demo scenarios sequentially after the shared duration', () => {
    vi.useFakeTimers()
    vi.stubGlobal('window', { setInterval, clearInterval })
    let scenarioIndex = 0

    const cleanup = scheduleDemoScenarioRotation(
      () => {
        scenarioIndex = getNextDemoScenarioIndex(scenarioIndex)
      },
      { duration: DEMO_SCENARIO_DURATION_MS },
    )

    vi.advanceTimersByTime(DEMO_SCENARIO_DURATION_MS)
    expect(DEMO_SCENARIOS[scenarioIndex].role).toBe('Frontend Developer')

    cleanup()
  })

  it('cleans up the demo scenario timer', () => {
    vi.useFakeTimers()
    vi.stubGlobal('window', { setInterval, clearInterval })
    let scenarioIndex = 0

    const cleanup = scheduleDemoScenarioRotation(
      () => {
        scenarioIndex = getNextDemoScenarioIndex(scenarioIndex)
      },
      { duration: DEMO_SCENARIO_DURATION_MS },
    )

    cleanup()
    vi.advanceTimersByTime(DEMO_SCENARIO_DURATION_MS)

    expect(scenarioIndex).toBe(0)
  })

  it('does not rotate demo scenarios when reduced motion is enabled', () => {
    expect(shouldRotateDemoScenarios(true)).toBe(false)
  })
})
