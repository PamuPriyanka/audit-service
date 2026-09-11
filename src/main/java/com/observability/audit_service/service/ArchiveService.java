package com.observability.audit_service.service;

import com.observability.audit_service.entity.AuditArchiveEntity;
import com.observability.audit_service.entity.AuditEntity;
import com.observability.audit_service.repository.AuditArchiveRepository;
import com.observability.audit_service.repository.AuditRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArchiveService {

    private final AuditRepository auditRepository;
    private final AuditArchiveRepository auditArchiveRepository;

    public ArchiveService(AuditRepository auditRepository, AuditArchiveRepository auditArchiveRepository) {
        this.auditRepository = auditRepository;
        this.auditArchiveRepository = auditArchiveRepository;
    }

    @Transactional
    public int archiveRecords(LocalDate archiveBefore) {
        List<AuditEntity> records = auditRepository.findByTimestampBefore(archiveBefore.atStartOfDay());
        if (records.isEmpty()) {
            return 0;
        }
        List<AuditArchiveEntity> archiveRecords = records.stream().map(record -> {
            AuditArchiveEntity archive = new AuditArchiveEntity();
            archive.setId(record.getId());
            archive.setEventType(record.getEventType());
            archive.setActorId(record.getActorId());
            archive.setResourceType(record.getResourceType());
            archive.setResourceId(record.getResourceId());
            archive.setPayload(record.getPayload());
            archive.setTimestamp(record.getTimestamp());
            archive.setCurrentHash(record.getCurrentHash());
            archive.setPreviousHash(record.getPreviousHash());
            archive.setArchivedAt(LocalDateTime.now());
            return archive;
        }).toList();
        auditArchiveRepository.saveAll(archiveRecords);
        auditRepository.deleteAllById(records.stream().map(AuditEntity::getId).toList());
        return records.size();
    }

}




