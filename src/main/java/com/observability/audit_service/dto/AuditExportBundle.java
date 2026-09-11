package com.observability.audit_service.dto;

import com.observability.audit_service.entity.AuditEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditExportBundle {

    private AuditExportManifest manifest;

    private List<AuditEntity> records;

}
