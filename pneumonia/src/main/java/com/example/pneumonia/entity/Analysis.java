package com.example.pneumonia.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity 

public class Analysis {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String label;
    private double probabilityPneumonia;

    @Column(columnDefinition = "TEXT")
    private String heatmapBase64;

    // Type estimé : BACTERIAL, VIRAL, UNDETERMINED, ou null (non évalué / pas de pneumonie).
    private String pneumoniaType;
    private Double typeConfidence;
    // Wrapper nullable : les anciennes lignes (créées avant cette colonne) restent valides.
    


    private Instant createdAt = Instant.now();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser owner;

    public AppUser getOwner() { return owner;}
     public void setOwner(AppUser owner) { this.owner = owner; }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getProbabilityPneumonia() { return probabilityPneumonia; }
    public void setProbabilityPneumonia(double p) { this.probabilityPneumonia = p; }
    public String getHeatmapBase64() { return heatmapBase64; }
    public void setHeatmapBase64(String h) { this.heatmapBase64 = h; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getPneumoniaType() { return pneumoniaType; }
    public void setPneumoniaType(String t) { this.pneumoniaType = t; }
    public Double getTypeConfidence() { return typeConfidence; }
    public void setTypeConfidence(Double c) { this.typeConfidence = c; }
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "temperature_c")
    private Double temperatureC;
    @Column(name = "spo2_percent")
    private Integer spo2Percent;
    @Column(name = "symptom_days")
    private Integer symptomDays;
    @Column(name = "model_version")
    private String modelVersion;
    @Column(name = "doctor_opinion")
    private String doctorOpinion;
    @Column(name = "doctor_comment", columnDefinition = "TEXT")
    private String doctorComment;
    @Column(name = "opinion_at")
    private Instant opinionAt;

    public Patient getPatient() { return patient; }
    public void setPatient(Patient p) { this.patient = p; }
    public Double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Double v) { this.temperatureC = v; }
    public Integer getSpo2Percent() { return spo2Percent; }
    public void setSpo2Percent(Integer v) { this.spo2Percent = v; }
    public Integer getSymptomDays() { return symptomDays; }
    public void setSymptomDays(Integer v) { this.symptomDays = v; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String v) { this.modelVersion = v; }
    public String getDoctorOpinion() { return doctorOpinion; }
    public void setDoctorOpinion(String v) { this.doctorOpinion = v; }
    public String getDoctorComment() { return doctorComment; }
    public void setDoctorComment(String v) { this.doctorComment = v; }
    public Instant getOpinionAt() { return opinionAt; }
    public void setOpinionAt(Instant v) { this.opinionAt = v; }



}
