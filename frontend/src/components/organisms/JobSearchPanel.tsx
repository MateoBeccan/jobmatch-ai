import { useEffect, useRef, useState } from 'react'
import { searchJobs } from '../../services/api'
import { toUserFacingJobSearchError } from '../../services/errorMessages'
import type { AnalysisErrorView } from '../../services/errorMessages'
import type { JobOffer, JobSearchLocation, JobSearchProfile } from '../../lib/types/types'
import { JobOfferCard } from '../molecules/JobOfferCard'

type JobSearchPanelProps = {
  profile: JobSearchProfile
}

const LOCATION_OPTIONS: Array<{ value: JobSearchLocation; label: string }> = [
  { value: 'Argentina', label: 'Argentina' },
  { value: 'LATAM', label: 'Latinoamérica' },
  { value: 'Global', label: 'Global' },
]

const SENIORITY_LABELS: Record<JobSearchProfile['seniority'], string> = {
  TRAINEE: 'Trainee',
  JUNIOR: 'Junior',
  MID: 'Semi Senior',
  SENIOR: 'Senior',
  UNSPECIFIED: 'No especificado',
}

export function JobSearchPanel({ profile }: JobSearchPanelProps) {
  const [selectedLocation, setSelectedLocation] = useState<JobSearchLocation>('Argentina')
  const [jobs, setJobs] = useState<JobOffer[] | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<AnalysisErrorView | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  useEffect(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setSelectedLocation('Argentina')
    setJobs(null)
    setError(null)
    setIsLoading(false)
  }, [profile])

  useEffect(() => () => {
    const controller = abortRef.current
    abortRef.current = null
    controller?.abort()
  }, [])

  async function runSearch() {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setError(null)
    setJobs(null)
    setIsLoading(true)

    try {
      const response = await searchJobs(profile, selectedLocation, controller.signal)
      setJobs(response.jobs)
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') return
      setError(toUserFacingJobSearchError(requestError))
    } finally {
      if (abortRef.current === controller) {
        abortRef.current = null
        setIsLoading(false)
      }
    }
  }

  function handleLocationChange(value: JobSearchLocation) {
    abortRef.current?.abort()
    abortRef.current = null
    setSelectedLocation(value)
    setJobs(null)
    setError(null)
    setIsLoading(false)
  }

  const hasSearched = jobs !== null
  const emptyState = hasSearched && jobs.length === 0

  return (
    <section className="results-panel job-search-panel" aria-labelledby="job-search-title" aria-busy={isLoading}>
      <div className="job-search-header">
        <div>
          <h2 id="job-search-title">Ofertas remotas relacionadas con tu perfil</h2>
          <p>Explorá vacantes públicas relacionadas con tu experiencia y tecnologías.</p>
        </div>
      </div>

      <dl className="job-profile-summary" aria-label="Perfil tecnico detectado">
        <div>
          <dt>Perfil detectado</dt>
          <dd>{profile.role}</dd>
        </div>
        <div>
          <dt>Seniority</dt>
          <dd>{SENIORITY_LABELS[profile.seniority]}</dd>
        </div>
      </dl>

      <p className="job-search-privacy">
        Tu CV no se envía a Jobicy. Las ofertas se filtran desde JobMatch AI utilizando el perfil técnico generado por tu análisis.
      </p>

      <div className="job-search-controls">
        <label htmlFor="job-search-location">Región de búsqueda</label>
        <select
          id="job-search-location"
          value={selectedLocation}
          onChange={(event) => handleLocationChange(event.target.value as JobSearchLocation)}
          disabled={isLoading}
        >
          {LOCATION_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        <button type="button" className="primary-action" disabled={isLoading} onClick={() => void runSearch()}>
          {isLoading ? 'Buscando ofertas...' : 'Buscar ofertas'}
        </button>
      </div>

      {isLoading && <p className="job-search-status" aria-live="polite">Buscando ofertas...</p>}

      {error && (
        <div className="job-search-alert" role="alert">
          {error.title && <strong>{error.title}</strong>}
          <p>{error.message}</p>
          {error.retryable && (
            <button type="button" onClick={() => void runSearch()}>Reintentar búsqueda</button>
          )}
        </div>
      )}

      {emptyState && (
        <div className="job-search-empty" aria-live="polite">
          <p>{emptyMessage(selectedLocation)}</p>
          {emptySuggestion(selectedLocation) && <small>{emptySuggestion(selectedLocation)}</small>}
        </div>
      )}

      {jobs && jobs.length > 0 && (
        <div className="job-offer-list" aria-live="polite">
          {jobs.map((job) => (
            <JobOfferCard key={job.id ?? job.url} job={job} />
          ))}
        </div>
      )}
    </section>
  )
}

function emptyMessage(location: JobSearchLocation) {
  if (location === 'Argentina') {
    return 'No encontramos ofertas remotas suficientemente relacionadas con tu perfil para Argentina en este momento.'
  }
  if (location === 'LATAM') {
    return 'No encontramos ofertas remotas suficientemente relacionadas con tu perfil para Latinoamérica en este momento.'
  }
  return 'No encontramos ofertas suficientemente relacionadas con tu perfil en este momento. Probá nuevamente más tarde.'
}

function emptySuggestion(location: JobSearchLocation) {
  if (location === 'Argentina') {
    return 'Podés ampliar la búsqueda a LATAM o Global, o volver a intentarlo más tarde.'
  }
  if (location === 'LATAM') {
    return 'Podés ampliar la búsqueda a Global o volver a intentarlo más tarde.'
  }
  return null
}
