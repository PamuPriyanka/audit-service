package com.observability.audit_service.controller;

import com.observability.audit_service.dto.AuditEventRequest;
import com.observability.audit_service.dto.ChainVerificationResult;
import com.observability.audit_service.entity.AuditEntity;
import com.observability.audit_service.service.AuditService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/postRecord")
    public ResponseEntity<AuditEntity> postRecord(@Valid @RequestBody AuditEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditService.createAuditEvent(request));
    }

    @GetMapping("/getRecords")
    public ResponseEntity<Page<AuditEntity>> search(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @PageableDefault(
                    size = 20,
                    sort = "id",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable) {

        return ResponseEntity.ok(
                auditService.searchAuditEvents(
                        actorId,
                        resourceType,
                        resourceId,
                        eventType,
                        from,
                        to,
                        pageable
                )
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<ChainVerificationResult> verify() {
        return ResponseEntity.ok(auditService.verifyChain()
        );
    }
}
