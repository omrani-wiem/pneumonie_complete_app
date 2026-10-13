package com.example.pneumonia.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Réponse du service ML.
 * pneumoniaType : "BACTERIAL", "VIRAL", "UNDETERMINED" (confiance insuffisante)
 * ou null si le modèle déployé est binaire et ne sait pas typer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MlResult(
        String label,
        @JsonProperty("probability_pneumonia") double probabilityPneumonia,
        @JsonProperty("heatmap_base64") String heatmapBase64,
        @JsonProperty("pneumonia_type") String pneumoniaType,
        @JsonProperty("type_confidence") Double typeConfidence,
        boolean simulated) {
}