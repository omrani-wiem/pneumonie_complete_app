package com.example.pneumonia.controller;

import com.example.pneumonia.entity.Role;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public AdminController(AnalysisService service, UserRepository users) {
        this.service = service;
        this.users = users;
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
    public ResponseEntity<?> setEnabled(@PathVariable Long id, @RequestBody EnabledRequest body) {
        return users.findById(id)
                .<ResponseEntity<?>>map(u -> {
                    if (u.getRole() == Role.ADMIN)
                        return ResponseEntity.badRequest().body("Un compte administrateur ne peut pas être modifié ici.");
                    u.setEnabled(body.enabled());
                    users.save(u);
                    return ResponseEntity.ok(new AdminUserDto(u.getId(), u.getEmail(), u.getRole(), u.isEnabled()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}