import { Compass, MapPin } from 'lucide-react'

const hasPoint = (point) => Number.isFinite(point?.lat) && Number.isFinite(point?.lng)

function fit(points) {
  const convert = ({ lat, lng }) => ({
    x: lng,
    y: Math.log(Math.tan(Math.PI / 4 + Math.max(-85, Math.min(lat, 85)) * Math.PI / 360)) * 180 / Math.PI,
  })
  const projected = points.map(convert)
  const xs = projected.map(({ x }) => x)
  const ys = projected.map(({ y }) => y)
  const centerX = (Math.max(...xs) + Math.min(...xs)) / 2
  const centerY = (Math.max(...ys) + Math.min(...ys)) / 2
  const size = Math.max(Math.max(...xs) - Math.min(...xs), Math.max(...ys) - Math.min(...ys), 0.001)
  return (point) => {
    const { x, y } = convert(point)
    return { x: 50 + 72 * (x - centerX) / size, y: 50 - 72 * (y - centerY) / size }
  }
}

export default function MapView({ start, items }) {
  const stops = items.filter((item) => item.type === 'ATTRACTION' || item.type === 'GASTRONOMY')
  const pins = [{ name: 'Punkt startowy', point: start, number: 0 },
    ...stops.map((item, index) => ({ name: item.name, point: item.coordinates, number: index + 1 }))]
  const routes = items.flatMap((item) => item.type === 'TRANSPORT' && item.route?.length >= 2
    && item.route.every(hasPoint) ? [item.route] : [])
  const points = [...pins.map((pin) => pin.point).filter(hasPoint), ...routes.flat()]
  if (!points.length) return null
  const project = fit(points)
  const groups = new Map()
  for (const pin of pins.filter(({ point }) => hasPoint(point))) {
    const key = `${pin.point.lat},${pin.point.lng}`
    const group = groups.get(key) ?? []
    group.push(pin)
    groups.set(key, group)
  }
  const routeSegments = routes.flatMap((route) => route.slice(1).map((point, index) => {
    const from = project(route[index])
    const to = project(point)
    return {
      left: `${from.x}%`, top: `${from.y}%`, width: `${Math.hypot(to.x - from.x, to.y - from.y)}%`,
      transform: `rotate(${Math.atan2(to.y - from.y, to.x - from.x)}rad)`,
    }
  }))

  return (
    <section className="panel map-panel" aria-labelledby="map-heading">
      <div className="section-heading"><span className="section-icon"><MapPin size={21} /></span>
        <div><h3 id="map-heading">Mapa punktów</h3><p>Schemat na podstawie współrzędnych.</p></div>
      </div>
      <div className="map" aria-hidden="true">
        <Compass size={27} className="map-compass" />
        {routeSegments.map((style, index) => <span key={index} className="map-segment" style={style} />)}
        {[...groups.values()].map((group) => {
          const { x, y } = project(group[0].point)
          return <span className="map-pin" key={group[0].number} style={{ left: `${x}%`, top: `${y}%` }}
            title={group.map((pin) => pin.name).join(', ')}>{group.map((pin) => pin.number || 'S').join('·')}</span>
        })}
      </div>
      <ol className="map-legend">
        {pins.map(({ number, name, point }) => <li key={number}><span>{number || 'S'}</span> {name}{!hasPoint(point) && <small>Brak współrzędnych</small>}</li>)}
      </ol>
      {routes.length === 0 && <p className="muted map-note">Przebieg trasy jest niedostępny.</p>}
    </section>
  )
}
