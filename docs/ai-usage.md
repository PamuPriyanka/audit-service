# AI Usage Log / Traceability

## 1. Hash Chain Implementation

### Task
Implement SHA-256 based hash chaining for audit records.

### Prompt / Intent
Asked AI how to create a tamper-evident audit chain where each
record contains its own hash and references the previous record's hash.

### AI Suggestion
AI suggested:
- Retrieve the latest audit record.
- Use its current hash as the previous hash.
- Generate the new record hash using the audit record data and previous hash.
- For the first record, use a predefined initial value / empty previous hash.

### Decision
Modified.

### What Was Implemented
The implementation retrieves the latest record from the database and
uses its `currentHash` as the `previousHash` for the new record.

For the first record, no previous record exists, so the initial
previous hash is handled separately.

### Why
The implementation needed to match the existing database model and
the requirement that each record be linked to the immediately preceding
record.

### Validation
Created multiple audit records and verified that each record's
`previousHash` matches the preceding record's `currentHash`.

---

## 2. SHA-256 Hash Generation

### Task
Generate a deterministic hash for an audit record.

### Prompt / Intent
Asked AI why SHA-256 should be used and how to generate a hash from
the audit event contents.

### AI Suggestion
AI suggested serializing the relevant audit fields into a deterministic
representation and applying SHA-256.

### Decision
Accepted with modification.

### What Was Implemented
The application creates a hashable representation of the audit event
and generates a SHA-256 hash using Java's `MessageDigest`.

### Why
SHA-256 provides a deterministic cryptographic digest and makes changes
to the protected audit content detectable.

### Validation
Created the same audit input multiple times and verified that the
resulting hash was deterministic.

---

## 3. Chain Verification

### Task
Implement an endpoint that verifies whether the audit hash chain is intact.

### Prompt / Intent
Asked AI how to verify each record against the previous record.

### AI Suggestion
AI suggested iterating through records in ID order and checking:
1. The current record's previous hash.
2. The recalculated hash.
3. The relationship with the previous record.

### Decision
Modified.

### What Was Implemented
The service retrieves audit records ordered by ID and verifies the
hash relationships sequentially.

### Why
The database uses the audit record ID as the ordering mechanism, so
verification follows the persisted audit sequence.

### Validation
Tested:
- Valid chain
- Modified payload
- Modified hash
- Broken previous-hash reference
- Empty database

---

## 4. Structured Redaction

### Task
Support redaction of sensitive fields without breaking the hash chain.

### Prompt / Intent
Asked AI how sensitive payload fields could be redacted while
maintaining tamper evidence.

### AI Suggestion
AI proposed designing redaction so that the original protected
representation remains verifiable while the sensitive value is no
longer exposed.

### Decision
Modified.

### What Was Implemented
The redaction design was adapted to the application's existing audit
model rather than simply deleting the sensitive value.

### Why
Simply changing or removing a value covered by the original hash would
cause the hash verification to fail. The implementation therefore
separates the privacy requirement from the integrity verification
mechanism.

### Validation
Verified that:
- Sensitive values are no longer returned after redaction.
- The audit record remains verifiable.
- Unrelated fields remain unchanged.