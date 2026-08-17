import type { JobOffer } from '../../lib/types/types'

type JobOfferCardProps = {
  job: JobOffer
}

const DATE_FORMATTER = new Intl.DateTimeFormat('es-AR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

export function JobOfferCard({ job }: JobOfferCardProps) {
  const publishedAt = formatPublishedAt(job.updatedAt)

  return (
    <article className="job-offer-card">
      <div className="job-offer-heading">
        <h3>{job.title}</h3>
        {job.company && <p>{job.company}</p>}
      </div>

      <div className="job-offer-meta" aria-label="Datos de la oferta">
        {job.location && <span>{job.location}</span>}
        {job.employmentType && <span>{job.employmentType}</span>}
        {job.salary && <span>{job.salary}</span>}
        {publishedAt && <span>{publishedAt}</span>}
      </div>

      {job.snippet && <p className="job-offer-snippet">{job.snippet}</p>}

      {job.matchedKeywords.length > 0 && (
        <div className="job-keyword-group">
          <span>Coincidencias con tu perfil</span>
          <div className="job-keyword-chips">
            {job.matchedKeywords.map((keyword) => (
              <span key={keyword} className="job-keyword-chip">{keyword}</span>
            ))}
          </div>
        </div>
      )}

      <div className="job-offer-footer">
        <span>Fuente: {job.source}</span>
        <a
          href={job.url}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`Ver oferta ${job.title} en Jobicy`}
        >
          Ver oferta
        </a>
      </div>
    </article>
  )
}

function formatPublishedAt(value: string | null) {
  if (!value) return null
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return null
  return `Publicada ${DATE_FORMATTER.format(date)}`
}
