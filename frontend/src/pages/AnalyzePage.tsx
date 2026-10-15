import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, clearToken, downloadPdf, request } from '../api/client'
import DisclaimerBanner from '../components/DisclaimerBanner'
import ResultCard from '../components/ResultCard'
import Recommendations from '../components/Recommendations'
import type { Reco } from '../components/Recommendations'
import Heatmap from '../components/Heatmap'

type Raw = Record<string, unknown>

const pickNum = (o: Raw, keys: string[]): number | null => {
  for (const k of keys) if (typeof o[k] === 'number') return o[k] as number
  return null
}
const pickStr = (o: Raw, keys: string[]): string | null => {
  for (const k of keys) if (typeof o[k] === 'string' && o[k]) return o[k] as string
  return null
}

export default function AnalyzePage() {
  const nav = useNavigate()
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<Raw | null>(null)
  const [pdfBusy, setPdfBusy] = useState(false)

  useEffect(() => {
    if (!file) { setPreview(null); return }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      nav('/login', { replace: true })
      return
    }
    setError(err instanceof Error ? err.message : 'Erreur inconnue')
  }

  function onPick(e: ChangeEvent<HTMLInputElement>) {
    setFile(e.target.files?.[0] ?? null)
    setResult(null)
    setError(null)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!file) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const form = new FormData()
      form.append('file', file)
      setResult(await request<Raw>('/api/analyses', { method: 'POST', body: form }))
    } catch (err) {
      handleError(err)
    } finally {
      setBusy(false)
    }
  }

  async function onPdf() {
    const id = result?.id
    if (typeof id !== 'number' && typeof id !== 'string') return
    setPdfBusy(true)
    try {
      await downloadPdf(id)
    } catch (err) {
      handleError(err)
    } finally {
      setPdfBusy(false)
    }
  }

  const probability = result ? pickNum(result, ['probabilityPneumonia', 'pneumoniaProbability', 'probability']) : null
  const pneumonia = probability !== null && probability >= 0.5
  const reco = (result?.recommendation ?? null) as (Reco & { typeLabel?: string }) | null
  const type = result ? pickStr(result, ['pneumoniaType']) : null
  const typeConf = result ? pickNum(result, ['typeConfidence']) : null
  const heatmap = result ? pickStr(result, ['heatmapBase64', 'heatmap', 'gradcam']) : null

  return (
    <div className="page">
      <header className="topbar">
        <h1>Aide à la décision : pneumonie</h1>
        <button onClick={() => { clearToken(); nav('/login') }}>Se déconnecter</button>
      </header>

      <DisclaimerBanner />

      <section className="panel">
        <h2>Nouvelle analyse</h2>
        <form onSubmit={onSubmit}>
          <input type="file" accept="image/jpeg,image/png" onChange={onPick} />
          {preview && <img className="preview" src={preview} alt="Radiographie sélectionnée" />}
          <button disabled={!file || busy}>{busy ? 'Analyse en cours…' : 'Analyser'}</button>
        </form>
        {busy && <p className="muted">L'analyse peut prendre quelques secondes.</p>}
        {error && <p className="error" role="alert">{error}</p>}
      </section>

      {result && (
        <>
          <ResultCard
            probability={probability}
            pneumoniaType={type}
            typeConfidence={typeConf}
            typeLabel={reco?.typeLabel ?? null}
          />
          {pneumonia && reco && <Recommendations reco={reco} />}
          <Heatmap base64={heatmap} />
          <button onClick={onPdf} disabled={pdfBusy || result.id == null}>
            {pdfBusy ? 'Téléchargement…' : 'Télécharger le rapport PDF'}
          </button>
          <details className="panel">
            <summary>Réponse brute du serveur (contrôle de développement)</summary>
            <pre>{JSON.stringify({ ...result, heatmapBase64: heatmap ? '[…base64…]' : null }, null, 2)}</pre>
          </details>
        </>
      )}
    </div>
  )
}