package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.ml.MlClient;
import com.example.pneumonia.ml.MlResult;
import com.example.pneumonia.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

    private final MlClient mlClient;
    private final AnalysisRepository repo;
    private final String modelVersion;

    public AnalysisService(MlClient mlClient, AnalysisRepository repo,
                           @Value("${app.ml.model-version:}") String modelVersion) {
        this.mlClient = mlClient;
        this.repo = repo;
        this.modelVersion = modelVersion == null || modelVersion.isBlank() ? null : modelVersion.trim();
    }

    public Analysis analyze(MultipartFile file, String safeFilename, AppUser owner, Patient patient,
                            Double temperatureC, Integer spo2Percent, Integer symptomDays) throws IOException {
        MlResult r = mlClient.predict(file);
        Analysis a = new Analysis();
        a.setOwner(owner);
        a.setPatient(patient);
        a.setFilename(safeFilename);
        boolean pneumonia = "PNEUMONIA".equals(r.label());
        a.setPneumoniaType(pneumonia ? r.pneumoniaType() : null);
        a.setTypeConfidence(pneumonia ? r.typeConfidence() : null);
        a.setLabel(r.label());
        a.setProbabilityPneumonia(r.probabilityPneumonia());
        a.setHeatmapBase64(r.heatmapBase64());
        a.setTemperatureC(temperatureC);
        a.setSpo2Percent(spo2Percent);
        a.setSymptomDays(symptomDays);
        a.setModelVersion(modelVersion);
        return repo.save(a);
    }

    public Analysis save(Analysis a) { return repo.save(a); }

    public List<Analysis> historyOf(AppUser owner) {
        return repo.findAllByOwnerOrderByCreatedAtDesc(owner);
    }

    public Optional<Analysis> findOf(Long id, AppUser owner) {
        return repo.findByIdAndOwner(id, owner);
    }

    public List<Analysis> allWithOwner() {
        return repo.findAllWithOwner();
    }
}