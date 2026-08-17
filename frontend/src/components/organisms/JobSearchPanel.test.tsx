import { renderToStaticMarkup } from 'react-dom/server'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { searchJobs } from '../../services/api'
import { JobSearchPanel } from './JobSearchPanel'
import type { JobSearchProfile } from '../../lib/types/types'

vi.mock('../../services/api', () => ({
  searchJobs: vi.fn(),
}))

describe('JobSearchPanel', () => {
  beforeEach(() => {
    vi.mocked(searchJobs).mockReset()
  })

  it('renders profile, location selector, privacy note, and does not search automatically', () => {
    const markup = renderToStaticMarkup(<JobSearchPanel profile={profile()} />)

    expect(markup).toContain('Ofertas remotas relacionadas con tu perfil')
    expect(markup).toContain('Java Backend Developer')
    expect(markup).toContain('Junior')
    expect(markup).toContain('Región de búsqueda')
    expect(markup).toContain('value="Argentina"')
    expect(markup).toContain('Latinoamérica')
    expect(markup).toContain('value="Global"')
    expect(markup).toContain('Buscar ofertas')
    expect(markup).toContain('Tu CV no se envía a Jobicy')
    expect(markup).not.toContain('Buscando ofertas...')
    expect(markup).not.toContain('compatible')
    expect(searchJobs).not.toHaveBeenCalled()
  })
})

function profile(): JobSearchProfile {
  return {
    role: 'Java Backend Developer',
    seniority: 'JUNIOR',
    keywords: ['Java', 'Spring Boot', 'SQL', 'REST API'],
  }
}
