package com.example.pneumonia.controller;

import com.example.pneumonia.dto.AnalysisDto;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AnalysisService;
import com.example.pneumonia.service.PdfReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.pneumonia.service.UploadValidator;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService service;
    private final UserRepository users;
        private final PdfReportService pdf;
    private final UploadValidator validator;

    public AnalysisController(AnalysisService service, UserRepository users,
                              PdfReportService pdf, UploadValidator validator) {
        this.service = service;
        this.users = users;
        this.pdf = pdf;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal Jwt jwt) throws IOException {
                String safeName;
        try {
            safeName = validator.validate(file);
        } catch (UploadValidator.InvalidUploadException e) {
            return ResponseEntity.status(e.isUnsupportedType()
                            ? HttpStatus.UNSUPPORTED_MEDIA_TYPE : HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
        AppUser me = currentUser(jwt);
        try {
            return ResponseEntity.ok(AnalysisDto.of(service.analyze(file, safeName, me), true));
        } catch (RestClientException e) {
            // Ne pas renvoyer e.getMessage() : il peut contenir l'URL interne du service ML.
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Service de prédiction indisponible. Réessayez plus tard.");
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

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> report(@PathVariable Long id,
                                         @AuthenticationPrincipal Jwt jwt) {
        return service.findOf(id, currentUser(jwt))
                .map(a -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename("rapport-analyse-" + a.getId() + ".pdf").build().toString())
                        .body(pdf.generate(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AppUser currentUser(Jwt jwt) {
        return users.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}