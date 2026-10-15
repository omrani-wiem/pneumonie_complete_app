export type Reco = {
  summary?: string
  confirmatoryWork?: string[] | string
  treatment?: string[] | string
  supportive?: string[] | string
  followUp?: string[] | string
  redFlags?: string[] | string
  limits?: string
}

const toList = (v: string[] | string | undefined): string[] =>
  Array.isArray(v) ? v : v ? [v] : []

function Block({ title, items, danger }: { title: string; items: string[]; danger?: boolean }) {
  if (items.length === 0) return null
  return (
    <div className={danger ? 'block danger' : 'block'}>
      <h3>{title}</h3>
      <ul>{items.map((t, i) => <li key={i}>{t}</li>)}</ul>
    </div>
  )
}

export default function Recommendations({ reco }: { reco: Reco }) {
  return (
    <section className="panel">
      <h2>Pistes de prise en charge à discuter (suggestions)</h2>
      {reco.summary && <p>{reco.summary}</p>}
      <Block title="Examens pour confirmer et préciser" items={toList(reco.confirmatoryWork)} />
      <Block title="Traitement : points à considérer" items={toList(reco.treatment)} />
      <Block title="Mesures de soutien" items={toList(reco.supportive)} />
      <Block title="Suivi" items={toList(reco.followUp)} />
      <Block title="Signes de gravité : évaluation urgente" items={toList(reco.redFlags)} danger />
      {reco.limits && <p className="muted"><strong>Limites :</strong> {reco.limits}</p>}
    </section>
  )
}