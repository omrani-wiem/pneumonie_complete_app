import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import type { Page, Patient } from '../api/types'
import { fmtAge, fmtDate } from '../format'

const SIZE = 20

export default function PatientsPage() {
  const [input, setInput] = useState('')
  const [q, setQ] = useState('')
  const [archived, setArchived] = useState(false)
  const [page, setPage] = useState(0)

  useEffect(() => {
    const t = setTimeout(() => { setQ(input.trim()); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [input])

  const { data, error, loading } = useFetch<Page<Patient>>(
    `/api/patients?q=${encodeURIComponent(q)}&archived=${archived}&page=${page}&size=${SIZE}`,
  )
  const totalPages = data ? Math.max(1, Math.ceil(data.total / SIZE)) : 1

  return (
    <>
      <div className="row">
        <h1>Patients</h1>
        <Link className="btn" to="/patients/new">Nouveau patient</Link>
      </div>
      <input
        className="search"
        placeholder="Rechercher (nom, prénom, n° de dossier)"
        value={input}
        onChange={(e) => setInput(e.target.value)}
      />
      <label className="inline">
        <input type="checkbox" checked={archived}
          onChange={(e) => { setArchived(e.target.checked); setPage(0) }} />
        Afficher les patients archivés
      </label>

      <section className="panel">
        {error ? <p className="error" role="alert">{error}</p>
        : loading ? <p>Chargement…</p>
        : (data?.items ?? []).length === 0 ? (
          <p className="muted">{q ? 'Aucun résultat.' : archived ? 'Aucun patient archivé.' : 'Aucun patient enregistré.'}</p>
        ) : (
          <table>
            <thead>
              <tr><th>Dossier</th><th>Nom</th><th>Naissance</th><th>Âge</th><th>Sexe</th><th></th></tr>
            </thead>
            <tbody>
              {data!.items.map((p) => (
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
        {data && data.total > SIZE && (
          <div className="row">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Précédent</button>
            <span className="muted">Page {page + 1} / {totalPages} ({data.total} patients)</span>
            <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Suivant</button>
          </div>
        )}
      </section>
    </>
  )
}