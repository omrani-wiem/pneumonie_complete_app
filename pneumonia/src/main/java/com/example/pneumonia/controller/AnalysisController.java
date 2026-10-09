package com.example.pneumonia.controller;

import com.example.pneumonia.dto.AnalysisDto;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/analyses")
@CrossOrigin(origins = "*")   // à restreindre plus tard
public class AnalysisController {

    private final AnalysisService service;
    private final UserRepository users;

    public AnalysisController(AnalysisService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichier vide");
        }
        AppUser me = currentUser(jwt);
        try {
            return ResponseEntity.ok(AnalysisDto.of(service.analyze(file, me), true));
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Service de prédiction indisponible : " + e.getMessage());
        }
    }

    @GetMapping
    public List<AnalysisDto> history(@AuthenticationPrincipal Jwt jwt) {
        return service.historyOf(currentUser(jwt)).stream()
                .map(a -> AnalysisDto.of(a, false))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisDto> one(@PathVariable Long id,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.findOf(id, currentUser(jwt))
                .map(a -> ResponseEntity.ok(AnalysisDto.of(a, true)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AppUser currentUser(Jwt jwt) {
        return users.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}