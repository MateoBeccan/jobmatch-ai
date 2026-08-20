import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { FileUploadCard } from './FileUploadCard'

function renderUploadCard(props: Partial<Parameters<typeof FileUploadCard>[0]> = {}) {
  return renderToStaticMarkup(
    <FileUploadCard
      file={null}
      isDragging={false}
      onChange={vi.fn()}
      onRemove={vi.fn()}
      onDrop={vi.fn()}
      onDragOver={vi.fn()}
      onDragLeave={vi.fn()}
      {...props}
    />,
  )
}

describe('FileUploadCard', () => {
  it('shows the empty CV dropzone guidance', () => {
    const markup = renderUploadCard()

    expect(markup).toContain('Arrastrá tu CV acá')
    expect(markup).toContain('o hacé clic para seleccionarlo')
    expect(markup).toContain('PDF · máximo 5 MB')
    expect(markup).toContain('Seleccionar archivo')
  })

  it('shows a compact ready state for a valid PDF', () => {
    const markup = renderUploadCard({
      file: { name: 'Mateo_Beccan_Agosto2026.pdf', size: 552960 },
    })

    expect(markup).toContain('Mateo_Beccan_Agosto2026.pdf')
    expect(markup).toContain('540 KB · PDF')
    expect(markup).toContain('CV listo')
    expect(markup).toContain('Cambiar archivo')
    expect(markup).toContain('Eliminar')
  })

  it('returns to the empty state when no file is provided', () => {
    const markup = renderUploadCard({ file: null })

    expect(markup).toContain('Arrastrá tu CV acá')
    expect(markup).not.toContain('CV listo')
  })

  it('shows the existing CV validation error inside the dropzone', () => {
    const markup = renderUploadCard({
      errorMessage: 'El CV debe ser un archivo PDF.',
    })

    expect(markup).toContain('No pudimos cargar este archivo')
    expect(markup).toContain('El CV debe ser un archivo PDF.')
    expect(markup).toContain('Intentar nuevamente')
  })

  it('shows the drag active copy while a file is over the dropzone', () => {
    const markup = renderUploadCard({ isDragging: true })

    expect(markup).toContain('Soltá tu CV para cargarlo')
    expect(markup).toContain('PDF detectado')
  })
})
