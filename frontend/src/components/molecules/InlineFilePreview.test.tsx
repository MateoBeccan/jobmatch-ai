import { renderToStaticMarkup } from 'react-dom/server'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useObjectUrl } from '../../lib/hooks/useObjectUrl'
import { InlineFilePreview } from './InlineFilePreview'

vi.mock('../../lib/hooks/useObjectUrl', () => ({
  useObjectUrl: vi.fn(),
}))

describe('InlineFilePreview', () => {
  beforeEach(() => {
    vi.mocked(useObjectUrl).mockReset()
  })

  it('renders automatic PDF preview with a local blob URL', () => {
    vi.mocked(useObjectUrl).mockReturnValue('blob:inline-preview')
    const file = new File(['pdf'], 'cv.pdf', { type: 'application/pdf' })

    const markup = renderToStaticMarkup(
      <InlineFilePreview file={file} type="pdf" title="Vista previa del CV" />,
    )

    expect(useObjectUrl).toHaveBeenCalledWith(file)
    expect(markup).toContain('<iframe')
    expect(markup).toContain('src="blob:inline-preview"')
    expect(markup).toContain('title="Vista previa del CV"')
  })

  it('does not render preview without a file', () => {
    vi.mocked(useObjectUrl).mockReturnValue(null)

    const markup = renderToStaticMarkup(
      <InlineFilePreview file={null} type="pdf" title="Vista previa del CV" />,
    )

    expect(markup).toBe('')
    expect(useObjectUrl).toHaveBeenCalledWith(null)
  })

  it('renders automatic image preview with a local blob URL', () => {
    vi.mocked(useObjectUrl).mockReturnValue('blob:inline-preview')
    const file = new File(['image'], 'oferta.png', { type: 'image/png' })

    const markup = renderToStaticMarkup(
      <InlineFilePreview file={file} type="image" title="Vista previa de la oferta" />,
    )

    expect(useObjectUrl).toHaveBeenCalledWith(file)
    expect(markup).toContain('<img')
    expect(markup).toContain('src="blob:inline-preview"')
    expect(markup).toContain('alt="Vista previa de oferta.png"')
  })
})
