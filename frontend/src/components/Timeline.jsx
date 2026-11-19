import { CarFront, ChevronDown, ChevronUp, Clock3, Hourglass, Landmark, MapPin, Ticket, Trash2, Utensils } from 'lucide-react'
import { duration, money } from './format'

const types = {
  ATTRACTION: { Icon: Landmark, label: 'Atrakcja' },
  TRANSPORT: { Icon: CarFront, label: 'Przejazd' },
  GASTRONOMY: { Icon: Utensils, label: 'Posiłek' },
  WAIT: { Icon: Hourglass, label: 'Przerwa' },
}

export default function Timeline({ items, busy, onDetail, onMove, onRemove, modes }) {
  const attractions = items.filter((item) => item.type === 'ATTRACTION')
  return (
    <ol className="timeline">
      {items.map((item) => {
        const { Icon, label } = types[item.type]
        const attraction = item.type === 'ATTRACTION'
        const index = attraction ? attractions.indexOf(item) : -1
        const mode = modes?.find((entry) => entry.value === item.transportMode)?.label ?? item.transportMode
        return (
          <li key={`${item.type}-${item.id}`} className={`timeline-item item-${item.type.toLowerCase()}`}>
            <div className="timeline-time"><strong>{item.startTime}</strong><span>{item.endTime ?? '—'}</span></div>
            <div className="timeline-icon"><Icon size={20} /></div>
            <article className="timeline-card">
              <div className="timeline-card-head">
                <div><span className="kind">{label}{mode && ` · ${mode}`}</span>
                  <h3>{attraction ? <button className="attraction-name" onClick={() => onDetail(item.id)}>{item.name}</button> : item.name}</h3>
                </div>
                {attraction && <div className="item-actions">
                  <button disabled={busy || index === 0} className="icon-button" aria-label={`Przesuń wyżej: ${item.name}`} onClick={() => onMove(index, -1)}><ChevronUp size={18} /></button>
                  <button disabled={busy || index === attractions.length - 1} className="icon-button" aria-label={`Przesuń niżej: ${item.name}`} onClick={() => onMove(index, 1)}><ChevronDown size={18} /></button>
                  <button disabled={busy} className="icon-button remove" aria-label={`Usuń: ${item.name}`} onClick={() => onRemove(item.id)}><Trash2 size={16} /></button>
                </div>}
              </div>
              <div className="metadata">
                {item.location && <span><MapPin size={15} />{item.location}</span>}
                <span><Clock3 size={15} />{duration(item.durationMinutes)}</span>
                {item.type !== 'WAIT' && <span><Ticket size={15} />{money(item.priceGroszy)}</span>}
                {item.type === 'TRANSPORT' && <span>{item.distanceKm == null ? 'Dystans niedostępny' : `${item.distanceKm.toLocaleString('pl-PL')} km`}</span>}
              </div>
            </article>
          </li>
        )
      })}
    </ol>
  )
}
