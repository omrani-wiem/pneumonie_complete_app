package com.example.pneumonia.repository;

import com.example.pneumonia.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findAllByOrderByCreatedAtDesc();
}
