import { Link } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import type { Analysis, Page, Patient } from '../api/types'
import { fmtDateTime, fmtPct } from '../format'

export default function DashboardPage() {
  const pats = useFetch<Page<Patient>>('/api/patients?size=1')
  const ans = useFetch<Analysis[]>('/api/analyses')

  if (pats.loading || ans.loading) return <p>Chargement…</p>
  const error = pats.error ?? ans.error
  if (error) return <p className="error" role="alert">{error}</p>

  const analyses = ans.data ?? []
  const suspected = analyses.filter((a) => a.label === 'PNEUMONIA').length
  const toReview = analyses.filter((a) => !a.doctorOpinion).length

  return (
    <>
      <h1>Tableau de bord</h1>
      <div className="stats">
        <div className="stat"><strong>{pats.data?.total ?? 0}</strong><span>Patients</span></div>
        <div className="stat"><strong>{analyses.length}</strong><span>Analyses</span></div>
        <div className="stat"><strong>{suspected}</strong><span>Pneumonies suspectées</span></div>
        <div className="stat"><strong>{toReview}</strong><span>Sans avis du médecin</span></div>
      </div>

      <section className="panel">
        <h2>Analyses récentes</h2>
        {analyses.length === 0 ? (
          <p className="muted">Aucune analyse. Ouvrez la fiche d'un patient pour en lancer une.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Date</th><th>Patient</th><th>Résultat</th><th>Avis</th><th></th></tr>
            </thead>
            <tbody>
              {analyses.slice(0, 8).map((a) => (
                <tr key={a.id}>
                  <td>{fmtDateTime(a.createdAt)}</td>
                  <td>{a.patient ? `${a.patient.lastName} ${a.patient.firstName}` : '—'}</td>
                  <td>
                    {a.label === 'PNEUMONIA'
                      ? `Pneumonie suspectée ${fmtPct(a.probabilityPneumonia)} %`
                      : `Pas de pneumonie (${fmtPct(a.probabilityPneumonia)} %)`}
                  </td>
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