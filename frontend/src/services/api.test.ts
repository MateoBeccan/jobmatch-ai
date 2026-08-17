import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { analyzeCV, ApiRequestError, createAnalysis, deleteAnalysis, ensureBackendReady, getAnalyses, getAnalysis, searchJobs, warmUpBackend } from './api'
import { getHistory, saveHistoryRecord } from '../lib/storage/historyStorage'
import type { AnalysisResponse, HistoryRecord, JobSearchProfile, JobSearchResponse } from '../lib/types/types'

const analysisResponse: AnalysisResponse = {
  matchPercentage: 82,
  matchingSkills: ['Java'],
  missingSkills: ['Docker'],
  recommendations: ['Practicar Docker'],
  interviewQuestions: ['Como disenarias una API REST?'],
  requirements: [
    { name: 'Java', status: 'match' },
    { name: 'Docker', status: 'missing' },
  ],
  jobSearchProfile: {
    role: 'Java Backend Developer',
    seniority: 'JUNIOR',
    keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
  },
}

const HISTORY_STORAGE_KEY = 'jobmatch-ai-history'

const jobSearchProfile: JobSearchProfile = {
  role: 'Java Backend Developer',
  seniority: 'JUNIOR',
  keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
}

const jobSearchResponse: JobSearchResponse = {
  provider: 'JOBICY',
  count: 1,
  jobs: [
    {
      id: '150845',
      title: 'Full Stack Developer - Java & React',
      company: 'Example',
      location: 'LATAM',
      snippet: 'Java and Spring Boot role',
      salary: null,
      employmentType: 'Full-Time',
      updatedAt: '2026-08-16T14:51:50+00:00',
      url: 'https://jobicy.com/jobs/full-stack-java-react',
      source: 'Jobicy',
      matchedKeywords: ['Java', 'Spring Boot'],
    },
  ],
}

describe('api', () => {
  let nextId: number
  let storage: MemoryStorage

  beforeEach(() => {
    vi.useFakeTimers()
    nextId = 0
    storage = new MemoryStorage()
    vi.stubGlobal('localStorage', storage)
    vi.stubGlobal('window', {
      localStorage: storage,
      setTimeout: globalThis.setTimeout,
      clearTimeout: globalThis.clearTimeout,
    })
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => `history-${++nextId}`) })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllEnvs()
    vi.restoreAllMocks()
  })

  it('posts to /api/analyze without Authorization header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(analysisResponse))
    vi.stubGlobal('fetch', fetchMock)

    await analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/analyze', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Accept: 'application/json' }),
    }))
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined()
    expect(init.body).toBeInstanceOf(FormData)
  })

  it('returns AnalysisResponse directly', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(analysisResponse)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).resolves.toEqual(analysisResponse)
  })

  it('sends jobImage instead of jobDescription in image mode', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(analysisResponse))
    vi.stubGlobal('fetch', fetchMock)

    await analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'image',
      '',
      new File(['image'], 'job.png', { type: 'image/png' }),
    )

    const init = fetchMock.mock.calls[0][1] as RequestInit
    const formData = init.body as FormData
    expect(formData.get('cvFile')).toBeInstanceOf(File)
    expect(formData.get('jobImage')).toBeInstanceOf(File)
    expect(formData.get('jobDescription')).toBeNull()
  })

  it('accepts null values in breakdown without throwing', async () => {
    const response = {
      ...analysisResponse,
      breakdown: {
        mandatoryTechnical: 83,
        experienceSeniority: 75,
        desirable: null,
        complementary: null,
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).resolves.toMatchObject({
      breakdown: {
        mandatoryTechnical: 83,
        experienceSeniority: 75,
      },
    })
  })

  it('omits null categories from normalized breakdown', async () => {
    const response = {
      ...analysisResponse,
      breakdown: {
        mandatoryTechnical: 83,
        experienceSeniority: null,
        desirable: 50,
        complementary: null,
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    const result = await analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )

    expect(result.breakdown).toEqual({
      mandatoryTechnical: 83,
      desirable: 50,
    })
  })

  it('normalizes null requirement evidence to undefined', async () => {
    const response = {
      ...analysisResponse,
      requirements: [
        { name: 'Java', status: 'match', evidence: null },
      ],
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    const result = await analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )

    expect(result.requirements?.[0]).toEqual({
      name: 'Java',
      status: 'match',
      evidence: undefined,
    })
  })

  it('preserves a valid job search profile when backend returns it', async () => {
    const response = {
      ...analysisResponse,
      jobSearchProfile: {
        role: ' Java Backend Developer ',
        seniority: 'JUNIOR',
        keywords: [' Java ', 'Spring Boot', 'SQL', 'REST API'],
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).resolves.toMatchObject({
      jobSearchProfile: {
        role: 'Java Backend Developer',
        seniority: 'JUNIOR',
        keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
      },
    })
  })

  it('rejects new API analysis responses without job search profile', async () => {
    const response = { ...analysisResponse }
    delete response.jobSearchProfile
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toThrow(/perfil de busqueda laboral/)
  })

  it('rejects invalid job search profile values from backend', async () => {
    const response = {
      ...analysisResponse,
      jobSearchProfile: {
        role: 'Java Backend Developer',
        seniority: 'LEAD',
        keywords: ['Java', 'Spring Boot', 'SQL'],
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toThrow(/perfil de busqueda laboral/)
  })

  it('rejects job search profile with blank role from backend', async () => {
    const response = {
      ...analysisResponse,
      jobSearchProfile: {
        role: '   ',
        seniority: 'JUNIOR',
        keywords: ['Java', 'Spring Boot', 'SQL'],
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toThrow(/perfil de busqueda laboral/)
  })

  it('rejects job search profile with fewer than three keywords from backend', async () => {
    const response = {
      ...analysisResponse,
      jobSearchProfile: {
        role: 'Java Backend Developer',
        seniority: 'JUNIOR',
        keywords: ['Java', 'SQL'],
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toThrow(/perfil de busqueda laboral/)
  })

  it('rejects invalid breakdown value types', async () => {
    const response = {
      ...analysisResponse,
      breakdown: {
        mandatoryTechnical: '83',
      },
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toThrow(/desglose de puntaje/)
  })

  it('preserves API error status code and Retry-After header', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(errorResponse(
      429,
      { code: 'RATE_LIMIT_EXCEEDED', message: 'Se supero el limite de analisis por minuto.' },
      { 'Retry-After': '60' },
    )))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toMatchObject({
      status: 429,
      code: 'RATE_LIMIT_EXCEEDED',
      retryAfterSeconds: 60,
      message: 'Se supero el limite de analisis por minuto.',
    })
  })

  it('uses a safe fallback when the error body is not JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      headers: responseHeaders({}),
      json: () => Promise.reject(new SyntaxError('invalid json')),
    } as Response))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toMatchObject({
      status: 503,
      message: 'No se pudo completar el análisis.',
    })
  })

  it('infers an error code from status when backend response has no code', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(errorResponse(
      503,
      { message: 'El servicio de inteligencia artificial no esta disponible temporalmente.' },
    )))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toMatchObject({
      status: 503,
      code: 'AI_UNAVAILABLE',
      message: 'El servicio de inteligencia artificial no esta disponible temporalmente.',
    })
  })

  it('infers a safe code for non JSON HTTP errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      headers: responseHeaders({}),
      json: () => Promise.reject(new SyntaxError('invalid json')),
    } as Response))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toMatchObject({
      status: 503,
      code: 'AI_UNAVAILABLE',
      message: 'No se pudo completar el análisis.',
    })
  })

  it('does not expose API_URL when the request cannot connect', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network failed')))

    await expect(analyzeCV(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
    )).rejects.toSatisfy((error: unknown) => (
      error instanceof ApiRequestError
      && error.code === 'CONNECTION_ERROR'
      && !error.message.includes('http://localhost:8080')
    ))
  })

  it('saves a created analysis in localStorage', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(analysisResponse)))

    const record = await createAnalysis(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
      'CV v1',
    )

    expect(record).toMatchObject({
      id: 'history-1',
      role: 'Java developer role',
      cvFileName: 'cv.pdf',
      cvVersion: 'CV v1',
      mode: 'text',
      score: 82,
      jobDescription: 'Java developer role',
      result: analysisResponse,
    })
    expect(getHistory()).toHaveLength(1)
  })

  it('recovers history with newest records first', async () => {
    saveHistoryRecord(historyRecord({ id: 'older', createdAt: 1000, role: 'Older role' }))
    saveHistoryRecord(historyRecord({ id: 'newer', createdAt: 2000, role: 'Newer role' }))

    const page = await getAnalyses(0, 20)

    expect(page.content.map((record) => record.id)).toEqual(['newer', 'older'])
    expect(page.totalElements).toBe(2)
    expect(page.totalPages).toBe(1)
  })

  it('keeps old local history records without job search profile compatible', () => {
    const legacyAnalysisResponse = { ...analysisResponse }
    delete legacyAnalysisResponse.jobSearchProfile
    storage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([
      historyRecord({ id: 'old-record', result: legacyAnalysisResponse }),
    ]))

    expect(getHistory().map((record) => record.id)).toEqual(['old-record'])
    expect(getHistory()[0].result.jobSearchProfile).toBeUndefined()
  })

  it('recovers a history record by id', async () => {
    saveHistoryRecord(historyRecord({ id: 'record-1', role: 'Java role' }))

    await expect(getAnalysis('record-1')).resolves.toMatchObject({
      id: 'record-1',
      role: 'Java role',
      result: analysisResponse,
    })
  })

  it('deletes a history record', async () => {
    saveHistoryRecord(historyRecord({ id: 'keep' }))
    saveHistoryRecord(historyRecord({ id: 'remove' }))

    await deleteAnalysis('remove')

    expect(getHistory().map((record) => record.id)).toEqual(['keep'])
  })

  it('persists history between calls', async () => {
    saveHistoryRecord(historyRecord({ id: 'persisted' }))

    expect(getHistory().map((record) => record.id)).toEqual(['persisted'])
    await expect(getAnalysis('persisted')).resolves.toMatchObject({ id: 'persisted' })
  })

  it('recovers empty history from corrupt JSON', async () => {
    storage.setItem(HISTORY_STORAGE_KEY, '{not-json')

    await expect(getAnalyses()).resolves.toMatchObject({
      content: [],
      totalElements: 0,
      totalPages: 0,
    })
  })

  it('recovers empty history when stored JSON is valid but not an array', async () => {
    storage.setItem(HISTORY_STORAGE_KEY, JSON.stringify({ id: 'not-an-array' }))

    await expect(getAnalyses()).resolves.toMatchObject({
      content: [],
      totalElements: 0,
      totalPages: 0,
    })
  })

  it('ignores invalid records inside stored history', () => {
    storage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([
      historyRecord({ id: 'valid' }),
      { ...historyRecord({ id: 'invalid' }), role: 42 },
    ]))

    expect(getHistory().map((record) => record.id)).toEqual(['valid'])
  })

  it('ignores records with non-string matching skills', () => {
    storage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([
      {
        ...historyRecord({ id: 'invalid-skills' }),
        result: {
          ...analysisResponse,
          matchingSkills: ['Java', 123],
        },
      },
    ]))

    expect(getHistory()).toEqual([])
  })

  it('ignores records with score outside 0..100', () => {
    storage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([
      { ...historyRecord({ id: 'invalid-score' }), score: 101 },
    ]))

    expect(getHistory()).toEqual([])
  })

  it('ignores records with invalid createdAt', () => {
    storage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([
      { ...historyRecord({ id: 'invalid-created-at' }), createdAt: Number.NaN },
    ]))

    expect(getHistory()).toEqual([])
  })

  it('limits local history to the newest 50 records', () => {
    for (let index = 1; index <= 55; index += 1) {
      saveHistoryRecord(historyRecord({
        id: `record-${index}`,
        createdAt: index,
      }))
    }

    const records = getHistory()
    expect(records).toHaveLength(50)
    expect(records[0].id).toBe('record-55')
    expect(records.at(-1)?.id).toBe('record-6')
  })

  it('createAnalysis uses /api/analyze without Authorization and not /api/analyses', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(analysisResponse))
    vi.stubGlobal('fetch', fetchMock)

    await createAnalysis(
      new File(['pdf bytes'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
      'CV v1',
    )

    expect(fetchMock).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8080/api/analyze')
    expect(fetchMock.mock.calls[0][0]).not.toContain('/api/analyses')
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined()
  })

  it('does not store File, PDF, image, or base64 content in localStorage', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(analysisResponse)))
    const cvFile = new File(['PDF_BINARY_CONTENT'], 'cv.pdf', { type: 'application/pdf' })
    const jobImage = new File(['data:image/png;base64,IMAGE_CONTENT'], 'job.png', { type: 'image/png' })

    await createAnalysis(cvFile, 'image', '', jobImage, 'CV v1')

    const stored = storage.getItem('jobmatch-ai-history') ?? ''
    expect(stored).toContain('cv.pdf')
    expect(stored).toContain('Oferta desde imagen')
    expect(stored).not.toContain('PDF_BINARY_CONTENT')
    expect(stored).not.toContain('IMAGE_CONTENT')
    expect(stored).not.toContain('data:image/png;base64')
    expect(stored).not.toContain('job.png')
  })

  it('returns HistoryRecord when localStorage.setItem throws QuotaExceededError', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(analysisResponse))
    vi.stubGlobal('fetch', fetchMock)
    storage.throwOnSet = true

    const record = await createAnalysis(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
      'CV v1',
    )

    expect(record).toMatchObject({
      id: 'history-1',
      role: 'Java developer role',
      result: analysisResponse,
    })
  })

  it('calls /api/analyze only once when localStorage.setItem throws QuotaExceededError', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(analysisResponse))
    vi.stubGlobal('fetch', fetchMock)
    storage.throwOnSet = true

    await createAnalysis(
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
      'text',
      'Java developer role',
      null,
      'CV v1',
    )

    expect(fetchMock).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8080/api/analyze')
  })

  it('searchJobs posts the exact backend contract and no analysis details', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(jobSearchResponse))
    vi.stubGlobal('fetch', fetchMock)

    await searchJobs(jobSearchProfile, 'LATAM')

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/jobs/search', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        Accept: 'application/json',
        'Content-Type': 'application/json',
      }),
    }))
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(JSON.parse(init.body as string)).toEqual({
      role: 'Java Backend Developer',
      seniority: 'JUNIOR',
      keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
      location: 'LATAM',
    })
    expect(init.body).not.toContain('cv')
    expect(init.body).not.toContain('cvFile')
    expect(init.body).not.toContain('cvText')
    expect(init.body).not.toContain('jobDescription')
    expect(init.body).not.toContain('personalData')
    expect(init.body).not.toContain('matchingSkills')
    expect(init.body).not.toContain('missingSkills')
  })

  it('searchJobs normalizes a valid response and optional null fields', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(jobSearchResponse)))

    await expect(searchJobs(jobSearchProfile, 'Argentina')).resolves.toEqual(jobSearchResponse)
  })

  it('searchJobs accepts zero results', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ provider: 'JOBICY', count: 0, jobs: [] })))

    await expect(searchJobs(jobSearchProfile, 'Global')).resolves.toEqual({ provider: 'JOBICY', count: 0, jobs: [] })
  })

  it('searchJobs rejects inconsistent counts and invalid jobs arrays', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({ provider: 'JOBICY', count: 2, jobs: [] })))
    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toThrow(/formato invalido|formato inválido/)

    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({ provider: 'JOBICY', count: 0, jobs: {} })))
    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toThrow(/formato invalido|formato inválido/)
  })

  it('searchJobs rejects invalid title, matchedKeywords, and non HTTPS URLs', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({
      ...jobSearchResponse,
      jobs: [{ ...jobSearchResponse.jobs[0], title: ' ' }],
    })))
    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toThrow(/formato invalido|formato inválido/)

    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({
      ...jobSearchResponse,
      jobs: [{ ...jobSearchResponse.jobs[0], matchedKeywords: ['Java', 123] }],
    })))
    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toThrow(/formato invalido|formato inválido/)

    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({
      ...jobSearchResponse,
      jobs: [{ ...jobSearchResponse.jobs[0], url: 'http://jobicy.com/jobs/insecure' }],
    })))
    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toThrow(/formato invalido|formato inválido/)
  })

  it('searchJobs preserves job search API errors and retry after', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(errorResponse(
      503,
      { code: 'JOB_SEARCH_UNAVAILABLE', message: 'Ofertas no disponibles.' },
      { 'Retry-After': '30' },
    )))

    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toMatchObject({
      status: 503,
      code: 'JOB_SEARCH_UNAVAILABLE',
      retryAfterSeconds: 30,
      message: 'Ofertas no disponibles.',
    })
  })

  it('searchJobs infers job search fallback error codes', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(errorResponse(504, { message: 'timeout' })))

    await expect(searchJobs(jobSearchProfile, 'Argentina')).rejects.toMatchObject({
      status: 504,
      code: 'JOB_SEARCH_TIMEOUT',
      message: 'timeout',
    })
  })

  it('searchJobs mock mode rejects an already aborted signal', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'true')
    const controller = new AbortController()
    controller.abort()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(searchJobs(jobSearchProfile, 'Argentina', controller.signal))
      .rejects.toMatchObject({ name: 'AbortError' })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('ensureBackendReady resolves when health returns UP JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ status: 'UP' }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(ensureBackendReady()).resolves.toBeUndefined()

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/actuator/health', expect.objectContaining({
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    }))
  })

  it('ensureBackendReady retries after a failed health request and resolves on UP', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('network failed'))
      .mockResolvedValueOnce(jsonResponse({ status: 'UP' }))
    vi.stubGlobal('fetch', fetchMock)

    const ready = ensureBackendReady()
    await vi.advanceTimersByTimeAsync(2000)

    await expect(ready).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('ensureBackendReady treats invalid health JSON as not ready and retries', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: () => Promise.reject(new SyntaxError('html')) } as Response)
      .mockResolvedValueOnce(jsonResponse({ status: 'UP' }))
    vi.stubGlobal('fetch', fetchMock)

    const ready = ensureBackendReady()
    await vi.advanceTimersByTimeAsync(2000)

    await expect(ready).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('ensureBackendReady retries when health status is not UP', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ status: 'DOWN' }))
      .mockResolvedValueOnce(jsonResponse({ status: 'UP' }))
    vi.stubGlobal('fetch', fetchMock)

    const ready = ensureBackendReady()
    await vi.advanceTimersByTimeAsync(2000)

    await expect(ready).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('ensureBackendReady rejects AbortError when the signal is already aborted', async () => {
    const controller = new AbortController()
    controller.abort()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(ensureBackendReady(controller.signal)).rejects.toMatchObject({ name: 'AbortError' })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('ensureBackendReady fails with BACKEND_STARTUP_TIMEOUT after the total limit', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ status: 'DOWN' })))

    const ready = ensureBackendReady()
    const assertion = expect(ready).rejects.toMatchObject({
      status: 0,
      code: 'BACKEND_STARTUP_TIMEOUT',
    })
    await vi.advanceTimersByTimeAsync(120000)

    await assertion
  })

  it('warmUpBackend does not fetch in mock mode', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'true')
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await warmUpBackend()

    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('warmUpBackend does not propagate health errors', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new TypeError('network failed'))
    vi.stubGlobal('fetch', fetchMock)

    await expect(warmUpBackend()).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledOnce()
  })
})

function jsonResponse(body: unknown) {
  return {
    ok: true,
    json: () => Promise.resolve(body),
  } as Response
}

function errorResponse(status: number, body: unknown, headers: Record<string, string> = {}) {
  return {
    ok: false,
    status,
    headers: responseHeaders(headers),
    json: () => Promise.resolve(body),
  } as Response
}

function responseHeaders(headers: Record<string, string>) {
  return {
    get: (name: string) => headers[name] ?? headers[name.toLowerCase()] ?? null,
  } as Headers
}

function historyRecord(overrides: Partial<HistoryRecord> = {}): HistoryRecord {
  return {
    id: 'record',
    role: 'Java developer role',
    company: 'Oferta laboral',
    cvFileName: 'cv.pdf',
    cvVersion: 'CV v1',
    mode: 'text',
    score: analysisResponse.matchPercentage,
    createdAt: 1000,
    jobDescription: 'Java developer role',
    result: analysisResponse,
    ...overrides,
  }
}

class MemoryStorage implements Storage {
  private items = new Map<string, string>()
  throwOnSet = false

  get length() {
    return this.items.size
  }

  clear() {
    this.items.clear()
  }

  getItem(key: string) {
    return this.items.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.items.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.items.delete(key)
  }

  setItem(key: string, value: string) {
    if (this.throwOnSet) throw new DOMException('Storage quota exceeded', 'QuotaExceededError')
    this.items.set(key, value)
  }
}
