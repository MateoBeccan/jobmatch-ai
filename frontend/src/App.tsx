import { useEffect, useRef, useState } from 'react'
import { createAnalysis, deleteAnalysis, getAnalyses } from './api'
import type { AnalysisMode, AnalysisResponse, HistoryRecord } from './types'

const MAX_FILE_SIZE = 5 * 1024 * 1024

function normalizeRoute(pathname: string) {
  if (pathname === '/' || pathname === '/historial') return '/historial'
  if (pathname === '/analizar') return '/analizar'
  if (/^\/analisis\/[^/]+$/.test(pathname)) return pathname
  return '/historial'
}

function formatHistoryDate(createdAt: number) {
  const minutes = Math.max(0, Math.floor((Date.now() - createdAt) / 60000))
  if (minutes < 1) return 'Ahora'
  if (minutes < 60) return `Hace ${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `Hace ${hours} h`
  const days = Math.floor(hours / 24)
  if (days < 7) return `Hace ${days} días`
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' }).format(createdAt)
}

function App() {
  const [route, setRoute] = useState(() => normalizeRoute(window.location.pathname))
  const [cvFile, setCvFile] = useState<File | null>(null)
  const [jobImage, setJobImage] = useState<File | null>(null)
  const [jobDescription, setJobDescription] = useState('')
  const [mode, setMode] = useState<AnalysisMode>('text')
  const [result, setResult] = useState<AnalysisResponse | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [history, setHistory] = useState<HistoryRecord[]>([])
  const [historyError, setHistoryError] = useState('')
  const [historyLoading, setHistoryLoading] = useState(true)
  const cvInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)

  function navigate(nextRoute: string, replace = false) {
    const normalizedRoute = normalizeRoute(nextRoute)
    if (window.location.pathname !== normalizedRoute) {
      if (replace) window.history.replaceState({}, '', normalizedRoute)
      else window.history.pushState({}, '', normalizedRoute)
    }
    setRoute(normalizedRoute)
  }

  function loadHistory() {
    setHistoryLoading(true)
    setHistoryError('')
    getAnalyses().then(setHistory).catch((requestError) => {
      setHistoryError(requestError instanceof Error ? requestError.message : 'No se pudo cargar el historial.')
    }).finally(() => setHistoryLoading(false))
  }

  useEffect(() => {
    if (window.location.pathname !== route) window.history.replaceState({}, '', route)
    const handlePopState = () => setRoute(normalizeRoute(window.location.pathname))
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [route])

  useEffect(() => { loadHistory() }, [])

  const detailId = route.startsWith('/analisis/') ? decodeURIComponent(route.slice('/analisis/'.length)) : ''
  const selectedRecord = detailId ? history.find((record) => record.id === detailId) : undefined

  useEffect(() => {
    if (selectedRecord && !result) openHistoryItem(selectedRecord)
  }, [selectedRecord, result])

  function validateFile(file: File, kind: 'cv' | 'image') {
    if (file.size > MAX_FILE_SIZE) {
      return kind === 'cv' ? 'El CV no puede superar los 5 MB.' : 'La imagen no puede superar los 5 MB.'
    }

    if (kind === 'cv' && (file.type !== 'application/pdf' || !file.name.toLowerCase().endsWith('.pdf'))) {
      return 'El CV debe ser un archivo PDF.'
    }

    if (kind === 'image' && !['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
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
    if (nextMode === 'text') setJobImage(null)
    else setJobDescription('')
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
    try {
      const record = await createAnalysis(cvFile, mode, jobDescription.trim(), jobImage)
      setResult(record.result)
      setHistory((current) => [record, ...current.filter((item) => item.id !== record.id)])
      navigate(`/analisis/${record.id}`)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'No se pudo completar el análisis.')
    } finally {
      setIsLoading(false)
    }
  }

  function resetForm() {
    setCvFile(null)
    setJobImage(null)
    setJobDescription('')
    setResult(null)
    setError('')
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

  function openHistoryItem(record: HistoryRecord) {
    navigate(`/analisis/${record.id}`)
    setMode(record.mode)
    setJobDescription(record.jobDescription)
    setResult(record.result)
    setError('')
    setCvFile(null)
  }

  if (route === '/historial') return <HistoryScreen records={history} loading={historyLoading} error={historyError} onRetry={loadHistory} onAnalyze={openAnalyzer} onOpenRecord={openHistoryItem} onDelete={async (id) => { await deleteAnalysis(id); setHistory((current) => current.filter((item) => item.id !== id)) }} />
  if (route.startsWith('/analisis/') && !result) return <RouteStatus loading={historyLoading} onBack={openHistory} />
  if (isLoading) return <LoadingScreen />

  return (
    <main className={`page-shell ${result ? 'has-results' : ''}`}>
      <header className="app-header">
        <button className="back-button" type="button" aria-label="Volver al historial" onClick={openHistory}>←</button>
        <button className="app-title" type="button" onClick={openHistory}>CV Matcher</button>
        <div className="desktop-brand">JobMatch <b>AI</b></div>
        <nav className="top-links" aria-label="Navegación principal">
          <button type="button" onClick={openHistory}>Historial</button>
          <button className="active" type="button">Analizar CV</button>
        </nav>
      </header>

      {!result && (
        <section className="evaluation-intro" id="analizar">
          <span className="intro-kicker">CV Matcher</span>
          <h1>Nueva Evaluación</h1>
          <p>Sube el currículum del candidato y describe el perfil buscado para obtener un análisis detallado de compatibilidad impulsado por IA.</p>
        </section>
      )}

      <form className="workspace" onSubmit={handleSubmit}>
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
          <input ref={cvInputRef} className="visually-hidden" type="file" accept="application/pdf,.pdf" onChange={(event) => handleCvChange(event.target.files?.[0])} />
        </section>

        <section className="form-card offer-card">
          <div className="section-label-row"><span className="step-number">02</span><h2>Descripción de la oferta</h2><span className="character-count">{mode === 'text' ? `${jobDescription.length} / 2000` : 'Imagen'}</span></div>
          <div className="mode-switch" role="tablist" aria-label="Formato de la oferta">
            <button type="button" role="tab" aria-selected={mode === 'text'} className={mode === 'text' ? 'active' : ''} onClick={() => changeMode('text')}>Pegar texto</button>
            <button type="button" role="tab" aria-selected={mode === 'image'} className={mode === 'image' ? 'active' : ''} onClick={() => changeMode('image')}>Subir imagen</button>
          </div>
          {mode === 'text' ? (
            <textarea value={jobDescription} onChange={(event) => setJobDescription(event.target.value)} maxLength={2000} placeholder="Pega aquí la descripción del puesto, responsabilidades, requisitos técnicos y habilidades blandas necesarias..." aria-label="Descripción de la oferta laboral" />
          ) : (
            <>
              <button className={`image-picker ${jobImage ? 'has-file' : ''}`} type="button" onClick={() => imageInputRef.current?.click()}>
                <span className="image-icon" aria-hidden="true">▧</span>
                <strong>{jobImage ? jobImage.name : 'Selecciona una captura de la oferta'}</strong>
                <small>PNG, JPEG o WEBP · Máx. 5 MB</small>
              </button>
              <input ref={imageInputRef} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => handleImageChange(event.target.files?.[0])} />
            </>
          )}
          <div className="field-footer"><span>Solo se utiliza para este análisis.</span><span className="secure-label">⌁ Datos protegidos</span></div>
        </section>

        {error && <div className="alert" role="alert" aria-live="assertive"><span>!</span><p>{error}</p></div>}
        <button className="submit-button" type="submit" disabled={isLoading}><span className="sparkle">✦</span> Analizar con IA <span className="submit-arrow">→</span></button>
      </form>

      {result && <Results result={result} onReset={resetForm} onHistory={openHistory} />}
      {!result && <p className="privacy-note"><span>✦</span> Tu información no se almacena ni se comparte.</p>}
      {!result && <BottomNav active="analyze" onHistory={openHistory} onAnalyze={openAnalyzer} />}
    </main>
  )
}

function RouteStatus({ loading, onBack }: { loading: boolean; onBack: () => void }) {
  return <main className="route-status"><div><span className="intro-kicker">CV MATCHER</span><h1>{loading ? 'Cargando análisis…' : 'Análisis no encontrado'}</h1><p>{loading ? 'Estamos recuperando el resultado guardado.' : 'Este análisis ya no existe o fue eliminado.'}</p><button type="button" onClick={onBack}>Volver al historial</button></div></main>
}

function HistoryScreen({ records, loading, error, onRetry, onAnalyze, onOpenRecord, onDelete }: { records: HistoryRecord[]; loading: boolean; error: string; onRetry: () => void; onAnalyze: () => void; onOpenRecord: (record: HistoryRecord) => void; onDelete: (id: string) => Promise<void> }) {
  const [query, setQuery] = useState('')
  const normalizedQuery = query.trim().toLowerCase()
  const visibleRecords = records.filter((record) => `${record.role} ${record.company} ${record.cvFileName}`.toLowerCase().includes(normalizedQuery))
  const recent = visibleRecords.filter((record) => Date.now() - record.createdAt < 7 * 24 * 60 * 60 * 1000)
  const older = visibleRecords.filter((record) => Date.now() - record.createdAt >= 7 * 24 * 60 * 60 * 1000)

  return (
    <main className="history-shell">
      <header className="history-header">
        <h1>Historial de Análisis</h1>
        <button className="menu-button" type="button" aria-label="Crear nuevo análisis" onClick={onAnalyze}><span className="menu-icon">☰</span><span className="menu-label">+ Nueva evaluación</span></button>
      </header>

      <label className="history-search">
        <span aria-hidden="true">⌕</span>
        <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por rol o empresa..." aria-label="Buscar análisis" />
      </label>
      {loading && <div className="history-status" role="status"><span className="spinner history-spinner" /> Cargando tus análisis...</div>}
      {error && <div className="history-status error" role="alert"><p>{error}</p><button type="button" onClick={onRetry}>Reintentar</button></div>}

      <HistoryGroup title="Recientes" records={recent} allRecords={records} onOpen={onOpenRecord} onDelete={onDelete} emptyMessage="Tus análisis aparecerán aquí después de completar una evaluación." />
      <HistoryGroup title="Anteriores" records={older} allRecords={records} onOpen={onOpenRecord} onDelete={onDelete} emptyMessage="Todavía no tienes análisis anteriores." older />
      <button className="new-analysis-button" type="button" onClick={onAnalyze}><span>+</span> Nueva evaluación</button>
    </main>
  )
}

function HistoryGroup({ title, records, allRecords, onOpen, onDelete, emptyMessage, older = false }: { title: string; records: HistoryRecord[]; allRecords: HistoryRecord[]; onOpen: (record: HistoryRecord) => void; onDelete: (id: string) => Promise<void>; emptyMessage: string; older?: boolean }) {
  return (
    <section className={`history-section ${older ? 'older-section' : ''}`}>
      <h2>{title}</h2>
      {records.length > 0 ? <div className="history-list">{records.map((record) => { const previous = allRecords.find((item) => item.role === record.role && item.createdAt < record.createdAt); return <HistoryItem key={record.id} {...record} date={formatHistoryDate(new Date(record.createdAt).getTime())} previousScore={previous?.score} onOpen={() => onOpen(record)} onDelete={() => onDelete(record.id)} /> })}</div> : <p className="history-empty">{emptyMessage}</p>}
    </section>
  )
}

function HistoryItem({ role, company, cvFileName, cvVersion, date, score, previousScore, onOpen, onDelete }: { role: string; company: string; cvFileName: string; cvVersion: string; date: string; score: number; previousScore?: number; onOpen: () => void; onDelete: () => Promise<void> }) {
  return (
    <article className={`history-item ${score < 70 ? 'muted' : ''}`}>
      <div className="history-item-copy"><h3>{role}</h3><p>{company}</p><small><span>◷</span> {date} · {cvFileName} · {cvVersion}</small>{previousScore !== undefined && <strong className={`score-change ${score >= previousScore ? 'up' : 'down'}`}>{score >= previousScore ? '↑' : '↓'} {Math.abs(score - previousScore)} puntos desde el CV anterior</strong>}</div>
      <div className="history-score" style={{ '--score': score } as React.CSSProperties}><span>{score}%</span></div>
      <button className="history-arrow" type="button" aria-label={`Abrir análisis de ${role}`} onClick={onOpen}>→</button>
      <button className="history-delete" type="button" aria-label={`Eliminar análisis de ${role}`} onClick={() => void onDelete()}>×</button>
    </article>
  )
}

function LoadingScreen() {
  return (
    <main className="loading-shell">
      <header className="loading-header">JobMatch <b>AI</b></header>
      <section className="loading-card" aria-live="polite">
        <div className="loading-ring"><span>▥</span></div>
        <p className="loading-kicker">ANÁLISIS EN CURSO</p>
        <h1>Analizando tu perfil...</h1>
        <p className="loading-copy">Nuestra IA está comparando tus habilidades con los requisitos de la oferta.</p>
        <div className="loading-steps"><span className="done">✓</span> Extrayendo experiencia</div>
        <div className="loading-steps current"><span>↻</span> Mapeando competencias</div>
        <div className="loading-steps pending"><span>⌛</span> Calculando JobMatch</div>
      </section>
    </main>
  )
}

function Results({ result, onReset, onHistory }: { result: AnalysisResponse; onReset: () => void; onHistory: () => void }) {
  const scoreClass = result.matchPercentage >= 80 ? 'high' : result.matchPercentage >= 60 ? 'medium' : 'low'
  const scoreTitle = result.matchPercentage >= 80 ? 'Un encaje muy prometedor' : result.matchPercentage >= 60 ? 'Un buen punto de partida' : 'Hay oportunidades para mejorar'

  return (
    <section className="results-card" aria-live="polite">
      <div className="results-heading"><div><span className="intro-kicker">RESULTADO DE TU ANÁLISIS</span><h1>Compatibilidad estimada</h1></div><button type="button" className="reset-button" onClick={onReset}>+ Nueva evaluación</button></div>
      <div className="score-row">
        <div className={`score ${scoreClass}`} style={{ '--score': result.matchPercentage } as React.CSSProperties}><strong>{result.matchPercentage}</strong><span>%</span><small>Compatibilidad</small></div>
        <div className="score-summary"><span className="summary-label">LECTURA GENERAL</span><h2>{scoreTitle}</h2><p>Este porcentaje es una estimación basada en la información de tu CV y los requisitos de la oferta.</p></div>
      </div>
      <div className="result-grid">
        <ResultList title="Habilidades que coinciden" items={result.matchingSkills} variant="positive" />
        <ResultList title="Habilidades o requisitos faltantes" items={result.missingSkills} variant="negative" />
        <ResultList title="Recomendaciones para tu postulación" items={result.recommendations} variant="neutral" numbered />
        <ResultList title="Posibles preguntas de entrevista" items={result.interviewQuestions} variant="neutral" questions />
      </div>
      <div className="result-actions"><button type="button" className="primary-action" onClick={onReset}>Analizar otra oferta</button><button type="button" className="secondary-action" onClick={onHistory}>Volver al historial</button></div>
      <BottomNav active="results" onHistory={onHistory} onAnalyze={onReset} />
    </section>
  )
}

function ResultList({ title, items, variant, numbered = false, questions = false }: { title: string; items: string[]; variant: 'positive' | 'negative' | 'neutral'; numbered?: boolean; questions?: boolean }) {
  return <div className={`result-panel ${numbered ? 'numbered' : ''} ${questions ? 'questions' : ''}`}><h3><span className={`list-icon ${variant}`}>{variant === 'positive' ? '✓' : variant === 'negative' ? '!' : '✦'}</span>{title}<small>{items.length}</small></h3>{items.length > 0 ? <ul>{items.map((item, index) => <li key={`${item}-${index}`}>{numbered && <span className="item-number">{index + 1}.</span>}{item}</li>)}</ul> : <p className="empty-list">No se encontraron elementos.</p>}</div>
}

function BottomNav({ active, onHistory, onAnalyze }: { active: 'history' | 'analyze' | 'results'; onHistory: () => void; onAnalyze: () => void }) {
  return <nav className="bottom-nav" aria-label="Navegación inferior"><button className={active === 'analyze' ? 'active' : ''} type="button" onClick={onAnalyze}><span>⊕</span>Inicio</button><button className={active === 'history' ? 'active' : ''} type="button" onClick={onHistory}><span>◷</span>Historial</button><button className={active === 'results' ? 'active' : ''} type="button" onClick={onAnalyze}><span>▥</span>Análisis</button><button type="button" onClick={onHistory}><span>♙</span>Perfil</button></nav>
}

export default App
