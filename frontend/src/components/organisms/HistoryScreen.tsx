import { useState } from 'react'
import type { AnalysisSummary, Theme } from '../../lib/types/types'
import { formatHistoryDate, getScoreClass } from '../../lib/helpers/format'
import { ThemeToggle } from '../atoms/ThemeToggle'

type HistoryScreenProps = {
  records: AnalysisSummary[]
  loading: boolean
  hasMore: boolean
  error: string
  onRetry: () => void
  onLoadMore: () => void
  onAnalyze: () => void
  onOpenRecord: (id: string) => void
  onDelete: (id: string) => Promise<void>
  theme: Theme
  onToggleTheme: () => void
}

export function HistoryScreen({ records, loading, hasMore, error, onRetry, onLoadMore, onAnalyze, onOpenRecord, onDelete, theme, onToggleTheme }: HistoryScreenProps) {
  const [query, setQuery] = useState('')
  const normalizedQuery = query.trim().toLowerCase()
  const currentTime = Date.now()
  const visibleRecords = records.filter((record) => `${record.role} ${record.company} ${record.cvFileName}`.toLowerCase().includes(normalizedQuery))
  const recent = visibleRecords.filter((record) => currentTime - record.createdAt < 7 * 24 * 60 * 60 * 1000)
  const older = visibleRecords.filter((record) => currentTime - record.createdAt >= 7 * 24 * 60 * 60 * 1000)

  return (
    <main className="history-shell" aria-busy={loading}>
      <header className="history-header">
        <h1>Historial de Análisis</h1>
        <div className="history-actions"><button className="menu-button" type="button" aria-label="Crear nuevo análisis" onClick={onAnalyze}><span className="menu-icon">☰</span><span className="menu-label">+ Nueva evaluación</span></button><ThemeToggle theme={theme} onToggle={onToggleTheme} /></div>
      </header>

      <label className="history-search">
        <span aria-hidden="true">⌕</span>
        <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por rol o empresa..." aria-label="Buscar análisis" />
      </label>
      {loading && <div className="history-status" role="status"><span className="spinner history-spinner" /> Cargando tus análisis...</div>}
      {error && <div className="history-status error" role="alert"><p>{error}</p><button type="button" onClick={onRetry}>Reintentar</button></div>}

      {!loading && !error && <>
        <HistoryGroup title="Recientes" records={recent} allRecords={records} onOpen={onOpenRecord} onDelete={onDelete} emptyMessage={normalizedQuery ? 'No hay análisis recientes que coincidan con tu búsqueda.' : 'Tus análisis aparecerán aquí después de completar una evaluación.'} />
        <HistoryGroup title="Anteriores" records={older} allRecords={records} onOpen={onOpenRecord} onDelete={onDelete} emptyMessage={normalizedQuery ? 'No hay análisis anteriores que coincidan con tu búsqueda.' : 'Todavía no tienes análisis anteriores.'} older />
        {hasMore && <button className="load-more-button mx-auto mt-7 block rounded-lg border border-[var(--line)] bg-[var(--card)] px-4 py-2.5 text-[13px] font-bold text-[var(--blue)] transition-colors hover:border-[var(--blue)] hover:bg-[var(--soft-blue)] disabled:cursor-wait disabled:opacity-65" type="button" disabled={loading} onClick={onLoadMore}>Cargar más análisis</button>}
      </>}
      <button className="new-analysis-button" type="button" onClick={onAnalyze}><span>+</span> Nueva evaluación</button>
    </main>
  )
}

type HistoryGroupProps = {
  title: string
  records: AnalysisSummary[]
  allRecords: AnalysisSummary[]
  onOpen: (id: string) => void
  onDelete: (id: string) => Promise<void>
  emptyMessage: string
  older?: boolean
}

function HistoryGroup({ title, records, allRecords, onOpen, onDelete, emptyMessage, older = false }: HistoryGroupProps) {
  return (
    <section className={`history-section ${older ? 'older-section' : ''}`}>
      <h2>{title}</h2>
      {records.length > 0 ? <div className="history-list">{records.map((record) => { const previous = allRecords.find((item) => item.role === record.role && item.createdAt < record.createdAt); return <HistoryItem key={record.id} {...record} date={formatHistoryDate(record.createdAt)} previousScore={previous?.score} onOpen={() => onOpen(record.id)} onDelete={() => onDelete(record.id)} /> })}</div> : <p className="history-empty">{emptyMessage}</p>}
    </section>
  )
}

type HistoryItemProps = {
  role: string
  company: string
  cvFileName: string
  cvVersion: string
  date: string
  score: number
  previousScore?: number
  onOpen: () => void
  onDelete: () => Promise<void>
}

function HistoryItem({ role, company, cvFileName, cvVersion, date, score, previousScore, onOpen, onDelete }: HistoryItemProps) {
  return (
    <article className={`history-item ${getScoreClass(score)}`}>
      <div className="history-item-copy"><h3>{role}</h3><p>{company}</p><small><span aria-hidden="true">◷</span> {date} · {cvFileName} · {cvVersion}</small>{previousScore !== undefined && <strong className={`score-change ${score >= previousScore ? 'up' : 'down'}`}>{score >= previousScore ? '↑' : '↓'} {Math.abs(score - previousScore)} puntos desde el CV anterior</strong>}</div>
      <div className={`history-score ${getScoreClass(score)}`} role="img" aria-label={`${score}% de compatibilidad`} style={{ '--score': score } as React.CSSProperties}><span>{score}%</span></div>
      <button className="history-arrow" type="button" aria-label={`Abrir análisis de ${role}`} onClick={onOpen}>→</button>
      <button className="history-delete" type="button" aria-label={`Eliminar análisis de ${role}`} onClick={() => void onDelete()}>×</button>
    </article>
  )
}
