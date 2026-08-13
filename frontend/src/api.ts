import type { AnalysisMode, HistoryRecord } from './types'

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

type ApiErrorBody = { message?: unknown }

async function request(path: string, init: RequestInit, connectionMessage: string, fallbackMessage: string) {
  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, init)
  } catch {
    throw new Error(connectionMessage)
  }

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null) as ApiErrorBody | null
    const message = typeof errorBody?.message === 'string' ? errorBody.message : fallbackMessage
    throw new Error(message)
  }

  return response
}

async function requestJson<T>(path: string, init: RequestInit, connectionMessage: string, fallbackMessage: string): Promise<T> {
  const response = await request(path, init, connectionMessage, fallbackMessage)
  return response.json() as Promise<T>
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

  return requestJson<HistoryRecord>(
    '/api/analyses',
    { method: 'POST', body: formData },
    `No se pudo conectar con el backend en ${API_URL}.`,
    'No se pudo guardar el análisis.',
  )
}

export async function getAnalyses(): Promise<HistoryRecord[]> {
  const records = await requestJson<Array<HistoryRecord & { createdAt: string | number }>>(
    '/api/analyses',
    {},
    `No se pudo conectar con el historial en ${API_URL}. Comprueba que Spring Boot esté iniciado.`,
    'No se pudo cargar el historial.',
  )
  return records.map((record) => ({ ...record, createdAt: typeof record.createdAt === 'string' ? new Date(record.createdAt).getTime() : record.createdAt }))
}

export async function deleteAnalysis(id: string): Promise<void> {
  await request(
    `/api/analyses/${encodeURIComponent(id)}`,
    { method: 'DELETE' },
    `No se pudo conectar con el backend en ${API_URL}.`,
    'No se pudo eliminar el análisis.',
  )
}
