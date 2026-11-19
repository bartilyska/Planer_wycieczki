import { CloudSun, Compass, Map, Route, Sparkles, Ticket } from 'lucide-react'
import AttractionList from '../components/AttractionList'
import PlanDraft from '../components/PlanDraft'
import PlannerForm from '../components/PlannerForm'
import { ErrorMessage, LoadingState } from '../components/States'

const features = [
  { Icon: Ticket, title: 'Atrakcje po Twojemu', copy: 'Wybierz miejsca i ułóż własną kolejność zwiedzania.' },
  { Icon: CloudSun, title: 'Pogoda pod ręką', copy: 'Dopasuj kolejność wizyt do prognozy na dany dzień.' },
  { Icon: Route, title: 'Dojazdy i przerwy', copy: 'Uwzględnij przejazdy, posiłek i powrót do punktu startowego.' },
]

export default function Dashboard({ data, form, update, cities, minDate, attractions, selected, selectedIds,
  toggle, onDetail, useFirstPoint, pointBusy, onGenerate, generating, error, onCreate }) {
  return (
    <>
      <section className="hero" aria-labelledby="hero-heading">
        <div className="hero-content">
          <div className="hero-label"><Compass size={17} /> Planer wycieczek miejskich</div>
          <h1 id="hero-heading">Zaplanuj swój<br /><em>idealny dzień.</em></h1>
          <p>Wybierz miejsca, które chcesz zobaczyć. Ułóż z nich plan dnia z prognozą pogody, dojazdami i przerwą na posiłek.</p>
          <button type="button" className="button hero-action" onClick={onCreate}><Sparkles size={18} /> Utwórz plan</button>
        </div>
      </section>
      {data.options?.demo && <div className="demo-banner" role="status">Tryb demonstracyjny. Atrakcje, prognoza, ceny i przejazdy mają charakter przykładowy.</div>}
      <section className="workspace" aria-labelledby="planner-heading" id="kreator">
        <header className="workspace-heading"><div><h2 id="planner-heading" tabIndex={-1}>Kreator planu</h2><p>Zaplanuj dzień w wybranym mieście.</p></div><Map size={26} className="workspace-icon" aria-hidden="true" /></header>
        {error && <ErrorMessage>{error}</ErrorMessage>}
        {generating && <LoadingState label="Generowanie harmonogramu" count={1} />}
        <form className="planner-grid" onSubmit={(event) => { event.preventDefault(); onGenerate() }}>
          <div className="planner-main">
            <PlannerForm {...{ form, update, cities, minDate, useFirstPoint }} options={data.options} canUseFirst={selectedIds.length > 0} busy={pointBusy || generating} />
            <AttractionList {...{ toggle, onDetail }} attractions={data.options ? attractions : []}
              loading={data.loading || (!data.options && !data.optionsError)} error={data.error || data.optionsError}
              retry={data.retry} selected={selectedIds} max={data.options?.maxAttractions ?? 12} />
          </div>
          <PlanDraft form={form} selected={selected} onSubmitBusy={generating} generateDisabled={!selectedIds.length || Boolean(data.error) || !data.options} />
        </form>
      </section>
      <section className="features" aria-labelledby="features-heading">
        <h2 id="features-heading">Wszystko na jeden dzień</h2>
        <div className="feature-grid">{features.map(({ Icon, title, copy }) => (
          <article key={title}><div className="section-icon"><Icon size={23} /></div><h3>{title}</h3><p>{copy}</p></article>
        ))}</div>
      </section>
    </>
  )
}
