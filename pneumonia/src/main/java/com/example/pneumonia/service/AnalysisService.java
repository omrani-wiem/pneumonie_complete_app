package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.ml.MlClient;
import com.example.pneumonia.ml.MlResult;
import com.example.pneumonia.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

    private final MlClient mlClient;
    private final AnalysisRepository repo;

    public AnalysisService(MlClient mlClient, AnalysisRepository repo) {
        this.mlClient = mlClient;
        this.repo = repo;
    }

        public Analysis analyze(MultipartFile file, String safeFilename, AppUser owner) throws IOException {
        MlResult r = mlClient.predict(file);
        Analysis a = new Analysis();
        a.setOwner(owner);
        a.setFilename(safeFilename);
            // Un type n'a de sens que s'il y a pneumonie.
        boolean pneumonia = "PNEUMONIA".equals(r.label());
        a.setPneumoniaType(pneumonia ? r.pneumoniaType() : null);
        a.setTypeConfidence(pneumonia ? r.typeConfidence() : null);
        a.setSimulated(r.simulated());
        a.setLabel(r.label());
        a.setProbabilityPneumonia(r.probabilityPneumonia());
        a.setHeatmapBase64(r.heatmapBase64());
        return repo.save(a);
    }

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