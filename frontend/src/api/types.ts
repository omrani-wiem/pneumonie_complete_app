import type { Reco } from '../components/Recommendations'

export type Patient = {
  id: number
  recordNumber: string
  lastName: string
  firstName: string
  birthDate: string
  sex: 'F' | 'M'
  notes?: string
  createdAt: string
  ageMonths: number
  ageInModelRange: boolean
}

export type Analysis = {
  id: number
  filename?: string
  label: string
  probabilityPneumonia: number
  createdAt: string
  heatmapBase64?: string
  pneumoniaType?: string
  typeConfidence?: number
  recommendation?: Reco & { typeLabel?: string }
  patient?: Patient
  temperatureC?: number
  spo2Percent?: number
  symptomDays?: number
  modelVersion?: string
  doctorOpinion?: 'AGREE' | 'DISAGREE'
  doctorComment?: string
  opinionAt?: string
}