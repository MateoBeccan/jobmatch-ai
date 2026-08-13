export type AnalysisResponse = {
  matchPercentage: number
  matchingSkills: string[]
  missingSkills: string[]
  recommendations: string[]
  interviewQuestions: string[]
}

export type AnalysisMode = 'text' | 'image'
