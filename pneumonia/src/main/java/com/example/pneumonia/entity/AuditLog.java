package com.example.pneumonia.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "actor_email")
    private String actorEmail;
    @Column(nullable = false)
    private String action;
    @Column(name = "target_type")
    private String targetType;
    @Column(name = "target_id")
    private Long targetId;
    private String ip;
    @Column(nullable = false)
    private boolean success;

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String v) { this.actorEmail = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long v) { this.targetId = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }
}