package com.example.pneumonia.controller;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Role;
import com.example.pneumonia.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;



@RestController 
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password) {}

    public record UserResponse(Long id, String email, Role role) {}

    private final  UserRepository users; 
    private final PasswordEncoder encoder; 

     public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
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
   
}
