package com.observability.audit_service.controller;

import com.observability.audit_service.dto.AuditExportBundle;
import com.observability.audit_service.service.AuditExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditZipController {

    private final AuditExportService auditExportService;

    public AuditZipController(AuditExportService auditExportService) {
        this.auditExportService = auditExportService;
    }

    @GetMapping("/export")
    public ResponseEntity<AuditExportBundle> export(@RequestParam(required = false) Long resourceId, @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(auditExportService.exportAuditRecords(resourceId, actorId));
    }

}

