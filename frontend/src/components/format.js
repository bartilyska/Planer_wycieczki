const currency = new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' })
const calendar = new Intl.DateTimeFormat('pl-PL', { dateStyle: 'long', timeZone: 'UTC' })

export function money(groszy) {
  return groszy == null ? 'Cena niedostępna' : currency.format(groszy / 100)
}

export function duration(minutes) {
  if (minutes == null) return 'Czas niedostępny'
  const hours = Math.floor(minutes / 60)
  const remaining = minutes % 60
  return [hours ? `${hours} godz.` : '', remaining || !hours ? `${remaining} min` : ''].filter(Boolean).join(' ')
}

export function dateLabel(date) {
  return date ? calendar.format(new Date(date + 'T00:00:00Z')) : 'Bez daty'
}

export function todayInWarsaw() {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Europe/Warsaw', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date())
  const fields = Object.fromEntries(parts.map(({ type, value }) => [type, value]))
  return `${fields.year}-${fields.month}-${fields.day}`
}

export function indoorLabel(indoor) {
  if (indoor == null) return 'Typ miejsca niedostępny'
  return indoor ? 'Wewnątrz' : 'Na zewnątrz'
}

export function normalize(value = '') {
  return value.toLocaleLowerCase('pl').normalize('NFD').replace(/\p{Diacritic}/gu, '').replaceAll('ł', 'l').trim()
}
