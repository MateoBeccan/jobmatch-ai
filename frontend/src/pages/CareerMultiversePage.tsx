import type {
  CareerLearningPriority,
  CareerMarketConfidence,
  CareerMultiverseRequest,
  CareerMultiverseResponse,
  CareerPath,
  CareerPathType,
  CareerRegion,
  JobSeniority,
  Theme,
} from '../lib/types/types'
import type { AnalysisErrorView } from '../services/errorMessages'
import { AppFooter } from '../components/atoms/AppFooter'
import { AppHeader } from '../components/molecules/AppHeader'
import { CareerWhatIfSimulator } from '../components/organisms/CareerWhatIfSimulator'

type CareerMultiversePageProps = {
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
  request: CareerMultiverseRequest | null
  response: CareerMultiverseResponse | null
  error: AnalysisErrorView | null
  isLoading: boolean
  selectedPathType: CareerPathType
  onSelectPath: (type: CareerPathType) => void
  onRetry: () => void
  hasAnalysisResult: boolean
}

const PATH_ORDER: CareerPathType[] = ['NATURAL', 'EXPANSION', 'ALTERNATIVE']

const PATH_LABELS: Record<CareerPathType, { label: string; title: string; description: string }> = {
  NATURAL: {
    label: 'Natural',
    title: 'Camino natural',
    description: 'El camino mas cercano a tu perfil actual.',
  },
  EXPANSION: {
    label: 'Expansion',
    title: 'Camino de expansion',
    description: 'Una evolucion que amplia tu stack actual.',
  },
  ALTERNATIVE: {
    label: 'Alternativo',
    title: 'Camino alternativo',
    description: 'Una direccion diferente que aprovecha habilidades transferibles.',
  },
}

const SENIORITY_LABELS: Record<JobSeniority, string> = {
  TRAINEE: 'Trainee',
  JUNIOR: 'Junior',
  MID: 'Semi Senior',
  SENIOR: 'Senior',
  UNSPECIFIED: 'No especificado',
}

const REGION_LABELS: Record<CareerRegion, string> = {
  ARGENTINA: 'Argentina',
  LATAM: 'LATAM',
  GLOBAL: 'Global',
}

const CONFIDENCE_LABELS: Record<CareerMarketConfidence, string> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
  INSUFFICIENT: 'Insuficiente',
}

const PRIORITY_LABELS: Record<CareerLearningPriority, string> = {
  NOW: 'Aprende ahora',
  NEXT: 'Proximo',
  LATER: 'Puede esperar',
}

export function CareerMultiversePage({
  theme,
  onToggleTheme,
  onNavigate,
  request,
  response,
  error,
  isLoading,
  selectedPathType,
  onSelectPath,
  onRetry,
  hasAnalysisResult,
}: CareerMultiversePageProps) {
  const orderedPaths = orderPaths(response?.paths ?? [])
  const selectedPath = orderedPaths.find((path) => path.type === selectedPathType) ?? orderedPaths[0] ?? null
  const profile = response?.profile ?? request

  return (
    <main className="page-shell career-shell">
      <AppHeader active="analyze" theme={theme} onToggleTheme={onToggleTheme} onNavigate={onNavigate} />

      {!request && (
        <section className="career-empty-state" aria-labelledby="career-empty-title">
          <span className="career-kicker">Career Multiverse</span>
          <h1 id="career-empty-title">Primero necesitamos conocer tu perfil.</h1>
          <p>Realiza un analisis para descubrir caminos profesionales relacionados con tus habilidades.</p>
          <button type="button" className="primary-action" onClick={() => onNavigate('/analizar')}>Analizar mi CV</button>
        </section>
      )}

      {request && isLoading && <CareerLoading />}

      {request && error && !isLoading && (
        <section className="career-empty-state career-error-state" role="alert" aria-labelledby="career-error-title">
          <span className="career-kicker">Career Multiverse</span>
          <h1 id="career-error-title">{error.title ?? 'No pudimos explorar tus caminos profesionales'}</h1>
          <p>{error.message}</p>
          {error.retryable && <button type="button" className="primary-action" onClick={onRetry}>Intentar nuevamente</button>}
          <button type="button" className="secondary-action" onClick={() => onNavigate('/analizar')}>Volver al analisis</button>
        </section>
      )}

      {request && response && !isLoading && !error && selectedPath && (
        <>
          <section className="career-hero" aria-labelledby="career-title">
            <div>
              <span className="career-kicker">Career Multiverse</span>
              <h1 id="career-title">Tu perfil no tiene un unico camino.</h1>
              <p>Exploramos distintas rutas profesionales a partir de tus habilidades y las contrastamos con ofertas laborales actuales.</p>
            </div>
            <aside>
              Orientacion basada en una muestra de ofertas. No representa una prediccion de contratacion.
            </aside>
          </section>

          {profile && <CurrentProfile profile={profile} region={response.region} />}

          <section className="career-map-section" aria-labelledby="career-map-title">
            <div className="career-section-heading">
              <span className="career-kicker">Tres caminos posibles</span>
              <h2 id="career-map-title">Desde tu perfil aparecen rutas distintas</h2>
            </div>
            <div className="career-universe-map" aria-hidden="true">
              <div className="career-origin-node">Tu perfil</div>
              <div className="career-branches">
                {PATH_ORDER.map((type) => (
                  <div key={type} className={`career-branch ${type.toLowerCase()}`}>
                    <span />
                    <strong>{PATH_LABELS[type].label}</strong>
                  </div>
                ))}
              </div>
            </div>
            <div className="career-path-grid" role="tablist" aria-label="Caminos profesionales">
              {orderedPaths.map((path) => (
                <PathCard
                  key={path.type}
                  path={path}
                  selected={path.type === selectedPath.type}
                  onSelect={() => onSelectPath(path.type)}
                />
              ))}
            </div>
          </section>

          <PathDetail path={selectedPath} provider={response.provider} />

          <section className="career-methodology" aria-labelledby="career-methodology-title">
            <h2 id="career-methodology-title">De donde salen estas recomendaciones</h2>
            <div className="career-methodology-grid">
              <article>
                <span>IA</span>
                <p>Propone caminos compatibles con tu perfil sin presentarlos como una verdad unica.</p>
              </article>
              <article>
                <span>Mercado</span>
                <p>Contrasta esos caminos con una muestra de ofertas actuales. Las frecuencias representan unicamente las ofertas analizadas y no todo el mercado laboral.</p>
              </article>
              <article>
                <span>Simulacion</span>
                <p>Calcula como cambiaria la cobertura si incorporaras skills observadas en esa misma muestra.</p>
              </article>
            </div>
            {response.provider && <p className="career-provider-note">Ofertas de la muestra provistas por {response.provider}.</p>}
          </section>

          <div className="career-end-actions">
            <button type="button" className="primary-action" onClick={() => onNavigate('/analizar')}>Analizar otra oferta</button>
            {hasAnalysisResult && <button type="button" className="secondary-action" onClick={() => onNavigate('/analizar')}>Volver al resultado</button>}
          </div>
        </>
      )}

      <AppFooter />
    </main>
  )
}

function CareerLoading() {
  return (
    <section className="career-loading" aria-live="polite" aria-busy="true">
      <div className="career-loading-orbit" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      <span className="career-kicker">Explorando caminos</span>
      <h1>Estamos contrastando tu perfil con distintas rutas profesionales.</h1>
      <ul>
        <li><span aria-hidden="true">✓</span> Interpretando tu perfil</li>
        <li><span aria-hidden="true">○</span> Explorando caminos</li>
        <li><span aria-hidden="true">○</span> Contrastando senales del mercado</li>
        <li><span aria-hidden="true">○</span> Preparando tu mapa profesional</li>
      </ul>
    </section>
  )
}

function CurrentProfile({ profile, region }: { profile: CareerMultiverseRequest | CareerMultiverseResponse['profile']; region: CareerRegion }) {
  return (
    <section className="career-profile-card" aria-labelledby="career-profile-title">
      <div>
        <span className="career-kicker">Tu punto de partida</span>
        <h2 id="career-profile-title">{profile.role}</h2>
      </div>
      <dl>
        <div>
          <dt>Nivel</dt>
          <dd>{SENIORITY_LABELS[profile.seniority]}</dd>
        </div>
        <div>
          <dt>Region</dt>
          <dd>{REGION_LABELS[region]}</dd>
        </div>
      </dl>
      <div className="career-chip-list" aria-label="Skills del perfil">
        {profile.skills.map((skill) => <span key={skill}>{skill}</span>)}
      </div>
    </section>
  )
}

function PathCard({ path, selected, onSelect }: { path: CareerPath; selected: boolean; onSelect: () => void }) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={selected}
      aria-current={selected ? 'true' : undefined}
      className={`career-path-card ${selected ? 'selected' : ''}`}
      onClick={onSelect}
    >
      <span className={`career-path-badge ${path.type.toLowerCase()}`}>{PATH_LABELS[path.type].label}</span>
      <strong>{path.role}</strong>
      <small>{PATH_LABELS[path.type].description}</small>
      <p>{path.summary}</p>
      <div className="career-card-metrics">
        <span>
          <b>{path.market.coveragePercentage}%</b>
          Cobertura actual
        </span>
        <span>
          <b>{path.market.sampleSize}</b>
          ofertas analizadas
        </span>
        <span>
          <b>{CONFIDENCE_LABELS[path.market.confidence]}</b>
          confianza
        </span>
      </div>
      {path.market.currentSkillsDetected.length > 0 && (
        <div className="career-mini-chips" aria-label="Skills actuales detectadas">
          {path.market.currentSkillsDetected.slice(0, 4).map((skill) => <span key={skill}>{skill}</span>)}
        </div>
      )}
      <em>Explorar camino</em>
    </button>
  )
}

function PathDetail({ path, provider }: { path: CareerPath; provider: string }) {
  const insufficient = path.market.confidence === 'INSUFFICIENT'

  return (
    <section className="career-path-detail" aria-labelledby="career-detail-title">
      <div className="career-detail-header">
        <div>
          <span className="career-kicker">{PATH_LABELS[path.type].title}</span>
          <h2 id="career-detail-title">{path.role}</h2>
        </div>
        <p>{path.summary}</p>
      </div>

      <div className="career-detail-block">
        <h3>Por que es un camino posible</h3>
        <p>{path.rationale}</p>
      </div>

      <div className="career-evidence-grid">
        <article className="career-coverage-card">
          <span>Cobertura actual de habilidades</span>
          <strong>{path.market.coveragePercentage}%</strong>
          <p>Indica que proporcion ponderada de las principales habilidades observadas en esta muestra ya aparecen en tu perfil.</p>
          <small>No representa probabilidad de contratacion.</small>
        </article>
        <article>
          <span>Muestra de mercado</span>
          <strong>{path.market.sampleSize}</strong>
          <p>{path.market.sampleSize} ofertas relacionadas analizadas.</p>
          <small>Confianza {CONFIDENCE_LABELS[path.market.confidence].toLowerCase()}. La confianza depende del tamano de la muestra disponible.</small>
        </article>
      </div>

      <SkillSection title="Ya esta en tu perfil" skills={path.market.currentSkillsDetected} empty="No se detectaron coincidencias suficientes en esta muestra." />

      <section className="career-detail-block">
        <h3>Que estan pidiendo las ofertas analizadas</h3>
        {path.market.skillDemand.length > 0 ? (
          <ul className="career-demand-list">
            {path.market.skillDemand.slice(0, 8).map((skill) => (
              <li key={skill.skill}>
                <span>{skill.skill}</span>
                <b>{skill.frequencyPercentage}%</b>
                <small>Detectado en {skill.frequencyPercentage}% de las ofertas relacionadas de esta muestra.</small>
              </li>
            ))}
          </ul>
        ) : (
          <p className="career-muted">No hay suficiente demanda agregada para mostrar una tendencia confiable.</p>
        )}
      </section>

      <section className="career-detail-block">
        <h3>Que aprender primero</h3>
        {insufficient ? (
          <p className="career-insufficient">No encontramos suficientes ofertas relacionadas para extraer una tendencia confiable.</p>
        ) : path.learningPriorities.length > 0 ? (
          <ul className="career-priority-list">
            {path.learningPriorities.map((priority) => (
              <li key={priority.skill} className={priority.priority.toLowerCase()}>
                <span>{PRIORITY_LABELS[priority.priority]}</span>
                <strong>{priority.skill}</strong>
                <p>Detectado en {priority.frequencyPercentage}% de las ofertas relacionadas de esta muestra.</p>
                {priority.priority === 'LATER' && <small>Esta habilidad aparece con menor frecuencia en la muestra actual. Podrias priorizar primero las anteriores.</small>}
              </li>
            ))}
          </ul>
        ) : (
          <p className="career-muted">No hay prioridades de aprendizaje confiables para esta muestra.</p>
        )}
      </section>

      <CareerWhatIfSimulator key={path.type} market={path.market} learningPriorities={path.learningPriorities} provider={provider} />

      {path.roadmap.length > 0 && (
        <section className="career-detail-block">
          <h3>Tu proximo movimiento</h3>
          <ol className="career-roadmap">
            {path.roadmap.slice(0, 4).map((step) => (
              <li key={step.step}>
                <span>{String(step.step).padStart(2, '0')}</span>
                <div>
                  <strong>{step.title}</strong>
                  <p>{step.description}</p>
                </div>
              </li>
            ))}
          </ol>
        </section>
      )}

      {path.projectChallenge && (
        <section className="career-project-card" aria-labelledby="career-project-title">
          <span className="career-kicker">Reto de portfolio</span>
          <h3 id="career-project-title">{path.projectChallenge.title}</h3>
          <p>{path.projectChallenge.description}</p>
          <small>Converti aprendizaje en evidencia concreta para tu proximo CV.</small>
          <div className="career-chip-list">
            {path.projectChallenge.skills.map((skill) => <span key={skill}>{skill}</span>)}
          </div>
        </section>
      )}

      {provider && <p className="career-provider-note">Muestra de ofertas provista por {provider}.</p>}
    </section>
  )
}

function SkillSection({ title, skills, empty }: { title: string; skills: string[]; empty: string }) {
  return (
    <section className="career-detail-block">
      <h3>{title}</h3>
      {skills.length > 0 ? (
        <div className="career-chip-list">
          {skills.map((skill) => <span key={skill}>{skill}</span>)}
        </div>
      ) : (
        <p className="career-muted">{empty}</p>
      )}
    </section>
  )
}

function orderPaths(paths: CareerPath[]) {
  return [...paths].sort((left, right) => PATH_ORDER.indexOf(left.type) - PATH_ORDER.indexOf(right.type))
}
