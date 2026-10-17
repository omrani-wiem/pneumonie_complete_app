export const fmtDate = (iso: string) =>
  new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })

export const fmtDateTime = (iso: string) =>
  new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })

export const fmtPct = (v: number, digits = 1) =>
  (v * 100).toLocaleString('fr-FR', { minimumFractionDigits: digits, maximumFractionDigits: digits })

export function fmtAge(months: number): string {
  if (months < 24) return `${months} mois`
  const y = Math.floor(months / 12)
  const m = months % 12
  return m === 0 ? `${y} ans` : `${y} ans ${m} mois`
}