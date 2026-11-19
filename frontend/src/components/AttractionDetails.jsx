import { useEffect, useRef, useState } from 'react'
import { Clock3, Landmark, MapPin, Ticket, X } from 'lucide-react'
import { errorMessage, getAttraction } from '../services/plannerApi'
import { duration, indoorLabel, money } from './format'
import { ErrorMessage, LoadingState } from './States'

export default function AttractionDetails({ id, close }) {
  const dialog = useRef(null)
  const [state, setState] = useState({ loading: true })

  useEffect(() => {
    const element = dialog.current
    element.showModal()
    return () => element.close()
  }, [])

  useEffect(() => {
    const request = new AbortController()
    setState({ loading: true })
    getAttraction(id, request.signal)
      .then((data) => { if (!request.signal.aborted) setState({ data }) })
      .catch((error) => { if (!request.signal.aborted) setState({ error: errorMessage(error) }) })
    return () => request.abort()
  }, [id])

  const item = state.data
  return (
    <dialog className="details-dialog" ref={dialog} onCancel={close} aria-labelledby="detail-title">
      <div className="dialog-head">
        <span className="section-icon"><Landmark size={23} /></span>
        <button className="icon-button" aria-label="Zamknij szczegóły" onClick={close} autoFocus><X size={21} /></button>
      </div>
      <h2 id="detail-title">{item?.name ?? 'Szczegóły atrakcji'}</h2>
      {state.loading && <LoadingState label="Ładowanie szczegółów atrakcji" count={2} />}
      {state.error && <ErrorMessage>{state.error}</ErrorMessage>}
      {item && <>
        <div className="metadata"><span><MapPin size={16} />{item.location || 'Lokalizacja niedostępna'}</span>
          {item.category && <span>{item.category}</span>}<span>{indoorLabel(item.indoor)}</span></div>
        <p>{item.description || 'Opis niedostępny.'}</p>
        <div className="detail-facts"><span><Ticket size={17} />{money(item.priceGroszy)}</span><span><Clock3 size={17} />{duration(item.durationMinutes)}</span></div>
        {item.coordinates && <p className="muted">Współrzędne: {item.coordinates.lat}, {item.coordinates.lng}</p>}
      </>}
    </dialog>
  )
}
