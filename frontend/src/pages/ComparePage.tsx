import { Link, useSearchParams } from 'react-router-dom'
import { useFetch } from '../api/useFetch'
import type { Analysis } from '../api/types'
import { fmtDateTime, fmtPct } from '../format'

type Cmp = { first: Analysis; second: Analysis; probabilityDelta: number }

const typeText = (a: Analysis) =>
  !a.pneumoniaType ? 'non évalué'
  : a.pneumoniaType === 'UNDETERMINED' ? 'indéterminé'
  : `${a.pneumoniaType === 'VIRAL' ? 'viral' : 'bactérien'}${a.typeConfidence != null ? ` (indice ${fmtPct(a.typeConfidence, 0)} %)` : ''}`
const opinion = (a: Analysis) =>
  a.doctorOpinion === 'AGREE' ? "D'accord" : a.doctorOpinion === 'DISAGREE' ? "Pas d'accord" : 'Aucun'

export default function ComparePage() {
  const [sp] = useSearchParams()
  const { data, error, loading } = useFetch<Cmp>(
    `/api/analyses/compare?a=${encodeURIComponent(sp.get('a') ?? '')}&b=${encodeURIComponent(sp.get('b') ?? '')}`)
  if (loading) return <p>Chargement…</p>
  if (error) return <p className="error" role="alert">{error}</p>
  if (!data) return null
  const { first: x, second: y, probabilityDelta: d } = data
  const rows: [string, (a: Analysis) => string][] = [
    ['Date', (a) => fmtDateTime(a.createdAt)],
    ['Probabilité de pneumonie', (a) => `${fmtPct(a.probabilityPneumonia)} %`],
    ['Type', typeText],
    ['Température', (a) => a.temperatureC != null ? `${a.temperatureC.toLocaleString('fr-FR')} °C` : '—'],
    ['Saturation', (a) => a.spo2Percent != null ? `${a.spo2Percent} %` : '—'],
    ['Durée des symptômes', (a) => a.symptomDays != null ? `${a.symptomDays} j` : '—'],
    ['Avis du médecin', opinion],
    ['Version du modèle', (a) => a.modelVersion ?? 'non renseignée'],
  ]
  return (
    <>
      <h1>Comparaison de deux analyses</h1>
      {x.patient && <p>Patient : <Link to={`/patients/${x.patient.id}`}>{x.patient.lastName} {x.patient.firstName}</Link></p>}
      <section className="panel">
        <table>
          <thead><tr><th></th><th>Analyse n° {x.id} (plus ancienne)</th><th>Analyse n° {y.id} (plus récente)</th></tr></thead>
          <tbody>
            {rows.map(([label, f]) => <tr key={label}><td><strong>{label}</strong></td><td>{f(x)}</td><td>{f(y)}</td></tr>)}
          </tbody>
        </table>
        <p>
          <strong>Évolution de la probabilité :</strong>{' '}
          {d > 0 ? '+' : ''}{(d * 100).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} points
        </p>
        <p className="muted">
          Un écart de probabilité entre deux radiographies n'est pas une mesure de l'évolution clinique : il
          dépend aussi de la qualité et de la position de l'image. À interpréter avec la clinique.
        </p>
      </section>
      <p><Link to={`/analyses/${x.id}`}>Ouvrir n° {x.id}</Link> · <Link to={`/analyses/${y.id}`}>Ouvrir n° {y.id}</Link></p>
    </>
  )
}