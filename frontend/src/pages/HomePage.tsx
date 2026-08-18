import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import type { Theme } from '../lib/types/types'
import { AppFooter } from '../components/atoms/AppFooter'
import { BottomNav } from '../components/atoms/BottomNav'
import { AppHeader } from '../components/molecules/AppHeader'

type HomePageProps = {
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
}

const BENEFITS = [
  {
    title: 'Compatibilidad',
    text: 'Obtené una estimación clara del nivel de ajuste entre tu CV y la oferta.',
  },
  {
    title: 'Habilidades',
    text: 'Identificá fortalezas, coincidencias técnicas y requisitos que todavía podés desarrollar.',
  },
  {
    title: 'Recomendaciones',
    text: 'Recibí acciones concretas para mejorar tu CV y tu candidatura.',
  },
  {
    title: 'Entrevista',
    text: 'Preparate con posibles preguntas relacionadas con el puesto.',
  },
]

const HOW_STEP_COUNT = 3
const MATCH_TARGET = 87

function shouldReduceMotion() {
  return typeof window !== 'undefined' && 'matchMedia' in window && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function HomePage({ theme, onToggleTheme, onNavigate }: HomePageProps) {
  const stepRefs = useRef<Array<HTMLElement | null>>([])
  const scoreAnimationRef = useRef<number | null>(null)
  const [visibleSteps, setVisibleSteps] = useState<boolean[]>(() =>
    Array(HOW_STEP_COUNT).fill(typeof window === 'undefined' || shouldReduceMotion()),
  )
  const [matchScore, setMatchScore] = useState(() => (typeof window === 'undefined' || shouldReduceMotion() ? MATCH_TARGET : 0))

  const scrollToHowItWorks = () => {
    document.getElementById('como-funciona')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

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
        entries.forEach((entry) => {
          const index = Number((entry.target as HTMLElement).dataset.stepIndex)
          const isStepVisible = entry.isIntersecting && entry.intersectionRatio >= 0.32

          setVisibleSteps((current) => {
            if (current[index] === isStepVisible) {
              return current
            }

            const next = [...current]
            next[index] = isStepVisible
            return next
          })
        })
      },
      { threshold: [0, 0.25, 0.32, 0.4], rootMargin: '0px 0px -10% 0px' },
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

  const visibleStepCount = visibleSteps[2] ? 3 : visibleSteps[1] ? 2 : visibleSteps[0] ? 1 : 0
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
            Compará tu perfil con una oferta, identificá habilidades coincidentes, requisitos faltantes y oportunidades concretas para mejorar tu candidatura.
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
              <strong>Backend Developer</strong>
            </div>
            <div className="hero-requirement-list">
              <div className="hero-requirement-row hero-row-java">
                <span>Java</span>
                <strong>✓ Coincide</strong>
              </div>
              <div className="hero-requirement-row hero-row-spring">
                <span>Spring Boot</span>
                <strong>✓ Coincide</strong>
              </div>
              <div className="hero-requirement-row hero-row-sql">
                <span>SQL</span>
                <strong>✓ Coincide</strong>
              </div>
              <div className="hero-requirement-row hero-row-docker">
                <span>Docker</span>
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
            <h2>{benefit.title}</h2>
            <p>{benefit.text}</p>
          </article>
        ))}
      </section>

      <section className="home-how" id="como-funciona" aria-labelledby="how-title">
        <div className="home-section-heading">
          <span className="intro-kicker">CÓMO FUNCIONA</span>
          <h2 id="how-title">De tu CV a una mejor postulación</h2>
          <p>Tres simples pasos para conocer tu compatibilidad con cualquier oferta laboral.</p>
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
              <p>Revisá compatibilidad, skills, requisitos y recomendaciones.</p>
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
              <span className="improvement-pill">2 skills por mejorar</span>
            </div>
          </article>
        </div>
        <p className="home-privacy-note">Tu CV y la oferta se procesan mediante inteligencia artificial para generar el análisis.</p>
        <button className="primary-action home-final-action" type="button" onClick={() => onNavigate('/analizar')}>
          Analizar mi CV
        </button>
      </section>

      <AppFooter />
      <BottomNav active="home" onNavigate={onNavigate} />
    </main>
  )
}
