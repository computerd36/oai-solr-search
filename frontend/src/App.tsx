import { useEffect, useState } from 'react'
import type { Filters } from './api'
import Facets from './components/Facets'
import Results from './components/Results'
import SearchField from './components/SearchField'
import { useSearch } from './useSearch'

const SIZE = 20

export default function App() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [filters, setFilters] = useState<Filters>({})
  const [page, setPage] = useState(0)

  // debounce, otherwise every keystroke hits solr. the page reset belongs in
  // the same update, else the old page fires a request that is aborted again
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(input)
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [input])

  const { result, loading, error } = useSearch(query, page, SIZE, filters)

  function toggleFilter(facet: string, value: string) {
    setFilters((current) => {
      const values = current[facet] ?? []
      const next = values.includes(value)
        ? values.filter((v) => v !== value)
        : [...values, value]
      const updated = { ...current, [facet]: next }
      if (next.length === 0) delete updated[facet]
      return updated
    })
    setPage(0)
  }

  const active = Object.entries(filters).flatMap(([facet, values]) =>
    values.map((value) => ({ facet, value })),
  )
  const lastPage = result ? Math.max(0, Math.ceil(result.total / SIZE) - 1) : 0

  return (
    <div className="page">
      <header>
        <h1>Digitalisierte Sammlungen durchsuchen</h1>
        <p>
          Metadaten der Staatsbibliothek zu Berlin, geholt über die
          OAI-PMH-Schnittstelle und indexiert in Apache Solr.
        </p>
      </header>

      <SearchField value={input} onChange={setInput} />

      {active.length > 0 && (
        <div className="active-filters">
          <h2>Gesetzte Filter</h2>
          <ul>
            {active.map(({ facet, value }) => (
              <li key={`${facet}-${value}`}>
                <button type="button" onClick={() => toggleFilter(facet, value)}>
                  {value}
                  <span className="visually-hidden"> als Filter entfernen</span>
                  <span aria-hidden="true"> &times;</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="status" role="status" aria-live="polite">
        {error
          ? `Fehler: ${error}`
          : loading
            ? 'Suche läuft'
            : result
              ? `${result.total} Treffer`
              : ''}
      </p>

      <div className="layout">
        {result && !error && (
          <aside>
            <h2>Filter</h2>
            <Facets facets={result.facets} selected={filters} onToggle={toggleFilter} />
          </aside>
        )}

        <main>
          <h2 className="visually-hidden">Treffer</h2>
          {error ? (
            <p className="empty">
              Die Suche ist gerade nicht erreichbar. Läuft Solr, und ist die Anwendung
              gestartet?
            </p>
          ) : result && result.items.length === 0 ? (
            <p className="empty">Keine Treffer. Andere Schreibweise oder weniger Filter?</p>
          ) : result ? (
            <>
              <Results items={result.items} />
              {lastPage > 0 && (
                <nav className="paging" aria-label="Seiten">
                  <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
                    Zurück
                  </button>
                  <span>
                    Seite {page + 1} von {lastPage + 1}
                  </span>
                  <button
                    type="button"
                    disabled={page >= lastPage}
                    onClick={() => setPage(page + 1)}
                  >
                    Weiter
                  </button>
                </nav>
              )}
            </>
          ) : null}
        </main>
      </div>
    </div>
  )
}
