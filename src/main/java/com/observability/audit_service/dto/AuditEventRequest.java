package com.observability.audit_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

@NotNull
public record AuditEventRequest(
    @NotBlank(message = "eventType is required") String eventType,
    @NotBlank(message = "actorId is required") String actorId,
    @NotBlank(message = "resourceType is required") String resourceType,
    @NotNull(message = "resourceId is required") Long resourceId,
    @NotNull(message = "payload is required") Map<String, Object> payload,
    @NotNull(message = "timestamp is required") LocalDateTime timestamp
) {}
