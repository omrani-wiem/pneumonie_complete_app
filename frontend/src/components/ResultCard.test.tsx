import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ResultCard from './ResultCard'

describe('ResultCard', () => {
  it('affiche « Type non évalué » quand il n\'y a pas de pneumonie', () => {
    render(<ResultCard probability={0.12} pneumoniaType={null} typeConfidence={null} typeLabel={null} />)
    expect(screen.getByText(/Pas de pneumonie détectée/)).toBeInTheDocument()
    expect(screen.getByText(/Type non évalué/)).toBeInTheDocument()
  })

  it('affiche l\'indice de confiance du modèle pour un type viral', () => {
    render(<ResultCard probability={0.9} pneumoniaType="VIRAL" typeConfidence={0.81} typeLabel="Pneumonie probablement virale" />)
    expect(screen.getByText(/Pneumonie suspectée/)).toBeInTheDocument()
    expect(screen.getByText(/indice de confiance du modèle/)).toBeInTheDocument()
    expect(screen.getByText(/fiabilité limitée/)).toBeInTheDocument()
  })

  it('signale l\'abstention quand le type est indéterminé', () => {
    render(<ResultCard probability={0.8} pneumoniaType="UNDETERMINED" typeConfidence={0.6} typeLabel={null} />)
    expect(screen.getByText(/indéterminé/)).toBeInTheDocument()
    expect(screen.queryByText(/indice de confiance du modèle : /)).not.toBeInTheDocument()
  })

  it('dit franchement quand la probabilité est absente', () => {
    render(<ResultCard probability={null} pneumoniaType={null} typeConfidence={null} typeLabel={null} />)
    expect(screen.getByText(/absente de la réponse/)).toBeInTheDocument()
  })
})