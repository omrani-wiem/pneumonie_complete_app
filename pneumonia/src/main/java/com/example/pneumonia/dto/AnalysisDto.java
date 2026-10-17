package com.example.pneumonia.dto;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.service.TreatmentRecommendationService;
import com.example.pneumonia.service.TreatmentRecommendationService.Recommendation;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisDto(
        Long id,
        String filename,
        String label,
        double probabilityPneumonia,
        Instant createdAt,
        String heatmapBase64,
        String pneumoniaType,
        Double typeConfidence,
        Recommendation recommendation,
        PatientDto patient,
        Double temperatureC,
        Integer spo2Percent,
        Integer symptomDays,
        String modelVersion,
        String doctorOpinion,
        String doctorComment,
        Instant opinionAt) {

    public static AnalysisDto of(Analysis a, boolean withDetails, TreatmentRecommendationService recos) {
        boolean pneumonia = "PNEUMONIA".equals(a.getLabel());
        return new AnalysisDto(a.getId(), a.getFilename(), a.getLabel(),
                a.getProbabilityPneumonia(), a.getCreatedAt(),
                withDetails ? a.getHeatmapBase64() : null,
                a.getPneumoniaType(), a.getTypeConfidence(),
                withDetails && pneumonia ? recos.recommend(a.getPneumoniaType()) : null,
                a.getPatient() == null ? null : PatientDto.of(a.getPatient()),
                a.getTemperatureC(), a.getSpo2Percent(), a.getSymptomDays(),
                a.getModelVersion(), a.getDoctorOpinion(), a.getDoctorComment(), a.getOpinionAt());
    }
}