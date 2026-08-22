import { useEffect, useMemo, useState } from 'react'
import type { CareerLearningPriority, CareerLearningPriorityItem, CareerPathMarket } from '../../lib/types/types'
import { calculateSimulatedCoverage, skillKey } from '../../lib/helpers/careerSimulation'

type CareerWhatIfSimulatorProps = {
  market: CareerPathMarket
  learningPriorities: CareerLearningPriorityItem[]
  provider?: string
}

const MAX_SIMULABLE_SKILLS = 6
const BASELINE_TOLERANCE_POINTS = 1

const PRIORITY_LABELS: Record<CareerLearningPriority, string> = {
  NOW: 'Aprende ahora',
  NEXT: 'Proximo',
  LATER: 'Puede esperar',
}

export function CareerWhatIfSimulator({ market, learningPriorities, provider }: CareerWhatIfSimulatorProps) {
  const [selectedSkills, setSelectedSkills] = useState<string[]>([])
  const priorityBySkill = useMemo(() => priorityMap(learningPriorities), [learningPriorities])
  const allSimulableSkills = useMemo(() => {
    const currentKeys = new Set(market.currentSkillsDetected.map(skillKey))
    return market.skillDemand
      .filter((demand) => !currentKeys.has(skillKey(demand.skill)))
      .sort((left, right) => right.jobsMentioning - left.jobsMentioning || left.skill.localeCompare(right.skill))
  }, [market.currentSkillsDetected, market.skillDemand])
  const simulableSkills = allSimulableSkills.slice(0, MAX_SIMULABLE_SKILLS)
  const hasHiddenSimulableSkills = allSimulableSkills.length > simulableSkills.length
  const simulation = useMemo(() => calculateSimulatedCoverage(
    market.skillDemand,
    market.currentSkillsDetected,
    selectedSkills,
  ), [market.currentSkillsDetected, market.skillDemand, selectedSkills])
  const baselineMatches = Math.abs(simulation.currentCalculatedCoverage - market.coveragePercentage) <= BASELINE_TOLERANCE_POINTS
  const simulatedCoverage = baselineMatches
    ? clampCoverage(market.coveragePercentage + (simulation.simulatedCoverage - simulation.currentCalculatedCoverage))
    : market.coveragePercentage
  const delta = simulatedCoverage - market.coveragePercentage

  useEffect(() => {
    setSelectedSkills([])
  }, [market])

  function toggleSkill(skill: string, checked: boolean) {
    setSelectedSkills((current) => {
      if (checked) {
        return current.some((selected) => skillKey(selected) === skillKey(skill)) ? current : [...current, skill]
      }
      return current.filter((selected) => skillKey(selected) !== skillKey(skill))
    })
  }

  if (market.confidence === 'INSUFFICIENT') {
    return (
      <section className="career-what-if career-what-if-state" aria-labelledby="career-what-if-title">
        <span className="career-kicker">Simula tu evolucion</span>
        <h3 id="career-what-if-title">Que pasaria si incorporaras estas habilidades?</h3>
        <p>Necesitamos una muestra mas amplia para simular este camino de forma responsable.</p>
      </section>
    )
  }

  if (!baselineMatches) {
    return (
      <section className="career-what-if career-what-if-state" aria-labelledby="career-what-if-title">
        <span className="career-kicker">Simula tu evolucion</span>
        <h3 id="career-what-if-title">Que pasaria si incorporaras estas habilidades?</h3>
        <p>Los datos publicos del response no permiten reproducir coverage de forma fiable para este camino.</p>
      </section>
    )
  }

  if (market.coveragePercentage >= 100) {
    return (
      <section className="career-what-if career-what-if-state" aria-labelledby="career-what-if-title">
        <span className="career-kicker">Simula tu evolucion</span>
        <h3 id="career-what-if-title">Que pasaria si incorporaras estas habilidades?</h3>
        <CoverageComparison current={market.coveragePercentage} simulated={market.coveragePercentage} delta={0} selectedCount={0} />
        <p>Cobertura completa dentro de las habilidades observadas en esta muestra.</p>
        <WhatIfContext market={market} provider={provider} />
      </section>
    )
  }

  if (simulableSkills.length === 0) {
    return (
      <section className="career-what-if career-what-if-state" aria-labelledby="career-what-if-title">
        <span className="career-kicker">Simula tu evolucion</span>
        <h3 id="career-what-if-title">Que pasaria si incorporaras estas habilidades?</h3>
        <CoverageComparison current={market.coveragePercentage} simulated={market.coveragePercentage} delta={0} selectedCount={0} />
        <p>Tu perfil ya cubre las principales habilidades detectadas en esta muestra.</p>
        <WhatIfContext market={market} provider={provider} />
      </section>
    )
  }

  return (
    <section className="career-what-if" aria-labelledby="career-what-if-title">
      <div className="career-what-if-heading">
        <span className="career-kicker">Simula tu evolucion</span>
        <h3 id="career-what-if-title">Que pasaria si incorporaras estas habilidades?</h3>
        <p>Selecciona habilidades para ver como cambiaria la cobertura de tu perfil dentro de esta muestra de ofertas.</p>
      </div>

      <div className="career-sim-skill-grid" aria-label="Habilidades simulables">
        {simulableSkills.map((demand) => {
          const checked = selectedSkills.some((selected) => skillKey(selected) === skillKey(demand.skill))
          const priority = priorityBySkill.get(skillKey(demand.skill))
          return (
            <label key={demand.skill} className={`career-sim-skill ${checked ? 'selected' : ''}`}>
              <input
                type="checkbox"
                checked={checked}
                onChange={(event) => toggleSkill(demand.skill, event.currentTarget.checked)}
              />
              <span>
                <strong>{demand.skill}</strong>
                <small>{demand.frequencyPercentage}% de la muestra</small>
                {priority && <em>{PRIORITY_LABELS[priority]}</em>}
              </span>
            </label>
          )
        })}
      </div>

      {hasHiddenSimulableSkills && (
        <p className="career-sim-limit-note">
          Mostramos las 6 habilidades con mayor presencia en la muestra. La cobertura tambien considera otras habilidades detectadas.
        </p>
      )}

      <CoverageComparison
        current={market.coveragePercentage}
        simulated={simulatedCoverage}
        delta={delta}
        selectedCount={selectedSkills.length}
      />

      {market.confidence === 'LOW' && (
        <p className="career-what-if-warning">Muestra pequena: interpreta esta simulacion como orientativa.</p>
      )}

      <WhatIfContext market={market} provider={provider} />
    </section>
  )
}

function CoverageComparison({
  current,
  simulated,
  delta,
  selectedCount,
}: {
  current: number
  simulated: number
  delta: number
  selectedCount: number
}) {
  return (
    <div className="career-sim-comparison">
      <div className="career-sim-metric">
        <span>Actual</span>
        <strong>{current}%</strong>
        <ProgressBar value={current} label={`Cobertura actual ${current} por ciento`} />
      </div>
      <span className="career-sim-arrow" aria-hidden="true">-&gt;</span>
      <div className="career-sim-metric">
        <span>Simulado</span>
        <strong>{simulated}%</strong>
        <ProgressBar value={simulated} label={`Cobertura simulada ${simulated} por ciento`} />
      </div>
      <p className="career-sim-delta" aria-live="polite">
        {selectedCount === 0
          ? 'Selecciona una habilidad para comenzar.'
          : `${delta >= 0 ? '+' : ''}${delta} puntos de cobertura`}
      </p>
    </div>
  )
}

function ProgressBar({ value, label }: { value: number; label: string }) {
  return (
    <span
      className="career-sim-progress"
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={value}
    >
      <span style={{ width: `${value}%` }} />
    </span>
  )
}

function WhatIfContext({ market, provider }: { market: CareerPathMarket; provider?: string }) {
  return (
    <div className="career-what-if-context">
      <p>Esta simulacion representa cobertura de habilidades dentro de la muestra actual. No representa una probabilidad de contratacion.</p>
      <small>
        Simulacion basada en {market.sampleSize} ofertas relacionadas.
        {provider ? ` Fuente de la muestra: ${provider}.` : ''}
      </small>
    </div>
  )
}

function priorityMap(priorities: CareerLearningPriorityItem[]) {
  return new Map(priorities.map((priority) => [skillKey(priority.skill), priority.priority]))
}

function clampCoverage(value: number) {
  return Math.max(0, Math.min(100, value))
}
