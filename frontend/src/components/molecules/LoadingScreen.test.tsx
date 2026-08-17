import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { LoadingScreen } from './LoadingScreen'

describe('LoadingScreen', () => {
  it('shows server preparation copy during preparing phase', () => {
    const markup = renderToStaticMarkup(<LoadingScreen phase="preparing" />)

    expect(markup).toContain('PREPARANDO SERVICIO')
    expect(markup).toContain('Preparando el servidor')
  })

  it('does not show analysis step copy during preparing phase', () => {
    const markup = renderToStaticMarkup(<LoadingScreen phase="preparing" />)

    expect(markup).not.toContain('role="progressbar"')
    expect(markup).not.toContain('Extrayendo tus habilidades')
    expect(markup).not.toContain('Extrayendo habilidades')
    expect(markup).not.toContain('Comparando requisitos')
    expect(markup).not.toContain('Generando recomendaciones')
  })

  it('keeps analysis copy during analyzing phase', () => {
    const markup = renderToStaticMarkup(<LoadingScreen phase="analyzing" />)

    expect(markup).toContain('role="progressbar"')
    expect(markup).toContain('aria-valuenow="22"')
    expect(markup).toContain('Leyendo tu CV')
    expect(markup).toContain('Extrayendo habilidades')
    expect(markup).toContain('Comparando requisitos')
    expect(markup).toContain('Generando recomendaciones')
  })
})
