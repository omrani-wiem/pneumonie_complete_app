package com.example.pneumonia.controller;

import com.example.pneumonia.dto.AnalysisDto;
import com.example.pneumonia.dto.PatientDto;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.repository.AnalysisRepository;
import com.example.pneumonia.repository.PatientRepository;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AuditService;
import com.example.pneumonia.service.TreatmentRecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    public record PatientRequest(String recordNumber, String lastName, String firstName,
                                 LocalDate birthDate, String sex, String notes) {}

    private final PatientRepository patients;
    private final AnalysisRepository analyses;
    private final UserRepository users;
    private final TreatmentRecommendationService recos;
    private final AuditService audit;

       public PatientController(PatientRepository patients, AnalysisRepository analyses,
                             UserRepository users, TreatmentRecommendationService recos,
                             AuditService audit) {
        this.patients = patients;
        this.analyses = analyses;
        this.users = users;
        this.recos = recos;
        this.audit = audit;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PatientRequest r, @AuthenticationPrincipal Jwt jwt) {
        AppUser me = currentUser(jwt);
        String error = validate(r);
        if (error != null) return ResponseEntity.badRequest().body(error);
        String rn = r.recordNumber().trim();
        if (patients.existsByOwnerAndRecordNumber(me, rn))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ce numéro de dossier existe déjà.");
        Patient p = new Patient();
        p.setOwner(me);
        p.setRecordNumber(rn);
        p.setLastName(r.lastName().trim());
        p.setFirstName(r.firstName().trim());
        p.setBirthDate(r.birthDate());
        p.setSex(r.sex().trim().toUpperCase());
        p.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        Patient saved = patients.save(p);
        audit.record(me.getEmail(), "PATIENT_CREATE", "patient", saved.getId(), true);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientDto.of(saved));
    }

    @GetMapping
    public List<PatientDto> list(@AuthenticationPrincipal Jwt jwt) {
        return patients.findAllByOwnerOrderByCreatedAtDesc(currentUser(jwt)).stream()
                .map(PatientDto::of).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> one(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        AppUser me = currentUser(jwt);
        return patients.findByIdAndOwner(id, me)
                .map(p -> {
                    audit.record(me.getEmail(), "PATIENT_VIEW", "patient", p.getId(), true);
                    return ResponseEntity.ok(PatientDto.of(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/analyses")
    public ResponseEntity<List<AnalysisDto>> analysesOf(@PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        AppUser me = currentUser(jwt);
        return patients.findByIdAndOwner(id, me)
                .map(p -> {
                    audit.record(me.getEmail(), "PATIENT_ANALYSES_VIEW", "patient", p.getId(), true);
                    return ResponseEntity.ok(
                            analyses.findAllByOwnerAndPatientOrderByCreatedAtDesc(me, p).stream()
                                    .map(a -> AnalysisDto.of(a, false, recos)).toList());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private static String validate(PatientRequest r) {
        if (r == null) return "Requête vide.";
        if (blank(r.recordNumber()) || r.recordNumber().trim().length() > 50) return "Numéro de dossier requis (50 caractères max).";
        if (blank(r.lastName()) || r.lastName().trim().length() > 100) return "Nom requis (100 caractères max).";
        if (blank(r.firstName()) || r.firstName().trim().length() > 100) return "Prénom requis (100 caractères max).";
        if (r.birthDate() == null || r.birthDate().isAfter(LocalDate.now()) || r.birthDate().getYear() < 1900)
            return "Date de naissance invalide.";
        if (blank(r.sex()) || !(r.sex().trim().equalsIgnoreCase("F") || r.sex().trim().equalsIgnoreCase("M")))
            return "Sexe invalide (F ou M).";
        if (r.notes() != null && r.notes().length() > 4000) return "Notes trop longues (4000 caractères max).";
        return null;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private AppUser currentUser(Jwt jwt) {
        return users.findByEmail(jwt.getSubject())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}