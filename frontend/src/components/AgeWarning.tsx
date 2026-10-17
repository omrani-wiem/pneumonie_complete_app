import type { Patient } from '../api/types'
import { fmtAge } from '../format'

export default function AgeWarning({ patient }: { patient: Patient }) {
  if (patient.ageInModelRange) return null
  return (
    <div className="warn" role="alert">
      <strong>Âge hors de la plage du modèle.</strong> Ce patient a {fmtAge(patient.ageMonths)}. Le modèle a été
      entraîné sur des enfants de 1 à 5 ans : son résultat n'est pas validé pour cet âge et doit être
      interprété avec une grande prudence.
    </div>
  )
}