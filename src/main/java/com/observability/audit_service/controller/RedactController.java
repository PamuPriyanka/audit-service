package com.observability.audit_service.controller;

import com.observability.audit_service.service.RedactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class RedactController {

    @Autowired
    private RedactService redactService;

    @PutMapping("/redact")
    public ResponseEntity<String> search(@RequestParam String redactParam) {
        redactService.redactPayload(redactParam);
        return new ResponseEntity<>("Redaction completed successfully", HttpStatus.OK);
    }

}
