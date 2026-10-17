package com.example.pneumonia.dto;

import com.example.pneumonia.entity.Patient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

public record PatientDto(Long id, String recordNumber, String lastName, String firstName,
                         LocalDate birthDate, String sex, String notes, Instant createdAt,
                         int ageMonths, boolean ageInModelRange) {

    public static PatientDto of(Patient p) {
        Period per = Period.between(p.getBirthDate(), LocalDate.now());
        int months = per.getYears() * 12 + per.getMonths();
        return new PatientDto(p.getId(), p.getRecordNumber(), p.getLastName(), p.getFirstName(),
                p.getBirthDate(), p.getSex(), p.getNotes(), p.getCreatedAt(),
                months, months >= 12 && months < 72);
    }
}