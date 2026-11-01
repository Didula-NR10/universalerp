# Database schema

Flyway migrations under `backend/src/main/resources/db/migration` define the PostgreSQL schema.

| Migration | Content |
| --- | --- |
| V1 | Company, branches, departments, warehouses, user profiles, roles, assignments and audit |
| V2 | Product catalog, stock balances, stock movements and base permissions |
| V3 | Stock operation idempotency |
| V4 | Customers, suppliers, expense categories and expenses |
| V5 | Purchase orders and lines |
| V6 | Sales, lines, cash payments and full refunds |
| V7 | Employees, attendance and leave |
| V8 | Notifications |
| V9 | Department/warehouse versions and organization permissions |
| V10 | Partial refund requests, decisions, line amounts and restocking rules |
| V11 | Shift templates, employee availability and work shifts |
| V12 | Widen sale and payment status columns for `PARTIALLY_REFUNDED` |
| V13 | ERP-owned password hashes, account lockout and session revocation |

V1-V13 were applied to the user-approved Supabase ERP test project on 2026-11-01. Flyway reported schema version 13, and the PostgreSQL authentication, repository and sales/refund integration tests passed with rolled back test data.

The API checks permissions and branch scope because its shared JDBC connection does not inherit browser user RLS. The `user_profile.auth_user_id` column name is retained for existing data, but the UUID now identifies an ERP-managed account. Application and migration database credentials can be separated through `MIGRATION_DB_*` variables. Restrict runtime UPDATE/DELETE on audit and stock movement history.
