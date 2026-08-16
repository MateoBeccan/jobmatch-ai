import { ApiRequestError } from './api'

export type AnalysisErrorView = {
  title?: string
  message: string
  retryable: boolean
}

export function toUserFacingAnalysisError(error: unknown): AnalysisErrorView {
  if (!(error instanceof ApiRequestError)) {
    return {
      title: 'Ocurrió un problema',
      message: 'No pudimos completar el análisis por un error interno. Intentá nuevamente más tarde.',
      retryable: true,
    }
  }

  switch (error.code) {
    case 'RATE_LIMIT_EXCEEDED':
      return {
        title: 'Límite temporal alcanzado',
        message: rateLimitMessage(error.retryAfterSeconds),
        retryable: false,
      }
    case 'AI_QUOTA_EXCEEDED':
      return {
        title: 'Límite de IA alcanzado',
        message: 'Se alcanzó el límite de uso disponible del servicio de inteligencia artificial. Intentá nuevamente más tarde.',
        retryable: false,
      }
    case 'AI_UNAVAILABLE':
      return {
        title: 'Servicio temporalmente no disponible',
        message: 'El servicio de inteligencia artificial no está disponible en este momento. Intentá nuevamente en unos minutos.',
        retryable: true,
      }
    case 'AI_TIMEOUT':
      return {
        title: 'El análisis tardó demasiado',
        message: 'El servicio de inteligencia artificial tardó más de lo esperado en responder. Intentá nuevamente.',
        retryable: true,
      }
    case 'AI_INVALID_RESPONSE':
      return {
        title: 'No pudimos completar el análisis',
        message: 'La inteligencia artificial devolvió una respuesta que no pudimos interpretar. Intentá nuevamente.',
        retryable: true,
      }
    case 'FILE_TOO_LARGE':
      return {
        title: 'Archivo demasiado grande',
        message: 'El archivo supera el tamaño máximo permitido de 5 MB.',
        retryable: false,
      }
    case 'INVALID_REQUEST':
    case 'MISSING_REQUEST_DATA':
      return {
        title: 'Revisá los datos cargados',
        message: error.message,
        retryable: false,
      }
    case 'CONFIGURATION_ERROR':
      return {
        title: 'Servicio no disponible',
        message: 'El servicio de análisis no está configurado correctamente en este momento. Intentá nuevamente más tarde.',
        retryable: false,
      }
    case 'FRONTEND_TIMEOUT':
      return {
        title: 'El análisis tardó demasiado',
        message: 'El análisis superó el tiempo de espera. Intentá nuevamente.',
        retryable: true,
      }
    case 'CONNECTION_ERROR':
      return {
        title: 'No pudimos conectarnos',
        message: 'No pudimos conectarnos con el servicio de análisis. Intentá nuevamente en unos instantes.',
        retryable: true,
      }
    case 'INTERNAL_ERROR':
    default:
      return {
        title: 'Ocurrió un problema',
        message: 'No pudimos completar el análisis por un error interno. Intentá nuevamente más tarde.',
        retryable: true,
      }
  }
}

function rateLimitMessage(retryAfterSeconds?: number) {
  if (retryAfterSeconds && retryAfterSeconds > 0) {
    return `Realizaste varios análisis en poco tiempo. Esperá aproximadamente ${retryAfterSeconds} segundos e intentá nuevamente.`
  }

  return 'Realizaste varios análisis en poco tiempo. Esperá aproximadamente un minuto e intentá nuevamente.'
}
