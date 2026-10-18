import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError, putJson } from '../api/client'
import { useFetch } from '../api/useFetch'
import type { Patient } from '../api/types'

export default function EditPatientPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const { data, error: loadError, loading } = useFetch<Patient>(`/api/patients/${id}`)
  const [f, setF] = useState({ recordNumber: '', lastName: '', firstName: '', birthDate: '', sex: 'F', notes: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const today = new Date().toISOString().slice(0, 10)

  useEffect(() => {
    if (data) setF({
      recordNumber: data.recordNumber, lastName: data.lastName, firstName: data.firstName,
      birthDate: data.birthDate, sex: data.sex, notes: data.notes ?? '',
    })
  }, [data])

  const set = (k: keyof typeof f) => (e: { target: { value: string } }) =>
    setF((s) => ({ ...s, [k]: e.target.value }))

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await putJson<Patient>(`/api/patients/${id}`, f)
      nav(`/patients/${id}`)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) { nav('/login', { replace: true }); return }
      setError(err instanceof Error ? err.message : 'Erreur inconnue')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <p>Chargement…</p>
  if (loadError) return <p className="error" role="alert">{loadError}</p>

  return (
    <>
      <h1>Modifier le patient</h1>
      <form className="panel form" onSubmit={onSubmit}>
        <label>Numéro de dossier
          <input value={f.recordNumber} onChange={set('recordNumber')} maxLength={50} required />
        </label>
        <label>Nom
          <input value={f.lastName} onChange={set('lastName')} maxLength={100} required />
        </label>
        <label>Prénom
          <input value={f.firstName} onChange={set('firstName')} maxLength={100} required />
        </label>
        <label>Date de naissance
          <input type="date" value={f.birthDate} onChange={set('birthDate')} max={today} min="1900-01-01" required />
        </label>
        <label>Sexe
          <select value={f.sex} onChange={set('sex')}>
            <option value="F">Féminin</option>
            <option value="M">Masculin</option>
          </select>
        </label>
        <label>Notes cliniques
          <textarea rows={4} value={f.notes} onChange={set('notes')} maxLength={4000} />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button disabled={busy}>{busy ? 'Enregistrement…' : 'Enregistrer les modifications'}</button>
      </form>
    </>
  )
}