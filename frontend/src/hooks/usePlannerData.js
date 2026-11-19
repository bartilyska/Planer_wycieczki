import { useEffect, useState } from 'react'
import { errorMessage, getAttractions, getOptions } from '../services/plannerApi'

export function usePlannerData() {
  const [attempt, setAttempt] = useState(0)
  const [options, setOptions] = useState(null)
  const [attractions, setAttractions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [optionsError, setOptionsError] = useState('')

  useEffect(() => {
    const request = new AbortController()
    setLoading(true)
    setOptions(null)
    setError('')
    setOptionsError('')
    getOptions(request.signal).then((data) => {
      if (!request.signal.aborted) setOptions(data)
    }).catch((failure) => {
      if (!request.signal.aborted) setOptionsError(errorMessage(failure))
    })
    getAttractions(request.signal).then((data) => {
      if (!request.signal.aborted) setAttractions(data)
    }).catch((failure) => {
      if (!request.signal.aborted) {
        setAttractions([])
        setError(errorMessage(failure))
      }
    }).finally(() => {
      if (!request.signal.aborted) setLoading(false)
    })
    return () => request.abort()
  }, [attempt])

  return { options, attractions, loading, error, optionsError, retry: () => setAttempt((value) => value + 1) }
}
