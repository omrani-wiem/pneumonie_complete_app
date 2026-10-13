package com.example.pneumonia.controller;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Role;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.JwtService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.pneumonia.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;

@RestController 
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password) {}

         public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record UserResponse(Long id, String email, Role role) {}

    public record TokenResponse(String token, String email, Role role) {}

    private final  UserRepository users; 
    private final PasswordEncoder encoder; 
        private final JwtService jwt;
    private final LoginAttemptService attempts;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                          LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email déjà utilisé");
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(Role.USER);   // l'inscription publique crée toujours un simple utilisateur
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(u.getId(), u.getEmail(), u.getRole()));
    }
   

         @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String email = req.email().trim().toLowerCase();
        String ip = http.getRemoteAddr();

        long wait = attempts.retryAfterSeconds(email, ip);
        if (wait > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(wait))
                    .body("Trop de tentatives. Réessayez dans " + ((wait + 59) / 60) + " minute(s).");
        }

        AppUser u = users.findByEmail(email).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            attempts.recordFailure(email, ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
        }
        attempts.recordSuccess(email);
        return ResponseEntity.ok(new TokenResponse(jwt.generate(u), u.getEmail(), u.getRole()));
    }

}

