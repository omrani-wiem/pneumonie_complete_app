export default function Heatmap({ base64 }: { base64: string | null }) {
  if (!base64) {
    return (
      <section className="panel">
        <h2>Zones influençant la prédiction (Grad-CAM)</h2>
        <p>Heatmap non disponible pour cette analyse.</p>
      </section>
    )
  }
  const src = base64.startsWith('data:')
    ? base64
    : `data:image/${base64.startsWith('/9j/') ? 'jpeg' : 'png'};base64,${base64}`
  return (
    <section className="panel">
      <h2>Zones influençant la prédiction (Grad-CAM)</h2>
      <img className="heatmap" src={src} alt="Carte de chaleur Grad-CAM" />
      <p className="muted">Les zones rouges et jaunes sont celles qui ont le plus pesé dans la décision du modèle.</p>
    </section>
  )
}