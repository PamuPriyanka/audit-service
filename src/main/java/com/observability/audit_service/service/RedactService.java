package com.observability.audit_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.observability.audit_service.entity.AuditEntity;
import com.observability.audit_service.repository.AuditRepository;
import com.observability.audit_service.utility.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RedactService {


    private final AuditRepository auditRepository;

    private final EncryptionUtil encryptionUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedactService(AuditRepository auditRepository, EncryptionUtil encryptionUtil) {
        this.auditRepository = auditRepository;
        this.encryptionUtil = encryptionUtil;
    }

    public void redactPayload(String redactParam) {
        List<AuditEntity> records = auditRepository.findAll();
        try {
            for (AuditEntity record : records) {
                Map<String, Object> payloadMap = objectMapper.readValue(record.getPayload(), new TypeReference<Map<String, Object>>() {
                });
                if (!payloadMap.containsKey(redactParam)) {
                    continue;
                }
                String encryptedOriginalPayload = encryptionUtil.encrypt(record.getPayload());
                record.setEncryptedPayload(encryptedOriginalPayload);
                payloadMap.put(redactParam, "[REDACTED]");
                String redactedPayload = objectMapper.writeValueAsString(payloadMap);
                record.setPayload(redactedPayload);
                auditRepository.save(record);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Unable to process audit payload", e
            );
        }
    }
}
