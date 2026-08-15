type ErrorStateProps = {
  title?: string
  message: string
  onRetry?: () => void
}

export function ErrorState({ title = 'No pudimos completar la acción', message, onRetry }: ErrorStateProps) {
  return (
    <div className="error-state" role="alert">
      <span className="error-state-icon" aria-hidden="true">!</span>
      <h3>{title}</h3>
      <p>{message}</p>
      {onRetry && <button type="button" onClick={onRetry}>Intentar nuevamente</button>}
    </div>
  )
}
