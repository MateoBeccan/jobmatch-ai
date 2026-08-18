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

const STEPS = [
  { title: 'Subí tu CV', text: 'Cargalo en PDF para analizar tu perfil profesional.' },
  { title: 'Agregá la oferta', text: 'Pegala como texto o subí una imagen de la publicación.' },
  { title: 'Recibí tu análisis', text: 'Revisá compatibilidad, skills, requisitos y recomendaciones.' },
]

export function HomePage({ theme, onToggleTheme, onNavigate }: HomePageProps) {
  const scrollToHowItWorks = () => {
    document.getElementById('como-funciona')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

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
          <span className="intro-kicker">Cómo funciona</span>
          <h2 id="how-title">Tres pasos para entender tu próxima postulación</h2>
        </div>
        <div className="home-step-grid">
          {STEPS.map((step, index) => (
            <article className="home-step-card" key={step.title}>
              <span>{String(index + 1).padStart(2, '0')}</span>
              <h3>{step.title}</h3>
              <p>{step.text}</p>
            </article>
          ))}
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
