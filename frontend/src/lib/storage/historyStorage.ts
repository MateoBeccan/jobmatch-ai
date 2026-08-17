import type { HistoryRecord } from '../types/types'

const HISTORY_STORAGE_KEY = 'jobmatch-ai-history'

// Keep the local MVP history bounded so localStorage cannot grow forever.
const MAX_HISTORY_RECORDS = 50

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isScore(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= 100
}

function isRequirementStatus(value: unknown) {
  return value === 'match' || value === 'partial' || value === 'missing'
}

function isRequirements(value: unknown) {
  if (value === undefined) return true
  if (!Array.isArray(value)) return false
  return value.every((item) => {
    if (!isRecord(item)) return false
    return typeof item.name === 'string'
      && (item.category === undefined || typeof item.category === 'string')
      && isRequirementStatus(item.status)
      && (item.evidence === undefined || typeof item.evidence === 'string')
  })
}

function isBreakdown(value: unknown) {
  if (value === undefined) return true
  if (!isRecord(value)) return false
  const keys = ['mandatoryTechnical', 'experienceSeniority', 'desirable', 'complementary']
  return keys.every((key) => value[key] === undefined || isScore(value[key]))
}

function isJobSeniority(value: unknown) {
  return value === 'TRAINEE'
    || value === 'JUNIOR'
    || value === 'MID'
    || value === 'SENIOR'
    || value === 'UNSPECIFIED'
}

function isJobSearchProfile(value: unknown) {
  if (value === undefined) return true
  if (!isRecord(value)) return false
  return typeof value.role === 'string'
    && isJobSeniority(value.seniority)
    && isStringArray(value.keywords)
}

function isHistoryRecord(value: unknown): value is HistoryRecord {
  if (!isRecord(value)) return false
  const result = value.result
  return typeof value.id === 'string'
    && typeof value.role === 'string'
    && typeof value.company === 'string'
    && typeof value.cvFileName === 'string'
    && typeof value.cvVersion === 'string'
    && (value.mode === 'text' || value.mode === 'image')
    && isScore(value.score)
    && typeof value.createdAt === 'number'
    && Number.isFinite(value.createdAt)
    && typeof value.jobDescription === 'string'
    && isRecord(result)
    && isScore(result.matchPercentage)
    && isStringArray(result.matchingSkills)
    && isStringArray(result.missingSkills)
    && isStringArray(result.recommendations)
    && isStringArray(result.interviewQuestions)
    && isRequirements(result.requirements)
    && isBreakdown(result.breakdown)
    && isJobSearchProfile(result.jobSearchProfile)
}

function readHistory(): HistoryRecord[] {
  try {
    const raw = window.localStorage.getItem(HISTORY_STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed.filter(isHistoryRecord)
  } catch {
    return []
  }
}

function writeHistory(records: HistoryRecord[]) {
  try {
    window.localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(records.slice(0, MAX_HISTORY_RECORDS)))
  } catch {
    // Storage can be unavailable or full; the analysis flow should still complete.
  }
}

export function getHistory(): HistoryRecord[] {
  return readHistory().sort((a, b) => b.createdAt - a.createdAt)
}

export function getHistoryRecord(id: string): HistoryRecord | null {
  return getHistory().find((record) => record.id === id) ?? null
}

export function saveHistoryRecord(record: HistoryRecord): HistoryRecord {
  const next = [record, ...getHistory().filter((current) => current.id !== record.id)]
    .slice(0, MAX_HISTORY_RECORDS)
  writeHistory(next)
  return record
}

export function deleteHistoryRecord(id: string) {
  writeHistory(getHistory().filter((record) => record.id !== id))
}

export function clearHistory() {
  writeHistory([])
}
