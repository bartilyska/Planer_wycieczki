import { useState } from 'react'
import { Check, ChevronDown, Clock3, Landmark, Leaf, MapPin, Plus, Search, Ticket } from 'lucide-react'
import { duration, indoorLabel, money, normalize } from './format'
import { EmptyState, ErrorMessage, LoadingState } from './States'

export default function AttractionList({ attractions, loading, error, retry, selected, toggle, onDetail, max }) {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const categories = [...new Set(attractions.map((item) => item.category).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'pl'))
  const activeCategory = categories.includes(category) ? category : ''
  const visible = attractions.filter((item) => (!activeCategory || item.category === activeCategory)
    && normalize(`${item.name} ${item.location ?? ''} ${item.category ?? ''}`).includes(normalize(query)))

  return (
    <section className="panel attractions" aria-labelledby="attractions-heading">
      <div className="section-heading">
        <div className="section-icon"><Landmark size={22} /></div>
        <div><h3 id="attractions-heading">Wybierz atrakcje</h3><p>Dodaj miejsca, które chcesz odwiedzić.</p></div>
      </div>
      <div className="attraction-filters">
        <label className="field-control"><Search size={18} /><input type="search" placeholder="Szukaj atrakcji" aria-label="Szukaj atrakcji" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
        <label className="field-control"><select aria-label="Kategoria atrakcji" value={activeCategory} onChange={(event) => setCategory(event.target.value)}>
          <option value="">Wszystkie kategorie</option>
          {categories.map((value) => <option key={value}>{value}</option>)}
        </select><ChevronDown className="select-chevron" size={16} /></label>
      </div>
      {loading ? <LoadingState label="Ładowanie atrakcji" /> : error ? <ErrorMessage retry={retry}>{error}</ErrorMessage> : (
        visible.length ? <ul className="attraction-list">
          {visible.map((item) => {
            const active = selected.includes(item.id)
            const Icon = item.indoor === false ? Leaf : Landmark
            return (
              <li className={active ? 'attraction-row selected' : 'attraction-row'} key={item.id}>
                <span className={item.indoor === false ? 'place-icon outdoors' : 'place-icon'}><Icon size={22} /></span>
                <div className="attraction-copy">
                  <button type="button" className="attraction-name" onClick={() => onDetail(item.id)}>{item.name}</button>
                  <div className="metadata"><span><MapPin size={14} />{item.location || 'Lokalizacja niedostępna'}</span><span>{item.category}</span><span>{indoorLabel(item.indoor)}</span></div>
                  <div className="metadata"><span><Clock3 size={14} />{duration(item.durationMinutes)}</span><span><Ticket size={14} />{item.priceGroszy === 0 ? 'Bezpłatnie' : money(item.priceGroszy)}</span></div>
                </div>
                <button type="button" className="icon-button choose" aria-pressed={active}
                  aria-label={(active ? 'Usuń: ' : 'Dodaj: ') + item.name}
                  disabled={!active && selected.length >= max} onClick={() => toggle(item.id)}>
                  {active ? <Check size={19} /> : <Plus size={19} />}
                </button>
              </li>
            )
          })}
        </ul> : <EmptyState>{attractions.length ? 'Nie znaleziono atrakcji pasujących do filtrów.' : 'Brak atrakcji dla wybranego miasta.'}</EmptyState>
      )}
    </section>
  )
}
