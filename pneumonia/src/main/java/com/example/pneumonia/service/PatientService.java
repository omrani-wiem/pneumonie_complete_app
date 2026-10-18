package com.example.pneumonia.service;

import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.repository.AnalysisRepository;
import com.example.pneumonia.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patients;
    private final AnalysisRepository analyses;

    public PatientService(PatientRepository patients, AnalysisRepository analyses) {
        this.patients = patients;
        this.analyses = analyses;
    }

    /** Droit à l'effacement : supprime le patient et toutes ses analyses. Retourne le nombre d'analyses supprimées. */
    @Transactional
    public long erase(Patient p) {
        long n = analyses.deleteByPatient(p);
        patients.delete(p);
        return n;
    }
}