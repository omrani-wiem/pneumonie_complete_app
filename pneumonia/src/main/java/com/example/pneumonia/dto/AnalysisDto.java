package com.example.pneumonia.dto;

import com.example.pneumonia.entity.Analysis;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisDto (
        Long id,
        String filename,
        String label,
        double probabilityPneumonia,
        Instant createdAt,
        String heatmapBase64) {

            public static AnalysisDto of(Analysis a, boolean withHeatmap) {
                return new AnalysisDto(a.getId(), a.getFilename(), a.getLabel(),
                     a.getProbabilityPneumonia(), a.getCreatedAt(),
                    withHeatmap ? a.getHeatmapBase64() : null);
    }
}
