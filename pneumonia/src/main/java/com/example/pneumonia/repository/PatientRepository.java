package com.example.pneumonia.repository;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndOwner(Long id, AppUser owner);

    boolean existsByOwnerAndRecordNumber(AppUser owner, String recordNumber);

    @Query("""
            select p from Patient p
            where p.owner = :owner and p.archived = :archived
              and (:q = '' or lower(p.lastName) like lower(concat('%', :q, '%'))
                           or lower(p.firstName) like lower(concat('%', :q, '%'))
                           or lower(p.recordNumber) like lower(concat('%', :q, '%')))
            """)
    Page<Patient> search(@Param("owner") AppUser owner, @Param("archived") boolean archived,
                         @Param("q") String q, Pageable pageable);
}