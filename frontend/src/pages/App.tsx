import { useEffect, useRef, useState } from 'react'
import { createAnalysis, deleteAnalysis, getAnalyses, getAnalysis } from '../services/api'
import type { AnalysisMode, AnalysisResponse, AnalysisSummary } from '../lib/types/types'
import { BottomNav, Results } from '../components/organisms/Results'
import { HistoryScreen } from '../components/organisms/HistoryScreen'
import { LoadingScreen } from '../components/molecules/LoadingScreen'
import { ThemeToggle } from '../components/atoms/ThemeToggle'
import { decodeRouteId, normalizeRoute } from '../routes/routes'
import { IMAGE_TYPES, MAX_FILE_SIZE, MAX_JOB_DESCRIPTION_LENGTH, PDF_TYPES } from '../lib/constants/app'
import { useTheme } from '../lib/hooks/useTheme'

function App() {
  const { theme, toggleTheme } = useTheme()
  const [route, setRoute] = useState(() => normalizeRoute(window.location.pathname))
  const [cvFile, setCvFile] = useState<File | null>(null)
  const [jobImage, setJobImage] = useState<File | null>(null)
  const [jobDescription, setJobDescription] = useState('')
  const [mode, setMode] = useState<AnalysisMode>('text')
  const [result, setResult] = useState<AnalysisResponse | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [history, setHistory] = useState<AnalysisSummary[]>([])
  const [historyError, setHistoryError] = useState('')
  const [historyLoading, setHistoryLoading] = useState(true)
  const [historyHasMore, setHistoryHasMore] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const cvInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const analysisAbortRef = useRef<AbortController | null>(null)

  function navigate(nextRoute: string, replace = false) {
    const normalizedRoute = normalizeRoute(nextRoute)
    if (window.location.pathname !== normalizedRoute) {
      if (replace) window.history.replaceState({}, '', normalizedRoute)
      else window.history.pushState({}, '', normalizedRoute)
    }
    setRoute(normalizedRoute)
  }

  async function loadHistory(page = 0, append = false) {
    setHistoryLoading(true)
    if (!append) setHistoryError('')
    try {
      const response = await getAnalyses(page)
      setHistory((current) => append ? [...current, ...response.content] : response.content)
      setHistoryHasMore(response.page + 1 < response.totalPages)
    } catch (requestError) {
      setHistoryError(requestError instanceof Error ? requestError.message : 'No se pudo cargar el historial.')
    } finally {
      setHistoryLoading(false)
    }
  }

  useEffect(() => {
    if (window.location.pathname !== route) window.history.replaceState({}, '', route)
    const handlePopState = () => setRoute(normalizeRoute(window.location.pathname))
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [route])

  useEffect(() => { void loadHistory() }, [])

  const detailId = route.startsWith('/analisis/') ? decodeRouteId(route.slice('/analisis/'.length)) : ''

  useEffect(() => {
    if (!detailId) {
      setDetailLoading(false)
      setDetailError('')
      if (route === '/analizar') setResult(null)
      return
    }

    let cancelled = false
    const controller = new AbortController()
    setDetailLoading(true)
    setDetailError('')
    setResult(null)

    getAnalysis(detailId, controller.signal).then((record) => {
      if (cancelled) return
      setMode(record.mode)
      setJobDescription(record.jobDescription)
      setResult(record.result)
    }).catch((requestError) => {
      if (cancelled || (requestError instanceof DOMException && requestError.name === 'AbortError')) return
      setDetailError(requestError instanceof Error ? requestError.message : 'No se pudo cargar el análisis.')
    }).finally(() => {
      if (!cancelled) setDetailLoading(false)
    })

    return () => {
      cancelled = true
      controller.abort()
    }
  }, [detailId, route])

  function validateFile(file: File, kind: 'cv' | 'image') {
    if (file.size > MAX_FILE_SIZE) {
      return kind === 'cv' ? 'El CV no puede superar los 5 MB.' : 'La imagen no puede superar los 5 MB.'
    }

    if (kind === 'cv' && (!PDF_TYPES.has(file.type) || !file.name.toLowerCase().endsWith('.pdf'))) {
      return 'El CV debe ser un archivo PDF.'
    }

    if (kind === 'image' && !IMAGE_TYPES.has(file.type)) {
      return 'La oferta debe ser PNG, JPEG o WEBP.'
    }

    return ''
  }

  function handleCvChange(file: File | undefined) {
    if (!file) return
    const validationError = validateFile(file, 'cv')
    setError(validationError)
    if (!validationError) setCvFile(file)
  }

  function handleImageChange(file: File | undefined) {
    if (!file) return
    const validationError = validateFile(file, 'image')
    setError(validationError)
    if (!validationError) setJobImage(file)
  }

  function handleCvDrop(event: React.DragEvent<HTMLButtonElement>) {
    event.preventDefault()
    setIsDragging(false)
    handleCvChange(event.dataTransfer.files[0])
  }

  function changeMode(nextMode: AnalysisMode) {
    setMode(nextMode)
    setError('')
    if (nextMode === 'text') {
      setJobImage(null)
      if (imageInputRef.current) imageInputRef.current.value = ''
    } else {
      setJobDescription('')
    }
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setResult(null)

    if (!cvFile) {
      setError('Selecciona tu CV en PDF para continuar.')
      return
    }
    if (mode === 'text' && !jobDescription.trim()) {
      setError('Escribe la descripción de la oferta laboral.')
      return
    }
    if (mode === 'image' && !jobImage) {
      setError('Selecciona una imagen con la oferta laboral.')
      return
    }

    setIsLoading(true)
    analysisAbortRef.current?.abort()
    const controller = new AbortController()
    analysisAbortRef.current = controller
    try {
      const record = await createAnalysis(cvFile, mode, jobDescription.trim(), jobImage, undefined, controller.signal)
      setResult(record.result)
      const summary: AnalysisSummary = {
        id: record.id,
        role: record.role,
        company: record.company,
        cvFileName: record.cvFileName,
        cvVersion: record.cvVersion,
        mode: record.mode,
        score: record.score,
        createdAt: record.createdAt,
      }
      setHistory((current) => [summary, ...current.filter((item) => item.id !== record.id)])
      navigate(`/analisis/${record.id}`)
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') return
      setError(requestError instanceof Error ? requestError.message : 'No se pudo completar el análisis.')
    } finally {
      if (analysisAbortRef.current === controller) analysisAbortRef.current = null
      setIsLoading(false)
    }
  }

  function resetForm() {
    analysisAbortRef.current?.abort()
    analysisAbortRef.current = null
    setCvFile(null)
    setJobImage(null)
    setJobDescription('')
    setResult(null)
    setError('')
    setIsDragging(false)
    if (cvInputRef.current) cvInputRef.current.value = ''
    if (imageInputRef.current) imageInputRef.current.value = ''
    navigate('/analizar')
  }

  function openAnalyzer() {
    if (route === '/historial') resetForm()
    else navigate('/analizar')
    setError('')
  }

  function openHistory() {
    navigate('/historial')
  }

  function openHistoryItem(id: string) {
    navigate(`/analisis/${id}`)
    setError('')
    setCvFile(null)
  }

  async function handleDelete(id: string) {
    try {
      await deleteAnalysis(id)
      setHistory((current) => current.filter((item) => item.id !== id))
    } catch (requestError) {
      setHistoryError(requestError instanceof Error ? requestError.message : 'No se pudo eliminar el análisis.')
    }
  }

  if (route === '/historial') return <HistoryScreen records={history} loading={historyLoading} hasMore={historyHasMore} error={historyError} onRetry={() => void loadHistory()} onLoadMore={() => void loadHistory(Math.ceil(history.length / 20), true)} onAnalyze={openAnalyzer} onOpenRecord={openHistoryItem} onDelete={handleDelete} theme={theme} onToggleTheme={toggleTheme} />
  if (route.startsWith('/analisis/') && (detailLoading || !result)) return <RouteStatus loading={detailLoading} error={detailError} onBack={openHistory} />
  if (isLoading) return <LoadingScreen />

  return (
    <main className={`page-shell ${result ? 'has-results' : ''}`}>
      <header className="app-header">
        <button className="back-button" type="button" aria-label="Volver al historial" onClick={openHistory}>←</button>
        <button className="app-title" type="button" onClick={openHistory}>CV Matcher</button>
        <div className="desktop-brand">JobMatch <b>AI</b></div>
        <nav className="top-links" aria-label="Navegación principal">
          <button type="button" aria-current={route === '/historial' ? 'page' : undefined} onClick={openHistory}>Historial</button>
          <button className="active" type="button" aria-current="page">Analizar CV</button>
        </nav>
        <ThemeToggle theme={theme} onToggle={toggleTheme} />
      </header>

      {!result && (
        <section className="evaluation-intro" id="analizar">
          <span className="intro-kicker">CV Matcher</span>
          <h1>Nueva Evaluación</h1>
          <p>Sube el currículum del candidato y describe el perfil buscado para obtener un análisis detallado de compatibilidad impulsado por IA.</p>
        </section>
      )}

      <form className="workspace" aria-busy={isLoading} aria-describedby={error ? 'form-error' : undefined} onSubmit={handleSubmit}>
        <section className="form-card candidate-card">
          <div className="section-label-row"><span className="step-number">01</span><h2>Documento del candidato</h2></div>
          <button
            className={`dropzone ${cvFile ? 'has-file' : ''} ${isDragging ? 'is-dragging' : ''}`}
            type="button"
            onClick={() => cvInputRef.current?.click()}
            onDragOver={(event) => { event.preventDefault(); setIsDragging(true) }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleCvDrop}
          >
            <span className="upload-icon" aria-hidden="true">↥</span>
            <span className="dropzone-copy">
              <strong>{cvFile ? cvFile.name : 'Arrastra y suelta el CV aquí'}</strong>
              <small>{cvFile ? `${(cvFile.size / 1024 / 1024).toFixed(2)} MB · PDF` : 'o haz clic para explorar tus archivos'}</small>
            </span>
            <span className="browse-label">PDF · Máx. 5 MB</span>
          </button>
          <input ref={cvInputRef} className="visually-hidden" type="file" accept="application/pdf,.pdf" aria-label="Seleccionar CV en PDF" onChange={(event) => handleCvChange(event.target.files?.[0])} />
        </section>

        <section className="form-card offer-card">
          <div className="section-label-row"><span className="step-number">02</span><h2>Descripción de la oferta</h2><span className="character-count" id="description-count">{mode === 'text' ? `${jobDescription.length} / ${MAX_JOB_DESCRIPTION_LENGTH}` : 'Imagen'}</span></div>
          <div className="mode-switch" role="group" aria-label="Formato de la oferta">
            <button type="button" aria-pressed={mode === 'text'} className={mode === 'text' ? 'active' : ''} onClick={() => changeMode('text')}>Pegar texto</button>
            <button type="button" aria-pressed={mode === 'image'} className={mode === 'image' ? 'active' : ''} onClick={() => changeMode('image')}>Subir imagen</button>
          </div>
          {mode === 'text' ? (
            <div>
              <textarea id="job-description" value={jobDescription} onChange={(event) => setJobDescription(event.target.value)} maxLength={MAX_JOB_DESCRIPTION_LENGTH} placeholder="Pega aquí la descripción del puesto, responsabilidades, requisitos técnicos y habilidades blandas necesarias..." aria-label="Descripción de la oferta laboral" aria-describedby="description-count" />
            </div>
          ) : (
            <>
              <div>
                <button className={`image-picker ${jobImage ? 'has-file' : ''}`} type="button" onClick={() => imageInputRef.current?.click()}>
                  <span className="image-icon" aria-hidden="true">▧</span>
                  <strong>{jobImage ? jobImage.name : 'Selecciona una captura de la oferta'}</strong>
                  <small>PNG, JPEG o WEBP · Máx. 5 MB</small>
                </button>
              </div>
              <input ref={imageInputRef} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" aria-label="Seleccionar imagen de la oferta" onChange={(event) => handleImageChange(event.target.files?.[0])} />
            </>
          )}
          <div className="field-footer"><span>Solo se utiliza para este análisis.</span><span className="secure-label">⌁ Datos protegidos</span></div>
        </section>

        {error && <div id="form-error" className="alert" role="alert" aria-live="assertive"><span>!</span><p>{error}</p></div>}
        <button className="submit-button" type="submit" disabled={isLoading}><span className="sparkle">✦</span> Analizar con IA <span className="submit-arrow">→</span></button>
      </form>

      {result && <Results result={result} onReset={resetForm} onHistory={openHistory} />}
      {!result && <p className="privacy-note"><span>✦</span> Este análisis se guarda en tu historial para que puedas consultarlo o eliminarlo.</p>}
      {!result && <BottomNav active="analyze" onHistory={openHistory} onAnalyze={openAnalyzer} />}
    </main>
  )
}

function RouteStatus({ loading, error, onBack }: { loading: boolean; error: string; onBack: () => void }) {
  return <main className="route-status"><div><span className="intro-kicker">CV MATCHER</span><h1>{loading ? 'Cargando análisis…' : 'Análisis no encontrado'}</h1><p>{loading ? 'Estamos recuperando el resultado guardado.' : error || 'Este análisis ya no existe o fue eliminado.'}</p><button type="button" onClick={onBack}>Volver al historial</button></div></main>
}

export default App
