package com.example.pneumonia.controller;

import com.example.pneumonia.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")

public class AdminController {

    public record AdminAnalysisDto(Long id, String ownerEmail, String filename,
                                   String label, double probabilityPneumonia,
                                   Instant createdAt) {}

    private final AnalysisService service;

    public AdminController(AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/analyses")
    public List<AdminAnalysisDto> all() {
        return service.allWithOwner().stream()
                .map(a -> new AdminAnalysisDto(a.getId(), a.getOwner().getEmail(),
                        a.getFilename(), a.getLabel(), a.getProbabilityPneumonia(),
                        a.getCreatedAt()))
                .toList();
    }
}