package com.example.pneumonia.controller;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Role;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.JwtService;
import com.example.pneumonia.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.pneumonia.service.AuditService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 72) String password) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record UserResponse(Long id, String email, Role role) {}

    public record TokenResponse(String token, String email, Role role) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService attempts;
    private final AuditService audit;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                          LoginAttemptService attempts, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
        this.audit = audit;
    }

    static String passwordError(String p) {
        if (p.length() < 10) return "Mot de passe : 10 caractères minimum.";
        if (!p.matches(".*\\p{L}.*")) return "Mot de passe : au moins une lettre.";
        if (!p.matches(".*\\d.*")) return "Mot de passe : au moins un chiffre.";
        if (!p.matches(".*[^\\p{L}\\d].*")) return "Mot de passe : au moins un caractère spécial.";
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String pwdError = passwordError(req.password());
        if (pwdError != null) return ResponseEntity.badRequest().body(pwdError);
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email déjà utilisé");
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(Role.USER);     // l'inscription publique crée toujours un simple utilisateur
        u.setEnabled(false);      // activation par un administrateur
        users.save(u);
        audit.record(email, "REGISTER", "user", u.getId(), true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(u.getId(), u.getEmail(), u.getRole()));
    }

        @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String email = req.email().trim().toLowerCase();
        String ip = http.getRemoteAddr();

        long wait = attempts.retryAfterSeconds(email, ip);
        if (wait > 0) {
            audit.record(email, "LOGIN_BLOCKED", null, null, false);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(wait))
                    .body("Trop de tentatives. Réessayez dans " + ((wait + 59) / 60) + " minute(s).");
        }

        AppUser u = users.findByEmail(email).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            attempts.recordFailure(email, ip);
            audit.record(email, "LOGIN_FAILURE", null, null, false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
        }
        if (!u.isEnabled()) {
            audit.record(email, "LOGIN_DISABLED", "user", u.getId(), false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Compte en attente de validation par un administrateur.");
        }
        attempts.recordSuccess(email);
        audit.record(email, "LOGIN_SUCCESS", "user", u.getId(), true);
        return ResponseEntity.ok(new TokenResponse(jwt.generate(u), u.getEmail(), u.getRole()));
    }
}