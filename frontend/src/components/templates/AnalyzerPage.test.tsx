import { describe, expect, it, vi } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { AnalyzerPage } from './AnalyzerPage'

describe('AnalyzerPage', () => {
  it('shows the AI privacy notice before analysis without changing the form flow', () => {
    const markup = renderToStaticMarkup(
      <AnalyzerPage
        theme="light"
        onToggleTheme={vi.fn()}
        onNavigate={vi.fn()}
      />,
    )

    expect(markup).toContain('aria-labelledby="ai-privacy-title"')
    expect(markup).toContain('Nueva Evaluación')
    expect(markup).toContain('Prepará tu CV y la oferta que querés analizar.')
    expect(markup).toContain('Tu currículum')
    expect(markup).toContain('Cargá el CV que querés evaluar')
    expect(markup).toContain('Arrastrá tu CV acá')
    expect(markup).toContain('PDF · máximo 5 MB')
    expect(markup).toContain('Oferta laboral')
    expect(markup).toContain('Agregá los requisitos del puesto')
    expect(markup).toContain('Privacidad y procesamiento con IA')
    expect(markup).toContain('El contenido de tu CV y de la oferta laboral será enviado a un servicio externo de inteligencia artificial')
    expect(markup).toContain('Google Gemini')
    expect(markup).toContain('No cargues información sensible que no desees procesar mediante IA.')
    expect(markup).toContain('Se utilizará para generar tu análisis.')
    expect(markup).toContain('Procesamiento mediante IA')
    expect(markup).toContain('Al continuar, el contenido cargado será procesado mediante inteligencia artificial.')
    expect(markup).toContain('<form')
    expect(markup).toContain('type="submit"')
    expect(markup).toContain('Analizar con IA')
    expect(markup).toContain('Proyecto académico desarrollado para CoderCUP IA 2026')
    expect(markup).not.toContain('Tu CV se utiliza únicamente para realizar este análisis.')
  })
})
