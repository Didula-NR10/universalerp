# Testing

Run `backend/.\mvnw.cmd clean verify` for foundation security, domain policy and module boundary tests. Run `frontend/npm.cmd run test`, `frontend/npm.cmd run build`, and `frontend/npm.cmd audit --audit-level=high` for the client. CI runs these checks.

The default automated suite does **not** boot PostgreSQL. The guarded `postgres-test` profile runs all available Flyway migrations, JPA schema validation, authentication, repository and sales/refund transaction tests only when `TEST_DB_DISPOSABLE=true` plus `TEST_DB_URL`, `TEST_DB_USERNAME` and `TEST_DB_PASSWORD` point to a disposable PostgreSQL database. Never point those variables at a production database. With that database and a local ERP test user, verify:

1. Flyway V1-V13 apply from an empty schema and JPA validation passes.
2. Bootstrap admin and log in. Check valid, expired, wrong issuer/audience tokens, inactive ERP users, missing profiles, account lockout, password change and logout revocation.
3. Create two branches and separate users. Verify branch roles cannot read or mutate the other branch, including schedules and reports.
4. Run concurrent stock deductions and reject overselling. Repeat an idempotency key and confirm one movement.
5. Create, approve and receive a purchase order, sell the stock, and refund. Compare balances and immutable movements.
6. Exercise expense approval, leave and shift decisions, audit records, low stock notices and CSV exports.
7. Test database grants, backup/restore, secret rotation and HTTPS browser behavior.

The approved Supabase PostgreSQL test database has V1-V13 applied and the normal backend starts against it; `/actuator/health` returned 200 and an unauthenticated `/api/v1/me` returned 401. All three guarded PostgreSQL tests passed separately against this test project with their writes rolled back. The auth test verifies password change, token revocation and account lockout. The sales/refund test verifies cash sale idempotency, two partial refund approvals, sale status and stock restoration. The default suite skips the three PostgreSQL tests. Isolated JWT decoder tests cover valid, expired, wrong-audience, wrong-issuer, revoked-session and invalid-signature tokens. A live ERP-owned HTTP login succeeded with first-login business access blocked until password change.
