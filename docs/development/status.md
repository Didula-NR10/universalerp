# Implementation status

The Supabase PostgreSQL test database is connected and Flyway V1-V13 have been applied to its `public` schema. The ERP API now owns email/password authentication and no longer uses Supabase Auth. The backend starts with Hibernate schema validation. This verifies the schema and tested workflows, not every business module.

| Phase | Status | Verified | Remaining |
| --- | --- | --- | --- |
| 1 Foundation | Implemented | Backend foundation tests and client build | Hosting verification |
| 2 Supabase database | Partially complete | JDBC connection, Flyway V1-V13, JPA validation, transactional auth/repository/sales tests, API health 200 | Clean database replay, backup/restore, runtime grants |
| 3 Authentication | Partially complete | ERP-owned BCrypt login, session restoration, password change, logout, account lockout, admin user creation/reset; live HTTP login and PostgreSQL auth tests | Password recovery for first admin, gateway rate limiting, cross-branch authorization tests |
| 4 Dashboard | Implemented; data verification pending | Backend queries and frontend build | Reconcile metrics against real transactions |
| 5 Organization and HR | Partially complete | Organization, employees, attendance, leave, scheduling schema/API/UI | Payroll, availability enforcement, complete HR UI and tests |
| 6 Inventory | Partially complete | Catalog, stock ledger, transfers and policy tests | Valuation, database concurrency and reconciliation tests |
| 7 Procurement | Partially complete | Orders, approval/rejection/cancellation, partial receiving and UI | Requisitions, returns and database tests |
| 8 Sales | Partially complete | PostgreSQL cash sale, idempotent retry, two partial refunds, status and stock restoration tests passed | Full refund, error rollback, concurrent request and financial reconciliation tests |
| 9 POS | Partially complete | Cash checkout code and UI | Register shifts, cash movements and reconciliation; cash only per user scope |
| 10 Finance | Partially complete | Expense workflow and tests | General ledger, AP, AR and reconciliation |
| 11 Reporting | Partially complete | Dashboard, branch overview, sales CSV | Additional reports and live data reconciliation |
| 12 Testing, security, deployment | Partially complete | Backend/frontend tests, three PostgreSQL integration tests, local-auth HTTP check, static review, image definitions | Other workflow/API/e2e tests, Docker build, deployment and operational checks |

## Verified in this workspace

- The supplied Supabase PostgreSQL database was initially empty. Flyway applied V1-V13 to the approved test project. Flyway reported version 13 and Hibernate validated entity mappings.
- The normal API started against that database on a local port. `/actuator/health` returned HTTP 200; `/api/v1/me` without a token returned HTTP 401. The local process was stopped after the check.
- Backend Maven verification and the frontend TypeScript/Vite build passed. All three guarded PostgreSQL tests passed separately against the approved test project with rolled back inserts.
- The PostgreSQL sales/refund test found the V10 status-width defect. V12 fixed it; rerun passed for two partial refunds and stock restoration.
- Live ERP-owned HTTP login succeeded. Session restoration matched the administrator profile; business access returned 403 until the first-login password change. Supabase Auth is no longer part of login.
- Docker is unavailable locally, so images were not built. No production deployment was performed.

Outstanding work is detailed in [completion checklist](completion-checklist.md).
