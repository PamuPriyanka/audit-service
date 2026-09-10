package com.observability.audit_service.utility;

import com.observability.audit_service.dto.AuditEventRequest;
import com.observability.audit_service.entity.AuditEntity;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public final class HashUtil {

    private HashUtil() {
    }

    public static String createPipedTextOfAuditRequest(AuditEventRequest request){
        return String.join("|", request.getEventType(), request.getActorId(), request.getResourceType(),String.valueOf(request.getResourceId()), jsonToString(request.getPayload()), request.getTimestamp().toString());
     }

    public static String createPipedTextOfAuditEntity(AuditEntity auditEntity){
        return String.join("|", auditEntity.getEventType(), auditEntity.getActorId(), auditEntity.getResourceType(),String.valueOf(auditEntity.getResourceId()), auditEntity.getPayload(), auditEntity.getTimestamp().toString());
    }

    public static String jsonToString(Map<String, Object> payload) {
            return new ObjectMapper().writeValueAsString(payload);
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)  hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}