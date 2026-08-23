import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import type { Theme } from '../lib/types/types'
import { AppFooter } from '../components/atoms/AppFooter'
import { BottomNav } from '../components/atoms/BottomNav'
import { AppHeader } from '../components/molecules/AppHeader'
import highCompatibilityIcon from '../assets/career-multiverse/paths/alta-compatibilidad-icon.png'
import alternativePathIcon from '../assets/career-multiverse/paths/alternative.png'
import expansionPathIcon from '../assets/career-multiverse/paths/expansion.png'
import naturalPathIcon from '../assets/career-multiverse/paths/natural.png'

type HomePageProps = {
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
}

const BENEFITS = [
  {
    kind: 'compatibility',
    title: 'Compatibilidad',
    text: 'Obtené una estimación clara del nivel de ajuste entre tu CV y la oferta.',
  },
  {
    kind: 'skills',
    title: 'Habilidades',
    text: 'Identificá fortalezas, coincidencias técnicas y requisitos que todavía podés desarrollar.',
  },
  {
    kind: 'recommendations',
    title: 'Recomendaciones',
    text: 'Recibí acciones concretas para mejorar tu CV y tu candidatura.',
  },
  {
    kind: 'future',
    title: 'Explorá tu futuro',
    text: 'Descubrí caminos profesionales posibles a partir de tu perfil y contrastalos con señales del mercado laboral.',
  },
]

const HOW_STEP_COUNT = 4
const MATCH_TARGET = 87
const CAREER_PATHS = {
  natural: {
    name: 'Natural',
    description: 'Continuidad con tu perfil',
    icon: naturalPathIcon,
  },
  expansion: {
    name: 'Expansión',
    description: 'Amplía tu especialización',
    icon: expansionPathIcon,
  },
  alternative: {
    name: 'Alternativo',
    description: 'Explora otra dirección',
    icon: alternativePathIcon,
  },
} as const
export const DEMO_SCENARIO_DURATION_MS = 5800
export const HOME_STEP_ENTER_THRESHOLD = 0.32
export const HOME_STEP_EXIT_THRESHOLD = 0.12

type DemoScenario = {
  role: string
  matchingSkills: [string, string, string]
  missingSkill: string
  strength: string
  recommendation: string
  recommendationImpact: string
  interviewQuestion: string
  interviewTag: string
}

export const DEMO_SCENARIOS: DemoScenario[] = [
  {
    role: 'Backend Developer',
    matchingSkills: ['Java', 'Spring Boot', 'SQL'],
    missingSkill: 'Docker',
    strength: 'Backend',
    recommendation: 'Sumá Docker a tus proyectos',
    recommendationImpact: 'Impacto alto',
    interviewQuestion: '¿Cómo diseñarías una API REST?',
    interviewTag: 'Backend',
  },
  {
    role: 'Frontend Developer',
    matchingSkills: ['React', 'JavaScript', 'HTML/CSS'],
    missingSkill: 'TypeScript',
    strength: 'Frontend',
    recommendation: 'Incorporá TypeScript en un proyecto',
    recommendationImpact: 'Impacto alto',
    interviewQuestion: '¿Cómo optimizarías una aplicación React?',
    interviewTag: 'Frontend',
  },
  {
    role: 'Data Analyst',
    matchingSkills: ['SQL', 'Excel', 'Python'],
    missingSkill: 'Power BI',
    strength: 'Análisis de datos',
    recommendation: 'Sumá un dashboard a tu portfolio',
    recommendationImpact: 'Impacto medio',
    interviewQuestion: '¿Cómo analizarías datos faltantes?',
    interviewTag: 'Data',
  },
  {
    role: 'QA Automation',
    matchingSkills: ['Java', 'APIs', 'Git'],
    missingSkill: 'Selenium',
    strength: 'Testing',
    recommendation: 'Practicá automatización con Selenium',
    recommendationImpact: 'Impacto alto',
    interviewQuestion: '¿Qué casos automatizarías primero?',
    interviewTag: 'QA',
  },
  {
    role: 'Full Stack Junior',
    matchingSkills: ['Java', 'React', 'SQL'],
    missingSkill: 'Testing',
    strength: 'Full Stack',
    recommendation: 'Agregá tests a tus proyectos',
    recommendationImpact: 'Impacto medio',
    interviewQuestion: '¿Cómo conectarías React con una API REST?',
    interviewTag: 'Full Stack',
  },
]

export function resolveHomeStepVisibility(isCurrentlyVisible: boolean, intersectionRatio: number) {
  return isCurrentlyVisible
    ? intersectionRatio > HOME_STEP_EXIT_THRESHOLD
    : intersectionRatio >= HOME_STEP_ENTER_THRESHOLD
}

export function getNextDemoScenarioIndex(currentIndex: number, scenarioCount = DEMO_SCENARIOS.length) {
  return (currentIndex + 1) % scenarioCount
}

export function shouldRotateDemoScenarios(reducedMotion: boolean) {
  return !reducedMotion
}

export function scheduleDemoScenarioRotation(
  advance: () => void,
  options: { duration?: number; documentRef?: Pick<Document, 'addEventListener' | 'removeEventListener' | 'visibilityState'> } = {},
) {
  const duration = options.duration ?? DEMO_SCENARIO_DURATION_MS
  const documentRef = options.documentRef
  let timer: ReturnType<typeof window.setInterval> | null = null

  const stop = () => {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  const start = () => {
    if (timer === null && documentRef?.visibilityState !== 'hidden') {
      timer = window.setInterval(advance, duration)
    }
  }

  const handleVisibilityChange = () => {
    if (documentRef?.visibilityState === 'hidden') {
      stop()
      return
    }
    start()
  }

  start()
  documentRef?.addEventListener('visibilitychange', handleVisibilityChange)

  return () => {
    stop()
    documentRef?.removeEventListener('visibilitychange', handleVisibilityChange)
  }
}

function shouldReduceMotion() {
  return typeof window !== 'undefined' && 'matchMedia' in window && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function BenefitVisual({ kind, scenario }: { kind: (typeof BENEFITS)[number]['kind']; scenario: DemoScenario }) {
  if (kind === 'compatibility') {
    return (
      <div className="benefit-visual benefit-visual-compatibility">
        <img className="benefit-compatibility-icon" src={highCompatibilityIcon} alt="" aria-hidden="true" />
        <span className="benefit-compatibility-copy">
          <strong>Alta</strong>
          <span>Buen nivel de coincidencias</span>
        </span>
      </div>
    )
  }

  if (kind === 'skills') {
    return (
      <div className="benefit-visual benefit-visual-skills">
        <span>
          <small>Fortaleza principal</small>
          <strong>{scenario.strength}</strong>
        </span>
        <span>
          <small>A reforzar</small>
          <strong>{scenario.missingSkill}</strong>
        </span>
      </div>
    )
  }

  if (kind === 'recommendations') {
    return (
      <div className="benefit-visual benefit-visual-recommendations">
        <span className="benefit-spark" aria-hidden="true">*</span>
        <small>Sugerencia</small>
        <strong>{scenario.recommendation}</strong>
        <span className="benefit-impact">{scenario.recommendationImpact}</span>
        <span className="benefit-arrow">↗</span>
      </div>
    )
  }

  if (kind === 'future') {
    return (
      <div className="benefit-visual benefit-visual-future">
        <span className="benefit-future-core">Perfil</span>
        <span className="benefit-future-branch natural">{CAREER_PATHS.natural.name}</span>
        <span className="benefit-future-branch expansion">{CAREER_PATHS.expansion.name}</span>
        <span className="benefit-future-branch alternative">{CAREER_PATHS.alternative.name}</span>
        <span className="benefit-future-legend">Tres formas de evolucionar tu perfil</span>
      </div>
    )
  }

  return (
    <div className="benefit-visual benefit-visual-interview">
      <span className="benefit-chat-bubble" aria-hidden="true">?</span>
      <small>Pregunta sugerida</small>
      <strong>{scenario.interviewQuestion}</strong>
      <span className="benefit-impact">{scenario.interviewTag}</span>
    </div>
  )
}

export function HomePage({ theme, onToggleTheme, onNavigate }: HomePageProps) {
  const stepRefs = useRef<Array<HTMLElement | null>>([])
  const scoreAnimationRef = useRef<number | null>(null)
  const [demoScenarioIndex, setDemoScenarioIndex] = useState(0)
  const [visibleSteps, setVisibleSteps] = useState<boolean[]>(() =>
    Array(HOW_STEP_COUNT).fill(typeof window === 'undefined' || shouldReduceMotion()),
  )
  const [matchScore, setMatchScore] = useState(() => (typeof window === 'undefined' || shouldReduceMotion() ? MATCH_TARGET : 0))
  const activeScenario = DEMO_SCENARIOS[demoScenarioIndex]

  const scrollToHowItWorks = () => {
    document.getElementById('como-funciona')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  useEffect(() => {
    if (typeof window === 'undefined' || !shouldRotateDemoScenarios(shouldReduceMotion())) {
      return
    }

    return scheduleDemoScenarioRotation(
      () => setDemoScenarioIndex((current) => getNextDemoScenarioIndex(current)),
      { documentRef: typeof document === 'undefined' ? undefined : document },
    )
  }, [])

  useEffect(() => {
    if (typeof window === 'undefined') {
      return
    }

    const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')

    const revealAllSteps = () => {
      setVisibleSteps(Array(HOW_STEP_COUNT).fill(true))
      setMatchScore(MATCH_TARGET)
    }

    if (reducedMotionQuery.matches || !('IntersectionObserver' in window)) {
      revealAllSteps()
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        setVisibleSteps((current) => {
          let next = current

          entries.forEach((entry) => {
            const index = Number((entry.target as HTMLElement).dataset.stepIndex)
            if (!Number.isInteger(index) || index < 0 || index >= current.length) {
              return
            }

            const isStepVisible = resolveHomeStepVisibility(current[index], entry.intersectionRatio)
            if (current[index] === isStepVisible) {
              return
            }

            if (next === current) {
              next = [...current]
            }
            next[index] = isStepVisible
          })

          return next
        })
      },
      { threshold: [0, HOME_STEP_EXIT_THRESHOLD, HOME_STEP_ENTER_THRESHOLD, 0.5], rootMargin: '0px 0px -10% 0px' },
    )

    stepRefs.current.forEach((step) => {
      if (step) {
        observer.observe(step)
      }
    })

    const handleReducedMotionChange = () => {
      if (reducedMotionQuery.matches) {
        revealAllSteps()
        observer.disconnect()
      }
    }

    reducedMotionQuery.addEventListener('change', handleReducedMotionChange)

    return () => {
      observer.disconnect()
      reducedMotionQuery.removeEventListener('change', handleReducedMotionChange)
    }
  }, [])

  useEffect(() => {
    if (!visibleSteps[2] || typeof window === 'undefined') {
      if (typeof window !== 'undefined' && scoreAnimationRef.current) {
        window.cancelAnimationFrame(scoreAnimationRef.current)
        scoreAnimationRef.current = null
      }
      if (!shouldReduceMotion()) {
        setMatchScore(0)
      }
      return
    }

    if (shouldReduceMotion()) {
      setMatchScore(MATCH_TARGET)
      return
    }

    if (scoreAnimationRef.current) {
      window.cancelAnimationFrame(scoreAnimationRef.current)
      scoreAnimationRef.current = null
    }

    let startTime = 0
    const duration = 1000

    const animateScore = (timestamp: number) => {
      if (!startTime) {
        startTime = timestamp
      }

      const progress = Math.min((timestamp - startTime) / duration, 1)
      const easedProgress = 1 - Math.pow(1 - progress, 3)
      setMatchScore(Math.round(easedProgress * MATCH_TARGET))

      if (progress < 1) {
        scoreAnimationRef.current = window.requestAnimationFrame(animateScore)
      } else {
        scoreAnimationRef.current = null
      }
    }

    setMatchScore(0)
    scoreAnimationRef.current = window.requestAnimationFrame(animateScore)

    return () => {
      if (scoreAnimationRef.current) {
        window.cancelAnimationFrame(scoreAnimationRef.current)
        scoreAnimationRef.current = null
      }
    }
  }, [visibleSteps[2]])

  const visibleStepCount = visibleSteps[3] ? 4 : visibleSteps[2] ? 3 : visibleSteps[1] ? 2 : visibleSteps[0] ? 1 : 0
  const matchRingStyle = {
    '--match-progress': `${matchScore}%`,
    '--match-accent-progress': `${Math.min(Math.round(matchScore * 0.2), 18)}%`,
  } as CSSProperties

  return (
    <main className="page-shell home-shell">
      <AppHeader active="home" theme={theme} onToggleTheme={onToggleTheme} onNavigate={onNavigate} />

      <section className="home-hero" aria-labelledby="home-title">
        <div className="home-hero-copy">
          <span className="intro-kicker">ANÁLISIS DE COMPATIBILIDAD CON IA</span>
          <h1 id="home-title">Descubrí qué tan bien encaja tu CV con una oferta laboral</h1>
          <p>
            Compará tu perfil con una oferta, identificá fortalezas y oportunidades de mejora, y descubrí caminos profesionales que podés explorar a partir de tus habilidades.
          </p>
          <div className="home-hero-actions">
            <button className="primary-action home-primary-action" type="button" onClick={() => onNavigate('/analizar')}>
              Comenzar análisis
            </button>
            <button className="secondary-action home-secondary-action" type="button" onClick={scrollToHowItWorks}>
              Ver cómo funciona
            </button>
          </div>
          <p className="home-ai-note">Análisis asistido por Google Gemini</p>
        </div>
        <div className="home-hero-visual" aria-hidden="true">
          <div className="hero-analysis-panel">
            <div className="hero-analysis-topbar">
              <span />
              <span />
              <span />
            </div>
            <div className="hero-job-preview">
              <span className="hero-job-kicker">Oferta</span>
              <strong>{activeScenario.role}</strong>
            </div>
            <div className="hero-requirement-list">
              <div className="hero-requirement-row hero-row-java">
                <span>{activeScenario.matchingSkills[0]}</span>
                <strong>✓ Coincide</strong>
              </div>
              <div className="hero-requirement-row hero-row-spring">
                <span>{activeScenario.matchingSkills[1]}</span>
                <strong>✓ Coincide</strong>
              </div>
              <div className="hero-requirement-row hero-row-sql">
                <span>{activeScenario.matchingSkills[2]}</span>
                <strong>✓ Coincide</strong>
              </div>
              <div className="hero-requirement-row hero-row-docker">
                <span>{activeScenario.missingSkill}</span>
                <strong>! Falta</strong>
              </div>
            </div>
            <div className="hero-analysis-summary">
              <strong>Compatibilidad alta</strong>
              <span className="hero-analysis-track">
                <span className="hero-analysis-fill" />
              </span>
              <span>3 coincidencias · 1 oportunidad de mejora</span>
            </div>
          </div>
        </div>
      </section>

      <section className="home-benefits" aria-label="Beneficios principales">
        {BENEFITS.map((benefit) => (
          <article className="home-benefit-card" key={benefit.title}>
            <div className="benefit-scenario-content" key={`${demoScenarioIndex}-${benefit.kind}`}>
              <BenefitVisual kind={benefit.kind} scenario={activeScenario} />
            </div>
            <h2>{benefit.title}</h2>
            <p>{benefit.text}</p>
          </article>
        ))}
      </section>

      <section className="home-how" id="como-funciona" aria-labelledby="how-title">
        <div className="home-section-heading">
          <span className="intro-kicker">CÓMO FUNCIONA</span>
          <h2 id="how-title">De tu CV a tu próximo paso profesional</h2>
          <p>Analizá una oferta concreta y usá ese perfil como punto de partida para mejorar tu candidatura y explorar nuevas oportunidades.</p>
        </div>
        <div className={`home-step-flow home-step-progress-${visibleStepCount}`}>
          <article
            className={`home-step-card home-step-upload${visibleSteps[0] ? ' is-visible' : ''}`}
            data-step-index="0"
            ref={(element) => {
              stepRefs.current[0] = element
            }}
          >
            <div className="home-step-copy">
              <span className="home-step-number">01</span>
              <h3>Subí tu CV</h3>
              <p>Cargalo en PDF para analizar tu perfil profesional.</p>
              <span className="home-step-pill">PDF • máx 5MB</span>
            </div>
            <div className="home-step-visual upload-visual" aria-hidden="true">
              <div className="cv-document">
                <span className="cv-avatar" />
                <span className="cv-line long" />
                <span className="cv-line" />
                <span className="cv-line short" />
                <b>PDF</b>
              </div>
              <span className="upload-arrow" />
              <div className="upload-tray">
                <span />
              </div>
            </div>
          </article>

          <article
            className={`home-step-card home-step-offer${visibleSteps[1] ? ' is-visible' : ''}`}
            data-step-index="1"
            ref={(element) => {
              stepRefs.current[1] = element
            }}
          >
            <div className="home-step-copy">
              <span className="home-step-number">02</span>
              <h3>Agregá la oferta</h3>
              <p>Pegala como texto o subí una imagen de la publicación.</p>
              <div className="home-input-pills" aria-label="Formatos aceptados">
                <span>Texto</span>
                <span>Imagen</span>
              </div>
            </div>
            <div className="home-step-visual offer-visual" aria-hidden="true">
              <span className="tech-chip java">Java</span>
              <span className="tech-chip sql">SQL</span>
              <span className="tech-chip spring">Spring Boot</span>
              <div className="job-window">
                <span className="window-dots" />
                <span className="briefcase" />
                <span className="job-line long" />
                <span className="job-line" />
                <span className="job-line short" />
              </div>
              <span className="search-badge">⌕</span>
            </div>
          </article>

          <article
            className={`home-step-card home-step-analysis${visibleSteps[2] ? ' is-visible' : ''}`}
            data-step-index="2"
            ref={(element) => {
              stepRefs.current[2] = element
            }}
          >
            <div className="home-step-copy">
              <span className="home-step-number">03</span>
              <h3>Recibí tu análisis</h3>
              <p>Revisá tu compatibilidad, habilidades y recomendaciones, y preparate con preguntas de entrevista y oportunidades relacionadas.</p>
            </div>
            <div className="home-step-visual analysis-visual" aria-hidden="true">
              <div className="match-ring" style={matchRingStyle}>
                <strong>{matchScore}<span>%</span></strong>
                <small>Match</small>
              </div>
              <div className="match-chip-row">
                <span>✓ Java</span>
                <span>✓ SQL</span>
                <span>✓ Spring Boot</span>
              </div>
              <span className="improvement-pill">2 habilidades por mejorar</span>
            </div>
          </article>

          <article
            className={`home-step-card home-step-future${visibleSteps[3] ? ' is-visible' : ''}`}
            data-step-index="3"
            ref={(element) => {
              stepRefs.current[3] = element
            }}
          >
            <div className="home-step-copy">
              <span className="home-step-number">04</span>
              <h3>Explorá tu futuro</h3>
              <p>Explorá caminos profesionales, habilidades a reforzar y próximos pasos a partir de tu perfil.</p>
            </div>
            <div className="home-step-visual future-visual" aria-hidden="true">
              <div className="future-map">
                <svg className="future-route-lines future-route-lines-desktop" viewBox="0 0 520 260" preserveAspectRatio="none" aria-hidden="true">
                  <defs>
                    <marker id="future-arrow-natural" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                      <path d="M 0 0 L 8 4 L 0 8 Z" />
                    </marker>
                    <marker id="future-arrow-expansion" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                      <path d="M 0 0 L 8 4 L 0 8 Z" />
                    </marker>
                    <marker id="future-arrow-alternative" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                      <path d="M 0 0 L 8 4 L 0 8 Z" />
                    </marker>
                  </defs>
                  <path className="future-line future-line-natural" d="M 118 76 C 188 76 214 42 286 42 L 340 42" />
                  <path className="future-line future-line-expansion" d="M 118 130 C 196 130 242 130 340 130" />
                  <path className="future-line future-line-alternative" d="M 118 184 C 188 184 214 218 286 218 L 340 218" />
                </svg>
                <div className="future-profile-card">
                  <span className="future-profile-avatar" />
                  <strong>Tu perfil</strong>
                  <span className="future-profile-line long" />
                  <span className="future-profile-line" />
                  <span className="future-profile-spark" />
                </div>
                <div className="future-destinations" aria-label="Rutas profesionales">
                  <div className="future-destination future-destination-natural">
                    <span className="future-icon-card">
                      <img className="future-path-image" src={CAREER_PATHS.natural.icon} alt="" aria-hidden="true" />
                    </span>
                    <span className="future-destination-copy">
                      <strong>{CAREER_PATHS.natural.name}</strong>
                      <small>{CAREER_PATHS.natural.description}</small>
                    </span>
                  </div>
                  <div className="future-destination future-destination-expansion">
                    <span className="future-icon-card">
                      <img className="future-path-image" src={CAREER_PATHS.expansion.icon} alt="" aria-hidden="true" />
                    </span>
                    <span className="future-destination-copy">
                      <strong>{CAREER_PATHS.expansion.name}</strong>
                      <small>{CAREER_PATHS.expansion.description}</small>
                    </span>
                  </div>
                  <div className="future-destination future-destination-alternative">
                    <span className="future-icon-card">
                      <img className="future-path-image" src={CAREER_PATHS.alternative.icon} alt="" aria-hidden="true" />
                    </span>
                    <span className="future-destination-copy">
                      <strong>{CAREER_PATHS.alternative.name}</strong>
                      <small>{CAREER_PATHS.alternative.description}</small>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>
        <div className="home-final-cta">
          <div className="home-final-copy">
            <span className="intro-kicker">PROBALO CON TU CV</span>
            <h2>¿Listo para conocer tu compatibilidad y explorar tu próximo paso?</h2>
            <p>Subí tu CV, agregá una oferta y recibí información concreta para mejorar tu candidatura y decidir tu próximo paso.</p>
            <button className="primary-action home-final-action" type="button" onClick={() => onNavigate('/analizar')}>
              Analizar mi CV
            </button>
            <p className="home-privacy-note">Análisis asistido por Google Gemini</p>
          </div>
          <div className="home-final-detail" aria-hidden="true">
            <span className="home-final-ready">Todo listo</span>
            <span className="home-final-state home-final-state-cv">✓ CV listo</span>
            <span className="home-final-state home-final-state-offer">✓ Oferta</span>
            <span className="home-final-state home-final-state-ai">IA preparada</span>
            <span className="home-final-line" />
            <span className="home-final-start">Analizar</span>
          </div>
        </div>
      </section>

      <AppFooter />
      <BottomNav active="home" onNavigate={onNavigate} />
    </main>
  )
}
