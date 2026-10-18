package com.example.pneumonia.controller;

import com.example.pneumonia.dto.AnalysisDto;
import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.repository.AnalysisRepository;
import com.example.pneumonia.repository.PatientRepository;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AuditService;
import com.example.pneumonia.service.PatientRecordPdfService;
import com.example.pneumonia.service.TreatmentRecommendationService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class InsightsController {

    public record Group(long agree, long disagree) {}
    public record Stats(long total, long withOpinion, long agree, long disagree,
                        Group pneumonia, Group normal) {}
    public record CompareDto(AnalysisDto first, AnalysisDto second, double probabilityDelta) {}

    private final AnalysisRepository analyses;
    private final PatientRepository patients;
    private final UserRepository users;
    private final AuditService audit;
    private final TreatmentRecommendationService recos;
    private final PatientRecordPdfService recordPdf;

    public InsightsController(AnalysisRepository analyses, PatientRepository patients, UserRepository users,
                              AuditService audit, TreatmentRecommendationService recos,
                              PatientRecordPdfService recordPdf) {
        this.analyses = analyses;
        this.patients = patients;
        this.users = users;
        this.audit = audit;
        this.recos = recos;
        this.recordPdf = recordPdf;
    }

    @GetMapping("/stats")
    public Stats stats(@AuthenticationPrincipal Jwt jwt) {
        AppUser me = currentUser(jwt);
        long agree = analyses.countByOwnerAndDoctorOpinion(me, "AGREE");
        long disagree = analyses.countByOwnerAndDoctorOpinion(me, "DISAGREE");
        return new Stats(analyses.countByOwner(me), agree + disagree, agree, disagree,
                new Group(analyses.countByOwnerAndLabelAndDoctorOpinion(me, "PNEUMONIA", "AGREE"),
                          analyses.countByOwnerAndLabelAndDoctorOpinion(me, "PNEUMONIA", "DISAGREE")),
                new Group(analyses.countByOwnerAndLabelAndDoctorOpinion(me, "NORMAL", "AGREE"),
                          analyses.countByOwnerAndLabelAndDoctorOpinion(me, "NORMAL", "DISAGREE")));
    }

    @GetMapping("/analyses/compare")
    public ResponseEntity<?> compare(@RequestParam Long a, @RequestParam Long b,
                                     @AuthenticationPrincipal Jwt jwt) {
        AppUser me = currentUser(jwt);
        if (a.equals(b)) return ResponseEntity.badRequest().body("Choisissez deux analyses différentes.");
        Analysis x = analyses.findByIdAndOwner(a, me).orElse(null);
        Analysis y = analyses.findByIdAndOwner(b, me).orElse(null);
        if (x == null || y == null) return ResponseEntity.notFound().build();
        if (x.getPatient() == null || y.getPatient() == null
                || !x.getPatient().getId().equals(y.getPatient().getId()))
            return ResponseEntity.badRequest().body("Les deux analyses doivent concerner le même patient.");
        if (x.getCreatedAt().isAfter(y.getCreatedAt())) { Analysis t = x; x = y; y = t; }
        audit.record(me.getEmail(), "ANALYSIS_COMPARE", "patient", x.getPatient().getId(), true);
        return ResponseEntity.ok(new CompareDto(AnalysisDto.of(x, false, recos), AnalysisDto.of(y, false, recos),
                y.getProbabilityPneumonia() - x.getProbabilityPneumonia()));
    }

    @GetMapping("/patients/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        AppUser me = currentUser(jwt);
        return patients.findByIdAndOwner(id, me)
                .map(p -> {
                    byte[] pdf = recordPdf.generate(p,
                            analyses.findAllByOwnerAndPatientOrderByCreatedAtDesc(me, p));
                    audit.record(me.getEmail(), "PATIENT_EXPORT", "patient", p.getId(), true);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment().filename("dossier-" + p.getId() + ".pdf").build().toString())
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private AppUser currentUser(Jwt jwt) {
        return users.findByEmail(jwt.getSubject())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}