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
- PostgreSQL run via Docker

### Run the full stack locally

Start the database, backend, and frontend simultaneously.

```bash
docker-compose up --build
```
- Database    -> `http://localhost:5432`
- Backend API -> `http://localhost:8080`
- Frontend    -> `http://localhost:5173`

### Run individually
Run the frontend and backend separately.

#### Backend
```bash
# Database via Docker
docker-compose up -d

cd backend 
./mvnw clean install -DskipTests # Install dependencies
./mvnw spring-boot:run # Start the server
```

The backend will be available at `http://localhost:8080`.

#### Frontend
```bash
cd frontend 
npm install # Install dependencies
npm run dev # Start the development server
```

The frontend will be available at `http://localhost:5173`.

---

## Testing

### Backend (Spring Boot)
The backend test suite is powered by JUnit 5, Mockito, and an in-memory **H2 Database**.

```bash
cd backend

# Run the entire test suite
./mvnw clean test

# Run a specific test class (e.g., StockTransactionRepositoryTest)
./mvnw test -Dtest=StockTransactionRepositoryTest
```

### Frontend (React + Vite)
```bash
cd frontend

# Run all UI unit tests
npm run test

# Run tests in watch mode (for active development)
npm run test:watch
```

---

### API Documentation & Testing

#### Swagger UI

Once the Spring Boot backend is running, access the auto-generated API documentation at:
`http://localhost:8080/swagger-ui/index.html`

**Authentication:** To test secured endpoints directly in the browser, click the **Authorize** padlock button at the top of the Swagger UI and paste your JWT token. Swagger will automatically inject the `Authorization: Bearer <token>` header into all requests.

---

# Documents

| Document | Location | Owner |
|----------|----------|-------|
| API Specification | `docs/api-spec.md` | M1 |
| Database Schema | `docs/database-schema.md` | M1 |
| ERD Diagram | `docs/diagrams/erd.png` | M1 |
| Architecture Diagram | `docs/diagrams/architecture.png` | Shared |
| Deployment Guide | `docs/deployment-guide.md` | M5 |
| Test Plan | `docs/test-plan.md` | M5 |