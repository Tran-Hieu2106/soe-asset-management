# Module 2 (M2): Fixed Asset Management

## Overview
The Fixed Asset Management module is the core registry for state-owned physical equipment and property. Based on the requirements defined in Milestone 1 (SRS), this module tracks the entire lifecycle of an asset from procurement to disposal. It fulfills four primary functional requirements:
* **FA-01 (Asset Registration):** Digital profiling of physical assets, capturing technical specifications, geographic locations, and initial financial valuations.
* **FA-02 (Depreciation Calculation):** Automated calculation of accumulated depreciation and net book value complying with Vietnamese Circular 45/2013/TT-BTC (supporting both Straight-Line and Declining Balance methods).
* **FA-03 (Operational Status Tracking):** Management of asset states (`IN_USE`, `MAINTENANCE`, `TRANSFERRED`, `LIQUIDATED`).
* **FA-04 (Asset Lifecycle History):** Maintenance of an immutable, append-only ledger that records every status change and lifecycle event specific to the asset.

---

## Detailed File Specifications

### 1. Enums
#### `enums/AssetStatus.java`
* **Purpose:** Defines the strict set of lifecycle states an asset can occupy.
* **Values:**
    * `IN_USE`: Asset is currently actively deployed at a managing unit.
    * `MAINTENANCE`: Asset is undergoing repair and temporarily out of service.
    * `IDLE`: Asset is functioning but not currently assigned or used.
    * `TRANSFERRED`: Asset has been successfully handed over to a new unit (Hooked by M4 Handover).
    * `LIQUIDATED`: Asset has been disposed of via auction, scrap, or donation (Hooked by M4 Liquidation).

### 2.2. Entities
#### `entity/FixedAsset.java`
* **Purpose:** The central JPA Entity mapping to the `assets` table. It holds all static and calculated data for an asset. It extends `BaseEntity` to inherit standard audit fields (`createdAt`, `updatedAt`, `createdBy`).
* **Attributes:**
    * `id (UUID)`: Primary key, auto-generated.
    * `assetCode (String)`: Unique business identifier (e.g., `TSCD-2026-001`). Must be unique.
    * `name (String)`: Human-readable asset name.
    * `categoryId (Integer)`: Foreign key linking to the asset classification taxonomy (from M1/M3).
    * `managingUnitId (UUID)`: Foreign key linking to the department currently holding/responsible for the asset.
    * `serialNumber (String)`: Manufacturer's serial number.
    * `manufacturer, model, countryOfOrigin (String)`: Standard technical hardware specifications.
    * `technicalSpecs (String)`: Detailed text/JSON block describing hardware specs.
    * `location (String)`: Physical location/room of the asset.
    * `originalCost (BigDecimal)`: The initial purchase or acquisition cost (Financial FA-01).
    * `acquisitionDate (LocalDate)`: Date the asset was officially acquired.
    * `fundingSource (String)`: Budget source (e.g., "State Budget", "Enterprise Fund").
    * `usefulLifeYears (Integer)`: Expected lifespan in years (Critical for FA-02 Depreciation).
    * `salvageValue (BigDecimal)`: Estimated residual value at the end of its useful life.
    * `depreciationMethod (String)`: `STRAIGHT_LINE` (default) or `DECLINING_BALANCE`.
    * `accumulatedDepreciation (BigDecimal)`: Computed total value lost to date.
    * `netBookValue (BigDecimal)`: Computed current worth (`originalCost` - `accumulatedDepreciation`).
    * `status (AssetStatus)`: Current operational state mapped to the Enum.
    * `statusReason (String)`: Contextual explanation for the current status.
    * `statusChangedAt (LocalDateTime)`: Timestamp of the last status update.
    * `statusChangedBy (String)`: Username who executed the last status update.
    * `purchaseDocumentRef (String)`: Reference code to physical/digital procurement paperwork.
    * `notes (String)`: General remarks.

#### `entity/AssetHistory.java`
* **Purpose:** Maps to `asset_history`. An append-only ledger for the asset. **Does not** extend `BaseEntity` to preserve immutability rules.
* **Attributes:**
    * `id (UUID)`: Primary key.
    * `assetId (UUID)`: Reference to the parent `FixedAsset`.
    * `eventType (String)`: The action performed (e.g., `CREATED`, `STATUS_CHANGED`).
    * `description (String)`: Human-readable explanation.
    * `oldValue, newValue (String)`: State changes (usually JSON or stringified values).
    * `performedBy (String)`: The username of the actor.
    * `performedAt (LocalDateTime)`: Exact timestamp of the event.

### 3. Data Transfer Objects (DTOs)
#### `dto/FixedAssetDTO.java`
* **Purpose:** Transport object for incoming requests and outgoing responses to prevent exposing JPA proxy objects or sensitive internal database columns.
* **Attributes:** Mirrors `FixedAsset.java` but applies `jakarta.validation` annotations (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@PastOrPresent`) to enforce data integrity at the Controller boundary.

#### `dto/AssetHistoryDTO.java`
* **Purpose:** Safe transport representation of `AssetHistory` records for the frontend.

### 4. Repositories
#### `repository/FixedAssetRepository.java`
* **Purpose:** Spring Data JPA interface for the `assets` table.
* **Methods:**
    * `Optional<FixedAsset> findByAssetCode(String assetCode)`: Fetches an asset by its unique business code.
    * `boolean existsByAssetCode(String assetCode)`: Validation helper for creation.

#### `repository/AssetHistoryRepository.java`
* **Purpose:** Spring Data JPA interface for the `asset_history` table.
* **Methods:**
    * `List<AssetHistory> findByAssetIdOrderByPerformedAtDesc(UUID assetId)`: Retrieves the chronological lifecycle of a specific asset for the FA-04 requirement.

### 5. Services
### 2.5. Services
#### `service/FixedAssetService.java`
* **Purpose:** Contains the core business logic, depreciation mathematics, and transaction boundaries.
* **Dependencies:** `FixedAssetRepository`, `AssetHistoryRepository`, `AuditLogService` (from M4).
* **Methods:**
    * `List<FixedAsset> getAllAssets()`: Fetches all assets from the DB.
    * `FixedAsset createAsset(FixedAssetDTO dto)`: Initializes a new asset. Forces `accumulatedDepreciation` to 0, `netBookValue` to `originalCost`, and `status` to `IN_USE`. Persists to DB, creates an `AssetHistory` log, and fires an `AuditLogService.log()` event.
    * `FixedAsset calculateCurrentDepreciation(UUID id)`: The router method for FA-02. Fetches the asset, checks `depreciationMethod`, routes to the correct mathematical private method, and returns the updated entity.
    * `FixedAsset calculateStraightLine(FixedAsset asset)` *(Private)*: Calculates linear depreciation. Formula: `(Cost - Salvage) * (MonthsUsed / TotalUsefulMonths)`. Updates entity financials.
    * `FixedAsset calculateDecliningBalance(FixedAsset asset)` *(Private)*: Implements accelerated depreciation (1.5x, 2.0x, 2.5x multipliers depending on life years) per Circular 45. Falls back to straight-line when the accelerated rate drops below the linear rate. Updates entity financials.
    * `updateFinancials(FixedAsset asset, BigDecimal accumulated, BigDecimal salvageValue)`: Helper method to update accumulated depreciation and net book value.
    * `FixedAsset updateAssetStatus(UUID id, AssetStatus newStatus, String reason, String performedBy)`: Core integration method for FA-03. Changes the asset's state, forces an `AssetHistory` insert, and triggers the `AuditLog`. Heavily utilized by Handover and Liquidation modules.
    * `void saveHistoryLog(UUID assetId, String eventType, String description, String oldValue, String newValue, String performedBy)` *(Private)*: Utility to build and persist an `AssetHistory` entity.

### 2.6. Controllers
#### `controller/FixedAssetController.java`
* **Purpose:** Exposes RESTful HTTP endpoints for the frontend. Protects routes based on `SecurityConfig`. Wraps all returns in `ApiResponse`.
* **Methods:**
    * `ResponseEntity<ApiResponse<List<FixedAssetDTO>>> getAllAssets()`: Mapped to `GET /api/assets`. Returns all assets.
    * `ResponseEntity<ApiResponse<FixedAssetDTO>> getAssetById(UUID id)`: Mapped to `GET /api/assets/{id}`. Retrieves specific asset, triggering `calculateCurrentDepreciation()` so the frontend sees real-time financial values.
    * `ResponseEntity<ApiResponse<FixedAssetDTO>> createAsset(@Valid FixedAssetDTO)`: Mapped to `POST /api/assets`.
    * `ResponseEntity<ApiResponse<FixedAssetDTO>> calculateDepreciation(UUID id)`: Mapped to `GET /api/assets/{id}/depreciation`. Explicit endpoint to force recalculation of financials.
    * `ResponseEntity<ApiResponse<FixedAssetDTO>> updateStatus(...)`: Mapped to `PATCH /api/assets/{id}/status`. Exposes status updates to authorized users.
    * `ResponseEntity<ApiResponse<List<AssetHistoryDTO>>> getAssetHistory(UUID id)`: Mapped to `GET /api/assets/{id}/history`. Returns chronological ledger (FA-04).
    * `FixedAssetDTO mapToDTO(FixedAsset)` & `AssetHistoryDTO mapToHistoryDTO(AssetHistory)` *(Private)*: Object mappers.
---

## Sequential Workflows

### Flow 1: Asset Registration (FA-01)
**Goal:** Introduce a newly procured piece of equipment into the enterprise system.
1.  **Request:** The ASSET_MANAGER user submits a `POST /api/assets` payload containing technical specs, original cost, useful life, and acquisition date.
2.  **Validation:** `FixedAssetController` applies `@Valid` to the `FixedAssetDTO`. If valid, it calls `FixedAssetService.createAsset()`.
3.  **Business Logic:** The service instantiates a `FixedAsset`. It forces `accumulatedDepreciation` to 0, `netBookValue` equal to `originalCost`, and `status` to `IN_USE`. 
4.  **Persistence:** The asset is saved via `FixedAssetRepository.save()`. JPA automatically populates `createdAt` and `createdBy` via `BaseEntity`.
5.  **Event Logging:** * `AssetHistory` is created denoting "Digital asset profile initialization".
    * `AuditLogService.log("ASSET", "CREATE")` is fired for global system tracking.
6.  **Response:** Controller maps the saved asset to DTO and returns `201 Created` wrapped in an `ApiResponse`.

### Flow 2: On-the-fly Depreciation Calculation (FA-02)
**Goal:** Provide accurate current financial values whenever an asset is viewed.
1.  **Request:** User accesses an asset profile via `GET /api/assets/{id}`.
2.  **Routing:** Controller calls `FixedAssetService.calculateCurrentDepreciation(id)`.
3.  **Calculation:**
    * Service fetches the asset.
    * It calculates `monthsUsed` using `ChronoUnit.MONTHS.between(acquisitionDate, LocalDate.now())`.
    * **Straight-Line:** Spreads the depreciable base (Cost - Salvage) evenly over total months.
    * **Declining Balance:** Applies accelerated depreciation rates based on Circular 45 multipliers, falling back to straight-line when the accelerated rate drops below the standard linear rate.
4.  **State Update:** The calculated `accumulatedDepreciation` and `netBookValue` are injected into the asset object.
5.  **Response:** The financially up-to-date asset is mapped to a DTO and returned to the client.

### Flow 3: Operational Status Update & Handover/Liquidation Integrations (FA-03 & FA-04)
**Goal:** Track the state of the asset as it moves through the enterprise or leaves it.
1.  **Trigger:** * *Manual:* ASSET_MANAGER calls `PATCH /api/assets/{id}/status` for maintenance.
    * *Automated Integration:* The Handover module (M4) or Liquidation module (M4) successfully approves a workflow and programmaticly calls `FixedAssetService.updateAssetStatus(...)`.
2.  **State Change:** The service updates `status`, `statusReason`, `statusChangedAt`, and `statusChangedBy`.
3.  **Persistence:** Changes are saved to `FixedAssetRepository`.
4.  **Ledger Update (FA-04):** The service records the `oldValue` (e.g., IN_USE) and `newValue` (e.g., TRANSFERRED) inside a new `AssetHistory` record.
5.  **Global Audit:** `AuditLogService` records an `UPDATE_STATUS` event to ensure the transaction is visible in the system-wide security log.
6.  **Resolution:** Transaction commits. The asset now formally reflects its new state across all reports and dashboards.