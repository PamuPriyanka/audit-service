package com.observability.audit_service.utility;

import com.observability.audit_service.dto.AuditEventRequest;
import com.observability.audit_service.entity.AuditArchiveEntity;
import com.observability.audit_service.entity.AuditEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Component
public class HashUtil {

    @Autowired
    public EncryptionUtil encryptiontUtil;

    public String createPipedTextOfAuditRequest(AuditEventRequest request) {
        return String.join("|", request.getEventType(), request.getActorId(), request.getResourceType(), String.valueOf(request.getResourceId()), mapToJsonToString(request.getPayload()), request.getTimestamp().toString());
    }

    public String mapToJsonToString(Map<String, Object> payload) {
        return new ObjectMapper().writeValueAsString(payload);
    }

    public AuditEntity shallowCopyToAuditEntity(AuditArchiveEntity archiveEntry) {
        AuditEntity auditEntity = new AuditEntity();
        auditEntity.setId(archiveEntry.getId());
        auditEntity.setEventType(archiveEntry.getEventType());
        auditEntity.setActorId(archiveEntry.getActorId());
        auditEntity.setResourceType(archiveEntry.getResourceType());
        auditEntity.setResourceId(archiveEntry.getResourceId());
        auditEntity.setPayload(archiveEntry.getPayload());
        auditEntity.setTimestamp(archiveEntry.getTimestamp());
        auditEntity.setCurrentHash(archiveEntry.getCurrentHash());
        auditEntity.setPreviousHash(archiveEntry.getPreviousHash());
        return auditEntity;
    }

    public String calculateHash(AuditEntity record) {
        String payloadForHashing;
        if (record.getEncryptedPayload() != null) {
            payloadForHashing = encryptiontUtil.decrypt(record.getEncryptedPayload());
        } else {
            payloadForHashing = record.getPayload();
        }
        String hashInput = String.join("|", record.getEventType(), record.getActorId(), record.getResourceType(), String.valueOf(record.getResourceId()), payloadForHashing, record.getTimestamp().toString());
        return sha256(hashInput);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}