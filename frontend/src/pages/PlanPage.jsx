import { AlertTriangle, CalendarDays, CloudOff, CloudRain, CloudSun, Clock3, RotateCcw, Settings2, Sparkles, Ticket, Wallet } from 'lucide-react'
import { dateLabel, duration, money } from '../components/format'
import MapView from '../components/MapView'
import { ErrorMessage, LoadingState } from '../components/States'
import Timeline from '../components/Timeline'

export default function PlanPage({ plan, onEdit, onRegenerate, onOptimize, modes, busy, error, ...actions }) {
  const count = plan.items.filter(({ type }) => type === 'ATTRACTION').length
  const WeatherIcon = !plan.weather.available ? CloudOff
    : plan.weather.condition === 'ADVERSE_OUTDOOR' ? CloudRain : CloudSun
  return (
    <div className="plan-page">
      <section className="plan-heading">
        <div><div className="plan-date"><CalendarDays size={17} />{dateLabel(plan.date)}</div><h1 tabIndex={-1} id="plan-heading">Twój dzień · {plan.city}</h1></div>
        <button type="button" className="button secondary" onClick={onEdit} disabled={busy}><Settings2 size={17} /> Wróć do kreatora</button>
      </section>
      {plan.demo && <div className="demo-banner">Tryb demonstracyjny. Dane harmonogramu są przykładowe.</div>}
      <dl className="summary-grid">
        <div><dt><WeatherIcon size={19} />Pogoda</dt>
          <dd>{plan.weather.available && plan.weather.temperature != null ? `${plan.weather.temperature.toLocaleString('pl-PL')}°C` : '—'}</dd>
          <small>{plan.weather.description || 'Prognoza niedostępna'}</small></div>
        <div><dt><Ticket size={19} />Atrakcje</dt><dd>{count}</dd><small>Wybrane miejsca</small></div>
        <div><dt><Clock3 size={19} />Czas planu</dt><dd>{duration(plan.totalDurationMinutes)}</dd><small>{plan.durationComplete ? 'Łączny czas' : 'Znany czas'}</small></div>
        <div><dt><Wallet size={19} />Koszt planu</dt><dd>{money(plan.totalCostGroszy)}</dd><small>{plan.costComplete ? 'Łączny koszt' : 'Suma znanych kwot'}</small></div>
      </dl>
      {!!plan.warnings.length && <div className="notice warning" role="status"><AlertTriangle size={20} /><div><strong>Informacje o planie</strong><ul>{plan.warnings.map((warning) => <li key={warning}>{warning}</li>)}</ul></div></div>}
      {error && <ErrorMessage>{error}</ErrorMessage>}
      {busy && <LoadingState label="Aktualizowanie harmonogramu" count={1} />}
      <div className="plan-grid">
        <section aria-labelledby="timeline-heading">
          <div className="timeline-heading"><h2 id="timeline-heading">Harmonogram dnia</h2>
            <div className="plan-actions">
              <button className="button secondary compact" disabled={busy} onClick={onRegenerate}><RotateCcw size={16} /> Przelicz</button>
              <button className="button primary compact" disabled={busy || count < 2} onClick={onOptimize}><Sparkles size={16} /> Optymalizuj kolejność</button>
            </div>
          </div>
          <Timeline items={plan.items} {...{ busy, modes, ...actions }} />
        </section>
        <MapView start={plan.startLocation} items={plan.items} />
      </div>
    </div>
  )
}
