package com.observability.audit_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.observability.audit_service.dto.AuditExportBundle;
import com.observability.audit_service.dto.AuditExportManifest;
import com.observability.audit_service.entity.AuditEntity;
import com.observability.audit_service.repository.AuditRepository;
import com.observability.audit_service.utility.HashUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuditExportService {

    private final AuditRepository auditRepository;

    private final HashUtil hashUtil;

    private final ObjectMapper objectMapper;

    public AuditExportService(AuditRepository auditRepository, HashUtil hashUtil, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.hashUtil = hashUtil;
        this.objectMapper = objectMapper;
    }

    public AuditExportBundle exportAuditRecords(Long resourceId, String actorId) {
        if ((resourceId == null && actorId == null) || (resourceId != null && actorId != null))
            throw new IllegalArgumentException("Provide exactly one of resourceId or actorId");
        List<AuditEntity> entities;
        String filterType;
        String filterValue;
        if (resourceId != null) {
            entities = auditRepository.findAllByResourceIdOrderByIdAsc(resourceId);
            filterType = "resourceId";
            filterValue = String.valueOf(resourceId);
        } else {
            entities = auditRepository.findAllByActorIdOrderByIdAsc(actorId);
            filterType = "actorId";
            filterValue = actorId;
        }
        String recordsJson = serializeRecords(entities);
        String recordsSha256 = hashUtil.sha256(recordsJson);
        AuditExportManifest manifest = createManifest(entities, filterType, filterValue, recordsSha256);
        return AuditExportBundle.builder().manifest(manifest).records(entities).build();
    }

    private AuditEntity toExportRecord(AuditEntity entity) {
        return AuditEntity.builder().id(entity.getId()).eventType(entity.getEventType()).actorId(entity.getActorId()).resourceType(entity.getResourceType()).resourceId(entity.getResourceId()).payload(entity.getPayload()).timestamp(entity.getTimestamp()).previousHash(entity.getPreviousHash()).currentHash(entity.getCurrentHash()).build();
    }

    private String serializeRecords(List<AuditEntity> records) {
        try {
            return objectMapper.writeValueAsString(records);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize audit export records", e);
        }
    }

    private AuditExportManifest createManifest(List<AuditEntity> records, String filterType, String filterValue, String recordsSha256) {
        if (records.isEmpty()) {
            return AuditExportManifest.builder().format("AUDIT_EXPORT_V1").hashAlgorithm("SHA-256").exportedAt(LocalDateTime.now()).filterType(filterType).filterValue(filterValue).recordCount(0).recordsSha256(recordsSha256).build();
        }
        AuditEntity first = records.get(0);
        AuditEntity last = records.get(records.size() - 1);
        return AuditExportManifest.builder().format("AUDIT_EXPORT_V1").hashAlgorithm("SHA-256").exportedAt(LocalDateTime.now()).filterType(filterType).filterValue(filterValue).recordCount(records.size()).firstRecordId(first.getId()).lastRecordId(last.getId()).firstPreviousHash(first.getPreviousHash()).lastCurrentHash(last.getCurrentHash()).recordsSha256(recordsSha256).build();
    }

}
