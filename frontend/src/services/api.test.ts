import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { analyzeCV } from './api'

const analysisResponse = {
  matchPercentage: 82,
  matchingSkills: ['Java'],
  missingSkills: ['Docker'],
  recommendations: ['Practicar Docker'],
  interviewQuestions: ['Como disenarias una API REST?'],
  requirements: [
    { name: 'Java', status: 'match' },
    { name: 'Docker', status: 'missing' },
  ],
}

describe('api', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
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
})

function jsonResponse(body: unknown) {
  return {
    ok: true,
    json: () => Promise.resolve(body),
  } as Response
}
