import { describe, expect, it } from 'vitest'
import { calculateSimulatedCoverage } from './careerSimulation'
import type { CareerSkillDemand } from '../types/types'

describe('careerSimulation', () => {
  it('keeps coverage unchanged without selected skills', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java'], [])).toMatchObject({
      currentCalculatedCoverage: 50,
      simulatedCoverage: 50,
      delta: 0,
    })
  })

  it('adds one selected skill weight once', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java'], ['Docker'])).toMatchObject({
      currentCalculatedCoverage: 50,
      simulatedCoverage: 80,
      delta: 30,
    })
  })

  it('adds two selected skill weights', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java'], ['Docker', 'Testing'])).toMatchObject({
      currentCalculatedCoverage: 50,
      simulatedCoverage: 100,
      delta: 50,
    })
  })

  it('models deselection by removing the skill from selectedSkills', () => {
    const selected = calculateSimulatedCoverage(demand(), ['Java'], ['Docker'])
    const deselected = calculateSimulatedCoverage(demand(), ['Java'], [])

    expect(selected.simulatedCoverage).toBe(80)
    expect(deselected.simulatedCoverage).toBe(50)
  })

  it('does not count duplicate selected skills twice', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java'], ['Docker', ' docker '])).toMatchObject({
      simulatedCoverage: 80,
      delta: 30,
    })
  })

  it('does not increase coverage for current skills', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java'], ['Java'])).toMatchObject({
      simulatedCoverage: 50,
      delta: 0,
    })
  })

  it('does not increase coverage for unknown selected skills', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java'], ['GraphQL'])).toMatchObject({
      simulatedCoverage: 50,
      delta: 0,
    })
  })

  it('clamps simulated coverage to 100', () => {
    expect(calculateSimulatedCoverage(demand(), ['Java', 'Docker'], ['Testing'])).toMatchObject({
      simulatedCoverage: 100,
      delta: 20,
    })
  })

  it('returns zero coverage when totalWeight is zero', () => {
    expect(calculateSimulatedCoverage([], ['Java'], ['Docker'])).toEqual({
      currentCalculatedCoverage: 0,
      simulatedCoverage: 0,
      delta: 0,
      totalWeight: 0,
      currentCoveredWeight: 0,
      simulatedCoveredWeight: 0,
    })
  })

  it('is independent from array order', () => {
    const left = calculateSimulatedCoverage(demand(), ['Java'], ['Testing', 'Docker'])
    const right = calculateSimulatedCoverage([...demand()].reverse(), ['Java'], ['Docker', 'Testing'])

    expect(left).toEqual(right)
  })

  it('does not mutate inputs', () => {
    const skillDemand = demand()
    const currentSkills = ['Java']
    const selectedSkills = ['Docker']
    const originalDemand = structuredClone(skillDemand)
    const originalCurrent = [...currentSkills]
    const originalSelected = [...selectedSkills]

    calculateSimulatedCoverage(skillDemand, currentSkills, selectedSkills)

    expect(skillDemand).toEqual(originalDemand)
    expect(currentSkills).toEqual(originalCurrent)
    expect(selectedSkills).toEqual(originalSelected)
  })

  it('rounds consistently with backend Math.round formula', () => {
    expect(calculateSimulatedCoverage([
      { skill: 'Java', jobsMentioning: 1, frequencyPercentage: 33 },
      { skill: 'Docker', jobsMentioning: 2, frequencyPercentage: 67 },
    ], ['Java'], [])).toMatchObject({
      currentCalculatedCoverage: 33,
      simulatedCoverage: 33,
    })
  })
})

function demand(): CareerSkillDemand[] {
  return [
    { skill: 'Java', jobsMentioning: 5, frequencyPercentage: 50 },
    { skill: 'Docker', jobsMentioning: 3, frequencyPercentage: 30 },
    { skill: 'Testing', jobsMentioning: 2, frequencyPercentage: 20 },
  ]
}
