import { useCallback, useEffect, useState } from 'react'
import { getAnalysis } from '../services/api'
import { useHistory } from '../lib/hooks/useHistory'
import type { HistoryRecord, Theme } from '../lib/types/types'
import { HistoryScreen } from '../components/organisms/HistoryScreen'
import { ComparisonView } from '../components/organisms/ComparisonView'
import { Results } from '../components/organisms/Results'
import { ThemeToggle } from '../components/atoms/ThemeToggle'
import { ErrorState } from '../components/molecules/ErrorState'
import { BottomNav } from '../components/atoms/BottomNav'
import { AppFooter } from '../components/atoms/AppFooter'

type HistoryPageProps = {
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
  onReanalyzeFromRecord: (record: HistoryRecord) => void
}

type ComparisonState = {
  previousId: string
  currentId: string
  data: { previous: HistoryRecord; current: HistoryRecord } | null
  loading: boolean
  error: string
}

export function HistoryPage({ theme, onToggleTheme, onNavigate }: HistoryPageProps) {
  const history = useHistory()
  const [comparison, setComparison] = useState<ComparisonState | null>(null)

  const handleCompare = useCallback(async (previousId: string, currentId: string) => {
    setComparison({ previousId, currentId, data: null, loading: true, error: '' })
    try {
      const [previous, current] = await Promise.all([getAnalysis(previousId), getAnalysis(currentId)])
      setComparison((state) => state ? { ...state, data: { previous, current }, loading: false } : state)
    } catch (requestError) {
      setComparison((state) => state
        ? { ...state, loading: false, error: requestError instanceof Error ? requestError.message : 'No se pudo cargar la comparación.' }
        : state)
    }
  }, [])

  if (comparison) {
    return (
      <ComparisonView
        previous={comparison.data?.previous ?? null}
        current={comparison.data?.current ?? null}
        loading={comparison.loading}
        error={comparison.error}
        onBack={() => setComparison(null)}
      />
    )
  }

  return (
    <HistoryScreen
      records={history.records}
      loading={history.loading}
      loadingMore={history.loadingMore}
      hasMore={history.hasMore}
      error={history.error}
      onRetry={() => history.loadFirstPage()}
      onLoadMore={() => history.loadMore()}
      onAnalyze={() => onNavigate('/analizar')}
      onOpenRecord={(id) => onNavigate(`/historial/${encodeURIComponent(id)}`)}
      onCompare={handleCompare}
      onDelete={history.deleteRecord}
      onNavigate={onNavigate}
      theme={theme}
      onToggleTheme={onToggleTheme}
    />
  )
}

type HistoryDetailProps = {
  id: string
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
  onReanalyzeFromRecord: (record: HistoryRecord) => void
}

export function HistoryDetail({ id, theme, onToggleTheme, onNavigate, onReanalyzeFromRecord }: HistoryDetailProps) {
  const [record, setRecord] = useState<HistoryRecord | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    getAnalysis(id)
      .then((result) => {
        if (active) setRecord(result)
      })
      .catch((requestError) => {
        if (active) setError(requestError instanceof Error ? requestError.message : 'No se pudo cargar el análisis.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [id])

  if (loading) {
    return (
      <main className="page-shell detail-shell">
        <header className="app-header"><div className="desktop-brand">JobMatch <b>AI</b></div><ThemeToggle theme={theme} onToggle={onToggleTheme} /></header>
        <div className="history-status" role="status"><span className="spinner history-spinner" /> Cargando análisis...</div>
        <BottomNav active="history" onNavigate={onNavigate} />
      </main>
    )
  }

  if (error || !record) {
    return (
      <main className="page-shell detail-shell">
        <header className="app-header"><div className="desktop-brand">JobMatch <b>AI</b></div><ThemeToggle theme={theme} onToggle={onToggleTheme} /></header>
        <ErrorState message={error || 'No se encontró el análisis.'} onRetry={() => onNavigate('/historial')} />
        <BottomNav active="history" onNavigate={onNavigate} />
      </main>
    )
  }

  return (
    <main className="page-shell detail-shell">
      <header className="app-header">
        <button className="back-button" type="button" aria-label="Volver al historial" onClick={() => onNavigate('/historial')}>←</button>
        <button className="app-title" type="button" onClick={() => onNavigate('/historial')}>Historial</button>
        <div className="desktop-brand">JobMatch <b>AI</b></div>
        <nav className="top-links" aria-label="Navegación principal">
          <button type="button" onClick={() => onNavigate('/analizar')}>Analizar CV</button>
          <button className="active" type="button" aria-current="page" onClick={() => onNavigate('/historial')}>Historial</button>
        </nav>
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />
      </header>
      <p className="detail-meta">{record.role} · {record.company} · {record.cvFileName} · {record.cvVersion}</p>
      <Results
        result={record.result}
        onReset={() => onNavigate('/analizar')}
        onReanalyze={() => onReanalyzeFromRecord(record)}
        onNavigate={onNavigate}
      />
      <AppFooter />
    </main>
  )
}
