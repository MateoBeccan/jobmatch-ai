import { useEffect, useRef, useState } from 'react'
import type { DragEvent, FormEvent } from 'react'
import { createAnalysis } from '../../services/api'
import type { AnalysisMode, AnalysisResponse, Theme } from '../../lib/types/types'
import { BottomNav } from '../atoms/BottomNav'
import { ThemeToggle } from '../atoms/ThemeToggle'
import { LoadingScreen } from '../molecules/LoadingScreen'
import { AnalysisStepper, ANALYSIS_STEPS } from '../molecules/AnalysisStepper'
import { FileUploadCard } from '../molecules/FileUploadCard'
import { Results } from '../organisms/Results'
import { IMAGE_TYPES, MAX_FILE_SIZE, MAX_JOB_DESCRIPTION_LENGTH, PDF_TYPES } from '../../lib/constants/app'
import { AppFooter } from '../atoms/AppFooter'

export type AnalyzerInitialOffer = {
  mode: AnalysisMode
  jobDescription: string
  cvFileName: string
}

type AnalyzerPageProps = {
  theme: Theme
  onToggleTheme: () => void
  onNavigate: (route: string) => void
  initialOffer?: AnalyzerInitialOffer | null
  onInitialOfferConsumed?: () => void
}

export function AnalyzerPage({ theme, onToggleTheme, onNavigate, initialOffer = null, onInitialOfferConsumed }: AnalyzerPageProps) {
  const [cvFile, setCvFile] = useState<File | null>(null)
  const [jobImage, setJobImage] = useState<File | null>(null)
  const [jobDescription, setJobDescription] = useState(initialOffer?.jobDescription ?? '')
  const [mode, setMode] = useState<AnalysisMode>(initialOffer?.mode ?? 'text')
  const [result, setResult] = useState<AnalysisResponse | null>(null)
  const [error, setError] = useState('')
  const [errorKind, setErrorKind] = useState<'validation' | 'analysis'>('validation')
  const [isLoading, setIsLoading] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [versionCount, setVersionCount] = useState(1)
  const cvInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const analysisAbortRef = useRef<AbortController | null>(null)

  const hasOffer = mode === 'text' ? jobDescription.trim().length > 0 : jobImage !== null
  const currentStep = !cvFile ? 'cv' : !hasOffer ? 'offer' : 'analysis'

  useEffect(() => {
    if (initialOffer) onInitialOfferConsumed?.()
  }, [initialOffer, onInitialOfferConsumed])

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
    setErrorKind('validation')
    setError(validationError)
    if (validationError) {
      setCvFile(null)
      if (cvInputRef.current) cvInputRef.current.value = ''
      return
    }
    setCvFile(file)
  }

  function handleImageChange(file: File | undefined) {
    if (!file) return
    const validationError = validateFile(file, 'image')
    setErrorKind('validation')
    setError(validationError)
    if (validationError) {
      setJobImage(null)
      if (imageInputRef.current) imageInputRef.current.value = ''
      return
    }
    setJobImage(file)
  }

  function handleCvRemove() {
    setCvFile(null)
    setError('')
    if (cvInputRef.current) cvInputRef.current.value = ''
  }

  function handleCvDrop(event: DragEvent<HTMLButtonElement>) {
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
  }

  async function runAnalysis() {
    if (!cvFile) return
    setError('')
    setResult(null)
    setIsLoading(true)
    analysisAbortRef.current?.abort()
    const controller = new AbortController()
    analysisAbortRef.current = controller
    try {
      const record = await createAnalysis(cvFile, mode, jobDescription.trim(), jobImage, `CV v${versionCount}`, controller.signal)
      setVersionCount((current) => current + 1)
      setResult(record.result)
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') return
      setErrorKind('analysis')
      setError(requestError instanceof Error ? requestError.message : 'No se pudo completar el análisis.')
    } finally {
      if (analysisAbortRef.current === controller) analysisAbortRef.current = null
      setIsLoading(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setResult(null)

    if (!cvFile) {
      setErrorKind('validation')
      setError('Seleccioná tu CV en PDF para continuar.')
      return
    }
    if (mode === 'text' && !jobDescription.trim()) {
      setErrorKind('validation')
      setError('Escribí la descripción de la oferta laboral.')
      return
    }
    if (mode === 'image' && !jobImage) {
      setErrorKind('validation')
      setError('Seleccioná una imagen con la oferta laboral.')
      return
    }

    await runAnalysis()
  }

  if (isLoading) return <LoadingScreen />

  return (
    <main className={`page-shell ${result ? 'has-results' : ''}`}>
      <header className="app-header">
        <button className="back-button" type="button" aria-label="Nueva evaluación" onClick={resetForm}>←</button>
        <button className="app-title" type="button" onClick={resetForm}>CV Matcher</button>
        <div className="desktop-brand">JobMatch <b>AI</b></div>
        <nav className="top-links" aria-label="Navegación principal">
          <button className="active" type="button" aria-current="page" onClick={() => onNavigate('/analizar')}>Analizar CV</button>
          <button type="button" onClick={() => onNavigate('/historial')}>Historial</button>
        </nav>
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />
      </header>

      {!result && (
        <section className="evaluation-intro" id="analizar">
          <span className="intro-kicker">CV Matcher</span>
          <h1>Nueva Evaluación</h1>
          <p>Subí el currículum del candidato y describí el perfil buscado para obtener un análisis detallado de compatibilidad impulsado por IA.</p>
          <AnalysisStepper steps={ANALYSIS_STEPS} current={currentStep} />
        </section>
      )}

      <form className="workspace" aria-busy={isLoading} aria-describedby={error ? 'form-error' : undefined} onSubmit={(event) => void handleSubmit(event)}>
        <section className="form-card candidate-card">
          <div className="section-label-row"><span className="step-number">01</span><h2>Documento del candidato</h2></div>
          {initialOffer?.cvFileName && !cvFile && (
            <p className="reuse-note">CV del análisis anterior: <b>{initialOffer.cvFileName}</b>. Volvé a subirlo para reanalizar.</p>
          )}
          <FileUploadCard
            file={cvFile ? { name: cvFile.name, size: cvFile.size } : null}
            isDragging={isDragging}
            onChange={() => cvInputRef.current?.click()}
            onRemove={handleCvRemove}
            onDrop={handleCvDrop}
            onDragOver={(event) => { event.preventDefault(); setIsDragging(true) }}
            onDragLeave={() => setIsDragging(false)}
          />
          <input ref={cvInputRef} className="visually-hidden" type="file" accept="application/pdf,.pdf" aria-label="Seleccionar CV en PDF" onChange={(event) => handleCvChange(event.target.files?.[0])} />
        </section>

        <section className="form-card offer-card">
          <div className="section-label-row"><span className="step-number">02</span><h2>Descripción de la oferta</h2><span className="character-count" id="description-count">{mode === 'text' ? `${jobDescription.length} / ${MAX_JOB_DESCRIPTION_LENGTH}` : 'Imagen'}</span></div>
          <div className="mode-switch" role="group" aria-label="Formato de la oferta">
            <button type="button" aria-pressed={mode === 'text'} className={mode === 'text' ? 'active' : ''} onClick={() => changeMode('text')}>Pegar texto</button>
            <button type="button" aria-pressed={mode === 'image'} className={mode === 'image' ? 'active' : ''} onClick={() => changeMode('image')}>Subir imagen</button>
          </div>
          {mode === 'text' ? (
            <textarea id="job-description" value={jobDescription} onChange={(event) => setJobDescription(event.target.value)} maxLength={MAX_JOB_DESCRIPTION_LENGTH} placeholder="Pegá aquí la descripción del puesto, responsabilidades, requisitos técnicos y habilidades blandas necesarias..." aria-label="Descripción de la oferta laboral" aria-describedby="description-count" />
          ) : (
            <>
              <button className={`image-picker ${jobImage ? 'has-file' : ''}`} type="button" onClick={() => imageInputRef.current?.click()}>
                <span className="image-icon" aria-hidden="true">▧</span>
                <strong>{jobImage ? jobImage.name : 'Seleccioná una captura de la oferta'}</strong>
                <small>PNG, JPEG o WEBP · Máx. 5 MB</small>
              </button>
              <input ref={imageInputRef} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" aria-label="Seleccionar imagen de la oferta" onChange={(event) => handleImageChange(event.target.files?.[0])} />
            </>
          )}
          <div className="field-footer"><span>Se utilizará para generar tu análisis.</span><span className="secure-label">⌁ Procesamiento mediante IA</span></div>
        </section>

        <aside className="ai-privacy-notice" aria-labelledby="ai-privacy-title">
          <div className="ai-privacy-icon" aria-hidden="true">i</div>
          <div>
            <h2 id="ai-privacy-title">Privacidad y procesamiento con IA</h2>
            <p>El contenido de tu CV y de la oferta laboral será enviado a un servicio externo de inteligencia artificial (Google Gemini) para realizar el análisis. No cargues información sensible que no desees procesar mediante IA.</p>
          </div>
        </aside>

        {error && (
          <div id="form-error" className={`alert ${errorKind === 'analysis' ? 'alert-analysis' : ''}`} role="alert" aria-live="assertive">
            <span>!</span>
            <p>{error}</p>
            {errorKind === 'analysis' && <button type="button" onClick={() => void runAnalysis()}>Intentar nuevamente</button>}
          </div>
        )}
        <p className="analysis-consent-note">Al continuar, el contenido cargado será procesado mediante inteligencia artificial.</p>
        <button className="submit-button" type="submit" disabled={isLoading}><span className="sparkle">✦</span> Analizar con IA <span className="submit-arrow">→</span></button>
      </form>

      {result && (
        <Results
          result={result}
          onReset={resetForm}
          onReanalyze={() => void runAnalysis()}
          onNavigate={onNavigate}
        />
      )}
      <AppFooter />
      {!result && <BottomNav active="analyze" onNavigate={onNavigate} />}
    </main>
  )
}
