type Props = {
  value: string
  onChange: (value: string) => void
}

export default function SearchField({ value, onChange }: Props) {
  return (
    <div className="search-field">
      <label htmlFor="q">Suchbegriff</label>
      <input
        id="q"
        type="search"
        value={value}
        autoComplete="off"
        aria-describedby="q-hint"
        onChange={(event) => onChange(event.target.value)}
      />
      <p id="q-hint" className="hint">
        Durchsucht Titel, Verfasser und Schlagwort. Leer lassen zeigt den ganzen Bestand.
      </p>
    </div>
  )
}
