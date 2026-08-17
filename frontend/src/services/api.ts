import { getMockEnabled, mockAnalysisResponse } from '../lib/mocks/analysisMock'
import { deleteHistoryRecord, getHistory, getHistoryRecord, saveHistoryRecord } from '../lib/storage/historyStorage'
import type {
  AnalysisHistoryPage,
  AnalysisMode,
  AnalysisResponse,
  HistoryRecord,
  JobSearchProfile,
  JobSeniority,
  RequirementMatch,
  RequirementStatus,
  ScoreBreakdown,
} from '../lib/types/types'

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')
const REQUEST_TIMEOUT_MS = 150000

type ApiErrorBody = { code?: unknown; message?: unknown }

export class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
    readonly retryAfterSeconds?: number,
  ) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isRequirementStatus(value: unknown): value is RequirementStatus {
  return value === 'match' || value === 'partial' || value === 'missing'
}

function isJobSeniority(value: unknown): value is JobSeniority {
  return value === 'TRAINEE'
    || value === 'JUNIOR'
    || value === 'MID'
    || value === 'SENIOR'
    || value === 'UNSPECIFIED'
}

function normalizeRequirements(value: unknown): RequirementMatch[] | undefined {
  if (value === undefined || value === null) return undefined
  if (!Array.isArray(value)) throw new Error('El análisis devolvió requisitos con un formato inválido.')

  const valid = value.every((item) => {
    if (!item || typeof item !== 'object') return false
    const record = item as Record<string, unknown>
    return typeof record.name === 'string'
      && isRequirementStatus(record.status)
      && (record.evidence === undefined || record.evidence === null || typeof record.evidence === 'string')
  })
  if (!valid) throw new Error('El análisis devolvió requisitos con un formato inválido.')

  return (value as Array<{ name: string; status: RequirementStatus; evidence?: string | null }>)
    .map(({ name, status, evidence }) => ({ name, status, evidence: evidence ?? undefined }))
}

function normalizeBreakdown(value: unknown): ScoreBreakdown | undefined {
  if (value === undefined || value === null) return undefined
  if (typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('El análisis devolvió un desglose de puntaje inválido.')
  }
  const record = value as Record<string, unknown>
  const keys = ['mandatoryTechnical', 'experienceSeniority', 'desirable', 'complementary'] as const
  const anyInvalid = keys.some((key) => record[key] !== undefined && record[key] !== null && typeof record[key] !== 'number')
  if (anyInvalid) throw new Error('El análisis devolvió un desglose de puntaje inválido.')

  return keys.reduce((breakdown, key) => {
    if (typeof record[key] === 'number') breakdown[key] = record[key] as number
    return breakdown
  }, {} as ScoreBreakdown)
}

function normalizeJobSearchProfile(value: unknown, required = false): JobSearchProfile | undefined {
  if (value === undefined || value === null) {
    if (required) throw new Error('El analisis devolvio un perfil de busqueda laboral invalido.')
    return undefined
  }
  if (typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('El analisis devolvio un perfil de busqueda laboral invalido.')
  }

  const record = value as Record<string, unknown>
  if (typeof record.role !== 'string' || !isJobSeniority(record.seniority) || !isStringArray(record.keywords)) {
    throw new Error('El analisis devolvio un perfil de busqueda laboral invalido.')
  }

  const role = record.role.trim()
  const keywords = record.keywords.map((keyword) => keyword.trim())
  if (role.length === 0 || role.length > 80
    || keywords.length < 3 || keywords.length > 6
    || keywords.some((keyword) => keyword.length === 0 || keyword.length > 50)) {
    throw new Error('El analisis devolvio un perfil de busqueda laboral invalido.')
  }

  return {
    role,
    seniority: record.seniority,
    keywords,
  }
}

function withDerivedRequirements(response: AnalysisResponse): AnalysisResponse {
  if (response.requirements) return response
  const requirements: RequirementMatch[] = [
    ...response.matchingSkills.map((name) => ({ name, status: 'match' as const })),
    ...response.missingSkills.map((name) => ({ name, status: 'missing' as const })),
  ]
  return { ...response, requirements }
}

function normalizeAnalysisResponse(response: AnalysisResponse, requireJobSearchProfile = false): AnalysisResponse {
  if (!response || typeof response.matchPercentage !== 'number' || response.matchPercentage < 0 || response.matchPercentage > 100
    || !isStringArray(response.matchingSkills)
    || !isStringArray(response.missingSkills)
    || !isStringArray(response.recommendations)
    || !isStringArray(response.interviewQuestions)) {
    throw new Error('El análisis devolvió un formato inválido.')
  }

  return withDerivedRequirements({
    ...response,
    requirements: normalizeRequirements(response.requirements),
    breakdown: normalizeBreakdown(response.breakdown),
    jobSearchProfile: normalizeJobSearchProfile(response.jobSearchProfile, requireJobSearchProfile),
  })
}

function normalizeCreatedAt(value: string | number) {
  const timestamp = typeof value === 'string' ? new Date(value).getTime() : value
  if (!Number.isFinite(timestamp)) throw new Error('La respuesta contiene una fecha inválida.')
  return timestamp
}

function normalizeHistoryRecord(record: HistoryRecord & { createdAt: string | number }): HistoryRecord {
  return {
    ...record,
    result: normalizeAnalysisResponse(record.result),
    createdAt: normalizeCreatedAt(record.createdAt),
  }
}

async function mockDelay(ms = 900) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

function firstLine(value: string) {
  const line = value.trim().split('\n')[0]?.trim()
  return line || 'Nueva oferta'
}

function createLocalId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function buildHistoryRecord(
  cvFile: File,
  mode: AnalysisMode,
  jobDescription: string,
  cvVersion: string,
  result: AnalysisResponse,
): HistoryRecord {
  return {
    id: createLocalId(),
    role: mode === 'text' ? firstLine(jobDescription) : 'Oferta desde imagen',
    company: 'Oferta laboral',
    cvFileName: cvFile.name,
    cvVersion,
    mode,
    score: result.matchPercentage,
    createdAt: Date.now(),
    jobDescription: mode === 'text' ? jobDescription : '',
    result,
  }
}

async function request(path: string, init: RequestInit, fallbackMessage: string) {
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
        Accept: 'application/json',
        ...init.headers,
      },
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError' && timedOut) {
      throw new ApiRequestError('La solicitud tardó demasiado. Intenta nuevamente.', 0, 'FRONTEND_TIMEOUT')
    }
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiRequestError('No se pudo conectar con el servicio de análisis.', 0, 'CONNECTION_ERROR')
  } finally {
    clearTimeout(timeout)
    init.signal?.removeEventListener('abort', abortRequest)
  }

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null) as ApiErrorBody | null
    const code = typeof errorBody?.code === 'string' ? errorBody.code : fallbackErrorCode(response.status)
    const message = typeof errorBody?.message === 'string' ? errorBody.message : fallbackMessage
    throw new ApiRequestError(message, response.status, code, retryAfterSeconds(response.headers.get('Retry-After')))
  }

  return response
}

function retryAfterSeconds(value: string | null) {
  if (!value) return undefined
  const seconds = Number.parseInt(value, 10)
  return Number.isFinite(seconds) && seconds >= 0 ? seconds : undefined
}

function fallbackErrorCode(status: number) {
  switch (status) {
    case 400:
      return 'INVALID_REQUEST'
    case 413:
      return 'FILE_TOO_LARGE'
    case 429:
      return 'RATE_LIMIT_EXCEEDED'
    case 502:
      return 'AI_INVALID_RESPONSE'
    case 503:
      return 'AI_UNAVAILABLE'
    case 504:
      return 'AI_TIMEOUT'
    case 500:
      return 'INTERNAL_ERROR'
    default:
      return undefined
  }
}

async function requestJson<T>(path: string, init: RequestInit, fallbackMessage: string): Promise<T> {
  const response = await request(path, init, fallbackMessage)
  return response.json() as Promise<T>
}

export async function analyzeCV(
  cvFile: File,
  mode: AnalysisMode,
  jobDescription: string,
  jobImage: File | null,
  signal?: AbortSignal,
): Promise<AnalysisResponse> {
  if (getMockEnabled()) {
    await mockDelay()
    return mockAnalysisResponse
  }

  const formData = new FormData()
  formData.append('cvFile', cvFile)
  if (mode === 'text') formData.append('jobDescription', jobDescription)
  else if (jobImage) formData.append('jobImage', jobImage)

  const response = await requestJson<AnalysisResponse>(
    '/api/analyze',
    { method: 'POST', body: formData, signal },
    'No se pudo completar el análisis.',
  )
  return normalizeAnalysisResponse(response, true)
}

export async function createAnalysis(
  cvFile: File,
  mode: AnalysisMode,
  jobDescription: string,
  jobImage: File | null,
  cvVersion = 'CV sin versión',
  signal?: AbortSignal,
): Promise<HistoryRecord> {
  if (getMockEnabled()) {
    await mockDelay()
    return saveHistoryRecord(buildHistoryRecord(cvFile, mode, jobDescription, cvVersion, mockAnalysisResponse))
  }

  const formData = new FormData()
  formData.append('cvFile', cvFile)
  if (mode === 'text') formData.append('jobDescription', jobDescription)
  else if (jobImage) formData.append('jobImage', jobImage)

  const response = await requestJson<AnalysisResponse>(
    '/api/analyze',
    { method: 'POST', body: formData, signal },
    'No se pudo completar el análisis.',
  )
  const result = normalizeAnalysisResponse(response, true)
  return saveHistoryRecord(buildHistoryRecord(cvFile, mode, jobDescription, cvVersion, result))
}

export async function getAnalyses(page = 0, size = 20, signal?: AbortSignal): Promise<AnalysisHistoryPage> {
  if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
  const records = getHistory()
  const start = page * size
  const content = records.slice(start, start + size).map(({ jobDescription, result, ...summary }) => summary)
  return {
    content,
    page,
    size,
    totalElements: records.length,
    totalPages: Math.ceil(records.length / size),
  }
}

export async function getAnalysis(id: string, signal?: AbortSignal): Promise<HistoryRecord> {
  if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
  const record = getHistoryRecord(id)
  if (!record) throw new Error('No se encontró el análisis.')
  return normalizeHistoryRecord(record)
}

export async function deleteAnalysis(id: string): Promise<void> {
  deleteHistoryRecord(id)
}
