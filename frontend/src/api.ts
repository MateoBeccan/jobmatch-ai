import type { AnalysisHistoryPage, AnalysisMode, AnalysisSummary, HistoryRecord } from './types'

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')
const API_USERNAME = import.meta.env.VITE_API_USERNAME ?? 'demo'
const API_PASSWORD = import.meta.env.VITE_API_PASSWORD ?? 'demo-password'
const REQUEST_TIMEOUT_MS = 35000

function authorizationHeader() {
  return `Basic ${btoa(`${API_USERNAME}:${API_PASSWORD}`)}`
}

type ApiErrorBody = { message?: unknown }

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function normalizeCreatedAt(value: string | number) {
  const timestamp = typeof value === 'string' ? new Date(value).getTime() : value
  if (!Number.isFinite(timestamp)) throw new Error('La respuesta contiene una fecha inválida.')
  return timestamp
}

function normalizeHistoryRecord(record: HistoryRecord & { createdAt: string | number }): HistoryRecord {
  const result = record.result
  if (!result || typeof result.matchPercentage !== 'number' || result.matchPercentage < 0 || result.matchPercentage > 100
    || !isStringArray(result.matchingSkills)
    || !isStringArray(result.missingSkills)
    || !isStringArray(result.recommendations)
    || !isStringArray(result.interviewQuestions)) {
    throw new Error('El análisis devolvió un formato inválido.')
  }

  return { ...record, createdAt: normalizeCreatedAt(record.createdAt) }
}

async function request(path: string, init: RequestInit, connectionMessage: string, fallbackMessage: string) {
  let response: Response
  const controller = new AbortController()
  let timedOut = false
  const timeout = setTimeout(() => {
    timedOut = true
    controller.abort()
  }, REQUEST_TIMEOUT_MS)
  const abortRequest = () => controller.abort()
  init.signal?.addEventListener('abort', abortRequest, { once: true })
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...init,
      signal: controller.signal,
      headers: {
        Authorization: authorizationHeader(),
        Accept: 'application/json',
        ...init.headers,
      },
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError' && timedOut) {
      throw new Error('La solicitud tardó demasiado. Intenta nuevamente.')
    }
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new Error(connectionMessage)
  } finally {
    clearTimeout(timeout)
    init.signal?.removeEventListener('abort', abortRequest)
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
  signal?: AbortSignal,
): Promise<HistoryRecord> {
  const formData = new FormData()
  formData.append('cvFile', cvFile)
  formData.append('cvVersion', cvVersion)
  if (mode === 'text') formData.append('jobDescription', jobDescription)
  else if (jobImage) formData.append('jobImage', jobImage)

  const response = await requestJson<HistoryRecord & { createdAt: string | number }>(
    '/api/analyses',
    { method: 'POST', body: formData, signal },
    `No se pudo conectar con el backend en ${API_URL}.`,
    'No se pudo guardar el análisis.',
  )
  return normalizeHistoryRecord(response)
}

export async function getAnalyses(page = 0, size = 20, signal?: AbortSignal): Promise<AnalysisHistoryPage> {
  const response = await requestJson<AnalysisHistoryPage & { content: Array<Omit<AnalysisSummary, 'createdAt'> & { createdAt: string | number }> }>(
    `/api/analyses?page=${page}&size=${size}`,
    { signal },
    `No se pudo conectar con el historial en ${API_URL}. Comprueba que Spring Boot esté iniciado.`,
    'No se pudo cargar el historial.',
  )
  if (!Array.isArray(response.content)) {
    throw new Error('El historial devolvió un formato inválido.')
  }

  return {
    ...response,
    content: response.content.map((record) => ({
      ...record,
      createdAt: normalizeCreatedAt(record.createdAt),
    })),
  }
}

export async function getAnalysis(id: string, signal?: AbortSignal): Promise<HistoryRecord> {
  const response = await requestJson<HistoryRecord & { createdAt: string | number }>(
    `/api/analyses/${encodeURIComponent(id)}`,
    { signal },
    `No se pudo conectar con el análisis en ${API_URL}.`,
    'No se pudo cargar el análisis.',
  )
  return normalizeHistoryRecord(response)
}

export async function deleteAnalysis(id: string): Promise<void> {
  await request(
    `/api/analyses/${encodeURIComponent(id)}`,
    { method: 'DELETE' },
    `No se pudo conectar con el backend en ${API_URL}.`,
    'No se pudo eliminar el análisis.',
  )
}
