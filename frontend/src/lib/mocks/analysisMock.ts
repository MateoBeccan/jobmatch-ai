import type { AnalysisResponse } from '../types/types'

/**
 * Respuesta de ejemplo utilizada únicamente en desarrollo cuando
 * VITE_USE_MOCKS está habilitado. Nunca se incluye en el bundle de producción.
 */
export const mockAnalysisResponse: AnalysisResponse = {
  matchPercentage: 82,
  matchingSkills: ['JavaScript', 'React', 'TypeScript', 'Git'],
  missingSkills: ['Docker', 'AWS'],
  recommendations: [
    'Destacar la experiencia con React en el primer puesto de experiencia laboral.',
    'Mencionar Docker si tenés experiencia real con contenedores.',
    'Preparar ejemplos concretos sobre consumo de APIs REST.',
  ],
  interviewQuestions: [
    '¿Cómo manejás el estado global en una aplicación React?',
    '¿Qué experiencia tenés con TypeScript en proyectos productivos?',
    '¿Cómo optimizarías el renderizado de listas largas en React?',
  ],
  requirements: [
    { name: 'JavaScript', status: 'match', evidence: 'Proyectos con JavaScript y React mencionados en el CV.' },
    { name: 'React', status: 'match', evidence: 'Experiencia de 2 años desarrollando con React.' },
    { name: 'TypeScript', status: 'match', evidence: 'Proyectos con TypeScript mencionados en el CV.' },
    { name: 'Docker', status: 'partial', evidence: 'Se menciona Docker en una lista de tecnologías, sin proyectos concretos.' },
    { name: 'AWS', status: 'missing', evidence: '' },
    { name: 'Git', status: 'match', evidence: 'Uso de Git en repositorios de proyectos anteriores.' },
  ],
  breakdown: {
    mandatoryTechnical: 83,
    experienceSeniority: 80,
    desirable: 50,
    complementary: 100,
  },
}

export function getMockEnabled(): boolean {
  return import.meta.env.VITE_USE_MOCKS === 'true'
}
