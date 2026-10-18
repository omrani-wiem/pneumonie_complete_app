import { useFetch } from '../api/useFetch'

type Group = { agree: number; disagree: number }
type Stats = { total: number; withOpinion: number; agree: number; disagree: number; pneumonia: Group; normal: Group }

const rate = (g: Group) => {
  const n = g.agree + g.disagree
  return n === 0 ? '—' : `${Math.round((g.agree / n) * 100)} % (${g.agree}/${n})`
}

export default function StatsPanel() {
  const { data, error, loading } = useFetch<Stats>('/api/stats')
  if (loading) return null
  if (error || !data) return <p className="error" role="alert">{error}</p>
  return (
    <section className="panel">
      <h2>Concordance médecin / modèle</h2>
      {data.withOpinion === 0 ? (
        <p className="muted">Aucun avis enregistré pour l'instant.</p>
      ) : (
        <table>
          <thead><tr><th>Groupe</th><th>Avis d'accord</th></tr></thead>
          <tbody>
            <tr><td>Toutes les analyses avec avis</td><td>{rate({ agree: data.agree, disagree: data.disagree })}</td></tr>
            <tr><td>Pneumonie suspectée par le modèle</td><td>{rate(data.pneumonia)}</td></tr>
            <tr><td>Pas de pneumonie selon le modèle</td><td>{rate(data.normal)}</td></tr>
          </tbody>
        </table>
      )}
      <p className="muted">
        Cet indicateur mesure l'accord avec l'avis du médecin, pas la justesse du modèle : l'avis n'est pas une
        vérité de référence. Il porte sur {data.withOpinion} analyse(s) sur {data.total}.
      </p>
    </section>
  )
}