package com.observability.audit_service.utility;

import com.observability.audit_service.entity.AuditEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AuditSpecification {

    private AuditSpecification() {
    }

    public static Specification<AuditEntity> add(Specification<AuditEntity> existing, Specification<AuditEntity> next) {
        if (existing == null) {
            return next;
        }
        return existing.and(next);
    }

    public static Specification<AuditEntity> actorIdEquals(String actorId) {
        return (root, query, cb) -> cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditEntity> resourceTypeEquals(String resourceType) {
        return (root, query, cb) -> cb.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<AuditEntity> resourceIdEquals(Long resourceId) {
        return (root, query, cb) -> cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<AuditEntity> eventTypeEquals(String eventType) {
        return (root, query, cb) -> cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<AuditEntity> from(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    public static Specification<AuditEntity> to(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }

}
