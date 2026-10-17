package com.example.pneumonia.repository;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findAllByOwnerOrderByCreatedAtDesc(AppUser owner);
    Optional<Patient> findByIdAndOwner(Long id, AppUser owner);
    boolean existsByOwnerAndRecordNumber(AppUser owner, String recordNumber);
}