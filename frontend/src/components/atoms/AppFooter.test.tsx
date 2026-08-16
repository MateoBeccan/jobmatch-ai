import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { AppFooter } from './AppFooter'

describe('AppFooter', () => {
  it('renders academic project credits', () => {
    const markup = renderToStaticMarkup(<AppFooter />)

    expect(markup).toContain('<footer')
    expect(markup).toContain('JobMatch AI')
    expect(markup).toContain('Proyecto académico desarrollado para CoderCUP IA 2026')
    expect(markup).toContain('Mateo Beccan')
    expect(markup).toContain('Francisco Lorenzo')
    expect(markup).toContain('Google Gemini')
  })
})
