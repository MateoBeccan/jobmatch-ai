import { useState } from 'react'
import type { RequirementMatch, RequirementStatus } from '../../lib/types/types'
import { getRequirementLabel } from '../../lib/helpers/analysis'

type RequirementsSectionProps = {
  requirements: RequirementMatch[]
}

type RequirementFilter = 'all' | RequirementStatus

const FILTERS: Array<{ value: RequirementFilter; label: string }> = [
  { value: 'all', label: 'Todos' },
  { value: 'match', label: 'Cumplo' },
  { value: 'partial', label: 'Parcial' },
  { value: 'missing', label: 'Faltan' },
]

const STATUS_ICONS: Record<RequirementStatus, string> = {
  match: '✓',
  partial: '⚠',
  missing: '✕',
}

export function RequirementsSection({ requirements }: RequirementsSectionProps) {
  const [filter, setFilter] = useState<RequirementFilter>('all')
  const visible = filter === 'all' ? requirements : requirements.filter((requirement) => requirement.status === filter)
  const countFor = (status: RequirementFilter) => status === 'all' ? requirements.length : requirements.filter((requirement) => requirement.status === status).length

  return (
    <section className="results-panel requirements-section" aria-label="Requisitos de la oferta">
      <h2>Requisitos</h2>
      <div className="requirement-filters" role="group" aria-label="Filtrar requisitos">
        {FILTERS.map(({ value, label }) => (
          <button key={value} type="button" aria-pressed={filter === value} className={filter === value ? 'active' : ''} onClick={() => setFilter(value)}>
            {label}<span className="filter-count">{countFor(value)}</span>
          </button>
        ))}
      </div>
      {visible.length > 0 ? (
        <ul className="requirement-list">
          {visible.map((requirement) => (
            <li key={`${requirement.name}-${requirement.status}`} className={`requirement-item ${requirement.status}`}>
              <span className="requirement-icon" aria-hidden="true">{STATUS_ICONS[requirement.status]}</span>
              <span className="requirement-copy">
                <strong>{requirement.name}</strong>
                <small>{getRequirementLabel(requirement.status)}</small>
              </span>
              {requirement.evidence && (
                <details className="requirement-evidence">
                  <summary aria-label={`Detalle de ${requirement.name}`}>Detalle</summary>
                  {requirement.evidence}
                </details>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <p className="empty-list">No hay requisitos para este filtro.</p>
      )}
    </section>
  )
}
