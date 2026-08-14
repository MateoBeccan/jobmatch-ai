import { Component, type ErrorInfo, type ReactNode } from 'react'

type AppErrorBoundaryProps = { children: ReactNode }
type AppErrorBoundaryState = { hasError: boolean }

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = { hasError: false }

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Unexpected frontend error', error, errorInfo)
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <main className="route-status" role="alert">
        <div>
          <span className="intro-kicker">JOBMATCH AI</span>
          <h1>Algo salió mal</h1>
          <p>Recarga la aplicación para continuar. Tus análisis guardados no se modificaron.</p>
          <button type="button" onClick={() => window.location.reload()}>Recargar aplicación</button>
        </div>
      </main>
    )
  }
}
