package com.example.pneumonia.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(nullable = false, length = 50)
    private String recordNumber;
    @Column(nullable = false, length = 100)
    private String lastName;
    @Column(nullable = false, length = 100)
    private String firstName;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(nullable = false, length = 1)
    private String sex;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }
    public String getRecordNumber() { return recordNumber; }
    public void setRecordNumber(String v) { this.recordNumber = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { this.birthDate = v; }
    public String getSex() { return sex; }
    public void setSex(String v) { this.sex = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public Instant getCreatedAt() { return createdAt; }
}