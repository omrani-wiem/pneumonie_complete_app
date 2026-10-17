import { Link, useParams } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import type { Analysis, Patient } from '../api/types'
import AgeWarning from '../components/AgeWarning'
import { fmtAge, fmtDate, fmtDateTime, fmtPct } from '../format'

export default function PatientDetailPage() {
  const { id } = useParams()
  const pat = useFetch<Patient>(`/api/patients/${id}`)
  const ans = useFetch<Analysis[]>(`/api/patients/${id}/analyses`)

  if (pat.loading || ans.loading) return <p>Chargement…</p>
  if (pat.error) return <p className="error" role="alert">{pat.error}</p>
  const p = pat.data
  if (!p) return null

  return (
    <>
      <div className="row">
        <h1>{p.lastName} {p.firstName}</h1>
        <Link className="btn" to={`/patients/${p.id}/analyse`}>Nouvelle analyse</Link>
      </div>
      <AgeWarning patient={p} />

      <section className="panel">
        <h2>Identité</h2>
        <p><strong>Dossier :</strong> {p.recordNumber}</p>
        <p><strong>Naissance :</strong> {fmtDate(p.birthDate)} ({fmtAge(p.ageMonths)}) · <strong>Sexe :</strong> {p.sex}</p>
        {p.notes && <p><strong>Notes :</strong> {p.notes}</p>}
      </section>

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
                  <td>{a.doctorOpinion === 'AGREE' ? 'D\'accord' : a.doctorOpinion === 'DISAGREE' ? 'Pas d\'accord' : 'À donner'}</td>
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