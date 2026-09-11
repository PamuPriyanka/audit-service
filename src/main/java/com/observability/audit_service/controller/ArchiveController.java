package com.observability.audit_service.controller;

import com.observability.audit_service.service.ArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/audit")
public class ArchiveController {

    public final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping("/archive")
    public ResponseEntity<String> archiveRecords(@RequestParam(required = true) LocalDate archiveBefore) {
        int count = archiveService.archiveRecords(archiveBefore);
        return ResponseEntity.ok(count + " Audit record(s) archived successfully.");
    }

}
