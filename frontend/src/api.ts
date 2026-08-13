import type { AnalysisResponse } from './types'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export async function analyzeJobMatch(
  cvFile: File,
  mode: 'text' | 'image',
  jobDescription: string,
  jobImage: File | null,
): Promise<AnalysisResponse> {
  const formData = new FormData()
  formData.append('cvFile', cvFile)

  if (mode === 'text') {
    formData.append('jobDescription', jobDescription)
  } else if (jobImage) {
    formData.append('jobImage', jobImage)
  }

  const response = await fetch(`${API_URL}/api/analyze`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(errorBody?.message ?? 'No se pudo completar el análisis.')
  }

  return response.json() as Promise<AnalysisResponse>
}
