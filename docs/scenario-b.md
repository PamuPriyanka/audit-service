# Scenario B — Technical Decisions, Trade-offs and Scope

## 1. Retention / Archiving

### Technical Design Decision

The retention implementation uses a separate archive table rather than permanently deleting audit information. `ArchiveService` identifies records older than the supplied cutoff using `findByTimestampBefore(...)`, copies the complete audit record including `eventType`, `actorId`, `resourceId`, `payload`, `timestamp`, `currentHash`, and `previousHash` into `AuditArchiveEntity`, records the `archivedAt` timestamp, and then deletes the original record from the active audit table. The operation is wrapped in `@Transactional`, so the archive insert and deletion are treated as one database transaction. The API accepts a `LocalDate` and converts it to `LocalDateTime` internally using `atStartOfDay()`. This keeps the API date-oriented while matching the `LocalDateTime` type used by the database entity.

### Trade-offs

The main advantage of the separate archive-table approach is that archived records are physically separated from active records while their original hash-chain metadata is preserved. This keeps normal audit queries smaller and makes the archive state explicit. The trade-off is additional database storage and additional verification logic because the chain can now span the active and archive tables. Another limitation is that the current cutoff conversion uses `atStartOfDay()`, so a cutoff such as `2026-09-12` means records before `2026-09-12T00:00:00`; records occurring later on September 12 are not archived.

### Chain Verification Decision

`AuditService.verifyChain()` attempts to account for archived records when a hash relationship appears broken. If the calculated hash of an active record does not match the next record's `previousHash`, the implementation checks `AuditArchiveRepository` using `currentRecord.getId() - 1` and, when present, recalculates the archived record's hash before deciding whether the chain is broken.

### Limitation

The current verification implementation assumes that an archived predecessor can be located using `currentRecord.id - 1`. This works only when IDs are sequential and the immediately previous record is the archived record. It does not robustly handle multiple archived records, non-sequential IDs, or a chain beginning with an active record whose predecessor exists in the archive. Therefore, this is a partial implementation of the requirement and would need stronger predecessor lookup logic to guarantee correct verification in all archival scenarios.

### Out of Scope

Physical deletion from the archive, external archival storage such as S3 or Glacier, scheduled/background retention jobs, configurable regulatory retention policies, and automated restoration of archived records are outside the current implementation.

```

## 2. Structured Redaction

### Technical Design Decision

The redaction implementation exposes a `PUT /audit/redact` endpoint that accepts the name of a payload field to redact. `RedactService` loads the audit records, deserializes each JSON payload into a `Map<String, Object>`, checks whether the requested field exists, encrypts and stores the original payload in `encryptedPayload`, replaces the selected field with `[REDACTED]`, serializes the modified payload back to JSON, and saves the record. This provides field-level redaction while retaining an encrypted copy of the original payload for controlled recovery or audit purposes.

### Trade-offs

The advantage of this approach is simplicity. It allows individual JSON fields to be hidden without changing the rest of the payload and preserves the original payload in encrypted form. It also avoids changing the structure of the existing audit entity significantly. The trade-off is that the encrypted original payload must be protected through secure encryption-key management; otherwise, storing the encrypted value does not provide meaningful privacy protection. The implementation also scans all audit records and therefore may become expensive as the audit table grows.

### Important Hash-Chain Limitation

The current implementation does **not recalculate or preserve a redaction-aware cryptographic commitment** after changing the payload. The original `currentHash` remains unchanged while `payload` is modified to contain `[REDACTED]`.

As a result, `verifyChain()` recalculates the hash using the redacted payload and compares it with the original `currentHash`. These values will normally differ. Therefore, the current implementation can cause a legitimately redacted record to be reported as a hash mismatch.

For the stated requirement, the implementation should be extended with an explicit redaction mechanism, such as preserving the original hash as the integrity commitment and adding redaction metadata that verification understands, or using a cryptographic commitment/hash strategy for sensitive fields. The current implementation demonstrates the redaction workflow but does not completely solve the hash-chain verification requirement.

### Out of Scope

Automatic discovery of sensitive fields, advanced nested JSON-path redaction, irreversible cryptographic erasure, external key-management systems, role-based redaction authorization, and regulatory-specific privacy workflows are outside the current scope.
```

## 3. Bulk Export

### Technical Design Decision

`AuditExportService` supports exporting audit records using exactly one of two filters: `resourceId` or `actorId`. The service validates that exactly one filter is supplied, retrieves the matching records in ascending ID order, serializes the records to JSON, calculates a SHA-256 hash of the complete serialized record collection, and creates an `AuditExportManifest`. The manifest contains the export format, hash algorithm, export timestamp, filter type and value, record count, first and last record IDs, the first record's `previousHash`, the last record's `currentHash`, and the SHA-256 hash of the exported records. This design gives the recipient metadata that can be used to identify the exported dataset and detect changes to the exported JSON.

### Trade-offs

The advantage of hashing the serialized export is that any modification to the exported record collection changes `recordsSha256`, allowing the recipient to detect alteration of the exported file contents. Including the first and last record identifiers and chain hashes also gives useful boundary information for validating the selected portion of the audit chain. The trade-off is that the export currently operates against `AuditRepository`, so archived records are not included. In addition, the current service returns an `AuditExportBundle` object rather than creating an actual ZIP/file bundle, so the "self-contained bundle" requirement is only partially implemented at the service level.

Another limitation is that the export is a filtered subset of the full chain. The manifest provides the first record's `previousHash`, but independent verification of the complete selected chain still requires a verifier that recalculates each record's hash and checks the `previousHash` relationships between the exported records. The current code creates the required metadata but does not itself implement the external verification process.

### Out of Scope

Digital signatures, certificate-based authenticity, encrypted export files, cloud storage, external regulatory submission, inclusion of archived records in the export, and a standalone verification utility/application are outside the current implementation.

```

## Overall Scope Boundary

The implementation covers the core mechanics of the three requested areas: archiving audit records into a separate archive store, field-level payload redaction with encrypted preservation of the original payload, and filtered audit export with SHA-256 manifest metadata. The main remaining engineering gaps are redaction-aware hash verification, more robust chain traversal across multiple archived records, inclusion of archived records in export where required, and generation of a truly self-contained file bundle that an external recipient can independently verify without the application.

The **redaction section is the biggest issue in your current code**: changing `payload` without changing the hash means your existing `verifyChain()` can fail after redaction. That is worth fixing before you present Scenario B as fully implemented.
```
