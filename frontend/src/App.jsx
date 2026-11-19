import { useEffect, useRef, useState } from 'react'
import Navbar from './components/Navbar'
import AttractionDetails from './components/AttractionDetails'
import { normalize, todayInWarsaw } from './components/format'
import { usePlannerData } from './hooks/usePlannerData'
import Dashboard from './pages/Dashboard'
import PlanPage from './pages/PlanPage'
import { errorMessage, generatePlan, getAttraction } from './services/plannerApi'

const initialForm = {
  city: '', date: '', startTime: '09:00', endTime: '', lat: '', lng: '',
  transportMode: 'WALK', includeGastronomy: false, returnToStart: false,
}

function focusSection(id) {
  requestAnimationFrame(() => {
    const heading = document.getElementById(id)
    heading?.focus({ preventScroll: true })
    heading?.scrollIntoView({ block: 'start' })
  })
}

export default function App() {
  const data = usePlannerData()
  const [input, setInput] = useState(initialForm)
  const [selectedIds, setSelectedIds] = useState([])
  const [plan, setPlan] = useState(null)
  const [request, setRequest] = useState(null)
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  const [generating, setGenerating] = useState(false)
  const [pointBusy, setPointBusy] = useState(false)
  const pointLookup = useRef(0)
  const cities = [...new Set(data.attractions.map((item) => item.location?.trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'pl'))
  const minDate = data.options?.earliestDate ?? todayInWarsaw()
  const form = { ...input, date: input.date || minDate }
  const visible = data.attractions.filter((item) => !form.city.trim() || !item.location
    || normalize(item.location) === normalize(form.city))
  const selected = selectedIds.map((id) => data.attractions.find((item) => item.id === id)).filter(Boolean)

  useEffect(() => () => { pointLookup.current += 1 }, [])

  function cancelPointLookup() {
    pointLookup.current += 1
    setPointBusy(false)
  }

  function update(field, value) {
    if (generating) return
    if (field === 'city') {
      const city = normalize(value)
      setSelectedIds((ids) => ids.filter((id) => data.attractions.some((item) => item.id === id
        && (!city || !item.location || normalize(item.location) === city))))
    }
    if (['city', 'lat', 'lng'].includes(field)) cancelPointLookup()
    setInput((previous) => ({ ...previous, [field]: value }))
  }

  function toggle(id) {
    if (generating) return
    cancelPointLookup()
    setSelectedIds((ids) => ids.includes(id) ? ids.filter((value) => value !== id) : [...ids, id])
  }

  async function useFirstPoint() {
    if (!selectedIds.length) return
    const lookupId = ++pointLookup.current
    setPointBusy(true)
    setError('')
    try {
      const item = await getAttraction(selectedIds[0])
      if (lookupId !== pointLookup.current) return
      if (!item.coordinates) {
        setError('Pierwsza atrakcja nie ma współrzędnych. Wpisz punkt początkowy ręcznie.')
        return
      }
      setInput((previous) => ({ ...previous, city: previous.city || item.location || '',
        lat: String(item.coordinates.lat), lng: String(item.coordinates.lng) }))
    } catch (failure) {
      if (lookupId === pointLookup.current) setError(errorMessage(failure))
    } finally {
      if (lookupId === pointLookup.current) setPointBusy(false)
    }
  }

  function edit() {
    if (generating) return
    setPlan(null)
    setError('')
    focusSection('planner-heading')
  }

  function home() {
    if (generating) return
    setPlan(null)
    setError('')
    window.scrollTo({ top: 0 })
  }

  async function calculate(body, optimize = false) {
    if (generating) return
    cancelPointLookup()
    setGenerating(true)
    setError('')
    try {
      const result = await generatePlan(body, optimize)
      const ids = result.items.filter(({ type }) => type === 'ATTRACTION').map(({ id }) => id)
      setRequest({ ...body, attractionIds: ids })
      setSelectedIds(ids)
      setPlan(result)
      if (!plan) focusSection('plan-heading')
    } catch (failure) {
      setError(errorMessage(failure))
      if (!plan) focusSection('planner-heading')
    } finally {
      setGenerating(false)
    }
  }

  function generate() {
    const city = cities.find((value) => normalize(value) === normalize(form.city)) ?? form.city.trim()
    calculate({
      city, date: form.date, startTime: form.startTime, endTime: form.endTime || null,
      startLocation: { lat: Number(form.lat), lng: Number(form.lng) },
      transportMode: form.transportMode, attractionIds: selectedIds,
      includeGastronomy: form.includeGastronomy, returnToStart: form.returnToStart,
    })
  }

  function move(index, step) {
    const ids = [...request.attractionIds]
    ;[ids[index], ids[index + step]] = [ids[index + step], ids[index]]
    calculate({ ...request, attractionIds: ids })
  }

  function remove(id) {
    const ids = request.attractionIds.filter((value) => value !== id)
    if (!ids.length) {
      setSelectedIds([])
      edit()
      return
    }
    calculate({ ...request, attractionIds: ids })
  }

  return (
    <>
      <Navbar demo={data.options?.demo || plan?.demo} onHome={home} onCreate={edit} />
      <main className="shell">
        {plan ? <PlanPage {...{ plan, error }} busy={generating} onEdit={edit} modes={data.options?.transportModes}
          onRegenerate={() => calculate(request)} onOptimize={() => calculate(request, true)}
          onDetail={setDetail} onMove={move} onRemove={remove} />
          : <Dashboard {...{ data, form, update, cities, minDate, selected, selectedIds, toggle, useFirstPoint, pointBusy, generating, error }}
            attractions={visible} onDetail={setDetail} onCreate={edit} onGenerate={generate} />}
      </main>
      {detail && <AttractionDetails id={detail} close={() => setDetail(null)} />}
      <footer className="shell site-footer"><span>TripPlanner</span><span>Planer jednodniowych wycieczek</span></footer>
    </>
  )
}
