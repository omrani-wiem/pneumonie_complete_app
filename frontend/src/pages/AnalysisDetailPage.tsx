import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError, downloadPdf, putJson } from '../api/client'
import { useFetch } from '../api/useFetch'
import type { Analysis } from '../api/types'
import AgeWarning from '../components/AgeWarning'
import DisclaimerBanner from '../components/DisclaimerBanner'
import ResultCard from '../components/ResultCard'
import Recommendations from '../components/Recommendations'
import Heatmap from '../components/Heatmap'
import { fmtDateTime } from '../format'

function OpinionForm({ a, onSaved }: { a: Analysis; onSaved: (a: Analysis) => void }) {
  const nav = useNavigate()
  const [opinion, setOpinion] = useState<string>(a.doctorOpinion ?? '')
  const [comment, setComment] = useState(a.doctorComment ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function save() {
    if (!opinion) return
    setBusy(true)
    setError(null)
    try {
      onSaved(await putJson<Analysis>(`/api/analyses/${a.id}/opinion`, { opinion, comment }))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) { nav('/login', { replace: true }); return }
      setError(err instanceof Error ? err.message : 'Erreur inconnue')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel">
      <h2>Avis du médecin</h2>
      <p className="muted">Votre avis est enregistré séparément du résultat du modèle.</p>
      <label className="inline">
        <input type="radio" name="op" checked={opinion === 'AGREE'} onChange={() => setOpinion('AGREE')} />
        D'accord avec l'orientation du modèle
      </label>
      <label className="inline">
        <input type="radio" name="op" checked={opinion === 'DISAGREE'} onChange={() => setOpinion('DISAGREE')} />
        Pas d'accord
      </label>
      <label>Commentaire / conclusion
        <textarea rows={3} value={comment} onChange={(e) => setComment(e.target.value)} maxLength={2000} />
      </label>
      {error && <p className="error" role="alert">{error}</p>}
      <button onClick={save} disabled={!opinion || busy}>{busy ? 'Enregistrement…' : 'Enregistrer mon avis'}</button>
      {a.opinionAt && <p className="muted">Dernier enregistrement : {fmtDateTime(a.opinionAt)}</p>}
    </section>
  )
}

export default function AnalysisDetailPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const { data, error, loading } = useFetch<Analysis>(`/api/analyses/${id}`)
  const [updated, setUpdated] = useState<Analysis | null>(null)
  const [pdfBusy, setPdfBusy] = useState(false)
  const [pdfError, setPdfError] = useState<string | null>(null)

  if (loading) return <p>Chargement…</p>
  if (error) return <p className="error" role="alert">{error}</p>
  const a = updated ?? data
  if (!a) return null

  const pneumonia = a.label === 'PNEUMONIA'
  const nearThreshold = a.probabilityPneumonia >= 0.4 && a.probabilityPneumonia <= 0.6

  async function onPdf() {
    setPdfBusy(true)
    setPdfError(null)
    try {
      await downloadPdf(a!.id)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) { nav('/login', { replace: true }); return }
      setPdfError(err instanceof Error ? err.message : 'Erreur inconnue')
    } finally {
      setPdfBusy(false)
    }
  }

  return (
    <>
      <div className="row">
        <h1>Analyse n° {a.id}</h1>
        <button onClick={onPdf} disabled={pdfBusy}>{pdfBusy ? 'Téléchargement…' : 'Rapport PDF'}</button>
      </div>
      {pdfError && <p className="error" role="alert">{pdfError}</p>}

      {a.patient && (
        <>
          <p>
            Patient : <Link to={`/patients/${a.patient.id}`}>{a.patient.lastName} {a.patient.firstName}</Link>{' '}
            (dossier {a.patient.recordNumber}) · {fmtDateTime(a.createdAt)}
          </p>
          <AgeWarning patient={a.patient} />
        </>
      )}

      <DisclaimerBanner />

      {nearThreshold && (
        <div className="warn" role="alert">
          <strong>Résultat proche du seuil de décision.</strong> La probabilité est voisine de 50 % : ce résultat
          est peu discriminant et doit être confronté à la clinique.
        </div>
      )}

      <ResultCard
        probability={a.probabilityPneumonia}
        pneumoniaType={a.pneumoniaType ?? null}
        typeConfidence={a.typeConfidence ?? null}
        typeLabel={a.recommendation?.typeLabel ?? null}
      />

      {(a.temperatureC != null || a.spo2Percent != null || a.symptomDays != null) && (
        <section className="panel">
          <h2>Contexte clinique</h2>
          <ul>
            {a.temperatureC != null && <li>Température : {a.temperatureC.toLocaleString('fr-FR')} °C</li>}
            {a.spo2Percent != null && <li>Saturation en oxygène : {a.spo2Percent} %</li>}
            {a.symptomDays != null && <li>Durée des symptômes : {a.symptomDays} jour(s)</li>}
          </ul>
        </section>
      )}

      {pneumonia && a.recommendation && <Recommendations reco={a.recommendation} />}
      <Heatmap base64={a.heatmapBase64 ?? null} />

      <OpinionForm key={a.id} a={a} onSaved={setUpdated} />

      <p className="muted">
        Version du modèle : {a.modelVersion ?? 'non renseignée'}. La radiographie d'origine n'est pas conservée
        par l'application.
      </p>
    </>
  )
}