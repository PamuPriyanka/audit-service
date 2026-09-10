package com.observability.audit_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuditEventRequest {

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "actorId is required")
    private String actorId;

    @NotBlank(message = "resourceType is required")
    private String resourceType;

    @NotNull(message = "resourceId is required")
    private Long resourceId;

    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    @NotNull(message = "timestamp is required")
    private LocalDateTime timestamp;

}