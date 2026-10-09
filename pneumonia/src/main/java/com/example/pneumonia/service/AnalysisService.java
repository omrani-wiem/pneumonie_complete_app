package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.ml.MlClient;
import com.example.pneumonia.ml.MlResult;
import com.example.pneumonia.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class AnalysisService {

    private final MlClient mlClient;
    private final AnalysisRepository repo;

    public AnalysisService(MlClient mlClient, AnalysisRepository repo) {
        this.mlClient = mlClient;
        this.repo = repo;
    }

    public Analysis analyze(MultipartFile file) throws IOException {
        MlResult r = mlClient.predict(file);
        Analysis a = new Analysis();
        a.setFilename(file.getOriginalFilename());
        a.setLabel(r.label());
        a.setProbabilityPneumonia(r.probabilityPneumonia());
        a.setHeatmapBase64(r.heatmapBase64());
        return repo.save(a);
    }

    public List<Analysis> history() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
}
