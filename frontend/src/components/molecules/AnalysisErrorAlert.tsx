import type { AnalysisErrorView } from '../../services/errorMessages'

type AnalysisErrorAlertProps = {
  error: AnalysisErrorView
  onRetry: () => void
}

export function AnalysisErrorAlert({ error, onRetry }: AnalysisErrorAlertProps) {
  return (
    <div id="form-error" className="alert alert-analysis" role="alert" aria-live="assertive">
      <span aria-hidden="true">!</span>
      <div className="alert-copy">
        {error.title && <strong>{error.title}</strong>}
        <p>{error.message}</p>
      </div>
      {error.retryable && <button type="button" onClick={onRetry}>Intentar nuevamente</button>}
    </div>
  )
}
