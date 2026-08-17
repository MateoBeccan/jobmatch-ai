import { describe, expect, it } from 'vitest'
import { ApiRequestError } from './api'
import { toUserFacingAnalysisError, toUserFacingJobSearchError } from './errorMessages'

describe('toUserFacingAnalysisError', () => {
  it('maps rate limit errors and disables immediate retry', () => {
    const view = toUserFacingAnalysisError(new ApiRequestError('backend message', 429, 'RATE_LIMIT_EXCEEDED', 60))

    expect(view).toEqual({
      title: 'Límite temporal alcanzado',
      message: 'Realizaste varios análisis en poco tiempo. Esperá aproximadamente 60 segundos e intentá nuevamente.',
      retryable: false,
    })
  })

  it('maps AI quota errors and disables immediate retry', () => {
    const view = toUserFacingAnalysisError(new ApiRequestError('quota', 429, 'AI_QUOTA_EXCEEDED'))

    expect(view.title).toBe('Límite de IA alcanzado')
    expect(view.message).toBe('Se alcanzó el límite de uso disponible del servicio de inteligencia artificial. Intentá nuevamente más tarde.')
    expect(view.retryable).toBe(false)
  })

  it('allows retry for temporary AI failures', () => {
    expect(toUserFacingAnalysisError(new ApiRequestError('down', 503, 'AI_UNAVAILABLE')).retryable).toBe(true)
    expect(toUserFacingAnalysisError(new ApiRequestError('timeout', 504, 'AI_TIMEOUT')).retryable).toBe(true)
    expect(toUserFacingAnalysisError(new ApiRequestError('bad json', 502, 'AI_INVALID_RESPONSE')).retryable).toBe(true)
  })

  it('maps file size and configuration errors without retry', () => {
    expect(toUserFacingAnalysisError(new ApiRequestError('large', 413, 'FILE_TOO_LARGE'))).toMatchObject({
      title: 'Archivo demasiado grande',
      retryable: false,
    })
    expect(toUserFacingAnalysisError(new ApiRequestError('Falta GEMINI_API_KEY', 500, 'CONFIGURATION_ERROR'))).toEqual({
      title: 'Servicio no disponible',
      message: 'El servicio de análisis no está configurado correctamente en este momento. Intentá nuevamente más tarde.',
      retryable: false,
    })
  })

  it('maps invalid CV content errors without retry', () => {
    const view = toUserFacingAnalysisError(new ApiRequestError('backend safe message', 400, 'INVALID_CV_CONTENT'))

    expect(view).toEqual({
      title: 'El archivo no parece ser un CV',
      message: 'No pudimos identificar información típica de un currículum en el PDF. Verificá que hayas seleccionado tu CV e intentá nuevamente.',
      retryable: false,
    })
  })

  it('uses safe fallbacks for internal and connection errors', () => {
    expect(toUserFacingAnalysisError(new ApiRequestError('secret', 500, 'INTERNAL_ERROR')).message).not.toContain('secret')
    expect(toUserFacingAnalysisError(new ApiRequestError('http://localhost:8080', 0, 'CONNECTION_ERROR')).message)
      .not.toContain('localhost')
  })

  it('maps backend startup timeout to a friendly retryable error', () => {
    expect(toUserFacingAnalysisError(new ApiRequestError('startup', 0, 'BACKEND_STARTUP_TIMEOUT'))).toEqual({
      title: 'El servidor tardó demasiado en iniciar',
      message: 'No pudimos preparar el servicio de análisis. Intentá nuevamente en unos instantes.',
      retryable: true,
    })
  })
})

describe('toUserFacingJobSearchError', () => {
  it('maps known job search errors without using analysis copy', () => {
    expect(toUserFacingJobSearchError(new ApiRequestError('invalid', 400, 'INVALID_JOB_SEARCH_REQUEST'))).toMatchObject({
      message: 'No pudimos realizar la busqueda con este perfil.',
      retryable: false,
    })
    expect(toUserFacingJobSearchError(new ApiRequestError('bad', 502, 'JOB_SEARCH_INVALID_RESPONSE'))).toMatchObject({
      message: 'El servicio de ofertas devolvio una respuesta inesperada.',
      retryable: true,
    })
    expect(toUserFacingJobSearchError(new ApiRequestError('down', 503, 'JOB_SEARCH_UNAVAILABLE'))).toMatchObject({
      message: 'Las ofertas no estan disponibles en este momento. Proba nuevamente mas tarde.',
      retryable: true,
    })
    expect(toUserFacingJobSearchError(new ApiRequestError('timeout', 504, 'JOB_SEARCH_TIMEOUT'))).toMatchObject({
      message: 'La busqueda de ofertas tardo demasiado. Proba nuevamente.',
      retryable: true,
    })
  })

  it('maps configuration, rate limit, connection, frontend timeout, and unknown errors', () => {
    expect(toUserFacingJobSearchError(new ApiRequestError('config', 500, 'CONFIGURATION_ERROR'))).toMatchObject({
      message: 'La busqueda de ofertas no esta disponible temporalmente.',
      retryable: false,
    })
    expect(toUserFacingJobSearchError(new ApiRequestError('limit', 429, 'RATE_LIMIT_EXCEEDED', 45))).toMatchObject({
      message: 'Realizaste varias busquedas en poco tiempo. Espera 45 segundos antes de intentar nuevamente.',
      retryable: false,
    })
    expect(toUserFacingJobSearchError(new ApiRequestError('network', 0, 'CONNECTION_ERROR'))).toMatchObject({
      message: 'No pudimos conectarnos al servicio de ofertas.',
      retryable: true,
    })
    expect(toUserFacingJobSearchError(new ApiRequestError('timeout', 0, 'FRONTEND_TIMEOUT')).retryable).toBe(true)
    expect(toUserFacingJobSearchError(new Error('unknown'))).toEqual({
      title: 'No pudimos buscar ofertas',
      message: 'No pudimos buscar ofertas en este momento.',
      retryable: true,
    })
  })
})
