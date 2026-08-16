import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const reactHooks = vi.hoisted(() => ({
  cleanup: undefined as (() => void) | undefined,
  state: null as string | null,
}))

vi.mock('react', () => ({
  useState: (initialValue: string | null) => [
    reactHooks.state ?? initialValue,
    (nextValue: string | null) => {
      reactHooks.state = nextValue
    },
  ],
  useEffect: (effect: () => void | (() => void)) => {
    reactHooks.cleanup = effect() ?? undefined
  },
}))

describe('useObjectUrl', () => {
  const originalCreateObjectURL = URL.createObjectURL
  const originalRevokeObjectURL = URL.revokeObjectURL

  beforeEach(() => {
    reactHooks.cleanup = undefined
    reactHooks.state = null
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: vi.fn((file: File) => `blob:${file.name}`),
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: vi.fn(),
    })
  })

  afterEach(() => {
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: originalCreateObjectURL,
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: originalRevokeObjectURL,
    })
    vi.resetModules()
  })

  it('creates an object URL inside the effect', async () => {
    const { useObjectUrl } = await import('./useObjectUrl')
    const file = new File(['pdf'], 'cv.pdf', { type: 'application/pdf' })

    const objectUrl = useObjectUrl(file)

    expect(objectUrl).toBeNull()
    expect(URL.createObjectURL).toHaveBeenCalledWith(file)
    expect(reactHooks.state).toBe('blob:cv.pdf')
  })

  it('revokes the previous object URL when the file changes', async () => {
    const { useObjectUrl } = await import('./useObjectUrl')
    const firstFile = new File(['pdf'], 'cv-v1.pdf', { type: 'application/pdf' })
    const secondFile = new File(['pdf'], 'cv-v2.pdf', { type: 'application/pdf' })

    useObjectUrl(firstFile)
    reactHooks.cleanup?.()
    useObjectUrl(secondFile)

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:cv-v1.pdf')
    expect(URL.createObjectURL).toHaveBeenCalledWith(secondFile)
    expect(reactHooks.state).toBe('blob:cv-v2.pdf')
  })

  it('revokes the current object URL when the file changes to null', async () => {
    const { useObjectUrl } = await import('./useObjectUrl')
    const file = new File(['image'], 'oferta.png', { type: 'image/png' })

    useObjectUrl(file)
    reactHooks.cleanup?.()
    const objectUrl = useObjectUrl(null)

    expect(objectUrl).toBeNull()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:oferta.png')
    expect(reactHooks.state).toBeNull()
  })

  it('revokes the current object URL on unmount', async () => {
    const { useObjectUrl } = await import('./useObjectUrl')
    const file = new File(['pdf'], 'cv.pdf', { type: 'application/pdf' })

    useObjectUrl(file)
    reactHooks.cleanup?.()

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:cv.pdf')
  })

  it('does not create an object URL when file is null', async () => {
    const { useObjectUrl } = await import('./useObjectUrl')

    const objectUrl = useObjectUrl(null)

    expect(objectUrl).toBeNull()
    expect(URL.createObjectURL).not.toHaveBeenCalled()
    expect(reactHooks.state).toBeNull()
  })
})
