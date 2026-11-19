import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 120000,
})

export async function getAttractions(signal) {
  const { data } = await api.get('/plans/attractions', { signal })
  return data
}

export async function getOptions(signal) {
  const { data } = await api.get('/plans/options', { signal })
  return data
}

export async function getAttraction(id, signal) {
  const { data } = await api.get('/plans/attractions/' + encodeURIComponent(id), { signal })
  return data
}

export async function generatePlan(request, optimize = false) {
  const { data } = await api.post(optimize ? '/plans/optimize' : '/plans/preview', request)
  return data
}

export function errorMessage(error) {
  const problem = error.response?.data
  if (problem?.fields) {
    return [problem.detail, ...Object.entries(problem.fields).map(([field, issue]) => `${field} — ${issue}`)].join(' ')
  }
  return typeof problem?.detail === 'string'
    ? problem.detail
    : 'Nie udało się połączyć z planerem. Sprawdź, czy backend jest uruchomiony, i spróbuj ponownie.'
}
