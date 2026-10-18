package com.example.pneumonia.repository;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findAllByOwnerOrderByCreatedAtDesc(AppUser owner);


    List<Analysis> findAllByOwnerAndPatientOrderByCreatedAtDesc(AppUser owner, com.example.pneumonia.entity.Patient patient);

    Optional<Analysis> findByIdAndOwner(Long id, AppUser owner);

    @Query("select a from Analysis a join fetch a.owner order by a.createdAt desc")
    List<Analysis> findAllWithOwner();

    long deleteByPatient(com.example.pneumonia.entity.Patient patient);
}