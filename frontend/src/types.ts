export type AnalysisResponse = {
  matchPercentage: number
  matchingSkills: string[]
  missingSkills: string[]
  recommendations: string[]
  interviewQuestions: string[]
}

export type AnalysisMode = 'text' | 'image'
export type Theme = 'light' | 'dark'

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
