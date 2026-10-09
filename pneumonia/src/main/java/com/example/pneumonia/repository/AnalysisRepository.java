package com.example.pneumonia.repository;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
     List<Analysis> findAllByOwnerOrderByCreatedAtDesc(AppUser owner);
    Optional<Analysis> findByIdAndOwner(Long id, AppUser owner);
    List<Analysis> findAllByOrderByCreatedAtDesc();   // servira pour l'admin
}
