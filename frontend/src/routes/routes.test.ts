import { describe, expect, it } from 'vitest'
import { normalizeRoute, parseRoute } from './routes'

describe('routes', () => {
  it('parses the home route', () => {
    expect(parseRoute('/')).toEqual({ name: 'home' })
  })

  it('keeps the analyze route unchanged', () => {
    expect(parseRoute('/analizar')).toEqual({ name: 'analyze' })
  })

  it('keeps history and detail routes unchanged', () => {
    expect(parseRoute('/historial')).toEqual({ name: 'history' })
    expect(parseRoute('/historial/abc-123')).toEqual({ name: 'detail', id: 'abc-123' })
  })

  it('keeps the Career Multiverse route unchanged', () => {
    expect(normalizeRoute('/multiverso')).toBe('/multiverso')
    expect(parseRoute('/multiverso')).toEqual({ name: 'multiverse' })
  })

  it('normalizes unknown routes to home', () => {
    expect(normalizeRoute('/no-existe')).toBe('/')
    expect(parseRoute('/no-existe')).toEqual({ name: 'home' })
  })
})
