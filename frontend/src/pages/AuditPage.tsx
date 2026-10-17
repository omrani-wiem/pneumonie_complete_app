import { useState } from 'react'
import { useFetch } from '../api/useFetch'
import { fmtDateTime } from '../format'

type Row = {
  id: number; createdAt: string; actorEmail?: string; action: string
  targetType?: string; targetId?: number; ip?: string; success: boolean
}
const SIZE = 50

export default function AuditPage() {
  const [page, setPage] = useState(0)
  const { data, error, loading } = useFetch<Row[]>(`/api/admin/audit?page=${page}&size=${SIZE}`)

  if (error) return <p className="error" role="alert">{error}</p>

  return (
    <>
      <h1>Journal d'audit</h1>
      <p className="muted">Accès réservé aux administrateurs. Le journal ne contient que des identifiants.</p>
      <section className="panel">
        {loading ? <p>Chargement…</p> : (data ?? []).length === 0 ? <p className="muted">Aucune entrée.</p> : (
          <table>
            <thead><tr><th>Date</th><th>Utilisateur</th><th>Action</th><th>Cible</th><th>IP</th><th>Résultat</th></tr></thead>
            <tbody>
              {(data ?? []).map((r) => (
                <tr key={r.id}>
                  <td>{fmtDateTime(r.createdAt)}</td>
                  <td>{r.actorEmail ?? '—'}</td>
                  <td>{r.action}</td>
                  <td>{r.targetType ? `${r.targetType} #${r.targetId ?? ''}` : '—'}</td>
                  <td>{r.ip ?? '—'}</td>
                  <td>{r.success ? 'OK' : 'Échec'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="row">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Précédent</button>
          <button disabled={(data ?? []).length < SIZE} onClick={() => setPage((p) => p + 1)}>Suivant</button>
        </div>
      </section>
    </>
  )
}