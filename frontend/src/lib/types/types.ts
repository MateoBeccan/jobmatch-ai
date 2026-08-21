export type AnalysisMode = 'text' | 'image'
export type Theme = 'light' | 'dark'

export type RequirementStatus = 'match' | 'partial' | 'missing'

export type RequirementCategory = 'mandatory_technical' | 'experience_seniority' | 'desirable' | 'complementary'

export type RequirementMatch = {
  name: string
  category?: RequirementCategory
  status: RequirementStatus
  evidence?: string
}

export type ScoreBreakdown = {
  mandatoryTechnical?: number
  experienceSeniority?: number
  desirable?: number
  complementary?: number
}

export type CriticalRequirementGap = {
  requirement: string
  category: string
  evidence: string
}

export type ExperienceGap = {
  requirement: string
  status: RequirementStatus
  critical: boolean
  summary: string
}

export type JobSeniority =
  | 'TRAINEE'
  | 'JUNIOR'
  | 'MID'
  | 'SENIOR'
  | 'UNSPECIFIED'

export type JobSearchProfile = {
  role: string
  seniority: JobSeniority
  keywords: string[]
}

export type JobSearchLocation =
  | 'Argentina'
  | 'LATAM'
  | 'Global'

export type JobSearchRequest = {
  role: string
  seniority: JobSeniority
  keywords: string[]
  location: JobSearchLocation
}

export type JobOffer = {
  id: string | null
  title: string
  company: string | null
  location: string | null
  snippet: string | null
  salary: string | null
  employmentType: string | null
  updatedAt: string | null
  url: string
  source: string
  matchedKeywords: string[]
}

export type JobSearchResponse = {
  provider: string
  count: number
  jobs: JobOffer[]
}

export type ScoreFactorType = 'positive' | 'partial' | 'missing'

export type ScoreFactor = {
  type: ScoreFactorType
  text: string
}

export type ScoreExplanation = {
  summary: string
  factors: ScoreFactor[]
}

export type Recommendation = {
  problem: string
  explanation?: string
  action?: string
}

export type CvSuggestionType = 'skill' | 'wording' | 'structure'

export type CvSuggestion = {
  id: string
  type: CvSuggestionType
  title: string
  detail: string
  action: string
}

export type AnalysisResponse = {
  matchPercentage: number
  matchingSkills: string[]
  missingSkills: string[]
  criticalMissingRequirements?: CriticalRequirementGap[]
  experienceGap?: ExperienceGap | null
  warnings?: string[]
  recommendations: string[]
  interviewQuestions: string[]
  requirements?: RequirementMatch[]
  breakdown?: ScoreBreakdown
  jobSearchProfile?: JobSearchProfile
}

export type AnalysisSummary = {
  id: string
  role: string
  company: string
  cvFileName: string
  cvVersion: string
  mode: AnalysisMode
  score: number
  createdAt: number
}

export type AnalysisHistoryPage = {
  content: AnalysisSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type HistoryRecord = AnalysisSummary & {
  jobDescription: string
  result: AnalysisResponse
}

export type ScoreRange = 'all' | 'top' | 'mid' | 'low'
export type HistorySort = 'date-desc' | 'date-asc' | 'score-desc' | 'score-asc'

export type HistoryStats = {
  total: number
  averageScore: number
  bestScore: number
  trendDelta?: number
}

export type AnalysisComparison = {
  role: string
  company: string
  previousCvVersion: string
  currentCvVersion: string
  previousScore: number
  currentScore: number
  difference: number
  newMatches: string[]
  stillMissing: string[]
  notes: string[]
}
