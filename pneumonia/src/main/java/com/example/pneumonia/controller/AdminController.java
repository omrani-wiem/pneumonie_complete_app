package com.example.pneumonia.controller;

import com.example.pneumonia.controller.AdminController.AuditDto;
import com.example.pneumonia.entity.Role;
import com.example.pneumonia.repository.AuditLogRepository;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AnalysisService;
import com.example.pneumonia.service.AuditService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;



import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record AdminAnalysisDto(Long id, String ownerEmail, String filename,
                                   String label, double probabilityPneumonia,
                                   Instant createdAt) {}

    public record AdminUserDto(Long id, String email, Role role, boolean enabled) {}

    public record EnabledRequest(boolean enabled) {}

    private final AnalysisService service;
    private final UserRepository users;
    private final AuditLogRepository auditRepo;
    private final AuditService audit;

    public AdminController(AnalysisService service, UserRepository users, AuditLogRepository auditRepo, AuditService audit ) {
        this.service = service;
        this.users = users;
        this.audit = audit;
        this.auditRepo = auditRepo;
    }
        public record AuditDto(Long id, Instant createdAt, String actorEmail, String action,
                           String targetType, Long targetId, String ip, boolean success) {}

    @GetMapping("/audit")
    public List<AuditDto> audit(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        int s = Math.min(Math.max(size, 1), 200);
        return auditRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), s)).stream()
                .map(e -> new AuditDto(e.getId(), e.getCreatedAt(), e.getActorEmail(), e.getAction(),
                        e.getTargetType(), e.getTargetId(), e.getIp(), e.isSuccess()))
                .toList();
    }

    @GetMapping("/analyses")
    public List<AdminAnalysisDto> all() {
        return service.allWithOwner().stream()
                .map(a -> new AdminAnalysisDto(a.getId(), a.getOwner().getEmail(),
                        a.getFilename(), a.getLabel(), a.getProbabilityPneumonia(),
                        a.getCreatedAt()))
                .toList();
    }

    @GetMapping("/users")
    public List<AdminUserDto> listUsers() {
        return users.findAllByOrderByIdDesc().stream()
                .map(u -> new AdminUserDto(u.getId(), u.getEmail(), u.getRole(), u.isEnabled()))
                .toList();
    }

        @PutMapping("/users/{id}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable Long id, @RequestBody EnabledRequest body,
                                        @AuthenticationPrincipal Jwt jwt) {
        return users.findById(id)
                .<ResponseEntity<?>>map(u -> {
                    if (u.getRole() == Role.ADMIN)
                        return ResponseEntity.badRequest().body("Un compte administrateur ne peut pas être modifié ici.");
                    u.setEnabled(body.enabled());
                    users.save(u);
                    audit.record(jwt.getSubject(), body.enabled() ? "USER_ENABLE" : "USER_DISABLE",
                            "user", u.getId(), true);
                    return ResponseEntity.ok(new AdminUserDto(u.getId(), u.getEmail(), u.getRole(), u.isEnabled()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}