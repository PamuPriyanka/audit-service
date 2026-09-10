package com.observability.audit_service.service;

import com.observability.audit_service.dto.AuditEventRequest;
import com.observability.audit_service.dto.ChainVerificationResult;
import com.observability.audit_service.entity.AuditEntity;
import com.observability.audit_service.repository.AuditRepository;
import com.observability.audit_service.utility.AuditSpecification;
import com.observability.audit_service.utility.HashUtil;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuditService {

    private static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public AuditEntity createAuditEvent(AuditEventRequest request) {
        Optional<AuditEntity> lastRecord = auditRepository.findTopByOrderByIdDesc();
        String previousHash = lastRecord.map(AuditEntity::getCurrentHash).orElse(GENESIS_HASH);
        String currentHash = HashUtil.sha256(HashUtil.createPipedTextOfAuditRequest(request));
        AuditEntity auditEntity = new AuditEntity(
                null,
                request.getEventType(),
                request.getActorId(),
                request.getResourceType(),
                request.getResourceId(),
                HashUtil.jsonToString(request.getPayload()),
                request.getTimestamp(),
                currentHash,
                previousHash
        );
        return auditRepository.save(auditEntity);
    }

    public Page<AuditEntity> searchAuditEvents(
            String actorId,
            String resourceType,
            Long resourceId,
            String eventType,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<AuditEntity> specification = null;

        if (actorId != null && !actorId.isBlank()) {
            specification = AuditSpecification.actorIdEquals(actorId);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            specification = add(specification, AuditSpecification.resourceTypeEquals(resourceType));
        }
        if (resourceId != null) {
            specification = add(specification, AuditSpecification.resourceIdEquals(resourceId));
        }
        if (eventType != null && !eventType.isBlank()) {
            specification = add(specification, AuditSpecification.eventTypeEquals(eventType));
        }
        if (from != null) {
            specification = add(specification, AuditSpecification.from(from));
        }
        if (to != null) {
            specification = add(specification, AuditSpecification.to(to));
        }
        return auditRepository.findAll(specification, pageable
        );
    }

    private Specification<AuditEntity> add(Specification<AuditEntity> existing, Specification<AuditEntity> next) {
        if (existing == null) {
            return next;
        }
        return existing.and(next);
    }


     public ChainVerificationResult verifyChain() {

        List<AuditEntity> records =  auditRepository.findAllByOrderByIdAsc();

        if (records.isEmpty()) {
            return new ChainVerificationResult(
                    true,
                    0,
                    null,
                    "NO_RECORDS",
                    "No records found to verify"
            );
        }

        if (records.size() == 1) {

            AuditEntity currentRecord = records.get(0);

            String calculatedHash = HashUtil.sha256( HashUtil.createPipedTextOfAuditEntity(currentRecord));

            if (!calculatedHash.equals(currentRecord.getCurrentHash())) {
                return new ChainVerificationResult( false,1,  currentRecord.getId(),"CONTENT_HASH_MISMATCH","The first record content does not match its stored currentHash."
                );
            }
            return new ChainVerificationResult(true, 1, currentRecord.getId(),"NO_MISMATCH","The audit chain contains one valid record."
            );
        }

        for (int i = 1; i < records.size(); i++) {

            AuditEntity previousRecord = records.get(i - 1);
            AuditEntity currentRecord = records.get(i);

            String calculatedPreviousHash = HashUtil.sha256( HashUtil.createPipedTextOfAuditEntity(previousRecord)  );

            if (!calculatedPreviousHash.equals(currentRecord.getPreviousHash())) {
                return new ChainVerificationResult(false,i + 1, previousRecord.getId(),"PREVIOUS_HASH_MISMATCH","The previous record content does not match "   + "the previousHash stored in the current record."
                );
            }
        }


        AuditEntity lastRecord = records.get(records.size() - 1);

        String calculatedLastHash =   HashUtil.sha256(HashUtil.createPipedTextOfAuditEntity(lastRecord) );

        if (!calculatedLastHash.equals(lastRecord.getCurrentHash())) {
            return new ChainVerificationResult( false, records.size(),lastRecord.getId(),"CONTENT_HASH_MISMATCH","The last record content does not match its stored currentHash.");
        }
        return new ChainVerificationResult(true, records.size(), lastRecord.getId(),  "NO_MISMATCH", "All records in the chain are valid."
        );
    }


}




