package com.observability.audit_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String actorId;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private Long resourceId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 64)
    private String currentHash;

    @Column(nullable = false, length = 64)
    private String previousHash;

}