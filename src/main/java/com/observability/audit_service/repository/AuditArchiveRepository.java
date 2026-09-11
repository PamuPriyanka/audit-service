package com.observability.audit_service.repository;

import com.observability.audit_service.entity.AuditArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditArchiveRepository extends JpaRepository<AuditArchiveEntity, Long> {
}