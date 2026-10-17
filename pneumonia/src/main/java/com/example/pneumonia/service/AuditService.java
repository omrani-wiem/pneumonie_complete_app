package com.example.pneumonia.service;

import com.example.pneumonia.entity.AuditLog;
import com.example.pneumonia.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void record(String actor, String action, String targetType, Long targetId, boolean success) {
        try {
            AuditLog e = new AuditLog();
            e.setActorEmail(trunc(actor, 255));
            e.setAction(action);
            e.setTargetType(targetType);
            e.setTargetId(targetId);
            e.setIp(currentIp());
            e.setSuccess(success);
            repo.save(e);
        } catch (RuntimeException ex) {
            log.warn("Entrée d'audit non enregistrée (action {})", action);
        }
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            return trunc(a.getRequest().getRemoteAddr(), 64);
        }
        return null;
    }

    private static String trunc(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}