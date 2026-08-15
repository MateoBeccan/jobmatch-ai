import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import { getScoreClass } from '../../lib/helpers/format'

type ScoreRingProps = {
  value: number
  size?: number
  label?: string
}

export function ScoreRing({ value, size = 190, label = 'Compatibilidad' }: ScoreRingProps) {
  const reducedMotion = useRef(
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches,
  ).current
  const [displayed, setDisplayed] = useState(reducedMotion ? value : 0)
  const scoreClass = getScoreClass(value)

  useEffect(() => {
    if (reducedMotion) return
    let frame = 0
    const start = performance.now()
    const duration = 1200
    const step = (now: number) => {
      const progress = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - progress, 3)
      setDisplayed(Math.round(eased * value))
      if (progress < 1) frame = window.requestAnimationFrame(step)
    }
    frame = window.requestAnimationFrame(step)
    return () => window.cancelAnimationFrame(frame)
  }, [value, reducedMotion])

  return (
    <div
      className={`score-ring ${scoreClass}`}
      role="progressbar"
      aria-label={`${value}% de compatibilidad`}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={value}
      style={{ '--score': displayed, width: size, height: size } as CSSProperties}
    >
      <div className="score-ring-value">
        <strong>{displayed}</strong>
        <span>%</span>
      </div>
      <small>{label}</small>
    </div>
  )
}
