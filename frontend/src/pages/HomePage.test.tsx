import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { HomePage } from './HomePage'
import { BRAND_LOGO_PATH } from '../lib/constants/brand'

describe('HomePage', () => {
  it('renders the product, main CTA, how it works section and logo', () => {
    const markup = renderToStaticMarkup(
      <HomePage theme="light" onToggleTheme={vi.fn()} onNavigate={vi.fn()} />,
    )

    expect(markup).toContain('JobMatch AI')
    expect(markup).toContain('ANÁLISIS DE COMPATIBILIDAD CON IA')
    expect(markup).toContain('Comenzar análisis')
    expect(markup).toContain('CÓMO FUNCIONA')
    expect(markup).toContain('De tu CV a una mejor postulación')
    expect(markup).toContain('Subí tu CV')
    expect(markup).toContain(`src="${BRAND_LOGO_PATH}"`)
    expect(markup).toContain('alt="JobMatch AI"')
  })

  it('renders CTA controls that target the analyzer flow', () => {
    const markup = renderToStaticMarkup(
      <HomePage theme="dark" onToggleTheme={vi.fn()} onNavigate={vi.fn()} />,
    )

    expect(markup).toContain('Comenzar análisis')
    expect(markup).toContain('Analizar mi CV')
    expect(markup).toContain('Análisis asistido por Google Gemini')
  })
})
