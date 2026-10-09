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




}
