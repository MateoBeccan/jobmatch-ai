import type { AnalysisMode, AnalysisResponse, HistoryRecord } from './types'

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

export async function analyzeJobMatch(
  cvFile: File,
  mode: 'text' | 'image',
  jobDescription: string,
  jobImage: File | null,
): Promise<AnalysisResponse> {
  const formData = new FormData()
  formData.append('cvFile', cvFile)

  if (mode === 'text') {
    formData.append('jobDescription', jobDescription)
  } else if (jobImage) {
    formData.append('jobImage', jobImage)
  }

  let response: Response
  try {
    response = await fetch(`${API_URL}/api/analyze`, {
      method: 'POST',
      body: formData,
    })
  } catch {
    throw new Error(`No se pudo conectar con el backend en ${API_URL}. Inicia el servidor Spring Boot y vuelve a intentarlo.`)
  }

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(errorBody?.message ?? 'No se pudo completar el análisis.')
  }

  return response.json() as Promise<AnalysisResponse>
}

export async function createAnalysis(
  cvFile: File,
  mode: AnalysisMode,
  jobDescription: string,
  jobImage: File | null,
  cvVersion = 'CV sin versión',
): Promise<HistoryRecord> {
  const formData = new FormData()
  formData.append('cvFile', cvFile)
  formData.append('cvVersion', cvVersion)
  if (mode === 'text') formData.append('jobDescription', jobDescription)
  else if (jobImage) formData.append('jobImage', jobImage)

  let response: Response
  try {
    response = await fetch(`${API_URL}/api/analyses`, { method: 'POST', body: formData })
  } catch {
    throw new Error(`No se pudo conectar con el backend en ${API_URL}.`)
  }
  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(errorBody?.message ?? 'No se pudo guardar el análisis.')
  }
  return response.json() as Promise<HistoryRecord>
}

export async function getAnalyses(): Promise<HistoryRecord[]> {
  let response: Response
  try {
    response = await fetch(`${API_URL}/api/analyses`)
  } catch {
    throw new Error(`No se pudo conectar con el historial en ${API_URL}. Comprueba que Spring Boot esté iniciado.`)
  }
  if (!response.ok) throw new Error(`No se pudo cargar el historial (HTTP ${response.status}).`)
  const records = await response.json() as Array<HistoryRecord & { createdAt: string | number }>
  return records.map((record) => ({ ...record, createdAt: typeof record.createdAt === 'string' ? new Date(record.createdAt).getTime() : record.createdAt }))
}

export async function deleteAnalysis(id: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/analyses/${encodeURIComponent(id)}`, { method: 'DELETE' })
  if (!response.ok) throw new Error('No se pudo eliminar el análisis.')
}
