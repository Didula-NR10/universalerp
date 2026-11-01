# Completion checklist

A passing compile or API startup does not complete a workflow. Each module needs persistence, authorization, frontend and database backed verification.

| Phase | Implemented | Remaining |
| --- | --- | --- |
| 2 Database | Flyway V1-V13 applied to the approved Supabase PostgreSQL test project; JDBC/JPA startup and rolled back auth/product/sales/refund writes verified | Clean database replay, backup/restore and runtime grants |
| 3 Identity | ERP-owned password hashes, JWTs, first-login change, session revocation, account lockout, local user creation/reset and live login check | First-admin recovery, per-IP rate limiting, role escalation and cross-branch API tests |
| 4 Dashboard | Backend branch/company metrics and authenticated page | Reconcile metrics against real transactions |
| 5 Organization/HR | Company, branch, warehouse, department, employee, attendance, leave and scheduling API/UI | Payroll, availability enforcement, complete HR UI and tests |
| 6 Inventory | Catalog, locked balances, movements, idempotency, transfers and alerts | PostgreSQL concurrency/rollback checks, valuation and ledger reconciliation |
| 7 Procurement | Orders, approval/rejection/cancellation, partial receiving and multi-item UI | Requisitions, purchase returns and database concurrency tests |
| 8 Sales | Cash sale and partial refunds verified on PostgreSQL with idempotency, approval and stock restoration | Full refund, error rollback, concurrency and financial reconciliation tests |
| 9 POS | Cash checkout | Register shifts, cash movements and reconciliation; cash only per user scope |
| 10 Finance | Expense workflow | General ledger, AP, AR, reconciliation and accounting tests |
| 11 Reporting | Dashboard, branch overview and sales CSV | Additional reports, filters, exports and live reconciliation |
| 12 Quality | Foundation/domain tests, CI build, Dockerfiles and static security review | Database/API/e2e tests, Docker image build and deployment |

## Current evidence and limits

- Flyway reached version 13 in the connected Supabase PostgreSQL test project. Hibernate validated its mapped tables and the normal API started. Health returned 200; unauthenticated `/api/v1/me` returned 401.
- The guarded PostgreSQL auth, repository and sales/refund tests passed separately against the approved test project with writes rolled back. Scheduling, procurement and reporting have not been exercised with persisted test transactions.
- Live ERP-owned login was checked over HTTP. Cross-branch role tests have not been run.
- Payroll, POS register operations, accounting ledgers, AP/AR and reconciliation have no schema or service implementation.
- The backend uses a shared JDBC role. Runtime database grants and browser security behavior need verification.
