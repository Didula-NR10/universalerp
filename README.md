# Universal ERP

Single-company ERP with a Spring Boot API, PostgreSQL schema and React frontend. It includes organization, identity, inventory, contacts, procurement, sales/POS, expenses, employees, reporting, notifications and audit features. See [status](docs/development/status.md) for tested scope and gaps.

## Development phases

| Phase | Development | Status |
| --- | --- | --- |
| Phase 1 | Project foundation | ✅ Completed |
| Phase 2 | Supabase database integration | 🟡 Integrated; further verification pending |
| Phase 3 | Authentication and authorization | 🟡 ERP login implemented; security verification pending |
| Phase 4 | Dashboard and application shell | 🟡 Implemented; data verification pending |
| Phase 5 | Organization and employee management | 🟡 Partially complete |
| Phase 6 | Inventory management | 🟡 Partially complete |
| Phase 7 | Procurement management | 🟡 Partially complete |
| Phase 8 | Sales management | 🟡 Partially complete |
| Phase 9 | POS functionality (cash only) | 🟡 Partially complete |
| Phase 10 | Finance management | 🟡 Partially complete |
| Phase 11 | Reporting and analytics | 🟡 Partially complete |
| Phase 12 | Testing, security, and deployment | 🟡 Partially complete |

Flyway V1-V13 are applied to the approved Supabase PostgreSQL test project. The API now authenticates ERP users itself; PostgreSQL login, password-change, product and cash sale/refund integration tests passed with test writes rolled back. See the [detailed status](docs/development/status.md) for what remains.

## Requirements

- JDK 17
- Node.js 20.19+ and npm
- Supabase PostgreSQL for business data; authentication is handled by the ERP API

## Run without credentials

From `backend` in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'foundation'
.\mvnw.cmd spring-boot:run
```

From `frontend`:

```powershell
npm.cmd ci
npm.cmd run dev
```

The foundation profile serves health and status only. The frontend shows its setup state until Supabase settings are supplied.

## Run the full application

Configure Supabase PostgreSQL and `AUTH_JWT_SECRET` in the root `.env` as in [setup](docs/development/setup.md). Start the backend from `backend` with `.\mvnw.cmd spring-boot:run`; it loads the root `.env` automatically. Run without the `foundation` profile and configure the frontend to call the local backend API. Flyway applies pending migrations at startup; V1-V13 are already applied to the approved ERP test project. The browser no longer uses Supabase Auth or a Supabase publishable key.

## Verify

```powershell
cd backend
.\mvnw.cmd clean verify
cd ..\frontend
npm.cmd ci
npm.cmd run test
npm.cmd run build
```

Database-backed verification is in [testing](docs/development/testing.md).
