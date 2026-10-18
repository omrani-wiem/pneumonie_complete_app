import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError, putJson, request } from '../api/client'
import { useFetch } from '../api/useFetch'
import type { Analysis, Patient } from '../api/types'
import AgeWarning from '../components/AgeWarning'
import { fmtAge, fmtDate, fmtDateTime, fmtPct } from '../format'

export default function PatientDetailPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const pat = useFetch<Patient>(`/api/patients/${id}`)
  const ans = useFetch<Analysis[]>(`/api/patients/${id}/analyses`)
  const [updated, setUpdated] = useState<Patient | null>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [showErase, setShowErase] = useState(false)
  const [confirm, setConfirm] = useState('')

  if (pat.loading || ans.loading) return <p>Chargement…</p>
  if (pat.error) return <p className="error" role="alert">{pat.error}</p>
  const p = updated ?? pat.data
  if (!p) return null

  function fail(err: unknown) {
    if (err instanceof ApiError && err.status === 401) { nav('/login', { replace: true }); return }
    setActionError(err instanceof Error ? err.message : 'Erreur inconnue')
  }

  async function toggleArchive() {
    setBusy(true)
    setActionError(null)
    try {
      setUpdated(await putJson<Patient>(`/api/patients/${p!.id}/archived`, { archived: !p!.archived }))
    } catch (err) { fail(err) } finally { setBusy(false) }
  }

  async function erase() {
    setBusy(true)
    setActionError(null)
    try {
      await request(`/api/patients/${p!.id}?confirm=${encodeURIComponent(confirm)}`, { method: 'DELETE' })
      nav('/patients', { replace: true })
    } catch (err) { fail(err) } finally { setBusy(false) }
  }

  return (
    <>
      <div className="row">
        <h1>{p.lastName} {p.firstName}</h1>
        {!p.archived && <Link className="btn" to={`/patients/${p.id}/analyse`}>Nouvelle analyse</Link>}
      </div>

      {p.archived && (
        <div className="banner" role="note">
          <strong>Patient archivé.</strong> Il n'apparaît plus dans la liste principale et aucune nouvelle analyse
          ne peut être lancée tant qu'il n'est pas restauré.
        </div>
      )}
      <AgeWarning patient={p} />

      <section className="panel">
        <h2>Identité</h2>
        <p><strong>Dossier :</strong> {p.recordNumber}</p>
        <p><strong>Naissance :</strong> {fmtDate(p.birthDate)} ({fmtAge(p.ageMonths)}) · <strong>Sexe :</strong> {p.sex}</p>
        {p.notes && <p><strong>Notes :</strong> {p.notes}</p>}
        <div className="row">
          <Link className="btn" to={`/patients/${p.id}/edit`}>Modifier</Link>
          <button onClick={toggleArchive} disabled={busy}>{p.archived ? 'Restaurer' : 'Archiver'}</button>
          <button className="danger-btn" onClick={() => setShowErase((s) => !s)} disabled={busy}>
            Supprimer définitivement
          </button>
        </div>
        {actionError && <p className="error" role="alert">{actionError}</p>}
      </section>

      {showErase && (
        <section className="panel">
          <div className="warn" role="alert">
            <strong>Suppression irréversible.</strong> Le patient et toutes ses analyses seront effacés
            définitivement. Pour confirmer, retapez le numéro de dossier <strong>{p.recordNumber}</strong>.
          </div>
          <label>Numéro de dossier
            <input value={confirm} onChange={(e) => setConfirm(e.target.value)} />
          </label>
          <button className="danger-btn" onClick={erase} disabled={busy || confirm.trim() !== p.recordNumber}>
            {busy ? 'Suppression…' : 'Confirmer la suppression'}
          </button>
        </section>
      )}

      <section className="panel">
        <h2>Historique des analyses</h2>
        {ans.error && <p className="error" role="alert">{ans.error}</p>}
        {(ans.data ?? []).length === 0 ? (
          <p className="muted">Aucune analyse pour ce patient.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Date</th><th>Résultat</th><th>Type</th><th>Avis</th><th></th></tr>
            </thead>
            <tbody>
              {(ans.data ?? []).map((a) => (
                <tr key={a.id}>
                  <td>{fmtDateTime(a.createdAt)}</td>
                  <td>
                    {a.label === 'PNEUMONIA'
                      ? `Pneumonie suspectée ${fmtPct(a.probabilityPneumonia)} %`
                      : `Pas de pneumonie (${fmtPct(a.probabilityPneumonia)} %)`}
                  </td>
                  <td>{a.pneumoniaType ?? 'non évalué'}</td>
                  <td>{a.doctorOpinion === 'AGREE' ? "D'accord" : a.doctorOpinion === 'DISAGREE' ? "Pas d'accord" : 'À donner'}</td>
                  <td><Link to={`/analyses/${a.id}`}>Ouvrir</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}