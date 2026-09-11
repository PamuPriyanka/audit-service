package com.observability.audit_service.service;

import com.observability.audit_service.dto.AuditEventRequest;
import com.observability.audit_service.dto.ChainVerificationResult;
import com.observability.audit_service.entity.AuditArchiveEntity;
import com.observability.audit_service.entity.AuditEntity;
import com.observability.audit_service.repository.AuditArchiveRepository;
import com.observability.audit_service.repository.AuditRepository;
import com.observability.audit_service.utility.AuditSpecification;
import com.observability.audit_service.utility.EncryptionUtil;
import com.observability.audit_service.utility.HashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuditService {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditRepository auditRepository;
    private final AuditArchiveRepository auditArchiveRepository;
    private final EncryptionUtil encryptionUtil;
    private final HashUtil hashUtil;

    public AuditService(AuditRepository auditRepository, AuditArchiveRepository auditArchiveRepository, EncryptionUtil encryptionUtil, HashUtil hashUtil) {
        this.auditRepository = auditRepository;
        this.auditArchiveRepository = auditArchiveRepository;
        this.encryptionUtil = encryptionUtil;
        this.hashUtil = hashUtil;
    }

    public AuditEntity createAuditEvent(AuditEventRequest request) {
        Optional<AuditEntity> lastRecord = auditRepository.findTopByOrderByIdDesc();
        String previousHash = lastRecord.map(AuditEntity::getCurrentHash).orElse(GENESIS_HASH);
        String currentHash = hashUtil.sha256(hashUtil.createPipedTextOfAuditRequest(request));
        AuditEntity auditEntity = new AuditEntity(null, request.getEventType(), request.getActorId(), request.getResourceType(), request.getResourceId(), hashUtil.mapToJsonToString(request.getPayload()), request.getTimestamp(), currentHash, previousHash, null);
        return auditRepository.save(auditEntity);
    }

    public Page<AuditEntity> searchAuditEvents(String actorId, String resourceType, Long resourceId, String eventType, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Specification<AuditEntity> specification = null;

        if (actorId != null && !actorId.isBlank()) {
            specification = AuditSpecification.add(specification, AuditSpecification.actorIdEquals(actorId));
        }
        if (resourceType != null && !resourceType.isBlank()) {
            specification = AuditSpecification.add(specification, AuditSpecification.resourceTypeEquals(resourceType));
        }
        if (resourceId != null) {
            specification = AuditSpecification.add(specification, AuditSpecification.resourceIdEquals(resourceId));
        }
        if (eventType != null && !eventType.isBlank()) {
            specification = AuditSpecification.add(specification, AuditSpecification.eventTypeEquals(eventType));
        }
        if (from != null) {
            specification = AuditSpecification.add(specification, AuditSpecification.from(from));
        }
        if (to != null) {
            specification = AuditSpecification.add(specification, AuditSpecification.to(to));
        }
        return auditRepository.findAll(specification, pageable);
    }

    public ChainVerificationResult verifyChain() {
        List<AuditEntity> records = auditRepository.findAllByOrderByIdAsc();
        if (records.isEmpty()) {
            return result(true, 0, null, "NO_RECORDS", "No records found to verify");
        }
        for (int i = 1; i < records.size(); i++) {
            AuditEntity firstRecord = records.get(0);
            if (firstRecord.getPreviousHash().equalsIgnoreCase(GENESIS_HASH)) {
                String calculatedFirstHash = hashUtil.calculateHash(firstRecord);
                if (!calculatedFirstHash.equals(firstRecord.getCurrentHash())) {
                    return result(false, 1, firstRecord.getId(), "INITIAL_CONTENT_HASH_MISMATCH", "Chain verification failed in the beginning");
                }
            }
            AuditEntity previousRecord = records.get(i - 1);
            AuditEntity currentRecord = records.get(i);
            String calculatedPreviousHash = hashUtil.calculateHash(previousRecord);
            if (!calculatedPreviousHash.equals(currentRecord.getPreviousHash())) {
                Optional<AuditArchiveEntity> auditEntity = auditArchiveRepository.findById(currentRecord.getId() - 1);
                if (auditEntity.isPresent()) {
                    AuditArchiveEntity archivedEntity = auditEntity.get();
                    String calculatedArchivedHash = hashUtil.calculateHash(hashUtil.shallowCopyToAuditEntity(archivedEntity));
                    if (!calculatedArchivedHash.equals(currentRecord.getPreviousHash())) {
                        return result(false, i, archivedEntity.getId(), "ARCHIVED_HASH_MISMATCH", "Chain verification failed");
                    }
                } else {
                    return result(false, i, previousRecord.getId(), "HASH_MISMATCH", "Chain verification failed");
                }
            }
        }
        //Verifying changes in last record
        AuditEntity lastRecord = records.get(records.size() - 1);
        String calculatedCurrentHash = hashUtil.calculateHash(lastRecord);
        if (!calculatedCurrentHash.equals(lastRecord.getCurrentHash())) {
            return result(false, records.size() - 1, lastRecord.getId(), "LAST_CONTENT_HASH_MISMATCH", "Chain verification failed in the end");
        }
        return result(true, records.size(), null, "CHAIN_INTACT", "Chain intact with no mismatches");
    }

    private ChainVerificationResult result(boolean valid, int recordNumber, Long recordId, String code, String message) {
        return new ChainVerificationResult(valid, recordNumber, recordId, code, message);
    }


}




