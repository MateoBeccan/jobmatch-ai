import { useRef, useState } from 'react'
import { analyzeJobMatch } from './api'
import type { AnalysisMode, AnalysisResponse } from './types'

const MAX_FILE_SIZE = 5 * 1024 * 1024

function App() {
  const [cvFile, setCvFile] = useState<File | null>(null)
  const [jobImage, setJobImage] = useState<File | null>(null)
  const [jobDescription, setJobDescription] = useState('')
  const [mode, setMode] = useState<AnalysisMode>('text')
  const [result, setResult] = useState<AnalysisResponse | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const cvInputRef = useRef<HTMLInputElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)

  function validateFile(file: File, kind: 'cv' | 'image') {
    if (file.size > MAX_FILE_SIZE) {
      return kind === 'cv'
        ? 'El CV no puede superar los 5 MB.'
        : 'La imagen no puede superar los 5 MB.'
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
      const response = await analyzeJobMatch(cvFile, mode, jobDescription.trim(), jobImage)
      setResult(response)
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
  }

  return (
    <main className="page-shell">
      <nav className="topbar">
        <a className="brand" href="/" aria-label="Jobmatch AI inicio">
          <span className="brand-mark">J</span>
          <span>jobmatch<span className="brand-accent">.ai</span></span>
        </a>
        <span className="status-pill"><span className="status-dot" /> Análisis profesional</span>
      </nav>

      <section className="hero">
        <div className="eyebrow"><span /> Encuentra tu siguiente oportunidad</div>
        <h1>¿Qué tan bien encaja<br /><em>tu perfil?</em></h1>
        <p className="hero-copy">Compara tu experiencia con una oferta laboral y recibe una lectura clara para postularte con más confianza.</p>
      </section>

      <form className="workspace" onSubmit={handleSubmit}>
        <section className="form-card">
          <div className="section-heading">
            <span className="step-number">01</span>
            <div><h2>Tu CV</h2><p>Sube el documento que quieres analizar.</p></div>
          </div>
          <button className={`dropzone ${cvFile ? 'has-file' : ''}`} type="button" onClick={() => cvInputRef.current?.click()}>
            <span className="upload-icon">↑</span>
            <span className="dropzone-copy">
              <strong>{cvFile ? cvFile.name : 'Arrastra tu CV aquí'}</strong>
              <small>{cvFile ? `${(cvFile.size / 1024 / 1024).toFixed(2)} MB · PDF` : 'o haz clic para buscar · PDF hasta 5 MB'}</small>
            </span>
            <span className="browse-label">Examinar</span>
          </button>
          <input ref={cvInputRef} className="visually-hidden" type="file" accept="application/pdf,.pdf" onChange={(event) => handleCvChange(event.target.files?.[0])} />
        </section>

        <section className="form-card offer-card">
          <div className="section-heading">
            <span className="step-number">02</span>
            <div><h2>La oferta laboral</h2><p>Cuéntanos qué requisitos quieres comparar.</p></div>
          </div>
          <div className="mode-switch" role="tablist" aria-label="Formato de oferta">
            <button type="button" className={mode === 'text' ? 'active' : ''} onClick={() => changeMode('text')}>Pegar texto</button>
            <button type="button" className={mode === 'image' ? 'active' : ''} onClick={() => changeMode('image')}>Subir imagen</button>
          </div>
          {mode === 'text' ? (
            <textarea value={jobDescription} onChange={(event) => setJobDescription(event.target.value)} maxLength={15000} placeholder="Pega aquí la descripción completa del puesto..." />
          ) : (
            <>
              <button className={`image-picker ${jobImage ? 'has-file' : ''}`} type="button" onClick={() => imageInputRef.current?.click()}>
                <span className="image-icon">▧</span>
                <strong>{jobImage ? jobImage.name : 'Selecciona una captura de la oferta'}</strong>
                <small>PNG, JPEG o WEBP · hasta 5 MB</small>
              </button>
              <input ref={imageInputRef} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => handleImageChange(event.target.files?.[0])} />
            </>
          )}
          <div className="field-footer"><span>La información se utiliza únicamente para este análisis.</span>{mode === 'text' && <span>{jobDescription.length.toLocaleString('es-ES')} / 15.000</span>}</div>
        </section>

        {error && <div className="alert" role="alert"><span>!</span>{error}</div>}
        <button className="submit-button" type="submit" disabled={isLoading}>
          {isLoading ? <><span className="spinner" /> Analizando tu perfil...</> : <>Analizar compatibilidad <span>→</span></>}
        </button>
      </form>

      {result && <Results result={result} onReset={resetForm} />}
      {!result && <p className="privacy-note"><span>✦</span> Tu información no se almacena ni se comparte.</p>}
    </main>
  )
}

function Results({ result, onReset }: { result: AnalysisResponse; onReset: () => void }) {
  const scoreClass = result.matchPercentage >= 80 ? 'high' : result.matchPercentage >= 60 ? 'medium' : 'low'

  return (
    <section className="results-card" aria-live="polite">
      <div className="results-header"><div><div className="eyebrow"><span /> Resultado del análisis</div><h2>Tu compatibilidad</h2></div><button type="button" className="reset-button" onClick={onReset}>Nuevo análisis</button></div>
      <div className="score-row">
        <div className={`score ${scoreClass}`}><strong>{result.matchPercentage}</strong><span>%</span><small>compatibilidad</small></div>
        <div className="score-summary"><h3>{result.matchPercentage >= 80 ? 'Un encaje muy prometedor' : result.matchPercentage >= 60 ? 'Un buen punto de partida' : 'Hay oportunidades para mejorar'}</h3><p>Revisa los detalles para entender dónde destaca tu perfil y qué puedes reforzar.</p></div>
      </div>
      <div className="result-grid">
        <ResultList title="Lo que coincide" items={result.matchingSkills} variant="positive" />
        <ResultList title="Lo que falta" items={result.missingSkills} variant="negative" />
        <ResultList title="Recomendaciones" items={result.recommendations} variant="neutral" />
        <ResultList title="Preguntas para preparar" items={result.interviewQuestions} variant="neutral" />
      </div>
    </section>
  )
}

function ResultList({ title, items, variant }: { title: string; items: string[]; variant: 'positive' | 'negative' | 'neutral' }) {
  return <div className="result-panel"><h3><span className={`list-icon ${variant}`}>{variant === 'positive' ? '✓' : variant === 'negative' ? '−' : '✦'}</span>{title}<small>{items.length}</small></h3>{items.length > 0 ? <ul>{items.map((item, index) => <li key={`${item}-${index}`}>{item}</li>)}</ul> : <p className="empty-list">No se encontraron elementos.</p>}</div>
}

export default App
