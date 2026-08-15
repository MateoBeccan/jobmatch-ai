import { describe, expect, it } from 'vitest'
import { getScoreClass } from './format'

describe('getScoreClass', () => {
  it('uses red for scores below sixty', () => {
    expect(getScoreClass(0)).toBe('low')
    expect(getScoreClass(59)).toBe('low')
  })

  it('uses amber for scores from sixty to seventy-nine', () => {
    expect(getScoreClass(60)).toBe('medium')
    expect(getScoreClass(79)).toBe('medium')
  })

  it('uses green for scores from eighty', () => {
    expect(getScoreClass(80)).toBe('high')
    expect(getScoreClass(100)).toBe('high')
  })
})
