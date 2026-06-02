# API Contract — SOE Asset Management System
# Version: 1.1 (aligned with SRS milestone1.txt + backend implementation)
# Base URL: http://localhost:8080/api

## Conventions

- All JSON responses: `{ "success": true|false, "message": "...", "data": ... }`
- Auth: `Authorization: Bearer <jwt>`
- Dates: ISO 8601 `YYYY-MM-DD`
- Money: integer/decimal VND in JSON (BigDecimal)
- IDs: UUID strings for entities; integer IDs for category lookup tables
- Pagination query: `page` (0-based), `size` (default 20)
- Paginated `data`: `{ "content": [], "page": 0, "size": 20, "totalElements": N, "totalPages": M }`
- File downloads: raw binary, no ApiResponse wrapper

---

## Authentication

### POST /api/auth/login
Public. Body: `{ "username", "password" }` → `{ "token", "username" }`

### GET /api/users/me
Authenticated. Returns user profile with `roles: ["SYSTEM_ADMIN", ...]` and `managingUnitCodes`.

---

## Lookups (forms & filters)

All require authentication.

| GET | Description |
|-----|-------------|
| `/api/lookups/managing-units` | Active departments |
| `/api/lookups/asset-categories` | FA categories (TT45) |
| `/api/lookups/material-categories` | CS categories |
| `/api/lookups/storage-locations` | Warehouses |

Response item: `{ "id": "...", "code": "PHKT", "name": "..." }`

---

## Fixed Assets (FA)

### GET /api/assets
Roles: SYSTEM_ADMIN, ASSET_MANAGER, FINANCE_AUDIT, APPROVING_AUTH

Query: `page`, `size`, `status`, `categoryId`, `managingUnitId`, `acquisitionFrom`, `acquisitionTo`, `keyword`

### GET /api/assets/{id}
Returns asset with **realtime depreciation** (FA-02): `accumulatedDepreciation`, `netBookValue`, `annualDepreciationAmount`, `annualDepreciationRate`, `depreciationStartDate`, `depreciationEndDate`, plus enriched `categoryCode`, `categoryName`, `managingUnitCode`, `managingUnitName`.

### POST /api/assets
Roles: SYSTEM_ADMIN, ASSET_MANAGER. Body uses `categoryId`, `managingUnitId` (UUID). Creates with status **IN_USE**.

### PUT /api/assets/{id}
Partial update when status is IN_USE or IDLE.

### PATCH /api/assets/{id}/status
Query: `newStatus`, `reason` (required).

Statuses: `IN_USE` | `MAINTENANCE` | `IDLE` | `TRANSFERRED` | `LIQUIDATED`

### GET /api/assets/{id}/history
Lifecycle ledger (FA-04).

---

## Consumable Stock (CS)

### GET /api/materials
Query: `page`, `size`, `categoryId`

### GET /api/materials/search?keyword=
### GET /api/materials/{id}
### POST /api/materials
### PUT /api/materials/{id}

### POST /api/stock/receipt
Single material per request:
```json
{
  "materialId": "uuid",
  "storageLocationId": "uuid",
  "quantity": 50.000,
  "unitPrice": 85000,
  "documentRef": "HD-2024-0601",
  "documentDate": "2024-06-01",
  "notes": "optional"
}
```

### POST /api/stock/issue
```json
{
  "materialId": "uuid",
  "storageLocationId": "uuid",
  "quantity": 10.000,
  "requestingDepartmentId": "uuid",
  "documentRef": "XK-2024-001",
  "documentDate": "2024-06-05",
  "requestedBy": "optional",
  "notes": "optional"
}
```

### GET /api/stock/balance
List of balances per material × storage location.

### GET /api/stock/usage?startDate&endDate
Department consumption summary (CS-04).

---

## Handover (HL-01, HL-03)

Workflow: **DRAFT → submit → PENDING_APPROVAL → approve → APPROVED → confirm → CONFIRMED → complete → COMPLETED**

Asset transfer occurs on **complete** (HL-01a).

| Method | Path | Roles | Notes |
|--------|------|-------|-------|
| GET | `/api/handovers` | ASSET_MANAGER, APPROVING_AUTH, SYSTEM_ADMIN | page, size |
| GET | `/api/handovers/{id}` | same | |
| POST | `/api/handovers` | ASSET_MANAGER, SYSTEM_ADMIN | Creates **DRAFT** |
| PUT | `/api/handovers/{id}/submit` | ASSET_MANAGER, SYSTEM_ADMIN | |
| PUT | `/api/handovers/{id}/approve?notes=` | APPROVING_AUTH, SYSTEM_ADMIN | |
| PUT | `/api/handovers/{id}/confirm?notes=` | ASSET_MANAGER, APPROVING_AUTH, SYSTEM_ADMIN | |
| PUT | `/api/handovers/{id}/complete` | ASSET_MANAGER, SYSTEM_ADMIN | Generates document |
| PUT | `/api/handovers/{id}/reject?reason=` | APPROVING_AUTH, SYSTEM_ADMIN | reason required |
| GET | `/api/handovers/{id}/document` | same read roles | PDF when **COMPLETED** |

Create body: `{ assetId, fromUnitId, toUnitId, reason, handoverDate?, assetCondition?, notes? }`

---

## Liquidation (HL-02)

Workflow: **DRAFT → submit → PENDING_MANAGER → approve-manager → PENDING_DIRECTOR → approve-director → APPROVED → complete → COMPLETED**

| Method | Path |
|--------|------|
| GET | `/api/liquidations` |
| GET | `/api/liquidations/{id}` |
| POST | `/api/liquidations` |
| PUT | `/api/liquidations/{id}/submit` |
| PUT | `/api/liquidations/{id}/approve-manager?notes=` |
| PUT | `/api/liquidations/{id}/approve-director?notes=` |
| PUT | `/api/liquidations/{id}/complete?finalDisposalValue=` |
| PUT | `/api/liquidations/{id}/reject?reason=` |

Create body: `{ assetId, requestingUnitId, justification, assetCondition, currentMarketValue?, disposalMethod, disposalNotes? }`

Disposal methods: `AUCTION` | `SCRAP` | `DONATION`

---

## Reports & Audit (RP)

### GET /api/reports/assets
Paginated, filterable: `status`, `categoryId`, `managingUnitId`, `acquisitionFrom`, `acquisitionTo`, `page`, `size`

### GET /api/reports/assets/export?format=EXCEL|PDF|CSV
Same filters as above.

### GET /api/reports/stock?startDate&endDate

### GET /api/reports/stock/export?format=EXCEL

### GET /api/audit-logs
Query: `module`, `action`, `performedBy`, `startDate`, `endDate`, `page`, `size`

---

## Users (AUTH-04)

| Method | Path | Role |
|--------|------|------|
| GET | `/api/users` | SYSTEM_ADMIN |
| GET | `/api/users/{id}` | SYSTEM_ADMIN |
| POST | `/api/users` | SYSTEM_ADMIN |
| PATCH | `/api/users/{id}/deactivate` | SYSTEM_ADMIN |

---

## Status labels (Vietnamese UI)

**Asset:** IN_USE=Đang sử dụng, MAINTENANCE=Đang bảo trì, IDLE=Chờ phân bổ, TRANSFERRED=Đã bàn giao, LIQUIDATED=Đã thanh lý

**Handover:** DRAFT=Nháp, PENDING_APPROVAL=Chờ phê duyệt, APPROVED=Đã phê duyệt, CONFIRMED=Đã xác nhận, COMPLETED=Hoàn thành, REJECTED=Đã từ chối

**Liquidation:** DRAFT, PENDING_MANAGER, PENDING_DIRECTOR, APPROVED, COMPLETED, REJECTED

---

## Seed users (dev)

| Username | Password | Role |
|----------|----------|------|
| admin | Password@123 | SYSTEM_ADMIN |
| asset.manager | Password@123 | ASSET_MANAGER |
| warehouse | Password@123 | WAREHOUSE |
| approver | Password@123 | APPROVING_AUTH |
| finance.audit | Password@123 | FINANCE_AUDIT |
