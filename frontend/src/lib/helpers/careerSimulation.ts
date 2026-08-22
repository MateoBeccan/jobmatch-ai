import type { CareerSkillDemand } from '../types/types'

export type CareerCoverageSimulation = {
  currentCalculatedCoverage: number
  simulatedCoverage: number
  delta: number
  totalWeight: number
  currentCoveredWeight: number
  simulatedCoveredWeight: number
}

export function calculateSimulatedCoverage(
  skillDemand: CareerSkillDemand[],
  currentSkillsDetected: string[],
  selectedSkills: string[],
): CareerCoverageSimulation {
  const currentKeys = new Set(currentSkillsDetected.map(skillKey))
  const selectedKeys = new Set(selectedSkills.map(skillKey))
  const totalWeight = skillDemand.reduce((sum, demand) => sum + Math.max(0, demand.jobsMentioning), 0)

  if (totalWeight <= 0) {
    return {
      currentCalculatedCoverage: 0,
      simulatedCoverage: 0,
      delta: 0,
      totalWeight: 0,
      currentCoveredWeight: 0,
      simulatedCoveredWeight: 0,
    }
  }

  const currentCoveredWeight = skillDemand
    .filter((demand) => currentKeys.has(skillKey(demand.skill)))
    .reduce((sum, demand) => sum + Math.max(0, demand.jobsMentioning), 0)
  const selectedCoveredWeight = skillDemand
    .filter((demand) => {
      const key = skillKey(demand.skill)
      return selectedKeys.has(key) && !currentKeys.has(key)
    })
    .reduce((sum, demand) => sum + Math.max(0, demand.jobsMentioning), 0)
  const simulatedCoveredWeight = Math.min(totalWeight, currentCoveredWeight + selectedCoveredWeight)
  const currentCalculatedCoverage = percentage(currentCoveredWeight, totalWeight)
  const simulatedCoverage = percentage(simulatedCoveredWeight, totalWeight)

  return {
    currentCalculatedCoverage,
    simulatedCoverage,
    delta: simulatedCoverage - currentCalculatedCoverage,
    totalWeight,
    currentCoveredWeight,
    simulatedCoveredWeight,
  }
}

export function skillKey(skill: string) {
  return skill.trim().toLowerCase()
}

function percentage(numerator: number, denominator: number) {
  if (denominator <= 0) return 0
  return Math.max(0, Math.min(100, Math.round((numerator * 100) / denominator)))
}
