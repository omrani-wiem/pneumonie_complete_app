package com.example.pneumonia.controller;

import com.example.pneumonia.dto.AnalysisDto;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.repository.PatientRepository;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AnalysisService;
import com.example.pneumonia.service.PdfReportService;
import com.example.pneumonia.service.TreatmentRecommendationService;
import com.example.pneumonia.service.UploadValidator;
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

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    public record OpinionRequest(String opinion, String comment) {}

    private final PdfReportService pdf;
    private final UploadValidator validator;
    private final TreatmentRecommendationService recos;
    private final AnalysisService service;
    private final UserRepository users;
    private final PatientRepository patients;

    public AnalysisController(AnalysisService service, UserRepository users, PdfReportService pdf,
                              UploadValidator validator, TreatmentRecommendationService recos,
                              PatientRepository patients) {
        this.service = service;
        this.users = users;
        this.pdf = pdf;
        this.validator = validator;
        this.recos = recos;
        this.patients = patients;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestParam("file") MultipartFile file,
                                    @RequestParam("patientId") Long patientId,
                                    @RequestParam(value = "temperatureC", required = false) Double temperatureC,
                                    @RequestParam(value = "spo2Percent", required = false) Integer spo2Percent,
                                    @RequestParam(value = "symptomDays", required = false) Integer symptomDays,
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
        Patient patient = patients.findByIdAndOwner(patientId, me).orElse(null);
        if (patient == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient introuvable.");
        if (temperatureC != null && (temperatureC < 30 || temperatureC > 45))
            return ResponseEntity.badRequest().body("Température invalide (30 à 45 °C).");
        if (spo2Percent != null && (spo2Percent < 50 || spo2Percent > 100))
            return ResponseEntity.badRequest().body("Saturation invalide (50 à 100 %).");
        if (symptomDays != null && (symptomDays < 0 || symptomDays > 60))
            return ResponseEntity.badRequest().body("Durée des symptômes invalide (0 à 60 jours).");
        try {
            return ResponseEntity.ok(AnalysisDto.of(
                    service.analyze(file, safeName, me, patient, temperatureC, spo2Percent, symptomDays),
                    true, recos));
        } catch (RestClientException e) {
            // Ne pas renvoyer e.getMessage() : il peut contenir l'URL interne du service ML.
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Service de prédiction indisponible. Réessayez plus tard.");
        }
    }

    @GetMapping
    public List<AnalysisDto> history(@AuthenticationPrincipal Jwt jwt) {
        return service.historyOf(currentUser(jwt)).stream()
                .map(a -> AnalysisDto.of(a, false, recos))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisDto> one(@PathVariable Long id,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.findOf(id, currentUser(jwt))
                .map(a -> ResponseEntity.ok(AnalysisDto.of(a, true, recos)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/opinion")
    public ResponseEntity<?> opinion(@PathVariable Long id, @RequestBody OpinionRequest body,
                                     @AuthenticationPrincipal Jwt jwt) {
        if (body == null || body.opinion() == null
                || !(body.opinion().equals("AGREE") || body.opinion().equals("DISAGREE")))
            return ResponseEntity.badRequest().body("Avis invalide (AGREE ou DISAGREE).");
        if (body.comment() != null && body.comment().length() > 2000)
            return ResponseEntity.badRequest().body("Commentaire trop long (2000 caractères max).");
        return service.findOf(id, currentUser(jwt))
                .map(a -> {
                    a.setDoctorOpinion(body.opinion());
                    a.setDoctorComment(body.comment() == null || body.comment().isBlank() ? null : body.comment().trim());
                    a.setOpinionAt(Instant.now());
                    return ResponseEntity.ok(AnalysisDto.of(service.save(a), true, recos));
                })
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
                        .body(pdf.generate(a, recos.recommend(a.getPneumoniaType()))))
                .orElse(ResponseEntity.notFound().build());
    }

       private AppUser currentUser(Jwt jwt) {
        return users.findByEmail(jwt.getSubject())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}