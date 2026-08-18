import { useCallback, useMemo, useState } from 'react'
import type { CSSProperties } from 'react'
import type { AnalysisSummary, HistorySort, ScoreRange, Theme } from '../../lib/types/types'
import { formatHistoryDate, getScoreClass } from '../../lib/helpers/format'
import { computeHistoryStats, filterByScoreRange } from '../../lib/helpers/analysis'
import { BottomNav } from '../atoms/BottomNav'
import { ErrorState } from '../molecules/ErrorState'
import { ConfirmDialog } from '../molecules/ConfirmDialog'
import { AppFooter } from '../atoms/AppFooter'
import { AppHeader } from '../molecules/AppHeader'
import newAnalysisIcon from '../../assets/navigation/new-analysis.png'

type HistoryScreenProps = {
  records: AnalysisSummary[]
  loading: boolean
  loadingMore: boolean
  hasMore: boolean
  error: string
  onRetry: () => void
  onLoadMore: () => void
  onAnalyze: () => void
  onOpenRecord: (id: string) => void
  onCompare: (previousId: string, currentId: string) => void
  onDelete: (id: string) => Promise<void>
  onNavigate: (route: string) => void
  theme: Theme
  onToggleTheme: () => void
}

const SCORE_RANGES: Array<{ value: ScoreRange; label: string }> = [
  { value: 'all', label: 'Todos' },
  { value: 'top', label: '>80%' },
  { value: 'mid', label: '60–80%' },
  { value: 'low', label: '<60%' },
]

const SORTS: Array<{ value: HistorySort; label: string }> = [
  { value: 'date-desc', label: 'Fecha · reciente' },
  { value: 'date-asc', label: 'Fecha · antiguo' },
  { value: 'score-desc', label: 'Score · mayor' },
  { value: 'score-asc', label: 'Score · menor' },
]

const WEEK_MS = 7 * 24 * 60 * 60 * 1000

export function HistoryScreen({
  records, loading, loadingMore, hasMore, error, onRetry, onLoadMore, onAnalyze, onOpenRecord, onCompare, onDelete, onNavigate,
  theme, onToggleTheme,
}: HistoryScreenProps) {
  const [query, setQuery] = useState('')
  const [range, setRange] = useState<ScoreRange>('all')
  const [sort, setSort] = useState<HistorySort>('date-desc')
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)

  const stats = useMemo(() => computeHistoryStats(records), [records])

  const requestDelete = useCallback((id: string) => setPendingDeleteId(id), [])

  const confirmDelete = useCallback(async () => {
    if (pendingDeleteId) {
      await onDelete(pendingDeleteId)
      setPendingDeleteId(null)
    }
  }, [pendingDeleteId, onDelete])

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const matchesQuery = normalizedQuery.length === 0
      ? records
      : records.filter((record) => `${record.role} ${record.company} ${record.cvFileName} ${record.cvVersion}`.toLowerCase().includes(normalizedQuery))
    const matchesRange = filterByScoreRange(matchesQuery, range)
    return [...matchesRange].sort((a, b) => {
      if (sort === 'date-asc') return a.createdAt - b.createdAt
      if (sort === 'score-desc') return b.score - a.score
      if (sort === 'score-asc') return a.score - b.score
      return b.createdAt - a.createdAt
    })
  }, [records, query, range, sort])

  const currentTime = Date.now()
  const grouped = sort === 'date-desc'
    ? {
        recent: filtered.filter((record) => currentTime - record.createdAt < WEEK_MS),
        older: filtered.filter((record) => currentTime - record.createdAt >= WEEK_MS),
      }
    : null
  const hasOlderRecords = records.some((record) => currentTime - record.createdAt >= WEEK_MS)

  return (
    <main className="history-shell" aria-busy={loading}>
      <AppHeader active="history" theme={theme} onToggleTheme={onToggleTheme} onNavigate={onNavigate} />

      <section className="history-page-heading" aria-labelledby="history-title">
        <div className="history-title-copy">
          <h1 id="history-title">Historial de Análisis</h1>
          <p>Revisá tus evaluaciones anteriores y compará tu evolución.</p>
        </div>
        <button className="menu-button" type="button" onClick={onAnalyze}>
          <img className="history-new-analysis-icon" src={newAnalysisIcon} alt="" />
          Nueva evaluación
        </button>
      </section>

      <section className="history-stats" aria-label="Estadísticas de tus análisis">
        <HistoryStat icon="doc" label="Ofertas analizadas" value={String(stats.total)} />
        <HistoryStat icon="gauge" label="Compatibilidad promedio" value={`${stats.averageScore}%`} />
        <HistoryStat icon="best" label="Mejor resultado" value={`${stats.bestScore}%`} />
        {typeof stats.trendDelta === 'number' && (
          <HistoryStat icon="trend" label="Tendencia" value={`${stats.trendDelta >= 0 ? '↑' : '↓'} ${Math.abs(stats.trendDelta)}%`} trend={stats.trendDelta >= 0 ? 'up' : 'down'} />
        )}
      </section>

      <section className="history-toolbar" aria-label="Controles de historial">
        <label className="history-search">
          <span aria-hidden="true">⌕</span>
          <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por rol, empresa o CV..." aria-label="Buscar análisis" />
        </label>

        <div className="history-controls">
          <div className="history-filters" role="group" aria-label="Filtrar por score">
            {SCORE_RANGES.map(({ value, label }) => (
              <button key={value} type="button" aria-pressed={range === value} className={range === value ? 'active' : ''} onClick={() => setRange(value)}>{label}</button>
            ))}
          </div>
          <label className="history-sort">
            <span className="visually-hidden">Ordenar por</span>
            <select value={sort} onChange={(event) => setSort(event.target.value as HistorySort)}>
              {SORTS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
        </div>
      </section>

      {loading && <div className="history-status" role="status"><span className="spinner history-spinner" /> Cargando tus análisis...</div>}
      {error && <ErrorState message={error} onRetry={onRetry} />}

      {!loading && !error && records.length === 0 && (
        <HistoryEmptyState
          imageSrc={newAnalysisIcon}
          title="Todavía no tenés análisis guardados"
          description="Realizá tu primera evaluación para empezar a construir tu historial."
          actionLabel="Nueva evaluación"
          onAction={onAnalyze}
        />
      )}

      {!loading && !error && records.length > 0 && filtered.length === 0 && (
        <HistoryEmptyState
          icon="⌕"
          title="No encontramos análisis con estos filtros"
          description="Probá cambiar la búsqueda o seleccionar Todos."
        />
      )}

      {!loading && !error && filtered.length > 0 && (
        <>
          {grouped ? (
            <>
              <HistoryGroup title="Recientes" records={grouped.recent} allRecords={records} onOpen={onOpenRecord} onCompare={onCompare} requestDelete={requestDelete} emptyMessage="No hay análisis recientes que coincidan con tu búsqueda." />
              <HistoryGroup
                title="Anteriores"
                records={grouped.older}
                allRecords={records}
                onOpen={onOpenRecord}
                onCompare={onCompare}
                requestDelete={requestDelete}
                emptyMessage="No hay análisis anteriores"
                emptyTitle={hasOlderRecords ? 'No hay análisis anteriores para estos filtros' : 'No hay análisis anteriores'}
                emptyDescription={hasOlderRecords
                  ? 'Los análisis más antiguos no coinciden con la búsqueda o filtros actuales.'
                  : 'Los análisis más antiguos aparecerán acá cuando existan.'}
                emptyIcon="◷"
                older
              />
            </>
          ) : (
            <HistoryGroup title="Todos los análisis" records={filtered} allRecords={records} onOpen={onOpenRecord} onCompare={onCompare} requestDelete={requestDelete} emptyMessage="No hay análisis que coincidan." />
          )}
          {hasMore && (
            <button className="load-more-button" type="button" disabled={loadingMore} onClick={onLoadMore}>
              {loadingMore ? 'Cargando...' : 'Cargar más análisis'}
            </button>
          )}
        </>
      )}

      <button className="new-analysis-button" type="button" onClick={onAnalyze}>
        <img className="history-new-analysis-icon" src={newAnalysisIcon} alt="" />
        Nueva evaluación
      </button>
      <AppFooter />
      <BottomNav active="history" onNavigate={onNavigate} />
      <ConfirmDialog
        open={pendingDeleteId !== null}
        title="Eliminar análisis"
        message="¿Eliminar este análisis? No se puede deshacer."
        confirmLabel="Eliminar"
        onConfirm={() => void confirmDelete()}
        onCancel={() => setPendingDeleteId(null)}
      />
    </main>
  )
}

type HistoryEmptyStateProps = {
  icon?: string
  imageSrc?: string
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
}

function HistoryEmptyState({ icon, imageSrc, title, description, actionLabel, onAction }: HistoryEmptyStateProps) {
  return (
    <div className="history-empty-state">
      {imageSrc ? <img className="history-empty-state-image" src={imageSrc} alt="" /> : <span className="history-empty-state-icon" aria-hidden="true">{icon ?? '◈'}</span>}
      <div className="history-empty-state-copy">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      {actionLabel && onAction && (
        <button className="history-empty-state-action" type="button" onClick={onAction}>
          {imageSrc && <img className="history-new-analysis-icon" src={imageSrc} alt="" />}
          {actionLabel}
        </button>
      )}
    </div>
  )
}

function HistoryStat({ icon, label, value, trend }: { icon: 'doc' | 'gauge' | 'best' | 'trend'; label: string; value: string; trend?: 'up' | 'down' }) {
  return (
    <div className={`history-stat${trend ? ` trend ${trend}` : ''}`}>
      <span className={`history-stat-icon ${icon}`} aria-hidden="true" />
      <span className="history-stat-copy">
        <strong>{value}</strong>
        <span>{label}</span>
      </span>
    </div>
  )
}

type HistoryGroupProps = {
  title: string
  records: AnalysisSummary[]
  allRecords: AnalysisSummary[]
  onOpen: (id: string) => void
  onCompare: (previousId: string, currentId: string) => void
  requestDelete: (id: string) => void
  emptyMessage: string
  emptyTitle?: string
  emptyDescription?: string
  emptyIcon?: string
  older?: boolean
}

function HistoryGroup({ title, records, allRecords, onOpen, onCompare, requestDelete, emptyMessage, emptyTitle, emptyDescription, emptyIcon, older = false }: HistoryGroupProps) {
  return (
    <section className={`history-section ${older ? 'older-section' : ''}`}>
      <h2>{title}</h2>
      {records.length > 0 ? (
        <div className="history-list">
          {records.map((record) => {
            const previous = findComparablePrevious(allRecords, record)
            return (
              <HistoryItem
                key={record.id}
                {...record}
                date={formatHistoryDate(record.createdAt)}
                previousScore={previous?.score}
                comparablePreviousId={previous?.id}
                onOpen={() => onOpen(record.id)}
                onCompare={comparablePreviousId => onCompare(comparablePreviousId, record.id)}
                onDelete={() => requestDelete(record.id)}
              />
            )
          })}
        </div>
      ) : (
        emptyTitle ? (
          <div className="history-group-empty">
            <span className="history-group-empty-icon" aria-hidden="true">{emptyIcon ?? '◈'}</span>
            <div>
              <h3>{emptyTitle}</h3>
              {emptyDescription && <p>{emptyDescription}</p>}
            </div>
          </div>
        ) : (
          <p className="history-empty">{emptyMessage}</p>
        )
      )}
    </section>
  )
}

function findComparablePrevious(records: AnalysisSummary[], current: AnalysisSummary) {
  return records
    .filter((record) => record.role === current.role && record.company === current.company && record.cvVersion !== current.cvVersion && record.createdAt < current.createdAt)
    .sort((a, b) => b.createdAt - a.createdAt)[0]
}

type HistoryItemProps = {
  role: string
  company: string
  cvFileName: string
  cvVersion: string
  date: string
  score: number
  previousScore?: number
  comparablePreviousId?: string
  onOpen: () => void
  onCompare: (previousId: string) => void
  onDelete: () => void
}

function HistoryItem({ role, company, cvFileName, cvVersion, date, score, previousScore, comparablePreviousId, onOpen, onCompare, onDelete }: HistoryItemProps) {
  const scoreDelta = previousScore === undefined ? undefined : score - previousScore
  const scoreChangeState = scoreDelta === undefined ? undefined : scoreDelta > 0 ? 'up' : scoreDelta < 0 ? 'down' : 'neutral'
  const scoreChangeIcon = scoreChangeState === 'up' ? '↑' : scoreChangeState === 'down' ? '↓' : '→'

  return (
    <article className={`history-item ${getScoreClass(score)}`}>
      <div className="history-item-copy">
        <h3>{role}</h3>
        <p>{company}</p>
        <small className="history-item-meta">
          <span className="history-item-date"><span aria-hidden="true">◷</span> {date}</span>
          <span className="history-meta-separator" aria-hidden="true">·</span>
          <span className="history-item-cv">{cvFileName}</span>
          <span className="history-meta-separator" aria-hidden="true">·</span>
          <span className="history-item-version">{cvVersion}</span>
        </small>
        {scoreDelta !== undefined && scoreChangeState && (
          <strong className={`score-change ${scoreChangeState}`}>
            {scoreChangeIcon} {Math.abs(scoreDelta)} puntos desde el CV anterior
          </strong>
        )}
      </div>
      <div className="history-score-zone">
        <div className={`history-score ${getScoreClass(score)}`} role="img" aria-label={`${score}% de compatibilidad`} style={{ '--score': score } as CSSProperties}><span>{score}%</span></div>
      </div>
      <div className="history-item-actions">
        {comparablePreviousId && (
          <button className="history-compare" type="button" aria-label={`Comparar ${cvVersion} con la versión anterior`} onClick={() => onCompare(comparablePreviousId)}>Comparar</button>
        )}
        <button className="history-arrow" type="button" aria-label={`Abrir análisis de ${role}`} onClick={onOpen}>→</button>
        <button className="history-delete" type="button" aria-label={`Eliminar análisis de ${role}`} onClick={onDelete}>×</button>
      </div>
    </article>
  )
}
