import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import type { Patient } from '../api/types'
import { fmtAge, fmtDate } from '../format'

export default function PatientsPage() {
  const { data, error, loading } = useFetch<Patient[]>('/api/patients')
  const [q, setQ] = useState('')

  if (loading) return <p>Chargement…</p>
  if (error) return <p className="error" role="alert">{error}</p>

  const term = q.trim().toLowerCase()
  const list = (data ?? []).filter((p) =>
    !term || `${p.lastName} ${p.firstName} ${p.recordNumber}`.toLowerCase().includes(term))

  return (
    <>
      <div className="row">
        <h1>Patients</h1>
        <Link className="btn" to="/patients/new">Nouveau patient</Link>
      </div>
      <input
        className="search"
        placeholder="Rechercher (nom, prénom, n° de dossier)"
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />
      <section className="panel">
        {list.length === 0 ? (
          <p className="muted">{data?.length ? 'Aucun résultat.' : 'Aucun patient enregistré.'}</p>
        ) : (
          <table>
            <thead>
              <tr><th>Dossier</th><th>Nom</th><th>Naissance</th><th>Âge</th><th>Sexe</th><th></th></tr>
            </thead>
            <tbody>
              {list.map((p) => (
                <tr key={p.id}>
                  <td>{p.recordNumber}</td>
                  <td>{p.lastName} {p.firstName}</td>
                  <td>{fmtDate(p.birthDate)}</td>
                  <td>
                    {fmtAge(p.ageMonths)}{' '}
                    {!p.ageInModelRange && <span className="badge warn-badge">hors plage</span>}
                  </td>
                  <td>{p.sex}</td>
                  <td><Link to={`/patients/${p.id}`}>Ouvrir</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}