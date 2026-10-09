package com.example.pneumonia.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlResult(
        String label,
        @JsonProperty("probability_pneumonia") double probabilityPneumonia,
        @JsonProperty("heatmap_base64") String heatmapBase64) {
}
