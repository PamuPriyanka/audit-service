# Scenario A — Greenfield: Core Audit Log Service

## Overview

In this scenario, we will build a new Audit Log Service that provides REST APIs to:

1. Create and persist audit records.
2. Query audit records using optional filters and pagination.
3. Verify the integrity of the audit hash chain.

The audit records will be persisted in an H2 database and mapped to the `audit_events` table.

---

## 1. POST API — Create Audit Record

POST /audit/postRecord
Content-Type: application/json

The API accepts the following parameters:

* `eventType` — what happened, for example `USER_LOGIN`, `RECORD_UPDATED`, `PERMISSION_GRANTED`.
* `actorId` — who or what caused the event.
* `resourceType` — type of resource affected.
* `resourceId` — specific resource affected.
* `payload` — structured JSON containing event-specific details.
* `timestamp` — time when the event occurred.

### Sample Request

```json
{
  "eventType": "RECORD_UPDATED",
  "actorId": "user-123",
  "resourceType": "ORDER",
  "resourceId": 456,
  "payload": {
    "fieldChanged": "status",
    "oldValue": "PENDING",
    "newValue": "APPROVED"
  },
  "timestamp": "2026-09-10T09:30:00"
}
```

### Timestamp Decision

The timestamp will be supplied by the upstream component. This ensures that the audit event represents the actual event time rather than the time at which the audit service receives or persists the request.

### Validation

The API validates all mandatory request fields before persisting the audit record.

Validation includes:

* `eventType` must not be null or blank.
* `actorId` must not be null or blank.
* `resourceType` must not be null or blank.
* `resourceId` must not be null.
* `payload` must not be null or empty.
* `timestamp` must not be null.

Invalid requests will return:

```text
HTTP 400 Bad Request
```

> `401 Unauthorized` is reserved for authentication-related failures. Security is explicitly out of scope for this scenario, so validation failures are handled as `400 Bad Request`.

---

### Technical decisions
1) Previous hash of teh first record is Genesis
2) Previous hash in every record is (eventType|actorId|resourceType|resourceId|payload|timestamp)

# 2. GET API — Query Audit Records

### Endpoint

```text
GET /audit/getRecords
```

The API supports optional filtering and pagination.

### Supported Query Parameters

| Parameter      | Type          | Description                         |
| -------------- | ------------- | ----------------------------------- |
| `actorId`      | String        | Filter by actor                     |
| `resourceType` | String        | Filter by resource type             |
| `resourceId`   | Long          | Filter by resource ID               |
| `eventType`    | String        | Filter by event type                |
| `from`         | LocalDateTime | Return records from this timestamp  |
| `to`           | LocalDateTime | Return records up to this timestamp |
| `page`         | Integer       | Page number                         |
| `size`         | Integer       | Number of records per page          |
| `sort`         | String        | Sorting criteria                    |

### Example

```text
GET /audit/getRecords?actorId=user-123&eventType=RECORD_UPDATED&page=0&size=20
```

### Default Pagination

The API defaults to:

```text
page size = 20
sort      = id ASC
```

This is implemented using Spring Data's `Pageable`:

```java
@PageableDefault(
        size = 20,
        sort = "id",
        direction = Sort.Direction.ASC
)
Pageable pageable
```

### Response

The API returns a paginated collection:

```text
Page<AuditEntity>
```

A successful request returns:

```text
HTTP 200 OK
```

The response contains the matching audit records along with pagination metadata such as total elements, total pages, current page, and page size.

### Query Implementation

The controller delegates the filtering logic to the service layer:

```java
@GetMapping("/getRecords")
public ResponseEntity<Page<AuditEntity>> query(
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
```

The service uses the repository layer to query only the records matching the supplied filters.

---

# Technical decision
1) Records from audit_events table are only returned not from the archive table

# 3. GET API — Verify Audit Chain

### Endpoint

```text
GET /audit/verify
```

This endpoint verifies the integrity of the complete audit hash chain.

### Purpose

The verification process checks two things for every audit record:

1. The record's content still produces the stored `currentHash`.
2. The record's `previousHash` correctly points to the `currentHash` of the preceding record.

### Example Chain

```text
Record 1
previousHash = GENESIS
currentHash  = AAA
       |
       v
Record 2
previousHash = AAA
currentHash  = BBB
       |
       v
Record 3
previousHash = BBB
currentHash  = CCC
```

### Verification Logic

For the first record:

```text
previousHash = GENESIS_HASH
```

For every subsequent record:

```text
previousHash = previous record's currentHash
```

The service recalculates the current record's hash and compares it with the stored `currentHash`.

It then verifies that:

```text
current record.previousHash
        ==
previous record.currentHash
```

### Endpoint Implementation

```java
@GetMapping("/verify")
public ResponseEntity<ChainVerificationResult> verify() {

    return ResponseEntity.ok(
            auditService.verifyChain()
    );
}
```

### Successful Response

If every record passes verification:

```json
{
  "valid": true,
  "recordNumber": 10,
  "recordId": null,
  "violationType": null,
  "message": "Audit hash chain is intact."
}
```

### Content Hash Failure

If the contents of a record have been modified:

```json
{
  "valid": false,
  "recordNumber": 5,
  "recordId": 5,
  "violationType": "CONTENT_HASH_MISMATCH",
  "message": "The record content does not match its stored hash."
}
```

### Previous Hash Failure

If the chain relationship has been modified:

```json
{
  "valid": false,
  "recordNumber": 5,
  "recordId": 5,
  "violationType": "PREVIOUS_HASH_MISMATCH",
  "message": "The previousHash does not match the preceding record."
}
```

---

# 4. Database Schema

The audit log is mapped to the `audit_events` table in the H2 database.

```sql
CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(255) NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    resource_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    timestamp DATETIME NOT NULL,
    current_hash VARCHAR(64) NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (id)
);
```

---

# 5. Hash Chain Design

Each audit record contains:

```text
currentHash
previousHash
```

The `currentHash` is generated from the deterministic representation of the record.

The `currentHash` itself is excluded from the data used to generate the hash.

Conceptually:

```text
Record data + previousHash
            |
            v
        SHA-256
            |
            v
       currentHash
```

The first record uses a predefined:

```text
GENESIS_HASH
```

The next record references the previous record's `currentHash`.

Therefore:

```text
Record N.previousHash
        =
Record N-1.currentHash
```

This creates a tamper-evident chain.

---

# 6. Concurrency

The service must support concurrent audit requests without corrupting the hash chain.

Normal database inserts are handled using transactional database operations.

However, because a new record depends on the previous record's hash, creation of a new chain entry must ensure that two concurrent requests do not read the same previous hash.

The implementation should therefore serialize the operation that obtains the latest chain state, for example by using a database lock or another concurrency-control mechanism.

The logical operation is:

```text
Read latest hash
      ↓
Create audit record
      ↓
Calculate current hash
      ↓
Persist record
      ↓
Update latest chain state
      ↓
Commit transaction
```

This ensures that concurrent requests cannot independently create two records using the same previous hash.

---

# 7. API Summary

| Method | Endpoint            | Purpose                                                |
| ------ | ------------------- | ------------------------------------------------------ |
| POST   | `/audit/postRecord` | Create a new audit record                              |
| GET    | `/audit/getRecords` | Search/query audit records with filters and pagination |
| GET    | `/audit/verify`     | Verify integrity of the complete audit hash chain      |

---

# Technical decision
1) Records from audit_events table and archived tables are also verified to maintain the chain

## Out of Scope

1. Security 