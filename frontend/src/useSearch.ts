import { useEffect, useState } from 'react'
import { search, type Filters, type SearchResult } from './api'

export function useSearch(query: string, page: number, size: number, filters: Filters) {
  const [result, setResult] = useState<SearchResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)

    search(query, page, size, filters, controller.signal)
      .then(setResult)
      .catch((e: unknown) => {
        if (e instanceof Error && e.name !== 'AbortError') setError(e.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [query, page, size, filters])

  return { result, loading, error }
}
