import { FACET_LABELS, type FacetValue, type Filters } from '../api'

type Props = {
  facets: Record<string, FacetValue[]>
  selected: Filters
  onToggle: (facet: string, value: string) => void
}

export default function Facets({ facets, selected, onToggle }: Props) {
  const groups = Object.entries(facets).filter(([, values]) => values.length > 0)

  if (groups.length === 0) {
    return null
  }

  return (
    <div className="facets">
      {groups.map(([facet, values]) => (
        <fieldset key={facet}>
          <legend>{FACET_LABELS[facet] ?? facet}</legend>
          {values.map(({ value, count }, index) => {
            // index, not the value itself: facet values are names and contain
            // spaces, which are not allowed in an id
            const id = `${facet}-${index}`
            return (
              <div className="facet-value" key={id}>
                <input
                  type="checkbox"
                  id={id}
                  checked={selected[facet]?.includes(value) ?? false}
                  onChange={() => onToggle(facet, value)}
                />
                <label htmlFor={id}>
                  {value} <span className="count">{count}</span>
                </label>
              </div>
            )
          })}
        </fieldset>
      ))}
    </div>
  )
}
