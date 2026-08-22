import { useEffect, useRef, useState } from 'react'
import { AnalyzerPage, type AnalyzerInitialOffer } from '../components/templates/AnalyzerPage'
import { CareerMultiversePage } from './CareerMultiversePage'
import { HistoryPage, HistoryDetail } from './HistoryPage'
import { HomePage } from './HomePage'
import { normalizeRoute, parseRoute, type AppRoute } from '../routes/routes'
import { useTheme } from '../lib/hooks/useTheme'
import type { AnalysisResponse, CareerMultiverseRequest, CareerMultiverseResponse, CareerPathType, HistoryRecord } from '../lib/types/types'
import { generateCareerMultiverse, warmUpBackend } from '../services/api'
import { toUserFacingCareerMultiverseError } from '../services/errorMessages'
import type { AnalysisErrorView } from '../services/errorMessages'

type CareerState = {
  request: CareerMultiverseRequest | null
  response: CareerMultiverseResponse | null
  error: AnalysisErrorView | null
  isLoading: boolean
}

function App() {
  const { theme, toggleTheme } = useTheme()
  const [route, setRoute] = useState<AppRoute>(() => parseRoute(window.location.pathname))
  const [initialOffer, setInitialOffer] = useState<AnalyzerInitialOffer | null>(null)
  const [latestAnalysisResult, setLatestAnalysisResult] = useState<AnalysisResponse | null>(null)
  const [selectedCareerPathType, setSelectedCareerPathType] = useState<CareerPathType>('NATURAL')
  const [careerState, setCareerState] = useState<CareerState>({
    request: null,
    response: null,
    error: null,
    isLoading: false,
  })
  const careerAbortRef = useRef<AbortController | null>(null)

  function navigate(nextPath: string, replace = false) {
    const normalized = normalizeRoute(nextPath)
    if (window.location.pathname !== normalized) {
      if (replace) window.history.replaceState({}, '', normalized)
      else window.history.pushState({}, '', normalized)
    }
    setRoute(parseRoute(normalized))
  }

  useEffect(() => {
    const normalized = normalizeRoute(window.location.pathname)
    if (window.location.pathname !== normalized) {
      window.history.replaceState({}, '', normalized)
      setRoute(parseRoute(normalized))
    }
    const handlePopState = () => setRoute(parseRoute(window.location.pathname))
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }, [route])

  useEffect(() => {
    void warmUpBackend()
  }, [])

  useEffect(() => () => {
    careerAbortRef.current?.abort()
    careerAbortRef.current = null
  }, [])

  function handleReanalyzeFromRecord(record: HistoryRecord) {
    setInitialOffer({
      mode: record.mode,
      jobDescription: record.jobDescription,
    })
    navigate('/analizar')
  }

  async function runCareerMultiverse(request: CareerMultiverseRequest) {
    careerAbortRef.current?.abort()
    const controller = new AbortController()
    careerAbortRef.current = controller
    setSelectedCareerPathType('NATURAL')
    setCareerState({
      request,
      response: null,
      error: null,
      isLoading: true,
    })

    try {
      const response = await generateCareerMultiverse(request, controller.signal)
      setCareerState({
        request,
        response,
        error: null,
        isLoading: false,
      })
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') return
      setCareerState({
        request,
        response: null,
        error: toUserFacingCareerMultiverseError(requestError),
        isLoading: false,
      })
    } finally {
      if (careerAbortRef.current === controller) {
        careerAbortRef.current = null
      }
    }
  }

  function handleExploreCareer(request: CareerMultiverseRequest) {
    navigate('/multiverso')
    void runCareerMultiverse(request)
  }

  function handleRetryCareer() {
    if (careerState.request) void runCareerMultiverse(careerState.request)
  }

  if (route.name === 'home') {
    return (
      <HomePage
        theme={theme}
        onToggleTheme={toggleTheme}
        onNavigate={navigate}
      />
    )
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

  if (route.name === 'multiverse') {
    return (
      <CareerMultiversePage
        theme={theme}
        onToggleTheme={toggleTheme}
        onNavigate={navigate}
        request={careerState.request}
        response={careerState.response}
        error={careerState.error}
        isLoading={careerState.isLoading}
        selectedPathType={selectedCareerPathType}
        onSelectPath={setSelectedCareerPathType}
        onRetry={handleRetryCareer}
        hasAnalysisResult={latestAnalysisResult !== null}
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
      initialResult={latestAnalysisResult}
      onResultChange={setLatestAnalysisResult}
      onExploreCareer={handleExploreCareer}
    />
  )
}

export default App
