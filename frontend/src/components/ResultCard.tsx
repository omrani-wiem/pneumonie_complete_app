type Props = {
  probability: number | null
  pneumoniaType: string | null
  typeConfidence: number | null
  typeLabel: string | null
}

const pct = (v: number, digits = 0) =>
  (v * 100).toLocaleString('fr-FR', { minimumFractionDigits: digits, maximumFractionDigits: digits })

export default function ResultCard({ probability, pneumoniaType, typeConfidence, typeLabel }: Props) {
  const pneumonia = probability !== null && probability >= 0.5

  let typeLine: string
  if (probability === null) typeLine = 'Non disponible'
  else if (!pneumonia || !pneumoniaType) typeLine = 'Type non évalué'
  else if (pneumoniaType === 'UNDETERMINED')
    typeLine = 'Type indéterminé : le modèle s\'abstient (confiance insuffisante)'
  else {
    const label = typeLabel ?? (pneumoniaType === 'VIRAL' ? 'Pneumonie probablement virale' : 'Pneumonie probablement bactérienne')
    typeLine =
      typeConfidence !== null
        ? `${label} (indice de confiance du modèle : ${pct(typeConfidence)} %)`
        : label
  }

  return (
    <section className="panel">
      <h2>Résultat</h2>
      {probability === null ? (
        <p className="error">La probabilité de pneumonie est absente de la réponse du serveur.</p>
      ) : pneumonia ? (
        <p className="big">Pneumonie suspectée {pct(probability, 1)} %</p>
      ) : (
        <p className="big">Pas de pneumonie détectée (probabilité de pneumonie : {pct(probability, 1)} %)</p>
      )}
      <p><strong>Type de pneumonie :</strong> {typeLine}</p>
      {pneumonia && pneumoniaType && pneumoniaType !== 'UNDETERMINED' && (
        <p className="muted">Orientation à fiabilité limitée. L'indice de confiance n'est pas une probabilité calibrée.</p>
      )}
    </section>
  )
}