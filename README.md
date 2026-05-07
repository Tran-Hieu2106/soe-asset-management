# SOE Asset Management System

A standardized platform for managing fixed assets, consumable stock, handovers, and liquidations for State-Owned Enterprises (SOEs), ensuring compliance with national accounting and equipment oversight regulations.

---

## Team & Ownership

| Member | Role | Responsible For |
|--------|------|-----------------|
| **M1** | Project Manager · Backend Foundation | Auth, RBAC, DB schema, config, shared infra |
| **M2** | Fixed Assets Module | FA-01 to FA-04 — asset CRUD, depreciation |
| **M3** | Consumable Stock Module | CS-01 to CS-04 — materials, stock transactions |
| **M4** | Handover · Liquidation · Audit · Reporting | HL-01 to HL-03, RP-01 to RP-03 |
| **M5** | Frontend (All Modules) | React + TypeScript + Ant Design UI |

---

## Project Structure

```
soe-asset-management/
├── backend/        -> Spring Boot REST API      [M1 foundation, M2/M3/M4 modules]
├── frontend/       -> React + TypeScript (Vite) [M5]
└── docs/           -> API spec, DB schema, diagrams [shared]
```

---

## Getting Started

### Prerequisites
- Java 17+, Maven 3.8+
- Node.js 18+, npm 9+
- Docker & Docker Compose
- PostgreSQL (or run via Docker)

### Run the full stack locally
```bash
docker-compose up --build
```

- Backend API -> `http://localhost:8080`
- Frontend    -> `http://localhost:5173`

### Run individually
```bash
# Backend
cd backend && ./mvnw spring-boot:run    

# Frontend
cd frontend && npm install && npm run dev
```

---

## Key Docs

| Document | Location | Owner |
|----------|----------|-------|
| API Specification | `docs/api-spec.md` | M1 |
| Database Schema | `docs/database-schema.md` | M1 |
| ERD Diagram | `docs/diagrams/erd.png` | M1 |
| Architecture Diagram | `docs/diagrams/architecture.png` | Shared |
| Deployment Guide | `docs/deployment-guide.md` | M5 |
| Test Plan | `docs/test-plan.md` | M5 |