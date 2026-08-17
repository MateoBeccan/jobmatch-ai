import { useEffect, useState } from 'react'
import { AnalyzerPage, type AnalyzerInitialOffer } from '../components/templates/AnalyzerPage'
import { HistoryPage, HistoryDetail } from './HistoryPage'
import { normalizeRoute, parseRoute, type AppRoute } from '../routes/routes'
import { useTheme } from '../lib/hooks/useTheme'
import type { HistoryRecord } from '../lib/types/types'
import { warmUpBackend } from '../services/api'

function App() {
  const { theme, toggleTheme } = useTheme()
  const [route, setRoute] = useState<AppRoute>(() => parseRoute(window.location.pathname))
  const [initialOffer, setInitialOffer] = useState<AnalyzerInitialOffer | null>(null)

  function navigate(nextPath: string, replace = false) {
    const normalized = normalizeRoute(nextPath)
    if (window.location.pathname !== normalized) {
      if (replace) window.history.replaceState({}, '', normalized)
      else window.history.pushState({}, '', normalized)
    }
    setRoute(parseRoute(normalized))
  }

  useEffect(() => {
    const handlePopState = () => setRoute(parseRoute(window.location.pathname))
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  useEffect(() => {
    void warmUpBackend()
  }, [])

  function handleReanalyzeFromRecord(record: HistoryRecord) {
    setInitialOffer({
      mode: record.mode,
      jobDescription: record.jobDescription,
      cvFileName: record.cvFileName,
    })
    navigate('/analizar')
  }

  if (route.name === 'history') {
    return (
      <HistoryPage
        theme={theme}
        onToggleTheme={toggleTheme}
        onNavigate={navigate}
        onReanalyzeFromRecord={handleReanalyzeFromRecord}
      />
    )
  }

  if (route.name === 'detail') {
    return (
      <HistoryDetail
        id={route.id}
        theme={theme}
        onToggleTheme={toggleTheme}
        onNavigate={navigate}
        onReanalyzeFromRecord={handleReanalyzeFromRecord}
      />
    )
  }

  return (
    <AnalyzerPage
      theme={theme}
      onToggleTheme={toggleTheme}
      onNavigate={navigate}
      initialOffer={initialOffer}
      onInitialOfferConsumed={() => setInitialOffer(null)}
    />
  )
}

export default App
