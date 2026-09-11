package com.observability.audit_service.repository;

import com.observability.audit_service.entity.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    Optional<AuditEntity> findTopByOrderByIdDesc();

    Page<AuditEntity> findAll(Specification<AuditEntity> specification, Pageable pageable);

    List<AuditEntity> findAllByOrderByIdAsc();

    List<AuditEntity> findAllByResourceIdOrderByIdAsc(Long resourceId);

    List<AuditEntity> findAllByActorIdOrderByIdAsc(String actorId);

    List<AuditEntity> findByTimestampBefore(LocalDateTime archiveBefore);
}