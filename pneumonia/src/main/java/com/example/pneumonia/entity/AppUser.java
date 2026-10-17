package com.example.pneumonia.entity;
import jakarta.persistence.*;

@Entity 
@Table(name = "users")
public class AppUser {
        @Column(nullable = false)
    private boolean enabled = true;

    public boolean isEnabled() {
         return enabled;
         }
         
    public void setEnabled(boolean enabled) {
         this.enabled = enabled;
         }

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

     public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }



}
