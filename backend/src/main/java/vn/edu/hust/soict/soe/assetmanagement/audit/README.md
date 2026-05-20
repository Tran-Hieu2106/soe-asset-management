# Module: Audit Log (M4 Scope)

## Overview
The **Audit Log** module satisfies the **RP-03** requirement from Milestone 1. It acts as the central Nervous System for the entire State-Owned Enterprise Asset Management application. 

Its primary responsibility is to provide a **searchable, immutable trail of all system actions**. Whenever a state change occurs in *any* other module (Assets, Stock, Handover, Liquidation), this module silently captures who did it, what they did, when they did it, their IP address, and the exact JSON snapshot of the data before and after the change.

By architectural rule, this module uses an **append-only** database table and strictly avoids modifying existing records to ensure compliance with financial and system auditing standards.

---

## Detailed File Specifications

### 1. Entities
#### `entity/AuditLog.java`
* **Purpose:** The JPA Entity mapping to the `audit_logs` table. Crucially, **this class does NOT extend `BaseEntity`**. Per the project's architectural rules, append-only tables do not need `updatedAt` tracking.
* **Attributes:**
    * `id (UUID)`: Primary key. Auto-generated. Updatable = false.
    * `module (String)`: The system area where the event occurred (e.g., `"ASSET"`, `"STOCK"`, `"HANDOVER"`).
    * `action (String)`: The specific event trigger (e.g., `"CREATE"`, `"UPDATE_STATUS"`, `"APPROVE"`).
    * `recordId (String)`: The UUID (cast to String) of the affected record in the target module.
    * `recordCode (String)`: The human-readable business code of the affected record (e.g., `"TSCD-001"`).
    * `performedBy (String)`: The username of the account that triggered the action.
    * `userId (UUID)`: The ID of the user account.
    * `ipAddress (String)`: The network IP address of the client making the request.
    * `oldValue (String)`: A JSON string representation of the record *before* the action. (Usually `{}` for creations).
    * `newValue (String)`: A JSON string representation of the record *after* the action.
    * `description (String)`: A human-readable summary of the event (e.g., `"Changed asset status to LIQUIDATED"`).
    * `performedAt (LocalDateTime)`: The exact timestamp of the event. Defaults to `LocalDateTime.now()`. Updatable = false.
* **Methods:**
    * Standard Lombok getters, setters, no-args constructor, all-args constructor, and Builder. No custom business methods exist here to ensure the entity remains a pure data container.

### 2. Data Transfer Objects (DTOs)
#### `dto/AuditLogDto.java`
* **Purpose:** Safely transports audit data to the frontend, preventing the exposure of the raw JPA entity.
* **Attributes:** Mirrors the `AuditLog` entity exactly, excluding the raw `userId` to minimize unnecessary data exposure.
* **Methods:**
    * `static AuditLogDto from(AuditLog log)`: A static factory method that maps the raw `AuditLog` JPA entity into this safe DTO using the Builder pattern.

### 3. Repositories
#### `repository/AuditLogRepository.java`
* **Purpose:** The Spring Data JPA interface bridging the application to the PostgreSQL `audit_logs` table.
* **Methods:**
    * `Page<AuditLog> searchLogs(String module, String action, Pageable pageable)`: A custom `@Query` using JPQL. It dynamically filters logs based on the `module` and `action` parameters. If a parameter is `NULL`, it bypasses that specific filter. It returns a `Page` object for pagination support.

### 4. Services
#### `service/AuditLogService.java`
* **Purpose:** The core engine of the module. It provides the read capabilities for auditors and the centralized write capability used by *every other module in the system*.
* **Attributes:**
    * `AuditLogRepository auditLogRepository`: Injected to handle database operations.
* **Methods:**
    * `Page<AuditLogDto> getAuditLogs(String module, String action, Pageable pageable)`: Calls the repository's search method, streams the result, and maps the `Page<AuditLog>` into a `Page<AuditLogDto>`. Marked as `@Transactional(readOnly = true)`.
    * `void log(String module, String action, String recordId, String recordCode, String oldValue, String newValue, String description)`: 
        * **Behavior:** This is the global hook. 
        * **Step 1:** It reaches into Spring's `SecurityContextHolder` to extract the `User` object, grabbing the active `username` and `userId`.
        * **Step 2:** It reaches into Spring's `RequestContextHolder` to grab the raw HTTP Request, securely extracting the user's `X-Forwarded-For` (Client IP) or falling back to `RemoteAddr`.
        * **Step 3:** It builds the `AuditLog` entity and saves it.
        * **Crucial Detail:** Marked with `@Transactional(propagation = Propagation.REQUIRED)`. This means it *joins* the database transaction of the module calling it. If a Handover approval fails and rolls back, the Audit Log also rolls back, preventing "phantom" logs of failed actions.

### 5. Controllers
#### `controller/AuditLogController.java`
* **Purpose:** Exposes the REST API for retrieving audit logs (RP-03).
* **Attributes:** * `AuditLogService auditLogService`: Handles the business logic.
* **Methods:**
    * `getAuditLogs(...)` mapped to `GET /api/audit-logs`:
        * **Security:** Protected by `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_AUDIT')")`. Only high-level oversight roles can read the system trail.
        * **Inputs:** Optional `module` and `action` query parameters, plus `page` and `size` for pagination.
        * **Behavior:** Constructs a `PageRequest` sorted by `performedAt` descending (newest first). Calls the service layer.
        * **Output:** Returns a `ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>>`. This perfectly complies with Cuong's global wrapper rules.

---

## Sequential Workflows

### Flow 1: Inter-Module Event Logging (Cross-cutting Concern)
**Goal:** Silently capture a state change triggered in another module (e.g., M2 Asset, M3 Stock, M4 Handover).

1. **Trigger:** A user calls an endpoint in another module, such as `PATCH /api/liquidations/{id}/approve`.
2. **Main Transaction Starts:** `LiquidationService.approveRequest()` begins a `@Transactional` block.
3. **Database Update:** The Liquidation is updated, and the M2 `FixedAsset` status is changed.
4. **Log Invocation:** The Liquidation service calls `auditLogService.log("LIQUIDATION", "APPROVE", id, code, oldJson, newJson, "Approved...")`.
5. **Context Extraction:** The `AuditLogService` intercepts the current HTTP Request thread. It extracts the approver's JWT credentials (Username/ID) and network IP address.
6. **Persistence:** The log is saved to the DB. Because of `Propagation.REQUIRED`, this save is attached to the Liquidation's transaction.
7. **Commit:** The overarching transaction commits. The liquidation, the asset update, and the audit log are all permanently written to PostgreSQL simultaneously.

### Flow 2: Management Audit Review (RP-03)
**Goal:** A Financial Auditor wants to review all system actions related to "STOCK" to investigate discrepancies.

1. **Request:** The auditor (holding the `FINANCE_AUDIT` role) opens the dashboard, which fires `GET /api/audit-logs?module=STOCK&page=0&size=50`.
2. **Security Check:** `SecurityConfig` and `@PreAuthorize` intercept the request. The JWT token is validated, and the role is confirmed. Access granted.
3. **Controller Routing:** `AuditLogController` receives the request, wraps the pagination parameters into a `PageRequest` sorted by newest-first, and calls `AuditLogService.getAuditLogs()`.
4. **Data Retrieval:** The service calls `AuditLogRepository.searchLogs()`. The JPQL query ignores the `action` filter (since it's null) but filters the table where `module = 'STOCK'`.
5. **Data Mapping:** The raw `Page<AuditLog>` entities are converted to `AuditLogDto` objects to strip out sensitive database mapping info.
6. **Response Formulation:** The DTOs are wrapped in Cuong's `PageResponse` (to provide total elements/pages metadata), which is then wrapped in an `ApiResponse.success()`.
7. **Delivery:** The auditor receives a structured 200 OK JSON response with the exact historical trail of all stock movements.