import { CalendarDays, CarFront, ChevronDown, Clock3, LocateFixed, MapPin, Route, Utensils } from 'lucide-react'

export default function PlannerForm({ form, update, cities, options, minDate, useFirstPoint, useSuggestedStart, canUseFirst, busy }) {
  const modes = options?.transportModes ?? [{ value: 'WALK', label: 'Pieszo' }]

  return (
    <section className="panel" aria-labelledby="trip-heading">
      <div className="section-heading">
        <div className="section-icon"><MapPin size={22} /></div>
        <div><h3 id="trip-heading">Szczegóły podróży</h3><p>Ustal, kiedy i skąd zaczynasz.</p></div>
      </div>
      <div className="fields">
        <label><span>Miasto</span><span className="field-control"><MapPin size={17} /><input required maxLength={100} list="planner-cities" placeholder="np. Warszawa" value={form.city} onChange={(event) => update('city', event.target.value)} /></span></label>
        <datalist id="planner-cities">{cities.map((city) => <option value={city} key={city} />)}</datalist>
        <label><span>Data wycieczki</span><span className="field-control"><CalendarDays size={17} /><input required type="date" min={minDate} value={form.date} onChange={(event) => update('date', event.target.value)} /></span></label>
        <label><span>Godzina rozpoczęcia</span><span className="field-control"><Clock3 size={17} /><input required type="time" value={form.startTime} onChange={(event) => update('startTime', event.target.value)} /></span></label>
        <label><span>Godzina zakończenia <small>(opcjonalnie)</small></span><span className="field-control"><Clock3 size={17} /><input type="time" value={form.endTime} onChange={(event) => update('endTime', event.target.value)} /></span></label>
        <label><span>Środek transportu</span><span className="field-control"><CarFront size={17} />
          <select value={form.transportMode} onChange={(event) => update('transportMode', event.target.value)}>
            {modes.map((mode) => <option key={mode.value} value={mode.value}>{mode.label}</option>)}
          </select><ChevronDown className="select-chevron" size={16} /></span></label>
        <label><span>Liczba osób</span><span className="field-control"><input required type="number" min="1" max="20"
          value={form.people} onChange={(event) => update('people', event.target.value)} /></span></label>
      </div>
      <fieldset className="location-fields">
        <legend>Punkt początkowy</legend>
        <div className="fields">
          <label><span>Szerokość geograficzna</span><span className="field-control"><input required type="number" step="any" min="-90" max="90" placeholder="52.2297" value={form.lat} onChange={(event) => update('lat', event.target.value)} /></span></label>
          <label><span>Długość geograficzna</span><span className="field-control"><input required type="number" step="any" min="-180" max="180" placeholder="21.0122" value={form.lng} onChange={(event) => update('lng', event.target.value)} /></span></label>
        </div>
        {options?.suggestedStart
          ? <button type="button" disabled={busy} className="text-button point-button" onClick={useSuggestedStart}><LocateFixed size={16} /> Użyj punktu startowego w Warszawie</button>
          : canUseFirst && <button type="button" disabled={busy} className="text-button point-button" onClick={useFirstPoint}><LocateFixed size={16} /> Użyj pierwszej wybranej atrakcji</button>}
      </fieldset>
      <div className="switches">
        <label className="switch-row"><Utensils size={19} /><span>Uwzględnij przerwę na posiłek</span><input type="checkbox" checked={form.includeGastronomy} onChange={(event) => update('includeGastronomy', event.target.checked)} /></label>
        <label className="switch-row"><Route size={19} /><span>Powrót do punktu startowego</span><input type="checkbox" checked={form.returnToStart} onChange={(event) => update('returnToStart', event.target.checked)} /></label>
      </div>
    </section>
  )
}
