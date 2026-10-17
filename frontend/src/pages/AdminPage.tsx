import { useState } from 'react'
import { putJson } from '../api/client'
import { useFetch } from '../api/useFetch'

type U = { id: number; email: string; role: string; enabled: boolean }

export default function AdminPage() {
  const { data, error, loading } = useFetch<U[]>('/api/admin/users')
  const [changes, setChanges] = useState<Record<number, boolean>>({})
  const [err, setErr] = useState<string | null>(null)

  if (loading) return <p>Chargement…</p>
  if (error) return <p className="error" role="alert">{error}</p>

  async function toggle(u: U) {
    const next = !(changes[u.id] ?? u.enabled)
    setErr(null)
    try {
      await putJson(`/api/admin/users/${u.id}/enabled`, { enabled: next })
      setChanges((c) => ({ ...c, [u.id]: next }))
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Erreur inconnue')
    }
  }

  return (
    <>
      <h1>Comptes</h1>
      {err && <p className="error" role="alert">{err}</p>}
      <section className="panel">
        <table>
          <thead><tr><th>Email</th><th>Rôle</th><th>Statut</th><th></th></tr></thead>
          <tbody>
            {(data ?? []).map((u) => {
              const enabled = changes[u.id] ?? u.enabled
              return (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.role}</td>
                  <td>{enabled ? 'Actif' : 'En attente / désactivé'}</td>
                  <td>
                    {u.role !== 'ADMIN' && (
                      <button onClick={() => toggle(u)}>{enabled ? 'Désactiver' : 'Activer'}</button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </section>
    </>
  )
}