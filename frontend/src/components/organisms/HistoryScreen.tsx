import { useCallback, useMemo, useState } from 'react'
import type { CSSProperties } from 'react'
import type { AnalysisSummary, HistorySort, ScoreRange, Theme } from '../../lib/types/types'
import { formatHistoryDate, getScoreClass } from '../../lib/helpers/format'
import { computeHistoryStats, filterByScoreRange } from '../../lib/helpers/analysis'
import { ThemeToggle } from '../atoms/ThemeToggle'
import { BottomNav } from '../atoms/BottomNav'
import { EmptyState } from '../molecules/EmptyState'
import { ErrorState } from '../molecules/ErrorState'
import { ConfirmDialog } from '../molecules/ConfirmDialog'
import { AppFooter } from '../atoms/AppFooter'

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

  return (
    <main className="history-shell" aria-busy={loading}>
      <header className="history-header">
        <h1>Historial de Análisis</h1>
        <div className="history-actions">
          <button className="menu-button" type="button" aria-label="Crear nuevo análisis" onClick={onAnalyze}>
            <span className="menu-icon">☰</span><span className="menu-label">+ Nueva evaluación</span>
          </button>
          <ThemeToggle theme={theme} onToggle={onToggleTheme} />
        </div>
      </header>

      <section className="history-stats" aria-label="Estadísticas de tus análisis">
        <HistoryStat label="Ofertas analizadas" value={String(stats.total)} />
        <HistoryStat label="Compatibilidad promedio" value={`${stats.averageScore}%`} />
        <HistoryStat label="Mejor resultado" value={`${stats.bestScore}%`} />
        {typeof stats.trendDelta === 'number' && (
          <HistoryStat label="Tendencia" value={`${stats.trendDelta >= 0 ? '↑' : '↓'} ${Math.abs(stats.trendDelta)}%`} trend={stats.trendDelta >= 0 ? 'up' : 'down'} />
        )}
      </section>

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

      {loading && <div className="history-status" role="status"><span className="spinner history-spinner" /> Cargando tus análisis...</div>}
      {error && <ErrorState message={error} onRetry={onRetry} />}

      {!loading && !error && filtered.length === 0 && (
        <EmptyState
          icon="◈"
          title="Todavía no tenés análisis"
          description="Analizá una oferta laboral para empezar a construir tu historial."
          actionLabel="Nuevo análisis"
          onAction={onAnalyze}
        />
      )}

      {!loading && !error && filtered.length > 0 && (
        <>
          {grouped ? (
            <>
              <HistoryGroup title="Recientes" records={grouped.recent} allRecords={records} onOpen={onOpenRecord} onCompare={onCompare} requestDelete={requestDelete} emptyMessage="No hay análisis recientes que coincidan con tu búsqueda." />
              <HistoryGroup title="Anteriores" records={grouped.older} allRecords={records} onOpen={onOpenRecord} onCompare={onCompare} requestDelete={requestDelete} emptyMessage="No hay análisis anteriores que coincidan con tu búsqueda." older />
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

      <button className="new-analysis-button" type="button" onClick={onAnalyze}><span>+</span> Nueva evaluación</button>
      <AppFooter />
      <BottomNav active="history" onNavigate={onNavigate} onNewAnalysis={onAnalyze} />
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

function HistoryStat({ label, value, trend }: { label: string; value: string; trend?: 'up' | 'down' }) {
  return (
    <div className={`history-stat${trend ? ` trend ${trend}` : ''}`}>
      <strong>{value}</strong>
      <span>{label}</span>
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
  older?: boolean
}

function HistoryGroup({ title, records, allRecords, onOpen, onCompare, requestDelete, emptyMessage, older = false }: HistoryGroupProps) {
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
        <p className="history-empty">{emptyMessage}</p>
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
  return (
    <article className={`history-item ${getScoreClass(score)}`}>
      <div className="history-item-copy">
        <h3>{role}</h3>
        <p>{company}</p>
        <small><span aria-hidden="true">◷</span> {date} · {cvFileName} · {cvVersion}</small>
        {previousScore !== undefined && (
          <strong className={`score-change ${score >= previousScore ? 'up' : 'down'}`}>
            {score >= previousScore ? '↑' : '↓'} {Math.abs(score - previousScore)} puntos desde el CV anterior
          </strong>
        )}
      </div>
      <div className={`history-score ${getScoreClass(score)}`} role="img" aria-label={`${score}% de compatibilidad`} style={{ '--score': score } as CSSProperties}><span>{score}%</span></div>
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
