**SRS Requirements:** HL-01 · HL-01a · HL-03  
**Business Regulation:** Nghị định 151/2017/NĐ-CP · Nghị định 30/2020/NĐ-CP

---

## Table of Contents

1. [What This Module Does](#1-what-this-module-does)
2. [Regulatory Context](#2-regulatory-context)
3. [File Reference — Class Detail](#3-file-reference--class-detail)
   - [HandoverStatus.java](#31-handoverstatusjava--entity)
   - [HandoverRequest.java](#32-handoverrequestjava--entity)
   - [CreateHandoverRequest.java](#33-createhandoverrequestjava--dto)
   - [HandoverDto.java](#34-handoverdtojava--dto)
   - [HandoverRepository.java](#35-handoverrepositoryjava--repository)
   - [HandoverService.java](#36-handoverservicejava--service)
   - [HandoverDocumentService.java](#37-handoverdocumentservicejava--service)
   - [HandoverController.java](#38-handovercontrollerjava--controller)
4. [Sequential Flow Walkthroughs](#4-sequential-flow-walkthroughs)
   - [Flow 1 — Create a Draft Handover Request](#flow-1--create-a-draft-handover-request)
   - [Flow 2 — Submit Draft for Approval](#flow-2--submit-draft-for-approval)
   - [Flow 3 — Approve the Request (with Asset Transfer)](#flow-3--approve-the-request-with-asset-transfer)
   - [Flow 4 — Receiving Unit Confirms Receipt](#flow-4--receiving-unit-confirms-receipt)
   - [Flow 5 — Complete and Generate Document (HL-03)](#flow-5--complete-and-generate-document-hl-03)
   - [Flow 6 — Reject at Approval Step](#flow-6--reject-at-approval-step)
   - [Flow 7 — Reject After Approval (Asset Rollback)](#flow-7--reject-after-approval-asset-rollback)
   - [Flow 8 — Read Handover List (Paginated)](#flow-8--read-handover-list-paginated)
   - [Flow 9 — Read Single Handover Detail](#flow-9--read-single-handover-detail)
5. [Business Rules Summary](#5-business-rules-summary)
6. [Cross-Module Dependencies](#6-cross-module-dependencies)
7. [Database Table Reference](#7-database-table-reference)
8. [API Endpoint Summary](#8-api-endpoint-summary)

---

## 1. What This Module Does

The handover module formalises the transfer of fixed assets between managing units inside a Vietnamese State-Owned Enterprise (SOE). Without this module, asset movements would be tracked informally through spreadsheets and paper forms, making it impossible to answer audit questions like "who transferred this asset, when, and who approved it."

### Core capabilities

**HL-01 — Structured multi-step handover workflow**  
Every asset transfer goes through a controlled, user-attributed sequence of steps. No single person can both initiate a transfer and approve it — this is the *separation of duties* rule required by BR-02 of the SRS. Each step is stored with a timestamp and the username of whoever performed it, creating an irrefutable paper trail.

**HL-01a — Atomic record updates on completion**  
When a handover is fully approved and confirmed, three things happen simultaneously inside a single database transaction:
1. The asset's `managing_unit_id` is updated to the receiving unit.
2. The asset's `status` is set to `TRANSFERRED`.
3. A `TRANSFERRED` event is appended to the asset lifecycle history (FA-04).
4. An entry is written to the system audit log (RP-01).

If any of these four operations fails, the entire transaction is rolled back — the system never ends up in a state where the asset shows "transferred" but the audit log has no record of it.

**HL-03 — Official document generation**  
After completion, the module generates a *Biên bản bàn giao tài sản* (Asset Handover Record) — a legally significant administrative document under Vietnamese state law (Nghị định 30/2020/NĐ-CP). The document reference number is stored on the request record. In production this becomes a downloadable PDF; the current implementation is a structured stub ready for iText7 or OpenHTMLtoPDF integration (see TBD-01 in the SRS).

### Who uses this module and what they can do

| Role | What they can do |
|------|-----------------|
| `ASSET_MANAGER` (R-02) | Create drafts, submit for approval, confirm receipt, complete the workflow |
| `APPROVING_AUTH` (R-04) | Approve or reject pending requests, confirm receipt |
| `SYSTEM_ADMIN` (R-01) | All of the above (full override access) |
| `FINANCE_AUDIT` (R-05) | Read-only (via reports module — no direct handover access) |

### What this module does NOT do
- It does not handle liquidation (separate `liquidation/` module).
- It does not perform asset depreciation calculations (that is the `asset/` module's job).
- It does not store stock/materials movements (that is the `stock/` module).
- It does not directly write to `asset_history` — it calls `FixedAssetService.updateAssetStatusAndUnit()` which writes that record.

---

## 2. Regulatory Context

| Regulation | What it mandates in this module |
|-----------|--------------------------------|
| **Nghị định 151/2017/NĐ-CP** | Formal procedures for state asset handovers must include documented justification, multi-level approval, and a signed transfer record. |
| **Nghị định 30/2020/NĐ-CP** | Document format standards for state administrative records. The generated *Biên bản bàn giao* must follow the prescribed format including asset details, parties involved, and all approver names/roles. |
| **BR-02 (SRS §5.5)** | Separation of duties: the initiator of a request must not be the same person who approves it. Enforced at the service layer — it cannot be bypassed from the UI. |
| **BR-07 (SRS §5.5)** | Document numbering: handover records use `BG-YYYY-NNN` format (Biên bản bàn Giao). The final document gets a `BBGTS-YYYY-NNN` reference (Biên Bản Giao Tài Sản). |
| **RP-01 (SRS §4.5.3)** | Every approval event must synchronously write to the audit log as part of the same transaction. |

---

## 3. File Reference — Class Detail

### 3.1 `HandoverStatus.java` — `entity/`

**Type:** Java `enum`  
**Purpose:** Defines every legal state a `HandoverRequest` can be in. Stored in the database as a `VARCHAR(30)` string via `@Enumerated(EnumType.STRING)`.

#### Why an enum (not a String)?
Using an enum instead of a plain String constant means the compiler catches typos (`APPROVD` would be a compile error), and it prevents invalid states from ever entering the system.

#### Constants

| Constant | Meaning | Is Terminal? |
|----------|---------|-------------|
| `DRAFT` | Request has been created but not yet submitted. The initiator can still edit it. | No |
| `PENDING_APPROVAL` | Request has been submitted and is waiting for the Approving Authority (R-04) to act. | No |
| `APPROVED` | The Approving Authority has approved the transfer. At this point the asset is already marked `TRANSFERRED` in the assets table. The receiving unit must now confirm physical receipt. | No |
| `CONFIRMED` | The receiving unit representative has confirmed they physically received the asset. Final document generation is pending. | No |
| `COMPLETED` | All steps are done. The document has been generated. The workflow is permanently closed. No further transitions are possible. | **Yes** |
| `REJECTED` | The request was rejected at any in-progress step. The rejection reason is recorded. A new request for the same asset can be submitted after this. | **Yes** |

#### Workflow state machine
```
DRAFT
  │ (initiator submits)
  ▼
PENDING_APPROVAL
  │ (approver approves)         │ (approver rejects)
  ▼                             ▼
APPROVED ──────────────────► REJECTED
  │ (receiving unit confirms)
  ▼
CONFIRMED
  │ (admin/manager completes)
  ▼
COMPLETED
```

---

### 3.2 `HandoverRequest.java` — `entity/`

**Type:** JPA `@Entity`  
**Table:** `handover_requests`  
**Extends:** `BaseEntity` (provides `createdAt`, `updatedAt`, `createdBy` via JPA Auditing)  
**Purpose:** The core data model. One row in `handover_requests` = one complete handover workflow instance, from initiation through final completion or rejection.

#### Why it extends BaseEntity (not append-only)
Unlike `AssetHistory` or `AuditLog` (which are append-only and never updated), a `HandoverRequest` is updated multiple times during its lifecycle — each workflow step writes new values to the approval/confirmation columns. It therefore needs `updated_at`, which is why it correctly extends `BaseEntity`.

#### Attributes

**Primary Key**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `id` | `UUID` | `id` | Auto-generated UUID. This is what the frontend uses in all subsequent API calls (approve, reject, etc.). Never shown to end users; they see `requestCode` instead. |

**Identity Fields**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `requestCode` | `String` | `request_code VARCHAR(50) UNIQUE` | Human-readable business identifier, e.g. `BG-2025-0042`. Generated by `HandoverService.generateRequestCode()`. This is the code that appears on the printed *Biên bản bàn giao* document. |
| `assetId` | `UUID` | `asset_id UUID NOT NULL` | The UUID of the fixed asset being transferred. Stored as a plain UUID (not a JPA `@ManyToOne` join) to avoid circular dependencies between the `handover` and `asset` packages. |

**Parties Involved**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `fromUnitId` | `UUID` | `from_unit_id UUID NOT NULL` | UUID of the managing unit that currently holds the asset (the sender). |
| `toUnitId` | `UUID` | `to_unit_id UUID NOT NULL` | UUID of the managing unit that will receive the asset (the receiver). |
| `initiatedBy` | `String` | `initiated_by VARCHAR(100) NOT NULL` | Username of the person who created the request. Never taken from the request body — always read from the JWT token in the controller to prevent impersonation. |

**Workflow Status**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `status` | `HandoverStatus` | `status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'` | The current state of the workflow. Starts as `DRAFT`. Changed by each workflow method in `HandoverService`. |

**Justification & Context**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `reason` | `String` | `reason TEXT NOT NULL` | The business justification for the transfer. Mandatory. Written by the initiator. |
| `handoverDate` | `LocalDate` | `handover_date DATE` | The planned or actual date of the physical handover. Optional at creation; can be confirmed later. |
| `assetCondition` | `String` | `asset_condition VARCHAR(50)` | The physical condition of the asset at transfer time: `GOOD`, `FAIR`, or `POOR`. |
| `notes` | `String` | `notes TEXT` | Any additional free-text notes from the initiator. |

**Step 1 — Department Head Approval**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `deptApprovedBy` | `String` | `dept_approved_by VARCHAR(100)` | Username of the person who approved at Step 1. Null until Step 1 is acted on. Business rule: must differ from `initiatedBy`. |
| `deptApprovedAt` | `LocalDateTime` | `dept_approved_at TIMESTAMP` | Exact timestamp of the Step 1 approval action. |
| `deptApprovalNotes` | `String` | `dept_approval_notes TEXT` | Optional explanatory notes left by the approver. |

**Step 2 — Receiving Unit Confirmation**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `confirmedBy` | `String` | `confirmed_by VARCHAR(100)` | Username of the person from the receiving unit who confirmed physical receipt of the asset. |
| `confirmedAt` | `LocalDateTime` | `confirmed_at TIMESTAMP` | Exact timestamp of the confirmation. |
| `confirmationNotes` | `String` | `confirmation_notes TEXT` | Optional notes from the receiving party (e.g. "received in good condition, missing one cable"). |

**Step 3 — Final Record Completion**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `completedBy` | `String` | `completed_by VARCHAR(100)` | Username of the person who triggered the final `COMPLETED` transition. |
| `completedAt` | `LocalDateTime` | `completed_at TIMESTAMP` | Timestamp when the workflow was closed. |

**Rejection (any step)**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `rejectedBy` | `String` | `rejected_by VARCHAR(100)` | Username of the person who rejected the request. |
| `rejectedAt` | `LocalDateTime` | `rejected_at TIMESTAMP` | Timestamp of the rejection. |
| `rejectionReason` | `String` | `rejection_reason TEXT` | Mandatory explanation of why the request was rejected. Blank rejection reasons are rejected by `HandoverService`. |

**HL-03 Document Fields**

| Field | Type | Column | Description |
|-------|------|--------|-------------|
| `documentRef` | `String` | `document_ref VARCHAR(255)` | Reference number of the generated *Biên bản bàn giao* document, e.g. `BBGTS-2025-0042`. Set by `HandoverDocumentService.generateDocument()`. |
| `documentGeneratedAt` | `LocalDateTime` | `document_generated_at TIMESTAMP` | Timestamp when the document was generated. |
| `documentSigned` | `Boolean` | `document_signed BOOLEAN NOT NULL DEFAULT FALSE` | Whether the document has been physically or digitally signed by both parties. |

**Inherited from BaseEntity**

| Field | Description |
|-------|-------------|
| `createdAt` | Auto-set by JPA Auditing on INSERT. |
| `updatedAt` | Auto-set by JPA Auditing on every UPDATE. |
| `createdBy` | Auto-set to current username from `SecurityContext` on INSERT. |

---

### 3.3 `CreateHandoverRequest.java` — `dto/`

**Type:** Input DTO (request body)  
**Used by:** `POST /api/handovers`  
**Purpose:** The object that the frontend sends in the request body when creating a new handover. Contains only the fields the client should supply — everything else (status, codes, approval timestamps) is generated server-side.

#### Why fields are excluded from the body
If `initiatedBy` were accepted from the body, any user could claim to be someone else. If `status` were accepted, a client could create a request already in `APPROVED` state. The controller always reads `initiatedBy` from the JWT token.

#### Attributes

| Field | Type | Validation | Description |
|-------|------|-----------|-------------|
| `assetId` | `UUID` | `@NotNull` | The asset being transferred. Validated at service layer: asset must exist. |
| `fromUnitId` | `UUID` | `@NotNull` | The sending unit. Service validates it differs from `toUnitId`. |
| `toUnitId` | `UUID` | `@NotNull` | The receiving unit. Must differ from `fromUnitId`. |
| `reason` | `String` | `@NotBlank`, `@Size(max=2000)` | Mandatory justification for the transfer. |
| `handoverDate` | `LocalDate` | `@PastOrPresent` (optional) | Planned date of physical transfer. Cannot be in the future. |
| `assetCondition` | `String` | `@Size(max=50)` (optional) | `GOOD`, `FAIR`, or `POOR`. |
| `notes` | `String` | `@Size(max=1000)` (optional) | Additional notes. |

If any `@NotNull` or `@NotBlank` constraint fails, `GlobalExceptionHandler` returns a `400 Bad Request` with a map of field → error message before the controller method even executes.

---

### 3.4 `HandoverDto.java` — `dto/`

**Type:** Output DTO (response body)  
**Used by:** Every endpoint that returns a handover (GET list, GET by ID, POST create, all PUT actions)  
**Purpose:** The safe, serializable representation of a `HandoverRequest` that is returned to the frontend. The JPA entity is never sent directly over the wire.

#### Static factory method: `HandoverDto.from(HandoverRequest entity)`
This is the single conversion point. Instead of mapping fields in every service method or controller, all conversion is centralised here. Pattern is identical to `AuditLogDto.from(AuditLog log)` in the audit module.

**Every field on `HandoverRequest` is exposed in `HandoverDto`**, including all step timestamps, approval notes, and document fields. This allows the frontend to render a complete audit trail of who did what at each step, directly from the single `/api/handovers/{id}` response.

---

### 3.5 `HandoverRepository.java` — `repository/`

**Type:** Spring Data JPA `@Repository` interface extending `JpaRepository<HandoverRequest, UUID>`  
**Purpose:** All database access for the `handover_requests` table. The service layer never writes raw SQL — it always calls methods on this interface.

#### Methods

**Inherited from `JpaRepository` (no declaration needed)**

| Method | What it does |
|--------|-------------|
| `save(entity)` | Inserts a new row or updates an existing one. Returns the saved entity with generated/updated fields filled in. Used after every status transition. |
| `findById(UUID)` | Returns `Optional<HandoverRequest>` for the given UUID. Used by `findOrThrow()` in the service. |
| `findAll(Pageable)` | Returns a `Page<HandoverRequest>` — the paginated list behind `GET /api/handovers`. |
| `count()` | Returns total number of rows. Used by `generateRequestCode()` to create sequential codes. |

**Custom declared methods**

| Method | Return type | Description |
|--------|-------------|-------------|
| `findByRequestCode(String)` | `Optional<HandoverRequest>` | Looks up a request by its human-readable code (e.g. `BG-2025-0042`). Used to check for duplicate codes before saving. |
| `existsByRequestCode(String)` | `boolean` | More efficient than `findByRequestCode` when only needing a yes/no existence check. |
| `hasActiveRequestForAsset(UUID assetId, List<HandoverStatus> terminalStates)` | `boolean` | **The blocking-rule query (HL-01).** Returns `true` if the given asset already has any request whose status is NOT in `terminalStates` (i.e. not COMPLETED and not REJECTED). Used in `createHandover()` to prevent two simultaneous workflows for the same asset. Written as a `@Query` using JPQL `COUNT(h) > 0 ... NOT IN :terminalStates`. |
| `findByAssetIdOrderByCreatedAtDesc(UUID)` | `List<HandoverRequest>` | Returns all handover requests ever created for one asset, newest first. Useful for the asset detail page's "handover history" panel. |
| `findByUnitId(UUID unitId, Pageable)` | `Page<HandoverRequest>` | Returns paginated requests where the unit is either the sender OR the receiver. Uses a `@Query` with `fromUnitId = :unitId OR toUnitId = :unitId`. |
| `findByStatus(HandoverStatus, Pageable)` | `Page<HandoverRequest>` | Filters by a specific workflow status. Useful for the approver dashboard ("show me all PENDING_APPROVAL requests"). |
| `findByInitiatedBy(String, Pageable)` | `Page<HandoverRequest>` | Returns all requests created by a specific user. Useful for "My Requests" views. |

#### JPQL vs SQL note
All custom `@Query` annotations use **JPQL** (Java Persistence Query Language), which uses Java class and field names — not SQL table/column names. For example, `h.assetId` in JPQL maps to the `asset_id` column in SQL. This keeps queries database-agnostic and lets Hibernate validate field names at startup.

---

### 3.6 `HandoverService.java` — `service/`

**Type:** Spring `@Service`  
**Annotation:** `@Transactional` (all writes), `@Transactional(readOnly = true)` (reads)  
**Purpose:** The single place where all handover business logic lives. Controllers call this; this calls the repository and other services. No business logic exists anywhere else.

#### Constructor-injected dependencies

| Dependency | Why it is needed |
|------------|-----------------|
| `HandoverRepository` | To read and write `handover_requests` rows. |
| `FixedAssetService` | Cross-module call to update the asset's status and managing unit when a handover is approved or rolled back on rejection. |
| `AuditLogService` | Cross-module call to write to `audit_logs` on every state transition. Required by RP-01. |
| `HandoverDocumentService` | Called during `completeHandover()` to generate the formal *Biên bản bàn giao* document (HL-03). |

#### Class-level constant

| Constant | Type | Value | Purpose |
|----------|------|-------|---------|
| `TERMINAL_STATUSES` | `List<HandoverStatus>` | `[COMPLETED, REJECTED]` | Passed to `hasActiveRequestForAsset()` to define which statuses do NOT block a new request. Declared as a constant so the business rule is written in exactly one place. |

#### Methods

---

##### `getAllHandovers(Pageable pageable) → Page<HandoverDto>`
**Access:** `@Transactional(readOnly = true)` — no database write, faster query plan.  
**What it does:** Calls `handoverRepository.findAll(pageable)` and maps each `HandoverRequest` entity to a `HandoverDto` using Java stream `.map(HandoverDto::from)`.  
**Called by:** `GET /api/handovers` in the controller.  
**Returns:** A `Page<HandoverDto>` containing at most `pageable.getPageSize()` items, plus pagination metadata (totalElements, totalPages, etc.) consumed by `PageResponse.of()`.

---

##### `getHandoverById(UUID id) → HandoverDto`
**Access:** `@Transactional(readOnly = true)`  
**What it does:** Calls `findOrThrow(id)` — if the ID exists, returns `HandoverDto.from(entity)`; if not, throws `ResourceNotFoundException` which `GlobalExceptionHandler` turns into a `404 Not Found`.  
**Called by:** `GET /api/handovers/{id}` in the controller.

---

##### `createHandover(CreateHandoverRequest request, String initiatedBy) → HandoverDto`
**Access:** `@Transactional` (writes)  
**What it does, step by step:**
1. Checks `request.fromUnitId != request.toUnitId` → throws `BusinessRuleException` if equal.
2. Calls `handoverRepository.hasActiveRequestForAsset(assetId, TERMINAL_STATUSES)` → throws `BusinessRuleException` if an in-progress request already exists for this asset.
3. Builds a new `HandoverRequest` entity with `status = DRAFT`, `documentSigned = false`, and `initiatedBy` from the JWT (not the request body).
4. Calls `handoverRepository.save(entity)` → persists the row, returns the saved entity with its generated UUID and audit timestamps filled in.
5. Calls `auditLogService.log("HANDOVER", "CREATE", ...)` → writes to `audit_logs`.
6. Returns `HandoverDto.from(saved)`.

**Called by:** `POST /api/handovers` in the controller.

---

##### `submitHandover(UUID id, String username) → HandoverDto`
**Access:** `@Transactional` (writes)  
**What it does:**
1. Calls `findOrThrow(id)`.
2. Calls `requireStatus(request, DRAFT, ...)` → throws if not in `DRAFT`.
3. Sets `status = PENDING_APPROVAL`.
4. Saves and writes audit log.

**Called by:** `PUT /api/handovers/{id}/submit` in the controller.

---

##### `approveHandover(UUID id, String approverNotes, String approvedBy) → HandoverDto`
**Access:** `@Transactional` (writes — includes cross-module side effects)  
**This is the most complex method.** What it does:
1. Calls `findOrThrow(id)`.
2. Calls `requireStatus(request, PENDING_APPROVAL, ...)` → throws if wrong state.
3. Checks `approvedBy.equals(request.getInitiatedBy())` → throws `BusinessRuleException` if same person (separation of duties, BR-02).
4. Sets `status = APPROVED`, fills in `deptApprovedBy`, `deptApprovedAt`, `deptApprovalNotes`.
5. Saves the handover request.
6. **Cross-module call:** `fixedAssetService.updateAssetStatusAndUnit(assetId, TRANSFERRED, toUnitId, reason, approvedBy)` — this writes to the `assets` table AND appends to `asset_history`. The whole thing happens inside the same `@Transactional` context, so if this fails, the handover status change is also rolled back.
7. Writes audit log.

**Called by:** `PUT /api/handovers/{id}/approve` in the controller.

---

##### `confirmHandover(UUID id, String confirmationNotes, String confirmedBy) → HandoverDto`
**Access:** `@Transactional` (writes)  
**What it does:**
1. Calls `findOrThrow(id)`.
2. Calls `requireStatus(request, APPROVED, ...)` → throws if not `APPROVED`.
3. Sets `status = CONFIRMED`, fills in `confirmedBy`, `confirmedAt`, `confirmationNotes`.
4. Saves and writes audit log.

**Called by:** `PUT /api/handovers/{id}/confirm` in the controller.

---

##### `completeHandover(UUID id, String completedBy) → HandoverDto`
**Access:** `@Transactional` (writes — includes document generation)  
**What it does:**
1. Calls `findOrThrow(id)`.
2. Calls `requireStatus(request, CONFIRMED, ...)` → throws if not `CONFIRMED`.
3. Calls `handoverDocumentService.generateDocument(request)` → returns a document reference string (e.g. `BBGTS-2025-0042`).
4. Sets `documentRef`, `documentGeneratedAt` on the entity.
5. Sets `status = COMPLETED`, fills in `completedBy`, `completedAt`.
6. Saves and writes audit log.

**Called by:** `PUT /api/handovers/{id}/complete` in the controller.

---

##### `rejectHandover(UUID id, String rejectionReason, String rejectedBy) → HandoverDto`
**Access:** `@Transactional` (writes — may include asset rollback)  
**This method handles two scenarios:**

*Scenario A — Rejected from PENDING_APPROVAL:*
The asset was never marked TRANSFERRED, so no rollback is needed. Just set status to REJECTED.

*Scenario B — Rejected from APPROVED (asset rollback):*
The asset was already marked `TRANSFERRED` and its `managingUnitId` was updated. Rejection must undo this. The method calls `fixedAssetService.updateAssetStatusAndUnit(assetId, IN_USE, fromUnitId, rollbackReason, rejectedBy)` to restore the original state.

**Step by step:**
1. Calls `findOrThrow(id)`.
2. Checks the current status is not already terminal (`COMPLETED` or `REJECTED`) or `CONFIRMED` (cannot reject after confirmation).
3. Checks `rejectionReason` is not blank.
4. If `status == APPROVED`: calls `fixedAssetService.updateAssetStatusAndUnit(...)` with `IN_USE` and `fromUnitId` to roll back.
5. Sets `status = REJECTED`, fills in `rejectedBy`, `rejectedAt`, `rejectionReason`.
6. Saves and writes audit log.

**Called by:** `PUT /api/handovers/{id}/reject` in the controller.

---

##### `findOrThrow(UUID id) → HandoverRequest` (private helper)
Calls `handoverRepository.findById(id)`. If the optional is empty, throws `ResourceNotFoundException("Không tìm thấy yêu cầu bàn giao với ID: " + id)`. Every public method that needs a specific request calls this — the error message is consistent across all endpoints.

---

##### `requireStatus(HandoverRequest, HandoverStatus, String errorMessage)` (private helper)
Checks if `request.getStatus() == expectedStatus`. If not, throws `BusinessRuleException(errorMessage + " Trạng thái hiện tại: " + request.getStatus().name())`. This eliminates duplicated if-checks across all workflow methods and ensures Vietnamese error messages are always shown.

---

##### `generateRequestCode() → String` (private helper)
Produces a code in the format `BG-YYYY-NNNN` where `YYYY` is the current year and `NNNN` is zero-padded to 4 digits based on `handoverRepository.count() + 1`. The `request_code UNIQUE` database constraint acts as the final safety net against duplicates in a concurrent scenario.

---

### 3.7 `HandoverDocumentService.java` — `service/`

**Type:** Spring `@Service`  
**Purpose:** Implements HL-03 — generates the *Biên bản bàn giao tài sản* (Asset Handover Record). Separated from `HandoverService` because document generation is a distinct concern that may later involve file I/O, template engines, and PDF libraries.

#### Methods

##### `generateDocument(HandoverRequest handoverRequest) → String`
**What it does (current stub implementation):**
1. Derives the document reference number by replacing `BG-` with `BBGTS-` in the request code (e.g. `BG-2025-0042` → `BBGTS-2025-0042`).
2. Logs all document metadata at `INFO` level in a structured block (simulates the real document generation for now).
3. Returns the reference string.

**What it will do in production (documented extension points):**
- Render a Thymeleaf HTML template with all request fields populated.
- Convert the HTML to PDF using iText7 or OpenHTMLtoPDF (TBD-01 in SRS).
- Save the PDF to a configured storage path.
- Return the file URI or path as the document reference.

The document must include per HL-03 requirements:
- Asset code and name
- Transferring unit and receiving unit
- Asset condition at time of transfer
- Handover date
- Net book value at time of transfer
- Names and roles of all approving parties

##### `markDocumentSigned(HandoverRequest handoverRequest) → boolean`
**What it does:** Logs that the document has been marked as signed and returns `true`. In production this would verify a digital signature, watermark the PDF, or update a document management system. The caller is responsible for persisting the `documentSigned = true` flag on the entity.

---

### 3.8 `HandoverController.java` — `controller/`

**Type:** Spring `@RestController`  
**Base path:** `/api/handovers`  
**Purpose:** Thin HTTP adapter. Receives requests, extracts the authenticated username from the `Authentication` object, calls the appropriate `HandoverService` method, and wraps the result in `ApiResponse<T>`.

**Three rules this controller always follows:**
1. **No business logic** — every decision is in `HandoverService`.
2. **No try/catch** — `GlobalExceptionHandler` handles all exceptions.
3. **Username from JWT only** — `authentication.getName()` is always used; the request body never supplies the actor's identity.

#### Methods / Endpoints

| Method | HTTP | Path | `@PreAuthorize` | Description |
|--------|------|------|----------------|-------------|
| `getAllHandovers(page, size)` | `GET` | `/api/handovers` | `SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH` | Calls `handoverService.getAllHandovers(pageable)`. Builds `Pageable` with `Sort.by("createdAt").descending()`. Returns `ApiResponse<PageResponse<HandoverDto>>`. |
| `getHandoverById(id)` | `GET` | `/api/handovers/{id}` | `SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH` | Calls `handoverService.getHandoverById(id)`. Returns `ApiResponse<HandoverDto>`. |
| `createHandover(request, auth)` | `POST` | `/api/handovers` | `SYSTEM_ADMIN, ASSET_MANAGER` | Validates `@Valid @RequestBody`, extracts `initiatedBy = auth.getName()`, calls `handoverService.createHandover(...)`. Returns `201 Created` with `ApiResponse<HandoverDto>`. |
| `submitHandover(id, auth)` | `PUT` | `/api/handovers/{id}/submit` | `SYSTEM_ADMIN, ASSET_MANAGER` | Calls `handoverService.submitHandover(id, auth.getName())`. Returns `200 OK`. |
| `approveHandover(id, notes, auth)` | `PUT` | `/api/handovers/{id}/approve` | `SYSTEM_ADMIN, APPROVING_AUTH` | Passes optional `notes` query param. Calls `handoverService.approveHandover(id, notes, auth.getName())`. Returns `200 OK`. |
| `confirmHandover(id, notes, auth)` | `PUT` | `/api/handovers/{id}/confirm` | `SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH` | Calls `handoverService.confirmHandover(id, notes, auth.getName())`. Returns `200 OK`. |
| `completeHandover(id, auth)` | `PUT` | `/api/handovers/{id}/complete` | `SYSTEM_ADMIN, ASSET_MANAGER` | Calls `handoverService.completeHandover(id, auth.getName())`. Returns `200 OK`. |
| `rejectHandover(id, reason, auth)` | `PUT` | `/api/handovers/{id}/reject` | `SYSTEM_ADMIN, APPROVING_AUTH` | `reason` is a **required** query param. Calls `handoverService.rejectHandover(id, reason, auth.getName())`. Returns `200 OK`. |

---

## 4. Sequential Flow Walkthroughs

Each flow below describes exactly what happens at every layer — HTTP, controller, service, repository, and cross-module calls — in the order they execute.

---

### Flow 1 — Create a Draft Handover Request

**Actor:** Asset Manager (R-02)  
**Trigger:** User fills in the "Tạo yêu cầu bàn giao" form and clicks submit.  
**SRS requirement:** HL-01

```
Frontend
  └─► POST /api/handovers
        Body: { assetId, fromUnitId, toUnitId, reason, handoverDate?, assetCondition?, notes? }
        Header: Authorization: Bearer <JWT>

HandoverController.createHandover(@Valid @RequestBody, Authentication)
  │
  ├── 1. Spring Security intercepts request:
  │       - Verifies JWT signature
  │       - Checks role: must be SYSTEM_ADMIN or ASSET_MANAGER → else 403
  │       - Populates Authentication object with username from JWT
  │
  ├── 2. Bean Validation (@Valid) checks CreateHandoverRequest:
  │       - assetId not null
  │       - fromUnitId not null
  │       - toUnitId not null
  │       - reason not blank, max 2000 chars
  │       - handoverDate, if provided, is past or present
  │       If any check fails → GlobalExceptionHandler returns 400 with field errors
  │
  ├── 3. initiatedBy = authentication.getName()
  │       (extracted from JWT — never from request body)
  │
  └── 4. Calls HandoverService.createHandover(request, initiatedBy)

HandoverService.createHandover(CreateHandoverRequest, String initiatedBy)
  │
  ├── 5. Check fromUnitId != toUnitId
  │       If equal → throw BusinessRuleException → 400
  │
  ├── 6. handoverRepository.hasActiveRequestForAsset(assetId, [COMPLETED, REJECTED])
  │       Executes: SELECT COUNT(h) > 0 FROM handover_requests h
  │                  WHERE h.asset_id = ? AND h.status NOT IN ('COMPLETED','REJECTED')
  │       If returns true → throw BusinessRuleException → 400
  │
  ├── 7. Build HandoverRequest entity:
  │       - requestCode = generateRequestCode()  →  "BG-2025-0001"
  │       - status = DRAFT
  │       - documentSigned = false
  │       - all fields from DTO
  │       - initiatedBy from JWT
  │
  ├── 8. handoverRepository.save(entity)
  │       Executes: INSERT INTO handover_requests (...)
  │       JPA Auditing auto-fills createdAt, updatedAt, createdBy
  │       Returns saved entity with generated UUID
  │
  ├── 9. auditLogService.log("HANDOVER", "CREATE", id, requestCode, null, "{...}", "...")
  │       Executes: INSERT INTO audit_logs (...)
  │       (same transaction — if audit log fails, handover insert also rolls back)
  │
  └── 10. Return HandoverDto.from(saved)

HandoverController
  └── Return ResponseEntity.status(201).body(ApiResponse.success("...", HandoverDto))

Frontend receives: 201 Created
  { "success": true, "message": "Tạo yêu cầu bàn giao thành công",
    "data": { "id": "...", "requestCode": "BG-2025-0001", "status": "DRAFT", ... } }
```

---

### Flow 2 — Submit Draft for Approval

**Actor:** Asset Manager (R-02) — same person who created the draft  
**Trigger:** User clicks "Nộp để phê duyệt" on their draft request.  
**SRS requirement:** HL-01 step 1

```
Frontend
  └─► PUT /api/handovers/{id}/submit
        Header: Authorization: Bearer <JWT>

HandoverController.submitHandover(UUID id, Authentication)
  └── Calls HandoverService.submitHandover(id, auth.getName())

HandoverService.submitHandover(UUID id, String username)
  │
  ├── 1. findOrThrow(id)
  │       SELECT * FROM handover_requests WHERE id = ?
  │       If not found → ResourceNotFoundException → 404
  │
  ├── 2. requireStatus(request, DRAFT, "Chỉ có thể nộp...")
  │       If status != DRAFT → BusinessRuleException → 400
  │
  ├── 3. request.setStatus(PENDING_APPROVAL)
  │
  ├── 4. handoverRepository.save(request)
  │       UPDATE handover_requests SET status='PENDING_APPROVAL', updated_at=NOW() WHERE id=?
  │
  ├── 5. auditLogService.log("HANDOVER", "STATUS_CHANGE", ...)
  │
  └── Return HandoverDto.from(saved)

Frontend receives: 200 OK
  { "data": { "status": "PENDING_APPROVAL", ... } }
```

---

### Flow 3 — Approve the Request (with Asset Transfer)

**Actor:** Approving Authority (R-04)  
**Trigger:** Approver sees the request in their queue and clicks "Phê duyệt".  
**SRS requirement:** HL-01 (approval step), HL-01a (atomic asset update)  
**This is the most impactful flow — it touches three tables in one transaction.**

```
Frontend
  └─► PUT /api/handovers/{id}/approve?notes=<optional>
        Header: Authorization: Bearer <JWT of APPROVING_AUTH user>

HandoverController.approveHandover(UUID id, String notes, Authentication)
  │
  ├── Spring Security: role must be SYSTEM_ADMIN or APPROVING_AUTH → else 403
  └── Calls HandoverService.approveHandover(id, notes, auth.getName())

HandoverService.approveHandover(UUID id, String approverNotes, String approvedBy)
  │
  ├── 1. findOrThrow(id) → load HandoverRequest from DB
  │
  ├── 2. requireStatus(request, PENDING_APPROVAL, "Chỉ có thể phê duyệt...")
  │       If status != PENDING_APPROVAL → 400
  │
  ├── 3. BR-02 Separation of duties check:
  │       if (approvedBy.equals(request.getInitiatedBy()))
  │         throw BusinessRuleException → 400
  │       "Người phê duyệt không được là người tạo yêu cầu."
  │
  ├── 4. Update HandoverRequest:
  │       status = APPROVED
  │       deptApprovedBy = approvedBy
  │       deptApprovedAt = LocalDateTime.now()
  │       deptApprovalNotes = approverNotes
  │
  ├── 5. handoverRepository.save(request)
  │       UPDATE handover_requests SET status='APPROVED', dept_approved_by=?, ... WHERE id=?
  │
  ├── 6. *** CROSS-MODULE CALL (HL-01a) ***
  │       fixedAssetService.updateAssetStatusAndUnit(
  │           assetId    = request.getAssetId(),
  │           newStatus  = AssetStatus.TRANSFERRED,
  │           newUnitId  = request.getToUnitId(),
  │           reason     = "Bàn giao theo yêu cầu BG-2025-0001",
  │           username   = approvedBy
  │       )
  │       Inside FixedAssetService (same @Transactional context):
  │         a) UPDATE assets SET status='TRANSFERRED', managing_unit_id=<toUnitId> WHERE id=<assetId>
  │         b) INSERT INTO asset_history (event_type='STATUS_UPDATE', ...) 
  │         c) auditLogService.log("ASSET", "UPDATE", ...)
  │              INSERT INTO audit_logs (module='ASSET', ...)
  │
  ├── 7. auditLogService.log("HANDOVER", "APPROVE", ...)
  │       INSERT INTO audit_logs (module='HANDOVER', action='APPROVE', ...)
  │
  └── Return HandoverDto.from(saved)

Three tables written in one atomic transaction:
  handover_requests  → status = APPROVED
  assets             → status = TRANSFERRED, managing_unit_id = toUnitId
  asset_history      → new TRANSFERRED event appended
  audit_logs         → two new rows (ASSET UPDATE + HANDOVER APPROVE)

Frontend receives: 200 OK
  { "data": { "status": "APPROVED", "deptApprovedBy": "approver", ... } }
```

---

### Flow 4 — Receiving Unit Confirms Receipt

**Actor:** Asset Manager or Approving Authority (R-02 or R-04) from the receiving unit  
**Trigger:** The receiving unit physically receives the asset and confirms in the system.  
**SRS requirement:** HL-01 step 3

```
Frontend
  └─► PUT /api/handovers/{id}/confirm?notes=<optional>

HandoverService.confirmHandover(UUID id, String confirmationNotes, String confirmedBy)
  │
  ├── 1. findOrThrow(id)
  ├── 2. requireStatus(request, APPROVED, "Chỉ có thể xác nhận khi đã APPROVED...")
  ├── 3. Set: status=CONFIRMED, confirmedBy, confirmedAt, confirmationNotes
  ├── 4. handoverRepository.save(request)
  ├── 5. auditLogService.log("HANDOVER", "CONFIRM", ...)
  └── Return HandoverDto

Tables written: handover_requests (status→CONFIRMED), audit_logs (1 row)

Frontend receives: 200 OK  { "data": { "status": "CONFIRMED", ... } }
```

---

### Flow 5 — Complete and Generate Document (HL-03)

**Actor:** Asset Manager or System Admin  
**Trigger:** After confirmation, user clicks "Hoàn tất" to close the workflow and generate the document.  
**SRS requirement:** HL-03

```
Frontend
  └─► PUT /api/handovers/{id}/complete

HandoverService.completeHandover(UUID id, String completedBy)
  │
  ├── 1. findOrThrow(id)
  ├── 2. requireStatus(request, CONFIRMED, "Chỉ có thể hoàn tất khi đã CONFIRMED...")
  │
  ├── 3. *** DOCUMENT GENERATION (HL-03) ***
  │       String documentRef = handoverDocumentService.generateDocument(request)
  │       Inside HandoverDocumentService:
  │         a) Derives ref: "BG-2025-0001" → "BBGTS-2025-0001"
  │         b) Logs document metadata (simulates PDF creation)
  │         c) Returns "BBGTS-2025-0001"
  │         [Production: renders Thymeleaf template, generates PDF, saves to /storage/handovers/]
  │
  ├── 4. Set on entity:
  │       documentRef = "BBGTS-2025-0001"
  │       documentGeneratedAt = now()
  │       status = COMPLETED
  │       completedBy, completedAt
  │
  ├── 5. handoverRepository.save(request)
  ├── 6. auditLogService.log("HANDOVER", "COMPLETE", ..., "documentRef":"BBGTS-2025-0001")
  └── Return HandoverDto

Tables written: handover_requests (status→COMPLETED, documentRef set), audit_logs (1 row)

Frontend receives: 200 OK
  { "data": { "status": "COMPLETED", "documentRef": "BBGTS-2025-0001",
              "documentGeneratedAt": "2025-06-01T10:30:00", ... } }
```

---

### Flow 6 — Reject at Approval Step

**Actor:** Approving Authority (R-04)  
**Trigger:** Approver reviews a `PENDING_APPROVAL` request and decides to reject it.  
**SRS requirement:** HL-01 (rejection branch)

```
Frontend
  └─► PUT /api/handovers/{id}/reject?reason=Tài+sản+chưa+đủ+điều+kiện+bàn+giao

HandoverService.rejectHandover(UUID id, String rejectionReason, String rejectedBy)
  │
  ├── 1. findOrThrow(id)
  │
  ├── 2. Status check: must NOT be COMPLETED or REJECTED or CONFIRMED
  │       If status = COMPLETED → BusinessRuleException → 400
  │       If status = REJECTED  → BusinessRuleException → 400
  │       If status = CONFIRMED → BusinessRuleException → 400 (cannot undo confirmation)
  │
  ├── 3. rejectionReason blank check → BusinessRuleException if blank
  │
  ├── 4. Current status is PENDING_APPROVAL — no asset rollback needed
  │       (asset was never marked TRANSFERRED yet)
  │
  ├── 5. Set: status=REJECTED, rejectedBy, rejectedAt, rejectionReason
  ├── 6. handoverRepository.save(request)
  ├── 7. auditLogService.log("HANDOVER", "REJECT", ...)
  └── Return HandoverDto

Tables written: handover_requests (status→REJECTED), audit_logs (1 row)
Asset table: NOT touched (no rollback needed)

Frontend receives: 200 OK  { "data": { "status": "REJECTED", "rejectionReason": "...", ... } }
```

---

### Flow 7 — Reject After Approval (Asset Rollback)

**Actor:** Approving Authority (R-04) — rejecting a previously-approved request  
**Trigger:** A request is in `APPROVED` status but a problem is discovered before confirmation.  
**SRS requirement:** HL-01 (rejection + rollback)  
**Important:** The asset was already marked `TRANSFERRED` during Flow 3. This flow must undo that.

```
Frontend
  └─► PUT /api/handovers/{id}/reject?reason=Quyết+định+thay+đổi
        (request is currently in APPROVED status)

HandoverService.rejectHandover(UUID id, String rejectionReason, String rejectedBy)
  │
  ├── 1-3. Same as Flow 6 (findOrThrow, status checks, reason check)
  │
  ├── 4. *** ASSET ROLLBACK — status is APPROVED ***
  │       fixedAssetService.updateAssetStatusAndUnit(
  │           assetId    = request.getAssetId(),
  │           newStatus  = AssetStatus.IN_USE,          ← restore to IN_USE
  │           newUnitId  = request.getFromUnitId(),     ← restore original unit
  │           reason     = "Hoàn trả do yêu cầu BG-2025-0001 bị từ chối",
  │           username   = rejectedBy
  │       )
  │       Inside FixedAssetService (same transaction):
  │         a) UPDATE assets SET status='IN_USE', managing_unit_id=<fromUnitId> WHERE id=<assetId>
  │         b) INSERT INTO asset_history (event_type='STATUS_UPDATE', ...) 
  │         c) auditLogService.log("ASSET", "UPDATE", ...)
  │
  ├── 5. Set: status=REJECTED, rejectedBy, rejectedAt, rejectionReason
  ├── 6. handoverRepository.save(request)
  ├── 7. auditLogService.log("HANDOVER", "REJECT", ...)
  └── Return HandoverDto

Three tables written atomically:
  handover_requests → status = REJECTED
  assets            → status = IN_USE, managing_unit_id = fromUnitId (RESTORED)
  asset_history     → new STATUS_UPDATE event appended
  audit_logs        → two new rows (ASSET UPDATE + HANDOVER REJECT)

Frontend receives: 200 OK  { "data": { "status": "REJECTED", ... } }
```

---

### Flow 8 — Read Handover List (Paginated)

**Actor:** Any of SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH  
**Trigger:** User navigates to the "Danh sách bàn giao" page.

```
Frontend
  └─► GET /api/handovers?page=0&size=20

HandoverController.getAllHandovers(page=0, size=20)
  │
  ├── Build Pageable: PageRequest.of(0, 20, Sort.by("createdAt").descending())
  └── Calls HandoverService.getAllHandovers(pageable)

HandoverService.getAllHandovers(Pageable)
  │   @Transactional(readOnly = true)
  │
  ├── handoverRepository.findAll(pageable)
  │     SELECT * FROM handover_requests ORDER BY created_at DESC LIMIT 20 OFFSET 0
  │
  └── .map(HandoverDto::from) → Page<HandoverDto>

HandoverController
  └── PageResponse.of(page) → wraps content + pagination metadata

Frontend receives: 200 OK
  { "data": {
      "content": [ { id, requestCode, status, assetId, ... }, ... ],
      "page": 0, "size": 20, "totalElements": 87, "totalPages": 5, "last": false
    }
  }
```

---

### Flow 9 — Read Single Handover Detail

**Actor:** Any of SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH  
**Trigger:** User clicks on a specific request to view full detail.

```
Frontend
  └─► GET /api/handovers/{id}

HandoverService.getHandoverById(UUID id)
  │   @Transactional(readOnly = true)
  │
  ├── findOrThrow(id) → SELECT * FROM handover_requests WHERE id = ?
  │     If not found → ResourceNotFoundException → 404
  │
  └── Return HandoverDto.from(entity) — all 30+ fields populated

Frontend receives: 200 OK with complete HandoverDto including all
  step timestamps, approval notes, document ref, rejection reason (if any), etc.
```

---

## 5. Business Rules Summary

| Rule | ID | Where enforced | What happens if violated |
|------|----|---------------|--------------------------|
| Initiator ≠ approver | BR-02 / HL-01 | `HandoverService.approveHandover()` | `BusinessRuleException` → HTTP 400 |
| Only one active request per asset | HL-01 | `HandoverService.createHandover()` | `BusinessRuleException` → HTTP 400 |
| Sender ≠ receiver unit | (implied HL-01) | `HandoverService.createHandover()` | `BusinessRuleException` → HTTP 400 |
| Status must match expected before transition | HL-01 | `requireStatus()` in every workflow method | `BusinessRuleException` → HTTP 400 |
| Rejection reason must not be blank | HL-01 | `HandoverService.rejectHandover()` | `BusinessRuleException` → HTTP 400 |
| Cannot reject CONFIRMED or COMPLETED | HL-01 | `HandoverService.rejectHandover()` | `BusinessRuleException` → HTTP 400 |
| Asset update must be atomic with handover status | HL-01a / Safety §5.2 | `@Transactional` on `HandoverService` | Full rollback if any step fails |
| Audit log must be written on every transition | RP-01 | `auditLogService.log()` in every method | Same `@Transactional` context — if audit fails, entire operation rolls back |
| Document ref format: BG-YYYY-NNN | BR-07 | `generateRequestCode()` | N/A — convention enforced in code |

---

## 6. Cross-Module Dependencies

```
handover/
  HandoverService
    │
    ├──calls──► asset/
    │             FixedAssetService.updateAssetStatusAndUnit()
    │               writes to: assets table
    │               writes to: asset_history table
    │               calls:     auditLogService.log("ASSET",...)
    │
    ├──calls──► audit/
    │             AuditLogService.log("HANDOVER",...)
    │               writes to: audit_logs table
    │
    └──calls──► handover/
                  HandoverDocumentService.generateDocument()
                    (internal, same module)
```

**Why `FixedAssetService` is called (not direct repository access):**  
The `handover` module must not access `FixedAssetRepository` directly. Doing so would create a cross-module repository dependency, making the modules harder to test and modify independently. Instead, `HandoverService` calls the public API of `FixedAssetService` — a method that was specifically designed for this purpose (`updateAssetStatusAndUnit`). This keeps the asset module's internal implementation hidden from the handover module.

---

## 7. Database Table Reference

**Table:** `handover_requests` (defined in `V4__create_handover_liquidation.sql`)

```sql
status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
-- Valid values: DRAFT | PENDING_APPROVAL | APPROVED | CONFIRMED | COMPLETED | REJECTED

Indexes:
  idx_handover_code      ON request_code
  idx_handover_asset     ON asset_id
  idx_handover_status    ON status
  idx_handover_from_unit ON from_unit_id
  idx_handover_to_unit   ON to_unit_id
```

The module also reads from these tables (via cross-module service calls, never directly):
- `assets` — read and written via `FixedAssetService`
- `asset_history` — written via `FixedAssetService`
- `audit_logs` — written via `AuditLogService`

---

## 8. API Endpoint Summary

| Method | Path | Role required | Status codes | Description |
|--------|------|--------------|-------------|-------------|
| `GET` | `/api/handovers` | ADMIN, ASSET_MANAGER, APPROVING_AUTH | 200 | Paginated list, sorted newest-first |
| `GET` | `/api/handovers/{id}` | ADMIN, ASSET_MANAGER, APPROVING_AUTH | 200, 404 | Full detail of one request |
| `POST` | `/api/handovers` | ADMIN, ASSET_MANAGER | 201, 400 | Create a new DRAFT request |
| `PUT` | `/api/handovers/{id}/submit` | ADMIN, ASSET_MANAGER | 200, 400, 404 | DRAFT → PENDING_APPROVAL |
| `PUT` | `/api/handovers/{id}/approve` | ADMIN, APPROVING_AUTH | 200, 400, 403, 404 | PENDING_APPROVAL → APPROVED + asset transferred |
| `PUT` | `/api/handovers/{id}/confirm` | ADMIN, ASSET_MANAGER, APPROVING_AUTH | 200, 400, 404 | APPROVED → CONFIRMED |
| `PUT` | `/api/handovers/{id}/complete` | ADMIN, ASSET_MANAGER | 200, 400, 404 | CONFIRMED → COMPLETED + document generated |
| `PUT` | `/api/handovers/{id}/reject` | ADMIN, APPROVING_AUTH | 200, 400, 404 | Any active state → REJECTED (+ asset rollback if was APPROVED) |

All responses follow the `ApiResponse<T>` wrapper:
```json
{ "success": true, "message": "...", "data": { ... } }
```
List responses additionally wrap data in `PageResponse<T>`:
```json
{ "success": true, "message": "...", "data": { "content": [...], "page": 0, "size": 20, "totalElements": 87, ... } }
```

