export type AnalysisResponse = {
  matchPercentage: number
  matchingSkills: string[]
  missingSkills: string[]
  recommendations: string[]
  interviewQuestions: string[]
}

export type AnalysisMode = 'text' | 'image'

export type HistoryRecord = {
  id: string
  role: string
  company: string
  cvFileName: string
  cvVersion: string
  jobDescription: string
  mode: AnalysisMode
  score: number
  createdAt: number
  result: AnalysisResponse
}
