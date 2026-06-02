# Backend — Spring Boot

---

## What This Layer Does

The backend is a Spring Boot REST API that:
- Authenticates users via JWT and enforces role-based access (RBAC)
- Exposes endpoints for all modules (assets, stock, handover, liquidation, reports)
- Persists data to PostgreSQL via JPA/Hibernate
- Tracks all changes through an audit log

---

## Package Structure

```
src/main/java/vn/edu/hust/soict/soe/assetmanagement/
│
├── config/         → Security, JWT, CORS, Audit setup       [M1]
├── common/         → ApiResponse, PageResponse, BaseEntity   [M1]
├── exception/      → Shared exception classes                [M1]
│
├── auth/           → Login, JWT filter, token service        [M1]
├── user/           → User & Role management                  [M1]
│
├── asset/          → Fixed assets + depreciation             [M2]
├── stock/          → Materials + stock transactions          [M3]
│
├── handover/       → Handover requests & approval flow       [M4]
├── liquidation/    → Liquidation requests & approval flow    [M4]
├── audit/          → Audit log queries                       [M4]
└── report/         → Asset & stock report generation         [M4]
```

---

## Module Development Guide

Each module follows the **same 4-layer pattern**.

```
[your-module]/
├── controller/   → @RestController — receives HTTP requests, calls service
├── service/      → Business logic only — no DB calls here directly
├── repository/   → @Repository — JPA queries only
├── entity/       → @Entity — maps to DB table, extends BaseEntity
└── dto/          → Request/Response objects — never expose entities directly
```

### Rules
- Always extend `BaseEntity` for your entities (gives you `id`, `createdAt`, `updatedAt`, `createdBy` for free)
- Always return `ApiResponse<T>` from your controllers
- Never expose JPA entity objects directly in API responses — use DTOs
- Throw `ResourceNotFoundException` or `BusinessRuleException` for errors — do not return raw 500s
- All endpoints must be secured — check `SecurityConfig` to understand which roles can access what

---

## Database Migrations

Migrations live in `src/main/resources/db/migration/` and run automatically via Flyway on startup.

| File | Purpose | Owner |
|------|---------|-------|
| `V1__create_users_roles.sql` | users, roles, managing_units tables | M1 |
| `V2__create_assets.sql` | assets, asset_history tables | M2 |
| `V3__create_stock.sql` | materials, stock_transactions tables | M3 |
| `V4__create_handover_liquidation.sql` | handover & liquidation tables | M4 |
| `V5__create_audit_log.sql` | audit_log table | M4 |
| `V6__seed_data.sql` | test/demo data | M1 |

---

## Configuration

| File | Purpose |
|------|---------|
| `application.yml` | Base config (port, JPA, Flyway) |
| `application-dev.yml` | Local dev overrides (DB URL, dev credentials) |

---

## Testing

Each module owner writes unit tests for their own services.

```
test/java/vn/edu/soe/assetmanagement/
├── asset/    → AssetServiceTest, DepreciationServiceTest   [M2]
├── stock/    → MaterialServiceTest, StockTransactionServiceTest [M3]
└── handover/ → HandoverServiceTest                         [M4]
```

Run all tests: `mvn test`