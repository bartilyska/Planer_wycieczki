import { CalendarDays, CloudSun, Clock3, ListChecks, MapPin, Sparkles, Ticket, Wallet } from 'lucide-react'
import { dateLabel, duration, money } from './format'

export default function PlanDraft({ form, selected, onSubmitBusy, generateDisabled }) {
  const time = selected.reduce((total, item) => total + item.durationMinutes, 0)
  const cost = selected.reduce((total, item) => total + (item.priceGroszy ?? 0), 0)
  const partial = selected.some((item) => item.priceGroszy == null)

  return (
    <aside className="draft panel" aria-labelledby="draft-heading">
      <div className="draft-head"><span className="draft-icon"><ListChecks size={24} /></span><div><h3 id="draft-heading">Twój plan</h3><p>Podsumowanie wyboru</p></div></div>
      <div className="draft-body">
        <div className="draft-place"><MapPin size={18} /><strong>{form.city.trim() || 'Wybierz miasto'}</strong></div>
        <div className="draft-date"><CalendarDays size={17} />{dateLabel(form.date)}{form.startTime && ` · ${form.startTime}`}</div>
        <dl className="draft-stats">
          <div><dt><Ticket size={17} />Atrakcje</dt><dd>{selected.length}</dd></div>
          <div><dt><Clock3 size={17} />Czas zwiedzania</dt><dd>{selected.length ? duration(time) : '—'}</dd></div>
          <div><dt><Wallet size={17} />{partial ? 'Znane ceny biletów' : 'Koszt biletów'}</dt><dd>{selected.length ? money(cost) : '—'}</dd></div>
        </dl>
        <button type="submit" className="button primary generate" disabled={generateDisabled || onSubmitBusy}>
          <Sparkles size={18} />{onSubmitBusy ? 'Układanie planu…' : 'Generuj plan'}
        </button>
        <p className="draft-note"><CloudSun size={19} />{selected.length ? 'Pogodę i dojazdy sprawdzimy podczas generowania planu.' : 'Dodaj co najmniej jedną atrakcję, aby wygenerować plan.'}</p>
      </div>
    </aside>
  )
}
