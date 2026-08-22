import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './pages/App'
import { AppErrorBoundary } from './components/templates/AppErrorBoundary'
import './styles/globals.css'
import './styles/loading.css'
import './styles/theme.css'
import './styles/components.css'
import './styles/career-multiverse.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary>
      <App />
    </AppErrorBoundary>
  </StrictMode>,
)
