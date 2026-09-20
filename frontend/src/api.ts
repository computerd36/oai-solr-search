export type Hit = {
  ppn: string
  title: string | null
  creators: string[]
  date: string | null
  subjects: string[]
  languages: string[]
  types: string[]
  url: string | null
}

export type FacetValue = { value: string; count: number }

export type SearchResult = {
  total: number
  page: number
  size: number
  items: Hit[]
  facets: Record<string, FacetValue[]>
}

export type Filters = Record<string, string[]>

export const FACET_LABELS: Record<string, string> = {
  creator: 'Verfasser',
  subject: 'Schlagwort',
  language: 'Sprache',
  year: 'Jahr',
}

export async function search(
  q: string,
  page: number,
  size: number,
  filters: Filters,
  signal: AbortSignal,
): Promise<SearchResult> {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  params.set('page', String(page))
  params.set('size', String(size))
  for (const [name, values] of Object.entries(filters)) {
    for (const value of values) params.append(name, value)
  }

  const response = await fetch(`/api/search?${params}`, { signal })
  if (!response.ok) {
    // the api answers with a problem detail, its message is more useful than the status
    const problem = await response.json().catch(() => null)
    throw new Error(problem?.detail ?? `Die Suche antwortete mit Status ${response.status}.`)
  }
  return response.json()
}
