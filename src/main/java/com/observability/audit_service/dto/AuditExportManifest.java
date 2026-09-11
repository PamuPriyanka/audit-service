package com.observability.audit_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditExportManifest {

    private String format;

    private String hashAlgorithm;

    private LocalDateTime exportedAt;

    private String filterType;

    private String filterValue;

    private int recordCount;

    private Long firstRecordId;

    private Long lastRecordId;

    private String firstPreviousHash;

    private String lastCurrentHash;

    private String recordsSha256;

}
