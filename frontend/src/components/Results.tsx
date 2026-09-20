import type { Hit } from '../api'

type Props = {
  items: Hit[]
}

export default function Results({ items }: Props) {
  return (
    <ul className="results">
      {items.map((hit) => (
        <li key={hit.ppn}>
          <h3>
            {hit.url ? (
              <a href={hit.url}>{hit.title ?? hit.ppn}</a>
            ) : (
              (hit.title ?? hit.ppn)
            )}
          </h3>
          <dl>
            {hit.creators.length > 0 && (
              <>
                <dt>Verfasser</dt>
                <dd>{hit.creators.join(', ')}</dd>
              </>
            )}
            {hit.date && (
              <>
                <dt>Jahr</dt>
                <dd>{hit.date}</dd>
              </>
            )}
            {hit.subjects.length > 0 && (
              <>
                <dt>Schlagwort</dt>
                <dd>{hit.subjects.join(', ')}</dd>
              </>
            )}
            <dt>PPN</dt>
            <dd>{hit.ppn}</dd>
          </dl>
        </li>
      ))}
    </ul>
  )
}
