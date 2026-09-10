package com.observability.audit_service.dto;

public record ChainVerificationResult(
        boolean intact,
        int recordsChecked,
        Long firstInvalidRecordId,
        String violationType,
        String message
) {
}