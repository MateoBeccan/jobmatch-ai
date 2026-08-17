import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { JobOfferCard } from './JobOfferCard'
import type { JobOffer } from '../../lib/types/types'

describe('JobOfferCard', () => {
  it('renders job details, keywords, date, and safe external link without match percentage', () => {
    const markup = renderToStaticMarkup(<JobOfferCard job={jobOffer()} />)

    expect(markup).toContain('Full Stack Developer - Java &amp; React')
    expect(markup).toContain('Example')
    expect(markup).toContain('LATAM')
    expect(markup).toContain('Full-Time')
    expect(markup).toContain('USD 100000 - 140000 yearly')
    expect(markup).toContain('Publicada')
    expect(markup).toContain('Java and Spring Boot role')
    expect(markup).toContain('Coincidencias con tu perfil')
    expect(markup).toContain('Java')
    expect(markup).toContain('Spring Boot')
    expect(markup).toContain('Fuente: Jobicy')
    expect(markup).toContain('href="https://jobicy.com/jobs/full-stack-java-react"')
    expect(markup).toContain('target="_blank"')
    expect(markup).toContain('rel="noopener noreferrer"')
    expect(markup).not.toContain('compatible')
    expect(markup).not.toContain('Match score')
  })

  it('omits nullable fields and renders snippet as escaped text', () => {
    const markup = renderToStaticMarkup(<JobOfferCard job={{
      ...jobOffer(),
      company: null,
      location: null,
      salary: null,
      employmentType: null,
      updatedAt: 'not-a-date',
      snippet: '<strong>Java</strong>',
    }} />)

    expect(markup).not.toContain('Example')
    expect(markup).not.toContain('USD')
    expect(markup).not.toContain('Publicada')
    expect(markup).toContain('&lt;strong&gt;Java&lt;/strong&gt;')
  })
})

function jobOffer(): JobOffer {
  return {
    id: '150845',
    title: 'Full Stack Developer - Java & React',
    company: 'Example',
    location: 'LATAM',
    snippet: 'Java and Spring Boot role',
    salary: 'USD 100000 - 140000 yearly',
    employmentType: 'Full-Time',
    updatedAt: '2026-08-16T14:51:50+00:00',
    url: 'https://jobicy.com/jobs/full-stack-java-react',
    source: 'Jobicy',
    matchedKeywords: ['Java', 'Spring Boot'],
  }
}
