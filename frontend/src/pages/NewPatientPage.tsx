import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, postJson } from '../api/client'
import type { Patient } from '../api/types'

export default function NewPatientPage() {
  const nav = useNavigate()
  const [f, setF] = useState({
    recordNumber: '', lastName: '', firstName: '', birthDate: '', sex: 'F', notes: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) =>
    setF((s) => ({ ...s, [k]: e.target.value }))
  const today = new Date().toISOString().slice(0, 10)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      const p = await postJson<Patient>('/api/patients', f)
      nav(`/patients/${p.id}`)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) { nav('/login', { replace: true }); return }
      setError(err instanceof Error ? err.message : 'Erreur inconnue')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Nouveau patient</h1>
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
        <label>Notes cliniques (antécédents, allergies…)
          <textarea rows={4} value={f.notes} onChange={set('notes')} maxLength={4000} />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button disabled={busy}>{busy ? 'Enregistrement…' : 'Enregistrer le patient'}</button>
      </form>
    </>
  )
}