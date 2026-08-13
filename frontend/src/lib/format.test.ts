import { describe, expect, it } from 'vitest'
import { getScoreClass } from './format'

describe('getScoreClass', () => {
  it('uses red for scores below thirty', () => {
    expect(getScoreClass(0)).toBe('low')
    expect(getScoreClass(29)).toBe('low')
  })

  it('uses amber for scores from thirty to fifty-nine', () => {
    expect(getScoreClass(30)).toBe('medium')
    expect(getScoreClass(59)).toBe('medium')
  })

  it('uses green for scores from sixty', () => {
    expect(getScoreClass(60)).toBe('high')
    expect(getScoreClass(100)).toBe('high')
  })
})
