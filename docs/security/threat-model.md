# Security review

| Risk | Current control and outstanding validation |
| --- | --- |
| Exposed secrets | Example files contain no credentials; `.env` files are ignored. Review deployment secret handling and add CI secret scanning. |
| Unauthenticated access | JWT verification and deny-by-default foundation profile. Test against real Supabase tokens. |
| Cross-branch access | Service-level role and branch checks. Add negative integration tests across branches. |
| Shared JDBC and RLS | Explicit application checks; do not assume Supabase RLS. Apply least-privilege runtime grants. |
| Audit tampering | Append-oriented API; revoke UPDATE/DELETE from runtime DB role and validate grants. |
| Duplicate stock/financial operations | Stock operation idempotency and transactional flows. Test concurrency and retry behavior on PostgreSQL. |

This review is not a production security signoff.
