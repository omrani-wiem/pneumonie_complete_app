import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, request } from './client'

export function useFetch<T>(path: string) {
  const nav = useNavigate()
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    request<T>(path)
      .then((d) => { if (!cancelled) setData(d) })
      .catch((e) => {
        if (cancelled) return
        if (e instanceof ApiError && e.status === 401) {
          nav('/login', { replace: true })
          return
        }
        setError(e instanceof Error ? e.message : 'Erreur inconnue')
      })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [path, nav])

  return { data, error, loading }
}