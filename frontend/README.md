# Frontend — React + TypeScript

**Owner:** M5
**Stack:** React 19 · TypeScript · Vite · Ant Design 6 · Axios · Zustand

---

## What This Layer Does

The frontend is the single UI for all modules. M4 is responsible for the entire `frontend/` directory — building all pages and wiring them to the backend APIs delivered by M1, M2, M3, and M4.

---

## Folder Structure

```
src/
├── api/          → One Axios file per backend module
├── components/   → Shared UI pieces reused across pages
├── pages/        → One folder per module, one file per screen
├── store/        → Global state (auth session, notifications)
├── types/        → TypeScript interfaces for all data shapes
└── utils/        → Small helper functions
```

---

## Development Order (Recommended)

1. **Foundation first** — `axiosInstance.ts`, `authStore.ts`, `AppLayout.tsx`, `Sidebar.tsx`, `LoginPage.tsx`
2. **Types second** — define all interfaces in `types/` based on `docs/api-spec.md` (Week 2 milestone)
3. **Module by module** — build pages in this order as backends become ready:
   - Fixed Assets (M2 delivers first)
   - Consumable Stock (M3)
   - Handover & Liquidation (M4)
   - Reports & Audit Log (M4)
   * Before running, copy .env.example to .env: cp .env.example .env
   

---

## `api/` — Backend Communication

One file per module. Each file exports typed async functions that call the backend.

| File | Calls |
|------|-------|
| `axiosInstance.ts` | Base Axios setup — attaches JWT token to every request automatically |
| `authApi.ts` | `login()`, `logout()` |
| `assetApi.ts` | `getAssets()`, `getAssetById()`, `createAsset()`, `updateAsset()`, etc. |
| `stockApi.ts` | `getMaterials()`, `getStockBalance()`, `createReceipt()`, `createIssue()`, etc. |
| `handoverApi.ts` | `getHandovers()`, `createHandover()`, `approveHandover()`, etc. |
| `reportApi.ts` | `getAssetReport()`, `getStockReport()`, `exportReport()` |

**Rule:** Pages never call `axios` directly. They always go through these API files.

---

## `components/` — Shared UI

Build these early — every page depends on them.

| Component | Purpose |
|-----------|---------|
| `AppLayout.tsx` | Main shell: sidebar + topbar + content area |
| `Sidebar.tsx` | Navigation menu with role-aware items (hide links the user can't access) |
| `PageHeader.tsx` | Consistent title + breadcrumb bar for all pages |
| `StatusBadge.tsx` | Colored badge for asset/handover/liquidation statuses |
| `ConfirmModal.tsx` | Reusable "Are you sure?" modal for delete/approve/reject actions |
| `ExportButton.tsx` | Button that triggers a report download from the backend |

---

## `pages/` — Screens to Build

### Auth
| File | What It Shows |
|------|-------------|
| `LoginPage.tsx` | Username + password form; stores JWT in `authStore` on success |

### Assets
| File | What It Shows |
|------|-------------|
| `AssetListPage.tsx` | Table of all assets, filterable by status and managing unit |
| `AssetDetailPage.tsx` | Full profile of a single asset with depreciation history |
| `AssetFormPage.tsx` | Create / Edit form for an asset |
| `AssetHistoryPage.tsx` | Timeline of all changes made to a specific asset |

### Stock
| File | What It Shows |
|------|-------------|
| `MaterialListPage.tsx` | Catalogue of materials with search |
| `StockReceiptPage.tsx` | Form to record incoming stock |
| `StockIssuePage.tsx` | Form to issue stock to a department |
| `StockBalancePage.tsx` | Table of current stock levels per material |

### Handover & Liquidation
| File | What It Shows |
|------|-------------|
| `HandoverListPage.tsx` | All handover requests with status badges |
| `HandoverFormPage.tsx` | Submit a new handover request |
| `HandoverDetailPage.tsx` | View a request and approve/reject (role-gated) |
| `LiquidationListPage.tsx` | All liquidation requests with status badges |
| `LiquidationFormPage.tsx` | Submit a new liquidation request |

### Reports
| File | What It Shows |
|------|-------------|
| `AssetReportPage.tsx` | Asset register report with export button |
| `StockReportPage.tsx` | Stock balance and usage report with export |
| `AuditLogPage.tsx` | Searchable audit log table (filter by user, date, action) |

---

## `types/` — TypeScript Interfaces

Define these in Week 2 — agreed with the whole team based on `docs/api-spec.md`.

| File | Contains |
|------|---------|
| `auth.types.ts` | `User`, `LoginRequest`, `LoginResponse` |
| `asset.types.ts` | `Asset`, `AssetHistory`, `CreateAssetRequest`, `DepreciationDto` |
| `stock.types.ts` | `Material`, `StockTransaction`, `StockBalance`, `DepartmentUsage` |
| `handover.types.ts` | `HandoverRequest`, `LiquidationRequest` and their statuses |
| `report.types.ts` | `AssetReport`, `StockReport`, `AuditLog` |

---

## `store/` — Global State

| File | Stores |
|------|--------|
| `authStore.ts` | Logged-in user, JWT token, role — persisted to localStorage |
| `notificationStore.ts` | Toast/alert queue for success and error messages |

---

## `utils/` — Helpers

| File | Does |
|------|------|
| `formatCurrency.ts` | Format numbers as Vietnamese Dong (VND) |
| `formatDate.ts` | Format ISO dates for display |
| `roleGuard.ts` | Helper to check if the current user has a required role — use in `Sidebar` and on action buttons |

---

## Key Rules

- Use **Ant Design** components throughout — do not mix in other UI libraries
- Use `roleGuard.ts` to hide or disable actions the user's role cannot perform (e.g. approve buttons for non-managers)
- Handle API errors globally in `axiosInstance.ts` — show a notification via `notificationStore` for 401/403/500
- All date display must go through `formatDate.ts`
- All money/value display must go through `formatCurrency.ts`