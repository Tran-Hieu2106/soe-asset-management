# API Contract — SOE Asset Management System
# Version: 1.0 | Base URL: http://localhost:8080/api
#
# CONVENTIONS
# ─────────────────────────────────────────────────────────────
# All responses are wrapped in ApiResponse:
#   { "success": true/false, "message": "...", "data": { ... } }
#
# Authentication: Bearer token in Authorization header
#   Authorization: Bearer <jwt_token>
#
# Dates: ISO 8601 -> "2024-01-15"
# Money: integer VND -> 25000000 (no decimals in JSON)
# UUIDs: lowercase with dashes -> "10000000-0000-0000-0000-000000000001"
#
# Pagination: page (0-indexed) + size query params
#   ?page=0&size=20
#   Paginated responses return a "page" wrapper inside "data":
#   { "content": [...], "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 }
#
# HTTP status codes:
#   200 OK           -> success (GET, PATCH, PUT)
#   201 Created      -> success (POST that creates a resource)
#   400 Bad Request  -> validation error or business rule violation
#   401 Unauthorized -> missing or invalid JWT token
#   403 Forbidden    -> authenticated but wrong role
#   404 Not Found    -> resource does not exist
#   500 Server Error -> unexpected error
# ─────────────────────────────────────────────────────────────

---

## MODULE: Authentication

### POST /api/auth/login
**Description:** Authenticate with username and password. Returns JWT token.
**Auth required:** No (public endpoint)

**Request body:**
```json
{
  "username": "admin",
  "password": "Password@123"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đăng nhập thành công.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin"
  }
}
```

**Response 400 (wrong password):**
```json
{
  "success": false,
  "message": "Bad credentials"
}
```

---

## MODULE: Users

### GET /api/users/me
**Description:** Get the currently authenticated user's profile.
**Auth required:** Yes — any role

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "10000000-0000-0000-0000-000000000001",
    "username": "admin",
    "fullName": "Lê Việt Cường",
    "email": "admin@soe.vn",
    "phone": null,
    "isActive": true,
    "roles": ["SYSTEM_ADMIN"],
    "managingUnitCodes": ["HQ"]
  }
}
```

---

### GET /api/users
**Description:** List all users in the system.
**Auth required:** Yes — SYSTEM_ADMIN only

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "10000000-0000-0000-0000-000000000001",
      "username": "admin",
      "fullName": "Lê Việt Cường",
      "email": "admin@soe.vn",
      "phone": null,
      "isActive": true,
      "roles": ["SYSTEM_ADMIN"],
      "managingUnitCodes": ["HQ"]
    }
  ]
}
```

---

### GET /api/users/{id}
**Description:** Get a single user by UUID.
**Auth required:** Yes — SYSTEM_ADMIN only

**Path param:** `id` — UUID of the user

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "10000000-0000-0000-0000-000000000002",
    "username": "asset.manager",
    "fullName": "Đinh Hà Hải",
    "email": "asset.manager@soe.vn",
    "phone": null,
    "isActive": true,
    "roles": ["ASSET_MANAGER"],
    "managingUnitCodes": ["PHKT"]
  }
}
```

**Response 404:**
```json
{
  "success": false,
  "message": "Không tìm thấy người dùng với id: <id>"
}
```

---

### POST /api/users
**Description:** Create a new user account.
**Auth required:** Yes — SYSTEM_ADMIN only

**Request body:**
```json
{
  "username": "new.user",
  "password": "Password@123",
  "fullName": "Nguyễn Văn A",
  "email": "new.user@soe.vn",
  "phone": "0901234567",
  "roleCode": "ASSET_MANAGER"
}
```

**Role codes:** `SYSTEM_ADMIN` | `ASSET_MANAGER` | `WAREHOUSE` | `APPROVING_AUTH` | `FINANCE_AUDIT`

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo người dùng thành công.",
  "data": {
    "id": "uuid-of-new-user",
    "username": "new.user",
    "fullName": "Nguyễn Văn A",
    "email": "new.user@soe.vn",
    "phone": "0901234567",
    "isActive": true,
    "roles": ["ASSET_MANAGER"],
    "managingUnitCodes": []
  }
}
```

**Response 400 (duplicate username):**
```json
{
  "success": false,
  "message": "Tên đăng nhập đã tồn tại: new.user"
}
```

---

### PATCH /api/users/{id}/deactivate
**Description:** Deactivate a user account. The user can no longer log in.
**Auth required:** Yes — SYSTEM_ADMIN only

**Path param:** `id` — UUID of the user

**Response 200:**
```json
{
  "success": true,
  "message": "Tài khoản đã bị vô hiệu hóa."
}
```

---

## MODULE: Fixed Assets (FA)

### GET /api/assets
**Description:** List all fixed assets. Supports pagination and filtering.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, FINANCE_AUDIT, APPROVING_AUTH

**Query params:**
- `page` (int, default 0) — page index (0-based)
- `size` (int, default 20) — page size
- `status` (string, optional) — filter by status: `IN_USE` | `MAINTENANCE` | `IDLE` | `TRANSFERRED` | `LIQUIDATED`
- `managingUnitCode` (string, optional) — filter by managing unit code e.g. `PHKT`
- `categoryCode` (string, optional) — filter by asset category code e.g. `IT`
- `keyword` (string, optional) — search by asset code or name

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "30000000-0000-0000-0000-000000000001",
        "assetCode": "TS-2024-001",
        "name": "Máy tính xách tay Dell Latitude 5540",
        "category": "Thiết bị công nghệ thông tin",
        "categoryCode": "IT",
        "managingUnit": "Phòng Hành chính - Kỹ thuật",
        "managingUnitCode": "PHKT",
        "serialNumber": "SN-DELL-2024-001",
        "manufacturer": "Dell",
        "originalCost": 25000000,
        "accumulatedDepreciation": 5000000,
        "netBookValue": 20000000,
        "acquisitionDate": "2024-01-15",
        "usefulLifeYears": 5,
        "depreciationMethod": "STRAIGHT_LINE",
        "status": "IN_USE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### POST /api/assets
**Description:** Create a new fixed asset record.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER

**Request body:**
```json
{
  "assetCode": "TS-2024-002",
  "name": "Máy in HP LaserJet Pro",
  "categoryCode": "IT",
  "managingUnitCode": "PHKT",
  "serialNumber": "SN-HP-2024-001",
  "manufacturer": "HP",
  "model": "LaserJet Pro M404n",
  "countryOfOrigin": "Vietnam",
  "technicalSpecs": "Tốc độ in: 38 trang/phút",
  "location": "Phòng 201",
  "originalCost": 8000000,
  "acquisitionDate": "2024-03-01",
  "fundingSource": "Ngân sách nhà nước",
  "usefulLifeYears": 5,
  "salvageValue": 0,
  "depreciationMethod": "STRAIGHT_LINE",
  "purchaseDocumentRef": "HD-2024-0301",
  "notes": null
}
```

**Depreciation methods:** `STRAIGHT_LINE` | `DECLINING_BALANCE`

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo tài sản thành công.",
  "data": {
    "id": "30000000-0000-0000-0000-000000000002",
    "assetCode": "TS-2024-002",
    "name": "Máy in HP LaserJet Pro",
    "category": "Thiết bị công nghệ thông tin",
    "categoryCode": "IT",
    "managingUnit": "Phòng Hành chính - Kỹ thuật",
    "managingUnitCode": "PHKT",
    "serialNumber": "SN-HP-2024-001",
    "manufacturer": "HP",
    "originalCost": 8000000,
    "accumulatedDepreciation": 0,
    "netBookValue": 8000000,
    "acquisitionDate": "2024-03-01",
    "fundingSource": "Ngân sách nhà nước",
    "usefulLifeYears": 5,
    "salvageValue": 0,
    "depreciationMethod": "STRAIGHT_LINE",
    "status": "IDLE"
  }
}
```

**Response 400 (duplicate asset code):**
```json
{
  "success": false,
  "message": "Mã tài sản đã tồn tại: TS-2024-002"
}
```

---

### GET /api/assets/{id}
**Description:** Get full detail of a single asset.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, FINANCE_AUDIT, APPROVING_AUTH

**Path param:** `id` — UUID of the asset

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "name": "Máy tính xách tay Dell Latitude 5540",
    "category": "Thiết bị công nghệ thông tin",
    "categoryCode": "IT",
    "managingUnit": "Phòng Hành chính - Kỹ thuật",
    "managingUnitCode": "PHKT",
    "serialNumber": "SN-DELL-2024-001",
    "manufacturer": "Dell",
    "model": null,
    "countryOfOrigin": null,
    "technicalSpecs": null,
    "location": null,
    "originalCost": 25000000,
    "acquisitionDate": "2024-01-15",
    "fundingSource": "Ngân sách nhà nước",
    "usefulLifeYears": 5,
    "salvageValue": 0,
    "depreciationMethod": "STRAIGHT_LINE",
    "accumulatedDepreciation": 5000000,
    "netBookValue": 20000000,
    "annualDepreciationRate": 20.0000,
    "depreciationStartDate": "2024-01-15",
    "depreciationEndDate": "2029-01-15",
    "status": "IN_USE",
    "statusReason": null,
    "statusChangedAt": null,
    "purchaseDocumentRef": null,
    "notes": null,
    "createdBy": "admin",
    "createdAt": "2024-01-15T08:00:00"
  }
}
```

**Response 404:**
```json
{
  "success": false,
  "message": "Không tìm thấy tài sản với id: <id>"
}
```

---

### PUT /api/assets/{id}
**Description:** Update an asset's information. Only allowed when status is IDLE or IN_USE.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER

**Path param:** `id` — UUID of the asset

**Request body:** (same fields as POST, all optional — only send fields to update)
```json
{
  "name": "Máy tính xách tay Dell Latitude 5540 (Updated)",
  "manufacturer": "Dell Technologies",
  "fundingSource": "Nguồn tự có"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật tài sản thành công.",
  "data": {
    "id": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "name": "Máy tính xách tay Dell Latitude 5540 (Updated)",
    "manufacturer": "Dell Technologies"
  }
}
```

---

### PATCH /api/assets/{id}/status
**Description:** Update the operational status of an asset.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER

**Path param:** `id` — UUID of the asset

**Request body:**
```json
{
  "status": "MAINTENANCE",
  "note": "Đang gửi bảo hành tại trung tâm Dell"
}
```

**Status values:** `IN_USE` | `MAINTENANCE` | `IDLE` | `TRANSFERRED` | `LIQUIDATED`

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật trạng thái tài sản thành công.",
  "data": {
    "id": "30000000-0000-0000-0000-000000000001",
    "status": "MAINTENANCE"
  }
}
```

---

### GET /api/assets/{id}/depreciation
**Description:** Get depreciation schedule for an asset (year-by-year breakdown).
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, FINANCE_AUDIT

**Path param:** `id` — UUID of the asset

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "assetId": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "originalCost": 25000000,
    "usefulLifeYears": 5,
    "depreciationMethod": "STRAIGHT_LINE",
    "schedule": [
      {
        "year": 2024,
        "depreciationAmount": 5000000,
        "accumulatedDepreciation": 5000000,
        "netBookValue": 20000000
      },
      {
        "year": 2025,
        "depreciationAmount": 5000000,
        "accumulatedDepreciation": 10000000,
        "netBookValue": 15000000
      },
      {
        "year": 2026,
        "depreciationAmount": 5000000,
        "accumulatedDepreciation": 15000000,
        "netBookValue": 10000000
      },
      {
        "year": 2027,
        "depreciationAmount": 5000000,
        "accumulatedDepreciation": 20000000,
        "netBookValue": 5000000
      },
      {
        "year": 2028,
        "depreciationAmount": 5000000,
        "accumulatedDepreciation": 25000000,
        "netBookValue": 0
      }
    ]
  }
}
```

---

### GET /api/assets/{id}/history
**Description:** Get the lifecycle event history of an asset (status changes, handovers, etc.).
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, FINANCE_AUDIT

**Path param:** `id` — UUID of the asset

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "uuid",
      "eventType": "CREATED",
      "description": "Tài sản được tạo mới",
      "performedBy": "admin",
      "occurredAt": "2024-01-15T08:00:00"
    },
    {
      "id": "uuid",
      "eventType": "STATUS_CHANGED",
      "description": "Trạng thái thay đổi: IDLE -> IN_USE",
      "performedBy": "asset.manager",
      "occurredAt": "2024-01-20T09:30:00"
    }
  ]
}
```

**Event types:** `CREATED` | `STATUS_CHANGED` | `COST_UPDATED` | `REVALUED` | `TRANSFERRED` | `DEPRECIATION_POSTED` | `LIQUIDATED`

---

## MODULE: Consumable Stock (CS)

### GET /api/materials
**Description:** List material catalogue. Supports pagination and filtering.
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE, APPROVING_AUTH, FINANCE_AUDIT

**Query params:**
- `page` (int, default 0)
- `size` (int, default 20)
- `categoryCode` (string, optional) — e.g. `OFFICE`
- `keyword` (string, optional) — search by material code or name

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "40000000-0000-0000-0000-000000000001",
        "materialCode": "VT-2024-001",
        "name": "Giấy A4 IK Premium 70gsm",
        "category": "Văn phòng phẩm",
        "categoryCode": "OFFICE",
        "unitOfMeasure": "Ream",
        "unitPrice": 85000,
        "minimumStock": 10.000,
        "currentBalance": 45.000
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### POST /api/materials
**Description:** Create a new material in the catalogue.
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE

**Request body:**
```json
{
  "materialCode": "VT-2024-002",
  "name": "Bút bi Thiên Long TL-027",
  "categoryCode": "OFFICE",
  "unitOfMeasure": "Box",
  "unitPrice": 35000,
  "minimumStock": 5.000
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo vật tư thành công.",
  "data": {
    "id": "40000000-0000-0000-0000-000000000002",
    "materialCode": "VT-2024-002",
    "name": "Bút bi Thiên Long TL-027",
    "category": "Văn phòng phẩm",
    "categoryCode": "OFFICE",
    "unitOfMeasure": "Box",
    "unitPrice": 35000,
    "minimumStock": 5.000,
    "currentBalance": 0.000
  }
}
```

**Response 400 (duplicate code):**
```json
{
  "success": false,
  "message": "Mã vật tư đã tồn tại: VT-2024-002"
}
```

---

### PUT /api/materials/{id}
**Description:** Update a material's catalogue information.
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE

**Path param:** `id` — UUID of the material

**Request body:** (all fields optional — only send what to update)
```json
{
  "name": "Bút bi Thiên Long TL-027 (Updated)",
  "unitPrice": 38000,
  "minimumStock": 10.000
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật vật tư thành công.",
  "data": {
    "id": "40000000-0000-0000-0000-000000000002",
    "materialCode": "VT-2024-002",
    "name": "Bút bi Thiên Long TL-027 (Updated)",
    "unitPrice": 38000,
    "minimumStock": 10.000
  }
}
```

---

### POST /api/stock/receipt
**Description:** Record a stock-in transaction (materials received from supplier).
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE

**Request body:**
```json
{
  "storageLocationCode": "KHO-01",
  "documentRef": "HD-2024-0601",
  "documentDate": "2024-06-01",
  "supplier": "Công ty TNHH Văn phòng phẩm Hà Nội",
  "notes": "Nhập hàng tháng 6",
  "items": [
    {
      "materialId": "40000000-0000-0000-0000-000000000001",
      "quantity": 50.000,
      "unitPrice": 85000
    },
    {
      "materialId": "40000000-0000-0000-0000-000000000002",
      "quantity": 20.000,
      "unitPrice": 35000
    }
  ]
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Nhập kho thành công.",
  "data": {
    "transactionIds": ["uuid-1", "uuid-2"],
    "storageLocationCode": "KHO-01",
    "documentRef": "HD-2024-0601",
    "documentDate": "2024-06-01",
    "supplier": "Công ty TNHH Văn phòng phẩm Hà Nội",
    "totalValue": 4950000,
    "createdBy": "warehouse",
    "items": [
      {
        "materialId": "40000000-0000-0000-0000-000000000001",
        "materialCode": "VT-2024-001",
        "materialName": "Giấy A4 IK Premium 70gsm",
        "quantity": 50.000,
        "unitPrice": 85000,
        "totalValue": 4250000
      },
      {
        "materialId": "40000000-0000-0000-0000-000000000002",
        "materialCode": "VT-2024-002",
        "materialName": "Bút bi Thiên Long TL-027",
        "quantity": 20.000,
        "unitPrice": 35000,
        "totalValue": 700000
      }
    ]
  }
}
```

---

### POST /api/stock/issue
**Description:** Record a stock-out transaction (materials issued to a department).
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE

**Request body:**
```json
{
  "storageLocationCode": "KHO-01",
  "documentRef": "XK-REQ-2024-0605",
  "documentDate": "2024-06-05",
  "receivingUnitCode": "PKD",
  "requestedBy": "Nguyễn Văn B",
  "notes": "Cấp phát văn phòng phẩm tháng 6",
  "items": [
    {
      "materialId": "40000000-0000-0000-0000-000000000001",
      "quantity": 10.000,
      "unitPrice": 85000
    }
  ]
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Xuất kho thành công.",
  "data": {
    "transactionIds": ["uuid-of-issue"],
    "storageLocationCode": "KHO-01",
    "documentRef": "XK-REQ-2024-0605",
    "documentDate": "2024-06-05",
    "receivingUnit": "Phòng Kinh doanh",
    "receivingUnitCode": "PKD",
    "requestedBy": "Nguyễn Văn B",
    "totalValue": 850000,
    "createdBy": "warehouse",
    "items": [
      {
        "materialId": "40000000-0000-0000-0000-000000000001",
        "materialCode": "VT-2024-001",
        "materialName": "Giấy A4 IK Premium 70gsm",
        "quantity": 10.000,
        "unitPrice": 85000,
        "totalValue": 850000
      }
    ]
  }
}
```

**Response 400 (insufficient stock):**
```json
{
  "success": false,
  "message": "Số lượng tồn kho không đủ cho vật tư: VT-2024-001. Tồn kho hiện tại: 5.000, yêu cầu: 10.000"
}
```

---

### GET /api/stock/balance
**Description:** Get real-time stock balance per material.
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE, APPROVING_AUTH, FINANCE_AUDIT

**Query params:**
- `page` (int, default 0)
- `size` (int, default 20)
- `categoryCode` (string, optional)
- `belowMinimum` (boolean, optional) — if true, return only materials below minimum stock

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "materialId": "40000000-0000-0000-0000-000000000001",
        "materialCode": "VT-2024-001",
        "materialName": "Giấy A4 IK Premium 70gsm",
        "unitOfMeasure": "Ream",
        "currentBalance": 45.000,
        "minimumStock": 10.000,
        "isBelowMinimum": false,
        "lastUpdated": "2024-06-05T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### GET /api/stock/usage
**Description:** Get department consumption per period.
**Auth required:** Yes — SYSTEM_ADMIN, WAREHOUSE, FINANCE_AUDIT, APPROVING_AUTH

**Query params:**
- `page` (int, default 0)
- `size` (int, default 20)
- `unitCode` (string, optional) — filter by department code
- `materialId` (UUID, optional) — filter by material
- `from` (date, required) — start date e.g. `2024-01-01`
- `to` (date, required) — end date e.g. `2024-06-30`

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "unitCode": "PKD",
        "unitName": "Phòng Kinh doanh",
        "materialCode": "VT-2024-001",
        "materialName": "Giấy A4 IK Premium 70gsm",
        "unitOfMeasure": "Ream",
        "totalQuantity": 30.000,
        "totalValue": 2550000
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## MODULE: Handover & Liquidation (HL)

### POST /api/handovers
**Description:** Initiate an asset handover request.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER

**Request body:**
```json
{
  "assetId": "30000000-0000-0000-0000-000000000001",
  "fromUnitCode": "PHKT",
  "toUnitCode": "PKD",
  "handoverDate": "2024-07-01",
  "assetCondition": "GOOD",
  "reason": "Điều chuyển phục vụ nhu cầu công tác phòng kinh doanh",
  "notes": null
}
```

**Asset condition values:** `GOOD` | `FAIR` | `POOR`

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo yêu cầu bàn giao thành công.",
  "data": {
    "id": "uuid-of-handover",
    "requestCode": "BG-2024-001",
    "assetId": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "assetName": "Máy tính xách tay Dell Latitude 5540",
    "fromUnit": "Phòng Hành chính - Kỹ thuật",
    "fromUnitCode": "PHKT",
    "toUnit": "Phòng Kinh doanh",
    "toUnitCode": "PKD",
    "handoverDate": "2024-07-01",
    "assetCondition": "GOOD",
    "reason": "Điều chuyển phục vụ nhu cầu công tác phòng kinh doanh",
    "status": "PENDING_APPROVAL",
    "initiatedBy": "asset.manager",
    "createdAt": "2024-06-20T09:00:00"
  }
}
```

**Response 400 (asset not transferable):**
```json
{
  "success": false,
  "message": "Tài sản không thể bàn giao khi đang ở trạng thái: MAINTENANCE"
}
```

---

### GET /api/handovers
**Description:** List handover requests. Supports pagination and filtering.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH

**Query params:**
- `page` (int, default 0)
- `size` (int, default 20)
- `status` (string, optional) — `DRAFT` | `PENDING_APPROVAL` | `APPROVED` | `CONFIRMED` | `COMPLETED` | `REJECTED`
- `fromUnitCode` (string, optional)
- `toUnitCode` (string, optional)

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "uuid-of-handover",
        "requestCode": "BG-2024-001",
        "assetCode": "TS-2024-001",
        "assetName": "Máy tính xách tay Dell Latitude 5540",
        "fromUnitCode": "PHKT",
        "toUnitCode": "PKD",
        "handoverDate": "2024-07-01",
        "assetCondition": "GOOD",
        "status": "PENDING_APPROVAL",
        "initiatedBy": "asset.manager",
        "createdAt": "2024-06-20T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### GET /api/handovers/{id}
**Description:** Get full detail of a handover request.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH

**Path param:** `id` — UUID of the handover

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "uuid-of-handover",
    "requestCode": "BG-2024-001",
    "assetId": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "assetName": "Máy tính xách tay Dell Latitude 5540",
    "fromUnit": "Phòng Hành chính - Kỹ thuật",
    "fromUnitCode": "PHKT",
    "toUnit": "Phòng Kinh doanh",
    "toUnitCode": "PKD",
    "handoverDate": "2024-07-01",
    "assetCondition": "GOOD",
    "reason": "Điều chuyển phục vụ nhu cầu công tác phòng kinh doanh",
    "notes": null,
    "status": "PENDING_APPROVAL",
    "initiatedBy": "asset.manager",
    "deptApprovedBy": null,
    "deptApprovedAt": null,
    "deptApprovalNotes": null,
    "confirmedBy": null,
    "confirmedAt": null,
    "confirmationNotes": null,
    "completedBy": null,
    "completedAt": null,
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "documentRef": null,
    "documentSigned": false,
    "createdAt": "2024-06-20T09:00:00"
  }
}
```

---

### PATCH /api/handovers/{id}/approve
**Description:** Approving authority approves the handover request.
**Auth required:** Yes — APPROVING_AUTH, SYSTEM_ADMIN

**Path param:** `id` — UUID of the handover

**Request body:**
```json
{
  "note": "Đồng ý điều chuyển."
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Phê duyệt bàn giao thành công.",
  "data": {
    "id": "uuid-of-handover",
    "status": "APPROVED",
    "approvedBy": "approver",
    "approvedAt": "2024-06-21T10:00:00"
  }
}
```

---

### PATCH /api/handovers/{id}/confirm
**Description:** Receiving unit confirms receipt of the asset. Completes the handover.
**Auth required:** Yes — ASSET_MANAGER, SYSTEM_ADMIN

**Path param:** `id` — UUID of the handover

**Request body:**
```json
{
  "note": "Đã nhận tài sản, tình trạng tốt."
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Xác nhận bàn giao thành công.",
  "data": {
    "id": "uuid-of-handover",
    "status": "COMPLETED",
    "confirmedBy": "asset.manager",
    "confirmedAt": "2024-07-01T14:00:00"
  }
}
```

---

### PATCH /api/handovers/{id}/reject
**Description:** Reject a handover request.
**Auth required:** Yes — APPROVING_AUTH, SYSTEM_ADMIN

**Path param:** `id` — UUID of the handover

**Request body:**
```json
{
  "reason": "Phòng kinh doanh chưa có nhu cầu thực sự."
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Từ chối bàn giao thành công.",
  "data": {
    "id": "uuid-of-handover",
    "status": "REJECTED",
    "rejectionReason": "Phòng kinh doanh chưa có nhu cầu thực sự."
  }
}
```

---

### GET /api/handovers/{id}/document
**Description:** Download the official handover PDF document.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH

**Path param:** `id` — UUID of the handover (must be COMPLETED)

**Response 200:**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="BG-2024-001.pdf"`
- Body: binary PDF stream

**Response 400 (not completed):**
```json
{
  "success": false,
  "message": "Chỉ có thể xuất tài liệu khi bàn giao đã hoàn thành."
}
```

---

### POST /api/liquidations
**Description:** Submit an asset liquidation request.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER

**Request body:**
```json
{
  "assetId": "30000000-0000-0000-0000-000000000001",
  "justification": "Tài sản hết khấu hao, hỏng không sửa được",
  "assetCondition": "DAMAGED",
  "currentMarketValue": 500000,
  "disposalMethod": "SCRAP",
  "disposalNotes": "Bán phế liệu"
}
```

**Asset condition values:** `GOOD` | `FAIR` | `POOR` | `DAMAGED`
**Disposal methods:** `AUCTION` | `SCRAP` | `DONATION`

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo yêu cầu thanh lý thành công.",
  "data": {
    "id": "uuid-of-liquidation",
    "requestCode": "TL-2024-001",
    "assetId": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "assetName": "Máy tính xách tay Dell Latitude 5540",
    "justification": "Tài sản hết khấu hao, hỏng không sửa được",
    "assetCondition": "DAMAGED",
    "currentMarketValue": 500000,
    "disposalMethod": "SCRAP",
    "status": "PENDING_MANAGER",
    "initiatedBy": "asset.manager",
    "createdAt": "2024-07-10T09:00:00"
  }
}
```

---

### GET /api/liquidations
**Description:** List liquidation requests. Supports pagination and filtering.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH

**Query params:**
- `page` (int, default 0)
- `size` (int, default 20)
- `status` (string, optional) — `DRAFT` | `PENDING_MANAGER` | `PENDING_DIRECTOR` | `APPROVED` | `COMPLETED` | `REJECTED`

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "uuid-of-liquidation",
        "requestCode": "TL-2024-001",
        "assetCode": "TS-2024-001",
        "assetName": "Máy tính xách tay Dell Latitude 5540",
        "assetCondition": "DAMAGED",
        "currentMarketValue": 500000,
        "disposalMethod": "SCRAP",
        "status": "PENDING_MANAGER",
        "initiatedBy": "asset.manager",
        "createdAt": "2024-07-10T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### GET /api/liquidations/{id}
**Description:** Get full detail of a liquidation request.
**Auth required:** Yes — SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH

**Path param:** `id` — UUID of the liquidation

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "uuid-of-liquidation",
    "requestCode": "TL-2024-001",
    "assetId": "30000000-0000-0000-0000-000000000001",
    "assetCode": "TS-2024-001",
    "assetName": "Máy tính xách tay Dell Latitude 5540",
    "requestingUnitCode": "PHKT",
    "justification": "Tài sản hết khấu hao, hỏng không sửa được",
    "assetCondition": "DAMAGED",
    "currentMarketValue": 500000,
    "disposalMethod": "SCRAP",
    "disposalNotes": "Bán phế liệu",
    "status": "PENDING_MANAGER",
    "initiatedBy": "asset.manager",
    "managerApprovedBy": null,
    "managerApprovedAt": null,
    "managerNotes": null,
    "directorApprovedBy": null,
    "directorApprovedAt": null,
    "directorNotes": null,
    "completedBy": null,
    "completedAt": null,
    "finalDisposalValue": null,
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "documentRef": null,
    "documentSigned": false,
    "createdAt": "2024-07-10T09:00:00"
  }
}
```

---

### PATCH /api/liquidations/{id}/approve
**Description:** Approve a liquidation request. Two-step workflow: first call moves `PENDING_MANAGER` → `PENDING_DIRECTOR` (manager approves), second call moves `PENDING_DIRECTOR` → `APPROVED` (director approves). Both steps use this same endpoint — the service determines which step applies based on current status and the caller's role.
**Auth required:** Yes — APPROVING_AUTH, SYSTEM_ADMIN

**Path param:** `id` — UUID of the liquidation

**Request body:**
```json
{
  "finalDisposalValue": 500000,
  "note": "Đồng ý thanh lý theo giá trị đề xuất."
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Phê duyệt thanh lý thành công.",
  "data": {
    "id": "uuid-of-liquidation",
    "status": "APPROVED",
    "directorApprovedBy": "approver",
    "directorApprovedAt": "2024-07-15T10:00:00",
    "finalDisposalValue": 500000
  }
}
```

---

### PATCH /api/liquidations/{id}/reject
**Description:** Reject a liquidation request.
**Auth required:** Yes — APPROVING_AUTH, SYSTEM_ADMIN

**Path param:** `id` — UUID of the liquidation

**Request body:**
```json
{
  "reason": "Tài sản có thể sửa chữa, chưa cần thanh lý."
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Từ chối thanh lý thành công.",
  "data": {
    "id": "uuid-of-liquidation",
    "status": "REJECTED",
    "rejectionReason": "Tài sản có thể sửa chữa, chưa cần thanh lý."
  }
}
```

---

## MODULE: Reports & Audit (RP)

### GET /api/audit-logs
**Description:** Paginated audit log of all system actions.
**Auth required:** Yes — SYSTEM_ADMIN, FINANCE_AUDIT

**Query params:**
- `page` (int, default 0)
- `size` (int, default 20)
- `module` (string, optional) — `ASSET` | `STOCK` | `HANDOVER` | `LIQUIDATION` | `USER` | `AUTH`
- `action` (string, optional) — `CREATE` | `UPDATE` | `DELETE` | `STATUS_CHANGE` | `LOGIN` | `LOGOUT` | `APPROVE` | `REJECT` | `CONFIRM` | `EXPORT`
- `performedBy` (string, optional) — filter by username
- `from` (date, optional) — e.g. `2024-01-01`
- `to` (date, optional) — e.g. `2024-12-31`

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "uuid",
        "module": "ASSET",
        "action": "STATUS_CHANGE",
        "recordId": "30000000-0000-0000-0000-000000000001",
        "recordCode": "TS-2024-001",
        "description": "Trạng thái thay đổi: IDLE -> IN_USE",
        "performedBy": "asset.manager",
        "performedAt": "2024-01-20T09:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### GET /api/reports/assets
**Description:** Asset inventory report — summary of all assets by status and unit.
**Auth required:** Yes — SYSTEM_ADMIN, FINANCE_AUDIT, APPROVING_AUTH

**Query params:**
- `managingUnitCode` (string, optional)
- `categoryCode` (string, optional)
- `status` (string, optional)
- `asOf` (date, optional, default today) — snapshot date e.g. `2024-06-30`

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "asOf": "2024-06-30",
    "summary": {
      "totalAssets": 10,
      "totalOriginalCost": 250000000,
      "totalAccumulatedDepreciation": 50000000,
      "totalNetBookValue": 200000000,
      "byStatus": {
        "IN_USE": 7,
        "MAINTENANCE": 1,
        "IDLE": 2,
        "TRANSFERRED": 0,
        "LIQUIDATED": 0
      }
    },
    "items": [
      {
        "assetCode": "TS-2024-001",
        "name": "Máy tính xách tay Dell Latitude 5540",
        "category": "Thiết bị công nghệ thông tin",
        "managingUnit": "Phòng Hành chính - Kỹ thuật",
        "originalCost": 25000000,
        "accumulatedDepreciation": 5000000,
        "netBookValue": 20000000,
        "acquisitionDate": "2024-01-15",
        "status": "IN_USE"
      }
    ]
  }
}
```

---

### GET /api/reports/assets/export
**Description:** Export the asset inventory report as Excel or PDF.
**Auth required:** Yes — SYSTEM_ADMIN, FINANCE_AUDIT, APPROVING_AUTH

**Query params:** (same filters as GET /api/reports/assets, plus)
- `format` (string, required) — `EXCEL` | `PDF`

**Response 200:**
- For `EXCEL`: Content-Type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, filename `asset-report.xlsx`
- For `PDF`: Content-Type `application/pdf`, filename `asset-report.pdf`
- Body: binary file stream

---

### GET /api/reports/stock
**Description:** Stock consumption report — materials issued per department per period.
**Auth required:** Yes — SYSTEM_ADMIN, FINANCE_AUDIT, APPROVING_AUTH

**Query params:**
- `from` (date, required) — e.g. `2024-01-01`
- `to` (date, required) — e.g. `2024-06-30`
- `unitCode` (string, optional)
- `categoryCode` (string, optional)

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "from": "2024-01-01",
    "to": "2024-06-30",
    "summary": {
      "totalIssuedValue": 15000000,
      "totalReceivedValue": 20000000,
      "netBalance": 5000000
    },
    "byUnit": [
      {
        "unitCode": "PKD",
        "unitName": "Phòng Kinh doanh",
        "totalIssuedValue": 5000000,
        "items": [
          {
            "materialCode": "VT-2024-001",
            "materialName": "Giấy A4 IK Premium 70gsm",
            "totalQuantity": 30.000,
            "unitOfMeasure": "Ream",
            "totalValue": 2550000
          }
        ]
      }
    ]
  }
}
```

---

### GET /api/reports/stock/export
**Description:** Export the stock consumption report as Excel.
**Auth required:** Yes — SYSTEM_ADMIN, FINANCE_AUDIT, APPROVING_AUTH

**Query params:** (same as GET /api/reports/stock, plus)
- `format` (string, required) — `EXCEL` | `PDF`

**Response 200:**
- For `EXCEL`: Content-Type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, filename `stock-report.xlsx`
- For `PDF`: Content-Type `application/pdf`, filename `stock-report.pdf`
- Body: binary file stream

---

## SHARED: Common response shapes

### Validation error (400)
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ.",
  "data": {
    "username": "Tên đăng nhập không được để trống.",
    "password": "Mật khẩu phải có ít nhất 8 ký tự."
  }
}
```

### Unauthorized (401)
```json
{
  "success": false,
  "message": "Unauthorized"
}
```

### Forbidden (403)
```json
{
  "success": false,
  "message": "Bạn không có quyền thực hiện thao tác này."
}
```

### Not found (404)
```json
{
  "success": false,
  "message": "Không tìm thấy tài nguyên với id: <id>"
}
```

---

## STATUS LABELS (Vietnamese)

### Asset status
```
IN_USE       -> Đang sử dụng
MAINTENANCE  -> Đang bảo trì
IDLE         -> Chờ phân bổ
TRANSFERRED  -> Đã bàn giao
LIQUIDATED   -> Đã thanh lý
```

### Handover status
```
DRAFT            -> Nháp
PENDING_APPROVAL -> Chờ phê duyệt
APPROVED         -> Đã phê duyệt
CONFIRMED        -> Đã xác nhận
COMPLETED        -> Hoàn thành
REJECTED         -> Đã từ chối
```

### Liquidation status
```
DRAFT            -> Nháp
PENDING_MANAGER  -> Chờ quản lý
PENDING_DIRECTOR -> Chờ giám đốc
APPROVED         -> Đã phê duyệt
COMPLETED        -> Hoàn thành
REJECTED         -> Đã từ chối
```

---

## MOCK DATA for frontend
# Use these while waiting for real backends to be ready.
# Shape matches exactly what the real API will return.

### Mock user (after login)
```json
{
  "id": "10000000-0000-0000-0000-000000000001",
  "username": "admin",
  "fullName": "Lê Việt Cường",
  "email": "admin@soe.vn",
  "isActive": true,
  "roles": ["SYSTEM_ADMIN"],
  "managingUnitCodes": ["HQ"]
}
```

### Mock asset list item
```json
{
  "id": "30000000-0000-0000-0000-000000000001",
  "assetCode": "TS-2024-001",
  "name": "Máy tính xách tay Dell Latitude 5540",
  "category": "Thiết bị công nghệ thông tin",
  "managingUnit": "Phòng Hành chính - Kỹ thuật",
  "originalCost": 25000000,
  "accumulatedDepreciation": 5000000,
  "netBookValue": 20000000,
  "acquisitionDate": "2024-01-15",
  "status": "IN_USE"
}
```

### Mock material list item
```json
{
  "id": "40000000-0000-0000-0000-000000000001",
  "materialCode": "VT-2024-001",
  "name": "Giấy A4 IK Premium 70gsm",
  "category": "Văn phòng phẩm",
  "unitOfMeasure": "Ream",
  "unitPrice": 85000,
  "currentBalance": 45.000
}
```