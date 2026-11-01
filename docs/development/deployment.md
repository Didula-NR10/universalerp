# Deployment prerequisites

The target is Supabase PostgreSQL, a hosted Spring Boot API and a hosted React frontend. Authentication is owned by the ERP API; do not configure Supabase Auth or a frontend publishable key. `backend/Dockerfile` and `frontend/Dockerfile` are deployment templates, not yet built locally because Docker is unavailable. Use `GET /actuator/health` for the API health check.

Configure HTTPS, `CORS_ALLOWED_ORIGIN`, `AUTH_JWT_SECRET` and database credentials through a managed secret store. Keep the signing secret stable across API instances; rotating it invalidates current sessions. The API uses HikariCP with `DB_POOL_MAX_SIZE` defaulting to 5. Set `MIGRATION_DB_URL`, `MIGRATION_DB_USERNAME` and `MIGRATION_DB_PASSWORD` for a separate migration account; otherwise Flyway uses the runtime database connection. Restrict runtime UPDATE/DELETE privileges on audit and stock movement history.

Before release, complete the [database-backed tests](testing.md), verify backup and restore, add gateway login rate limits, review XSS/CSRF and security headers, and conduct an external security review. The `foundation` profile is status-only. No production deployment has been performed.
