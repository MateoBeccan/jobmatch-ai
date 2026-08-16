import { describe, expect, it, vi } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { AnalysisErrorAlert } from './AnalysisErrorAlert'

describe('AnalysisErrorAlert', () => {
  it('shows retry only when the error is retryable', () => {
    const retryable = renderToStaticMarkup(
      <AnalysisErrorAlert
        error={{ title: 'Servicio temporalmente no disponible', message: 'Intentá nuevamente.', retryable: true }}
        onRetry={vi.fn()}
      />,
    )
    const notRetryable = renderToStaticMarkup(
      <AnalysisErrorAlert
        error={{ title: 'Límite de IA alcanzado', message: 'Intentá más tarde.', retryable: false }}
        onRetry={vi.fn()}
      />,
    )

    expect(retryable).toContain('Intentar nuevamente')
    expect(notRetryable).not.toContain('Intentar nuevamente')
  })
})
