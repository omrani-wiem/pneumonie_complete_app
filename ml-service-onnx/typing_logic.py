"""Interprétation des sorties des deux modèles (pure Python, testable sans onnxruntime).

Cascade :
1. Modèle de détection (existant) : P(pneumonie).
2. Modèle de type (optionnel)     : P(virale) parmi les pneumonies. Appelé seulement si
   la détection dit "pneumonie". Absent => aucun type n'est proposé.
"""


def interpret(p_pneu: float, p_viral=None, type_min_conf: float = 0.65) -> dict:
    p_pneu = float(p_pneu)
    label = "PNEUMONIA" if p_pneu >= 0.5 else "NORMAL"
    result = {
        "label": label,
        "probability_pneumonia": p_pneu,
        "pneumonia_type": None,        # None = non évalué (pas de pneumonie ou pas de modèle de type)
        "type_confidence": None,
    }
    if label != "PNEUMONIA" or p_viral is None:
        return result

    p_viral = float(p_viral)
    ptype, conf = ("VIRAL", p_viral) if p_viral >= 0.5 else ("BACTERIAL", 1.0 - p_viral)
    result["type_confidence"] = conf
    # Trop incertain : on ne force pas un type
    result["pneumonia_type"] = ptype if conf >= type_min_conf else "UNDETERMINED"
    return result