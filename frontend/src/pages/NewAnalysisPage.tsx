import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError, request } from '../api/client'
import { useFetch } from '../api/useFetch'
import type { Analysis, Patient } from '../api/types'
import AgeWarning from '../components/AgeWarning'
import DisclaimerBanner from '../components/DisclaimerBanner'

export default function NewAnalysisPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const pat = useFetch<Patient>(`/api/patients/${id}`)
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [temp, setTemp] = useState('')
  const [spo2, setSpo2] = useState('')
  const [days, setDays] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!file) { setPreview(null); return }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!file || !id) return
    setBusy(true)
    setError(null)
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('patientId', id)
      if (temp.trim()) form.append('temperatureC', temp.replace(',', '.'))
      if (spo2.trim()) form.append('spo2Percent', spo2)
      if (days.trim()) form.append('symptomDays', days)
      const a = await request<Analysis>('/api/analyses', { method: 'POST', body: form })
      nav(`/analyses/${a.id}`)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) { nav('/login', { replace: true }); return }
      setError(err instanceof Error ? err.message : 'Erreur inconnue')
    } finally {
      setBusy(false)
    }
  }

  if (pat.loading) return <p>Chargement…</p>
  if (pat.error) return <p className="error" role="alert">{pat.error}</p>
  const p = pat.data
  if (!p) return null

  return (
    <>
      <h1>Nouvelle analyse</h1>
      <p>Patient : <strong>{p.lastName} {p.firstName}</strong> (dossier {p.recordNumber})</p>
      <AgeWarning patient={p} />
      <DisclaimerBanner />

      <form className="panel form" onSubmit={onSubmit}>
        <h2>Radiographie thoracique</h2>
        <input
          type="file"
          accept="image/jpeg,image/png"
          onChange={(e: ChangeEvent<HTMLInputElement>) => setFile(e.target.files?.[0] ?? null)}
          required
        />
        {preview && <img className="preview" src={preview} alt="Radiographie sélectionnée" />}

        <h2>Contexte clinique <span className="muted">(facultatif, mais recommandé)</span></h2>
        <label>Température (°C)
          <input inputMode="decimal" value={temp} onChange={(e) => setTemp(e.target.value)} placeholder="ex. 38,5" />
        </label>
        <label>Saturation en oxygène (%)
          <input type="number" min={50} max={100} value={spo2} onChange={(e) => setSpo2(e.target.value)} />
        </label>
        <label>Durée des symptômes (jours)
          <input type="number" min={0} max={60} value={days} onChange={(e) => setDays(e.target.value)} />
        </label>

        {error && <p className="error" role="alert">{error}</p>}
        <button disabled={!file || busy}>{busy ? 'Analyse en cours…' : 'Lancer l\'analyse'}</button>
        {busy && <p className="muted">L'analyse peut prendre quelques secondes.</p>}
      </form>
    </>
  )
}